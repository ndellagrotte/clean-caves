package com.personthecat.cleancaves.world.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

import static org.assertj.core.api.Assertions.assertThat;

/** A structure should land around the position that was found for it, however it is turned. */
class StructureGeneratorTest {

    private static final BlockPos TARGET = new BlockPos(100, 30, -40);
    private static final BlockPos SIZE = new BlockPos(7, 5, 3);

    /** The lowest x and z a structure occupies once it is placed at {@code origin}. */
    private static BlockPos lowestCorner(BlockPos origin, PlacementSettings placement) {
        final BlockPos far = Template.transformedBlockPos(placement, new BlockPos(SIZE.getX() - 1, 0, SIZE.getZ() - 1));
        return origin.add(Math.min(0, far.getX()), 0, Math.min(0, far.getZ()));
    }

    @ParameterizedTest
    @EnumSource(Rotation.class)
    @DisplayName("the footprint is centered on the target for every rotation")
    void centersRotatedFootprint(Rotation rotation) {
        final PlacementSettings placement = new PlacementSettings().setRotation(rotation);
        final BlockPos turned = rotation == Rotation.NONE || rotation == Rotation.CLOCKWISE_180
                ? SIZE
                : new BlockPos(SIZE.getZ(), SIZE.getY(), SIZE.getX());

        final BlockPos origin = StructureGenerator.centerBySize(TARGET, SIZE, placement);

        assertThat(lowestCorner(origin, placement)).isEqualTo(TARGET.add(-(turned.getX() / 2), 0,
                -(turned.getZ() / 2)));
    }

    @ParameterizedTest
    @EnumSource(Mirror.class)
    @DisplayName("the footprint is centered on the target for every mirror")
    void centersMirroredFootprint(Mirror mirror) {
        final PlacementSettings placement = new PlacementSettings().setMirror(mirror);

        final BlockPos origin = StructureGenerator.centerBySize(TARGET, SIZE, placement);

        assertThat(lowestCorner(origin, placement)).isEqualTo(TARGET.add(-(SIZE.getX() / 2), 0, -(SIZE.getZ() / 2)));
    }

}
