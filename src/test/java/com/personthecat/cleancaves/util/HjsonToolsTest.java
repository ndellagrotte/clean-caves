package com.personthecat.cleancaves.util;

import com.personthecat.cleancaves.model.ScalableFloat;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The readers that turn loosely-shaped preset values into settings. */
class HjsonToolsTest {

    private static final ScalableFloat DEFAULTS = new ScalableFloat(1.0F, 2.0F, 3.0F, 4.0F, 5.0F);

    @Test
    @DisplayName("a scalable float written as a bare number keeps its fraction")
    void scalableFloatFromNumber() {
        final JsonObject json = new JsonObject().add("stretch", 1.5).add("scale", 0.5).add("pitch", 2);

        assertThat(HjsonTools.getScalableFloatOr(json, "stretch", DEFAULTS).startVal()).isEqualTo(1.5F);
        assertThat(HjsonTools.getScalableFloatOr(json, "scale", DEFAULTS).startVal()).isEqualTo(0.5F);
        assertThat(HjsonTools.getScalableFloatOr(json, "pitch", DEFAULTS).startVal()).isEqualTo(2.0F);
        assertThat(HjsonTools.getScalableFloatOr(json, "absent", DEFAULTS)).isEqualTo(DEFAULTS);
    }

    @Test
    @DisplayName("a bare number only replaces the starting value")
    void scalableFloatFromNumberKeepsDefaults() {
        final ScalableFloat result = HjsonTools.toScalableFloat(0.75F, DEFAULTS);
        assertThat(result).isEqualTo(new ScalableFloat(0.75F, 2.0F, 3.0F, 4.0F, 5.0F));
    }

    @Test
    @DisplayName("an array fills the scalable float in order and defaults the rest")
    void scalableFloatFromArray() {
        final ScalableFloat result = HjsonTools.toScalableFloat(new JsonArray().add(-2.5).add(0), DEFAULTS);
        assertThat(result).isEqualTo(new ScalableFloat(-2.5F, 0.0F, 3.0F, 4.0F, 5.0F));
    }

    @Test
    @DisplayName("the error for an unknown biome id names the id")
    void noBiomeIdMessage() {
        assertThat(HjsonTools.noBiomeID(9001).getMessage()).contains("9001");
    }

}
