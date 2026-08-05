package com.personthecat.cleancaves.world.feature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.personthecat.cleancaves.CaveInit;
import com.personthecat.cleancaves.CleanCaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import com.personthecat.cleancaves.util.Result;
import static com.personthecat.cleancaves.util.CommonMethods.*;
import static com.personthecat.cleancaves.io.SafeFileIO.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.personthecat.cleancaves.Reference;

public class StructureSpawner {
    private static final Logger log = LogManager.getLogger(Reference.MOD_NAME);


    /** The location where structures will be kept: {@code config/cleancaves/structures}. */
    public static Path dir() {
        return CaveInit.caveDir().resolve("structures");
    }

    /** Loads all structures into the container from the disk. */
    public static void loadAllStructures(Map<String, Template> structures) {
        final Path dir = dir();
        // Verify the folder's integrity before proceeding.
        ensureDirExists(dir).expect("Error creating structure directory");
        // Clear the structures. Allows them to be reloaded.
        structures.clear();

        for (final Path file : listFiles(dir).orElse(List.of())) {
            final String name = file.getFileName().toString();
            // Limit to nbt files only.
            if (!name.endsWith(".nbt")) {
                continue;
            }
            final Template template = new Template();
            read(template, file).expectF("Error: unable to read structure file {}", name);
            warnSizeLimitations(template, name);
            structures.put(removeNbt(name), template);
        }
        // To-do: Make sure to return an error when nothing is returned.
    }

    /** Attempts to read nbt data from the input file into {@code template}. */
    private static Result<IOException> read(Template template, Path nbt) {
        try (final InputStream is = Files.newInputStream(nbt)) {
            template.read(CompressedStreamTools.readCompressed(is));
        } catch (final IOException e) {
            return Result.of(e);
        }
        return Result.ok();
    }

    /** Warns the user if their structure is too large. */
    private static void warnSizeLimitations(Template template, String name) {
        if (template.getSize().getX() > 15 || template.getSize().getZ() > 15) {
            log.warn("Large structures are not yet fully supported. Expect cascading generation lag caused by {}.", name);
        }
    }

    /**
     * Attempts to load a template from the map or TemplateManager.
     * throws a RuntimeException when no template is found.
     */
    public static Template getTemplate(String fileOrResource, World world) {
        final Map<String, Template> structures = CleanCaves.instance.structures;
        fileOrResource = removeNbt(fileOrResource);
        // Attempt to load the preset directly from the map.
        final Optional<Template> fromMap = Optional.ofNullable(structures.get(fileOrResource));
        if (fromMap.isPresent()) {
            return fromMap.get();
        }
        // Verify that we're on the server side, first.
        if (world.isRemote) {
            throw runEx("Build error: Somehow called StructureSpawner#getTemplate with an invalid World object.");
        }
        // It wasn't found. Try getting it from TemplateManager.
        WorldServer worldServer = (WorldServer) world;
        MinecraftServer mcServer = world.getMinecraftServer();
        TemplateManager manager = worldServer.getStructureTemplateManager();
        ResourceLocation location = new ResourceLocation(fileOrResource);
        // This isn't safe. Scary.
        Template template = manager.get(mcServer, location);
        // He remembered to check.
        if (template == null) {
            throw templateNotFound(fileOrResource);
        }
        return template;
    }

    /** Indicates that no template was found by the above search. */
    private static RuntimeException templateNotFound(String name) {
        return runExF("Error: No template named \"{}\" was found. Please verify that this refers to a valid file or registry name.", name);
    }

    /** Does not account for offsets. Must be calculated beforehand. */
    public static void spawnStructure(Template template, PlacementSettings settings, World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        template.addBlocksToWorld(world, pos, settings);
    }

    private static String removeNbt(String name) {
        return name.replace(".nbt", "");
    }
}
