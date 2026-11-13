package com.personthecat.cleancaves.world.generator;

import com.personthecat.cleancaves.data.ClusterSettings;
import com.personthecat.cleancaves.model.Conditions;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.BlockMask;
import com.personthecat.cleancaves.util.XoRoShiRo;
import com.personthecat.cleancaves.world.RandomChunkSelector;
import com.personthecat.cleancaves.world.parallel.GenerationPool;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ClusterGenerator extends ListGenerator<ClusterSettings> {

    /** The number of columns in a chunk. */
    private static final int COLUMNS = 16 * 16;

    /** A rough measure of how much work one column is, for the workload detector. */
    private static final long UNITS_PER_COLUMN = 96L;

    /**
     * Clusters grouped by the conditions they spawn under. {@link Conditions} compares by identity,
     * and the map keeps insertion order so that overlapping clusters are resolved the same way in
     * every session rather than by wherever the keys happen to hash.
     */
    private final Map<Conditions, List<ClusterInfo>> clusterMap = new LinkedHashMap<>();
    private final RandomChunkSelector selector;

    /** Prepared columns, one entry per group of clusters, reused between chunks. */
    private final Map<Conditions, ConditionScope> scopes = new IdentityHashMap<>();
    private final List<ConditionScope> ordered = new ArrayList<>();

    public ClusterGenerator(List<ClusterSettings> cfg, World world) {
        super(cfg, c -> c.conditions(), world);
        this.selector = new RandomChunkSelector(world.getSeed());
    }

    private static void spawnCluster(List<ClusterInfo> clusters, ChunkPrimer primer, Random rand, int x, int y, int z,
            int actualX, int actualZ) {
        final IBlockState state = primer.getBlockState(x, y, z);
        for (ClusterInfo info : clusters) {
            if (info.cluster.canSpawn(state)) {
                final BlockPos origin = info.center;
                final double distX = actualX - origin.getX();
                final double distY = y - origin.getY();
                final double distZ = actualZ - origin.getZ();
                final double distX2 = distX * distX;
                final double distY2 = distY * distY;
                final double distZ2 = distZ * distZ;

                // Ensure that we're within the sphere. Note: wall blocks could be && sum >= 0.9
                if (distX2 / info.radX2 + distY2 / info.radY2 + distZ2 / info.radZ2 <= 1) {
                    final double chance = info.cluster.integrity();
                    if (chance == 1.0 || rand.nextFloat() <= chance) {
                        primer.setBlockState(x, y, z, info.state);
                        return; // Already placed. Don't continue.
                    }
                }
            }
        }
    }

    @Override
    public void generate(PrimerContext ctx) {
        if (!features.isEmpty()) {
            generateChecked(ctx);
        }
    }

    @Override
    protected void generateChecked(PrimerContext ctx) {
        // Always reset the seed for clusters.
        ctx.world.rand.setSeed(ctx.world.getSeed());
        this.clusterMap.clear();
        this.locateFinalClusters(ctx.world, ctx.world.rand, ctx.chunkX, ctx.chunkZ);
        this.gatherScopes();
        if (this.ordered.isEmpty()) {
            return;
        }
        GenerationPool.forEach(GenerationPool.CLUSTER_COLUMNS, COLUMNS, UNITS_PER_COLUMN * this.ordered.size(),
                i -> this.prepareColumn(ctx, i));
        this.generateClusters(ctx.primer, ctx.localRand, ctx.chunkX, ctx.chunkZ);
    }

    /** Pairs each group of placed clusters with its reusable column buffers. */
    private void gatherScopes() {
        this.ordered.clear();
        for (Map.Entry<Conditions, List<ClusterInfo>> entry : this.clusterMap.entrySet()) {
            final ConditionScope scope = this.scopes.computeIfAbsent(entry.getKey(), ConditionScope::new);
            scope.clusters = entry.getValue();
            scope.reset();
            this.ordered.add(scope);
        }
    }

    /** Computes the noise and height limits of one column for every group of clusters. */
    private void prepareColumn(PrimerContext ctx, int i) {
        final int x = i >> 4;
        final int z = i & 15;
        final int actualX = x + (ctx.chunkX * 16);
        final int actualZ = z + (ctx.chunkZ * 16);
        for (ConditionScope scope : this.ordered) {
            final Range column = scope.conditions.getColumn(actualX, actualZ);
            scope.columns[i] = column;
            final BlockMask mask = scope.masks[i];
            for (int y : column) {
                if (scope.conditions.noise.GetBoolean(actualX, y, actualZ)) {
                    mask.set(y);
                }
            }
        }
    }

    private void locateFinalClusters(World world, Random rand, int chunkX, int chunkZ) {
        forEachFeature((cfg, conditions) -> {
            final int cRadiusX = (cfg.radiusX().max() / 16) + 1;
            final int cRadiusZ = (cfg.radiusZ().max() / 16) + 1;
            final double threshold = cfg.selectionThreshold();
            final int clusterSeed = rand.nextInt();

            // Locate any possible origins for this cluster based on its radii.
            for (int cx = chunkX - cRadiusX; cx <= chunkX + cRadiusX; cx++) {
                for (int cz = chunkZ - cRadiusZ; cz <= chunkZ + cRadiusZ; cz++) {

                    // Get absolute coordinates, generate in the center.
                    final int x = (cx * 16) + 8;
                    final int z = (cz * 16) + 8;
                    final Biome b = world.getBiomeProvider().getBiome(new BlockPos(x, 0, z));
                    if (conditions.biomes.test(b)) {
                        for (Pair<IBlockState, Integer> pair : cfg.states()) {
                            final IBlockState state = pair.getLeft();
                            final int id = pair.getRight();

                            if (selector.getBooleanForCoordinates(id, cx, cz, threshold)) {
                                // Get an RNG unique to this chunk.
                                final Random localRand = new XoRoShiRo(PrimerContext.chunkSeed(clusterSeed, cx, cz));
                                final int y = cfg.centerHeight().rand(localRand);
                                // Finalize all values.
                                final BlockPos origin = new BlockPos(x, y, z);
                                final int radX = cfg.radiusX().rand(localRand) - (cfg.radiusX().diff() / 2);
                                final int radY = cfg.radiusY().rand(localRand) - (cfg.radiusY().diff() / 2);
                                final int radZ = cfg.radiusZ().rand(localRand) - (cfg.radiusZ().diff() / 2);
                                // Add the new information to be returned.
                                this.clusterMap
                                        .computeIfAbsent(conditions, c -> new ArrayList<>())
                                        .add(new ClusterInfo(cfg, state, id, origin, radX, radY, radZ));
                            }
                        }
                    }
                }
            }
        });
    }

    private void generateClusters(ChunkPrimer primer, Random rand, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (chunkZ * 16);
                this.spawnColumn(primer, rand, x, z, actualX, actualZ);
            }
        }
    }

    private void spawnColumn(ChunkPrimer primer, Random rand, int x, int z, int actualX, int actualZ) {
        final int i = x << 4 | z;
        for (ConditionScope scope : this.ordered) {
            final BlockMask mask = scope.masks[i];
            if (mask.isEmpty()) {
                continue;
            }
            for (int y : scope.columns[i]) {
                if (mask.get(y)) {
                    spawnCluster(scope.clusters, primer, rand, x, y, z, actualX, actualZ);
                }
            }
        }
    }

    /** One group of clusters and the columns already computed for it. */
    private static final class ConditionScope {

        final Conditions conditions;
        final Range[] columns = new Range[COLUMNS];
        final BlockMask[] masks = new BlockMask[COLUMNS];

        List<ClusterInfo> clusters = List.of();

        ConditionScope(Conditions conditions) {
            this.conditions = conditions;
            for (int i = 0; i < COLUMNS; i++) {
                this.masks[i] = new BlockMask();
            }
        }

        void reset() {
            for (BlockMask mask : this.masks) {
                mask.clear();
            }
        }

    }

    /** Generated info related to how the current cluster will be spawned in the world. */
    private static class ClusterInfo {

        /** A reference to the original cluster to be spawned. */
        final ClusterSettings cluster;
        final IBlockState state;
        final int id;

        /** The generated center coordinates of this cluster. */
        final BlockPos center;

        /** Storing the original vertical radius to avoid unnecessary calculations. */
        final int radY;

        /** Squared radii. */
        final int radX2;
        final int radY2;
        final int radZ2;

        ClusterInfo(ClusterSettings cluster, IBlockState state, int id, BlockPos center, int radX, int radY, int radZ) {
            this.cluster = cluster;
            this.state = state;
            this.id = id;
            this.center = center;
            this.radY = radY;
            this.radY2 = radY * radY;
            this.radX2 = radX * radX;
            this.radZ2 = radZ * radZ;
        }

    }

}
