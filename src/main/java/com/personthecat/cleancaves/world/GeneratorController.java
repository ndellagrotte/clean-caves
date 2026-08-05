package com.personthecat.cleancaves.world;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.data.BurrowSettings;
import com.personthecat.cleancaves.data.CavernSettings;
import com.personthecat.cleancaves.data.ClusterSettings;
import com.personthecat.cleancaves.world.feature.PillarGenerator;
import com.personthecat.cleancaves.world.feature.StalactiteGenerator;
import com.personthecat.cleancaves.world.feature.StructureGenerator;
import com.personthecat.cleancaves.world.feature.WorldContext;
import com.personthecat.cleancaves.world.generator.BurrowGenerator;
import com.personthecat.cleancaves.world.generator.CavernGenerator;
import com.personthecat.cleancaves.world.generator.ClusterGenerator;
import com.personthecat.cleancaves.world.generator.LayerGenerator;
import com.personthecat.cleancaves.world.generator.PrimerContext;
import com.personthecat.cleancaves.world.generator.RavineGenerator;
import com.personthecat.cleancaves.world.generator.TunnelConnector;
import com.personthecat.cleancaves.world.generator.TunnelGenerator;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Every generator a single preset contributes, already compiled against one world, grouped by the
 * phase of chunk generation it runs in.
 */
public record GeneratorController(
    List<TunnelGenerator> tunnels,
    List<RavineGenerator> ravines,
    List<CavernGenerator> caverns,
    List<CavernGenerator> deferredCaverns,
    List<BurrowGenerator> burrows,
    List<LayerGenerator> layers,
    List<TunnelConnector<CavernGenerator>> cavernTunnels,
    List<TunnelConnector<BurrowGenerator>> burrowTunnels,
    ClusterGenerator globalClusters,
    ClusterGenerator layeredClusters,
    List<StalactiteGenerator> stalactites,
    List<PillarGenerator> pillars,
    List<StructureGenerator> structures
) {
    public static GeneratorController from(CavePreset preset, World world) {
        // Clusters with no matchers apply everywhere; the rest only within a matching layer.
        final List<ClusterSettings> global = new ArrayList<>();
        final List<ClusterSettings> layered = new ArrayList<>();
        preset.clusters().forEach(c -> (c.matchers().isEmpty() ? global : layered).add(c));

        final Caverns caverns = mapCaverns(preset.caverns(), world);
        final Burrows burrows = mapBurrows(preset.burrows(), world);

        return new GeneratorController(
            preset.tunnels().stream().map(t -> new TunnelGenerator(t, world)).toList(),
            preset.ravines().stream().map(r -> new RavineGenerator(r, world)).toList(),
            caverns.generators,
            caverns.deferred,
            burrows.generators,
            preset.layers().stream().map(l -> new LayerGenerator(l, world)).toList(),
            caverns.connectors,
            burrows.connectors,
            new ClusterGenerator(global, world),
            new ClusterGenerator(layered, world),
            preset.stalactites().stream().map(s -> new StalactiteGenerator(s, world)).toList(),
            preset.pillars().stream().map(p -> new PillarGenerator(p, world)).toList(),
            preset.structures().stream().map(s -> new StructureGenerator(s, world)).toList());
    }

    private record Caverns(
        List<CavernGenerator> generators,
        List<CavernGenerator> deferred,
        List<TunnelConnector<CavernGenerator>> connectors
    ) {}

    private static Caverns mapCaverns(List<CavernSettings> caverns, World world) {
        final List<CavernGenerator> generators = new ArrayList<>();
        final List<CavernGenerator> deferred = new ArrayList<>();
        final List<TunnelConnector<CavernGenerator>> connectors = new ArrayList<>();
        for (final CavernSettings cavern : caverns) {
            final CavernGenerator generator = new CavernGenerator(cavern, world);
            (cavern.deferred() ? deferred : generators).add(generator);
            cavern.branches().ifPresent(b -> connectors.add(new TunnelConnector<>(b, generator, world)));
        }
        return new Caverns(generators, deferred, connectors);
    }

    private record Burrows(
        List<BurrowGenerator> generators,
        List<TunnelConnector<BurrowGenerator>> connectors
    ) {}

    private static Burrows mapBurrows(List<BurrowSettings> burrows, World world) {
        final List<BurrowGenerator> generators = new ArrayList<>();
        final List<TunnelConnector<BurrowGenerator>> connectors = new ArrayList<>();
        for (final BurrowSettings burrow : burrows) {
            final BurrowGenerator generator = new BurrowGenerator(burrow, world);
            generators.add(generator);
            burrow.branches().ifPresent(b -> connectors.add(new TunnelConnector<>(b, generator, world)));
        }
        return new Burrows(generators, connectors);
    }

    /** Generate noise-based features in the world before anything else. */
    public void earlyGenerate(PrimerContext ctx) {
        this.globalClusters.generate(ctx);
        this.layers.forEach(l -> l.generate(ctx));
        this.layeredClusters.generate(ctx);
        this.caverns.forEach(c -> c.generate(ctx));
        this.burrows.forEach(t -> t.generate(ctx));
    }

    /** Generate all of the early, MapGenBase-style features that make up the bulk of the caves. */
    public void mapGenerate(PrimerContext ctx) {
        this.tunnels.forEach(t -> t.generate(ctx));
        this.ravines.forEach(r -> r.generate(ctx));
        this.cavernTunnels.forEach(t -> t.generate(ctx));
        this.burrowTunnels.forEach(t -> t.generate(ctx));
        this.deferredCaverns.forEach(c -> c.generate(ctx)); // tmp
    }

    /** Spawn all of the superficial decorations that take place later in the chunk generation cycle. */
    public void featureGenerate(WorldContext ctx) {
        this.pillars.forEach(p -> p.generate(ctx));
        this.stalactites.forEach(s -> s.generate(ctx));
        this.structures.forEach(s -> s.generate(ctx));
    }
}
