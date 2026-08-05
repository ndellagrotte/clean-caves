package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import fastnoise.FastNoise.FractalType;
import fastnoise.FastNoise.Interp;
import fastnoise.FastNoise.NoiseType;
import org.hjson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The safety net for replacing Lombok's generated builders with hand-written ones.
 *
 * <p>Two properties have to hold or presets silently change meaning: a builder with nothing set
 * must produce the documented defaults, and {@code toBuilder()} must round-trip. Only the noise
 * types are covered here -- the rest default to block states, which need a live registry.
 */
class SettingsBuilderTest {

    @Test
    @DisplayName("NoiseSettings defaults match the documented values")
    void noiseSettingsDefaults() {
        final NoiseSettings s = NoiseSettings.builder().build();
        assertTrue(s.seed().isEmpty());
        assertEquals(1.0F, s.frequency());
        assertEquals(0.0F, s.threshold().min());
        assertEquals(0.0F, s.threshold().max());
        assertEquals(1.0F, s.stretch());
        assertEquals(1.0F, s.lacunarity());
        assertEquals(0.5F, s.gain());
        assertEquals(0.45F, s.jitterX());
        assertEquals(0.45F, s.jitterY());
        assertEquals(0.45F, s.jitterZ());
        assertEquals(1, s.octaves());
        assertEquals(0, s.offset());
        assertFalse(s.perturb());
        assertFalse(s.invert());
        assertFalse(s.cache());
        assertFalse(s.dummy());
        assertEquals(1.0F, s.dummyOutput());
        assertEquals(Interp.Hermite, s.interp());
        assertEquals(NoiseType.SimplexFractal, s.type());
        assertEquals(FractalType.FBM, s.fractal());
    }

    @Test
    @DisplayName("NoiseMapSettings defaults match the documented values")
    void noiseMapSettingsDefaults() {
        final NoiseMapSettings s = NoiseMapSettings.builder().build();
        assertEquals(1.0F, s.frequency());
        assertEquals(NoiseType.SimplexFractal, s.type());
        assertEquals(1, s.octaves());
        assertEquals(-1, s.range().min());
        assertEquals(1, s.range().max());
        assertTrue(s.dummyOutput().isEmpty());
    }

    @Test
    @DisplayName("NoiseRegionSettings defaults match the documented values")
    void noiseRegionSettingsDefaults() {
        final NoiseRegionSettings s = NoiseRegionSettings.builder().build();
        assertEquals(1.0F, s.frequency());
        assertEquals(1.0F, s.dummyOutput());
        assertEquals(NoiseType.SimplexFractal, s.type());
        assertFalse(s.invert());
    }

    @Test
    @DisplayName("toBuilder round-trips every component")
    void toBuilderRoundTrips() {
        final NoiseSettings original = NoiseSettings.builder()
            .frequency(0.0143F)
            .threshold(Range.of(-0.6F))
            .stretch(0.5F)
            .octaves(3)
            .offset(12)
            .invert(true)
            .cache(true)
            .type(NoiseType.Cellular)
            .build();
        assertEquals(original, original.toBuilder().build());
    }

    @Test
    @DisplayName("a preset value overrides only the field it names")
    void jsonOverridesOneFieldAtATime() {
        final NoiseSettings defaults = NoiseSettings.builder().octaves(4).frequency(0.5F).build();
        final NoiseSettings parsed =
            NoiseSettings.from(new JsonObject().add("frequency", 0.01), defaults);
        assertEquals(0.01F, parsed.frequency());
        assertEquals(4, parsed.octaves(), "inherited fields must survive");
    }

    @Test
    @DisplayName("the `jitter` shorthand sets all three axes")
    void jitterShorthandSetsAllAxes() {
        final NoiseSettings parsed =
            NoiseSettings.from(new JsonObject().add("jitter", 0.3));
        assertEquals(0.3F, parsed.jitterX());
        assertEquals(0.3F, parsed.jitterY());
        assertEquals(0.3F, parsed.jitterZ());
    }

    @Test
    @DisplayName("an explicit axis still wins over the shorthand")
    void explicitAxisOverridesShorthand() {
        final JsonObject json = new JsonObject().add("jitter", 0.3).add("jitterY", 0.9);
        final NoiseSettings parsed = NoiseSettings.from(json);
        assertEquals(0.3F, parsed.jitterX());
        assertEquals(0.9F, parsed.jitterY());
    }
}
