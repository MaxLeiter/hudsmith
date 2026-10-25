#!/usr/bin/env bash
# Runs a Gradle task on a virtual X display, for machines without a screen (cloud sessions, CI).
#
#   tools/headless.sh :neoforge:26.3:runClient -Pautopilot   # screenshots land in neoforge/versions/26.3/runs/client/screenshots/
#   tools/headless.sh snap [out.png]                            # grab what's on the virtual display right now
#
# Rendering is Mesa's software Vulkan (lavapipe): slow but complete. Xvfb's GLX has no sRGB-capable visual, which
# Minecraft's OpenGL backend asks for, so the client options are set to Vulkan (and NeoForge's early loading window,
# which is OpenGL, is off). They also skip the first-launch accessibility screen.
#
# Needs JDK 25, xvfb, mesa-vulkan-drivers and ffmpeg (for snap). Adapted from ingame-mod-maker's tools/headless.sh.
set -euo pipefail
cd "$(dirname "$0")/.."

DISPLAY_NUM=${HEADLESS_DISPLAY:-:77}
SIZE=${HEADLESS_SIZE:-1920x1080}

if [[ "${1:-}" == snap ]]; then
    out=${2:-headless-snap.png}
    ffmpeg -loglevel error -y -f x11grab -video_size "$SIZE" -i "$DISPLAY_NUM" -frames:v 1 "$out"
    echo "$out"
    exit
fi

# The build needs 25; a preinstalled JAVA_HOME is often an older JDK
if [[ -d /usr/lib/jvm/java-25-openjdk-amd64 ]] && ! "${JAVA_HOME:-/nonexistent}/bin/java" -version 2>&1 | grep -q '"2[5-9]'; then
    export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
fi

# Sets a key in a file of "key<separator>value" lines, creating it if need be: options.txt uses ":", fml.toml " = "
set_key() {
    local file=$1 separator=$2 key=$3 value=$4
    mkdir -p "$(dirname "$file")"
    touch "$file"
    if grep -q "^$key$separator" "$file"; then
        sed -i "s/^$key$separator.*/$key$separator$value/" "$file"
    else
        echo "$key$separator$value" >> "$file"
    fi
}
set_option() { set_key "$1" ":" "$2" "$3"; }
set_toml() { set_key "$1" " = " "$2" "$3"; }
# Every loader and Minecraft version's client run directory. The versions come from settings.gradle: the directories
# only exist once Gradle has configured them, which may not have happened yet.
versions=$(grep '^def minecraftVersions' settings.gradle | grep -o "'[^']*'" | tr -d "'")
for version in $versions; do
    for loader in neoforge fabric; do
        dir=$loader/versions/$version/runs/client
        set_option "$dir/options.txt" onboardAccessibility false
        set_option "$dir/options.txt" preferredGraphicsBackend '"vulkan"'
        set_option "$dir/options.txt" pauseOnLostFocus false
    done
    set_toml "neoforge/versions/$version/runs/client/config/fml.toml" earlyWindowControl false
done

# Our own Xvfb on a known display, so `snap` can look at it mid-run
if ! xdpyinfo -display "$DISPLAY_NUM" >/dev/null 2>&1; then
    Xvfb "$DISPLAY_NUM" -screen 0 "${SIZE}x24" -nolisten tcp >/dev/null 2>&1 &
    xvfb_pid=$!
    trap 'kill $xvfb_pid 2>/dev/null' EXIT
    for _ in $(seq 50); do xdpyinfo -display "$DISPLAY_NUM" >/dev/null 2>&1 && break; sleep 0.1; done
    xdpyinfo -display "$DISPLAY_NUM" >/dev/null 2>&1 || { echo "Xvfb didn't start on $DISPLAY_NUM (a stale /tmp/.X${DISPLAY_NUM#:}-lock?)" >&2; exit 1; }
fi
export DISPLAY=$DISPLAY_NUM
export XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR:-/tmp}
# No sound card: OpenAL's null device, and no ALSA noise
export ALSOFT_DRIVERS=null

./gradlew --console=plain "$@"
