package com.maxleiter.hudsmith.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

class ValuesTest {
    @Test
    void namesMustBeSnakeCase() {
        assertThrows(IllegalArgumentException.class, () -> Values.register("Mana", "Test", "", mc -> 1));
        assertThrows(IllegalArgumentException.class, () -> Values.register("my-mana", "Test", "", mc -> 1));
        assertThrows(IllegalArgumentException.class, () -> Values.register("1mana", "Test", "", mc -> 1));
    }

    @Test
    void namesThePageAlreadyHasAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Values.register("vellum", "Test", "", mc -> 1));
    }

    @Test
    void aNameIsTakenOnce() {
        Values.register("test_taken", "Test", "", mc -> 1);
        assertThrows(IllegalArgumentException.class, () -> Values.register("test_taken", "Test", "", mc -> 2));
        assertTrue(Values.all().stream().anyMatch(v -> v.name().equals("test_taken")));
    }

    @Test
    void resultsBecomeJson() {
        assertEquals(JsonNull.INSTANCE, Values.toJson(null));
        assertEquals(new JsonPrimitive("a"), Values.toJson("a"));
        assertEquals(new JsonPrimitive(3), Values.toJson(3));
        assertEquals(new JsonPrimitive(true), Values.toJson(true));
        assertEquals(new JsonArray(), Values.toJson(new JsonArray()));
        assertThrows(IllegalStateException.class, () -> Values.toJson(new Object()));
    }
}
