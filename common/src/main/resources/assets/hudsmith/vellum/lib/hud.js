// Hudsmith's helpers for HUD pages. Include it before your own script:
//   <script src="hudsmith:vellum/lib/hud.js"></script>
// Values are raw numbers and objects; these turn them into what a HUD shows. All of them take their inputs as
// arguments, so they work in templates ({{ fmt(x) }}) and in scripts alike.

// A number with a fixed number of decimals: fmt(12.3456) is "12.3". Null and undefined show as nothing.
function fmt(n, places = 1) {
  return n == null ? '' : Number(n).toFixed(places);
}

// The time of day as a 24-hour clock: clock(0) is "06:00", sunrise. time is ticks since sunrise, 0 to 23999.
function clock(time) {
  const ofDay = (time + 6000) % 24000;
  const pad = n => String(n).padStart(2, '0');
  return pad(Math.floor(ofDay / 1000)) + ':' + pad(Math.floor(ofDay % 1000 * 60 / 1000));
}

// Whether it's night, from 19:00 to 05:00.
function isNight(time) {
  const hour = Math.floor((time + 6000) % 24000 / 1000);
  return hour >= 19 || hour < 5;
}

// The chunk a coordinate is in, and where in it (0 to 15).
function chunk(v) {
  return Math.floor(v) >> 4;
}

function inChunk(v) {
  return Math.floor(v) & 15;
}

// The region file a position is in, like r.0.-1.
function region(x, z) {
  return 'r.' + (Math.floor(x) >> 9) + '.' + (Math.floor(z) >> 9);
}

// Where you'd be in the other dimension through a portal: {name, x, z} in the Overworld or the Nether, else null.
function otherWorld(dimension, x, z) {
  if (dimension === 'minecraft:overworld') return { name: 'Nether', x: x / 8, z: z / 8 };
  if (dimension === 'minecraft:the_nether') return { name: 'Overworld', x: x * 8, z: z * 8 };
  return null;
}

// The axis you face along: axis('north') is "-Z".
function axis(facing) {
  return { north: '-Z', south: '+Z', east: '+X', west: '-X' }[facing] ?? '';
}

// A compass heading from Minecraft's yaw: 0 north, 90 east, 180 south, 270 west.
function heading(yaw) {
  return ((yaw ?? 0) + 180 + 360) % 360;
}

// What's left of an item's durability, 0 to 1, or null for items that don't wear.
function wear(stack) {
  return stack && stack.max_durability ? stack.durability / stack.max_durability : null;
}

// Green while an item is fine, yellow past half, red when it's about to break.
function wearColor(stack) {
  const left = wear(stack);
  return left == null ? '#ffffff' : left > 0.5 ? '#55ff55' : left > 0.2 ? '#ffff55' : '#ff5555';
}

// A size in MB, shorter once it's big: size(812) is "812 MB", size(16384) is "16.0 GB".
function size(mb) {
  return mb < 1024 ? mb + ' MB' : (mb / 1024).toFixed(1) + ' GB';
}

// A percentage, rounded: percent(3, 4) is 75.
function percent(part, whole) {
  return whole ? Math.round(part / whole * 100) : 0;
}

// Effect levels as the game writes them: roman(2) is "II".
function roman(n) {
  return ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'][n] ?? String(n);
}

// Whether hostile mobs can spawn where you stand: no block light, and little enough light overall.
function mobsCanSpawn(blockLight, light) {
  return blockLight === 0 && light <= 7;
}
