package com.personthecat.cleancaves.config;

import com.personthecat.cleancaves.data.BurrowSettings;
import com.personthecat.cleancaves.data.CavernSettings;
import com.personthecat.cleancaves.data.ClusterSettings;
import com.personthecat.cleancaves.data.LayerSettings;
import com.personthecat.cleancaves.data.OverrideSettings;
import com.personthecat.cleancaves.data.PillarSettings;
import com.personthecat.cleancaves.data.RavineSettings;
import com.personthecat.cleancaves.data.StalactiteSettings;
import com.personthecat.cleancaves.data.StructureSettings;
import com.personthecat.cleancaves.data.TunnelSettings;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import java.util.List;

/**
 * One fully-resolved preset: everything a single {@code .cave} file asks the generator to produce.
 *
 * @param raw the expanded source JSON, kept for diagnostics and for writing to {@code generated/}
 */
public record CavePreset(
    boolean enabled,
    OverrideSettings overrides,
    List<TunnelSettings> tunnels,
    List<RavineSettings> ravines,
    List<CavernSettings> caverns,
    List<BurrowSettings> burrows,
    List<LayerSettings> layers,
    List<ClusterSettings> clusters,
    List<StalactiteSettings> stalactites,
    List<PillarSettings> pillars,
    List<StructureSettings> structures,
    JsonObject raw
) {
    /** JSON key names. */
    public static final class Keys {
        public static final String enabled = "enabled";
        public static final String overrides = "overrides";
        public static final String tunnels = "tunnels";
        public static final String ravines = "ravines";
        public static final String caverns = "caverns";
        public static final String burrows = "burrows";
        public static final String layers = "layers";
        public static final String clusters = "clusters";
        public static final String stalactites = "stalactites";
        public static final String pillars = "pillars";
        public static final String structures = "structures";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static CavePreset from(JsonObject json) {
        final OverrideSettings overrides = OverrideSettings.from(json);
        final Builder builder = builder().overrides(overrides).raw(json);
        return new HjsonMapper(json)
            .mapBool(Keys.enabled, builder::enabled)
            .mapArray(Keys.tunnels, o -> TunnelSettings.from(o, overrides), builder::tunnels)
            .mapArray(Keys.ravines, o -> RavineSettings.from(o, overrides), builder::ravines)
            .mapArray(Keys.caverns, o -> CavernSettings.from(o, overrides), builder::caverns)
            .mapArray(Keys.burrows, o -> BurrowSettings.from(o, overrides), builder::burrows)
            .mapArray(Keys.layers, o -> LayerSettings.from(o, overrides), builder::layers)
            .mapArray(Keys.clusters, o -> ClusterSettings.from(o, overrides), builder::clusters)
            .mapArray(Keys.stalactites, o -> StalactiteSettings.from(o, overrides), builder::stalactites)
            .mapArray(Keys.pillars, o -> PillarSettings.from(o, overrides), builder::pillars)
            .mapArray(Keys.structures, o -> StructureSettings.from(o, overrides), builder::structures)
            .release(builder::build);
    }

    public static final class Builder {
        private boolean enabled = true;
        private OverrideSettings overrides = OverrideSettings.builder().build();
        private List<TunnelSettings> tunnels = List.of();
        private List<RavineSettings> ravines = List.of();
        private List<CavernSettings> caverns = List.of();
        private List<BurrowSettings> burrows = List.of();
        private List<LayerSettings> layers = List.of();
        private List<ClusterSettings> clusters = List.of();
        private List<StalactiteSettings> stalactites = List.of();
        private List<PillarSettings> pillars = List.of();
        private List<StructureSettings> structures = List.of();
        private JsonObject raw;

        private Builder() {}

        private Builder(CavePreset source) {
            this.enabled = source.enabled;
            this.overrides = source.overrides;
            this.tunnels = source.tunnels;
            this.ravines = source.ravines;
            this.caverns = source.caverns;
            this.burrows = source.burrows;
            this.layers = source.layers;
            this.clusters = source.clusters;
            this.stalactites = source.stalactites;
            this.pillars = source.pillars;
            this.structures = source.structures;
            this.raw = source.raw;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder overrides(OverrideSettings overrides) {
            this.overrides = overrides;
            return this;
        }

        public Builder tunnels(List<TunnelSettings> tunnels) {
            this.tunnels = tunnels;
            return this;
        }

        public Builder ravines(List<RavineSettings> ravines) {
            this.ravines = ravines;
            return this;
        }

        public Builder caverns(List<CavernSettings> caverns) {
            this.caverns = caverns;
            return this;
        }

        public Builder burrows(List<BurrowSettings> burrows) {
            this.burrows = burrows;
            return this;
        }

        public Builder layers(List<LayerSettings> layers) {
            this.layers = layers;
            return this;
        }

        public Builder clusters(List<ClusterSettings> clusters) {
            this.clusters = clusters;
            return this;
        }

        public Builder stalactites(List<StalactiteSettings> stalactites) {
            this.stalactites = stalactites;
            return this;
        }

        public Builder pillars(List<PillarSettings> pillars) {
            this.pillars = pillars;
            return this;
        }

        public Builder structures(List<StructureSettings> structures) {
            this.structures = structures;
            return this;
        }

        public Builder raw(JsonObject raw) {
            this.raw = raw;
            return this;
        }

        public CavePreset build() {
            return new CavePreset(this.enabled, this.overrides, this.tunnels, this.ravines,
                this.caverns, this.burrows, this.layers, this.clusters, this.stalactites,
                this.pillars, this.structures, this.raw);
        }
    }
}
