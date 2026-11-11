package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import fastnoise.FastNoise.FractalType;
import fastnoise.FastNoise.Interp;
import fastnoise.FastNoise.NoiseType;
import org.hjson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(s.seed().isEmpty()).isTrue();
        assertThat(s.frequency()).isEqualTo(1.0F);
        assertThat(s.threshold().min()).isEqualTo(0.0F);
        assertThat(s.threshold().max()).isEqualTo(0.0F);
        assertThat(s.stretch()).isEqualTo(1.0F);
        assertThat(s.lacunarity()).isEqualTo(1.0F);
        assertThat(s.gain()).isEqualTo(0.5F);
        assertThat(s.jitterX()).isEqualTo(0.45F);
        assertThat(s.jitterY()).isEqualTo(0.45F);
        assertThat(s.jitterZ()).isEqualTo(0.45F);
        assertThat(s.octaves()).isEqualTo(1);
        assertThat(s.offset()).isEqualTo(0);
        assertThat(s.perturb()).isFalse();
        assertThat(s.invert()).isFalse();
        assertThat(s.cache()).isFalse();
        assertThat(s.dummy()).isFalse();
        assertThat(s.dummyOutput()).isEqualTo(1.0F);
        assertThat(s.interp()).isEqualTo(Interp.Hermite);
        assertThat(s.type()).isEqualTo(NoiseType.SimplexFractal);
        assertThat(s.fractal()).isEqualTo(FractalType.FBM);
    }

    @Test
    @DisplayName("NoiseMapSettings defaults match the documented values")
    void noiseMapSettingsDefaults() {
        final NoiseMapSettings s = NoiseMapSettings.builder().build();
        assertThat(s.frequency()).isEqualTo(1.0F);
        assertThat(s.type()).isEqualTo(NoiseType.SimplexFractal);
        assertThat(s.octaves()).isEqualTo(1);
        assertThat(s.range().min()).isEqualTo(-1);
        assertThat(s.range().max()).isEqualTo(1);
        assertThat(s.dummyOutput().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("NoiseRegionSettings defaults match the documented values")
    void noiseRegionSettingsDefaults() {
        final NoiseRegionSettings s = NoiseRegionSettings.builder().build();
        assertThat(s.frequency()).isEqualTo(1.0F);
        assertThat(s.dummyOutput()).isEqualTo(1.0F);
        assertThat(s.type()).isEqualTo(NoiseType.SimplexFractal);
        assertThat(s.invert()).isFalse();
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
        assertThat(original.toBuilder().build()).isEqualTo(original);
    }

    @Test
    @DisplayName("a preset value overrides only the field it names")
    void jsonOverridesOneFieldAtATime() {
        final NoiseSettings defaults = NoiseSettings.builder().octaves(4).frequency(0.5F).build();
        final NoiseSettings parsed = NoiseSettings.from(new JsonObject().add("frequency", 0.01), defaults);
        assertThat(parsed.frequency()).isEqualTo(0.01F);
        assertThat(parsed.octaves()).as("inherited fields must survive").isEqualTo(4);
    }

    @Test
    @DisplayName("the `jitter` shorthand sets all three axes")
    void jitterShorthandSetsAllAxes() {
        final NoiseSettings parsed = NoiseSettings.from(new JsonObject().add("jitter", 0.3));
        assertThat(parsed.jitterX()).isEqualTo(0.3F);
        assertThat(parsed.jitterY()).isEqualTo(0.3F);
        assertThat(parsed.jitterZ()).isEqualTo(0.3F);
    }

    @Test
    @DisplayName("an explicit axis still wins over the shorthand")
    void explicitAxisOverridesShorthand() {
        final JsonObject json = new JsonObject().add("jitter", 0.3).add("jitterY", 0.9);
        final NoiseSettings parsed = NoiseSettings.from(json);
        assertThat(parsed.jitterX()).isEqualTo(0.3F);
        assertThat(parsed.jitterY()).isEqualTo(0.9F);
    }

}
