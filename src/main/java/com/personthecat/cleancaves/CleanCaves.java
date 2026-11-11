package com.personthecat.cleancaves;

import com.personthecat.cleancaves.commands.CommandCave;
import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.io.JarFiles;
import com.personthecat.cleancaves.noise.CachedNoiseHelper;
import com.personthecat.cleancaves.proxy.IProxy;
import com.personthecat.cleancaves.world.GeneratorController;
import com.personthecat.cleancaves.world.event.DisablePopulateChunkEvent;
import com.personthecat.cleancaves.world.event.DisableVanillaStoneGen;
import com.personthecat.cleancaves.world.event.ReplaceVanillaCaveGen;
import com.personthecat.cleancaves.world.feature.FeatureCaveHook;
import com.personthecat.cleancaves.world.feature.StructureSpawner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION,
        dependencies = "after:worleycaves;", acceptableRemoteVersions = "*")
public class CleanCaves {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    /** The main instance of this mod, as required by Forge. */
    @Instance public static CleanCaves instance;

    @SidedProxy(modId = Reference.MOD_ID, clientSide = "com.personthecat.cleancaves.proxy.ClientProxy",
            serverSide = "com.personthecat.cleancaves.proxy.CommonProxy") public static IProxy proxy;

    /** Preset name -> the generators compiled from it. Filled on first worldgen call. */
    public final Map<String, GeneratorController> generators = new TreeMap<>();

    /** Preset name -> its parsed settings. Filled when presets are (re)loaded. */
    public final Map<String, CavePreset> presets = new TreeMap<>();

    /** Structure name -> its NBT template. Filled when structures are loaded. */
    public final Map<String, Template> structures = new HashMap<>();

    @EventHandler
    public void init(FMLInitializationEvent event) {
        JarFiles.copyFiles();
        StructureSpawner.loadAllStructures(this.structures);
        CaveInit.initPresets(this.presets);
        MinecraftForge.TERRAIN_GEN_BUS.register(ReplaceVanillaCaveGen.class);
        MinecraftForge.ORE_GEN_BUS.register(DisableVanillaStoneGen.class);
        MinecraftForge.TERRAIN_GEN_BUS.register(DisablePopulateChunkEvent.class);
        GameRegistry.registerWorldGenerator(new FeatureCaveHook(), 0);
        LOGGER.info("{} init phase complete.", Reference.MOD_NAME);
    }

    @EventHandler
    public void onServerStartingEvent(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCave());
        LOGGER.info("{} commands registered.", Reference.MOD_NAME);
    }

    @EventHandler
    public void onServerStoppingEvent(FMLServerStoppingEvent event) {
        LOGGER.info("Unloading generators.");
        this.generators.clear();
        CachedNoiseHelper.removeAll();
    }

    /**
     * Compiles every enabled preset against this world, once. The result is cached in
     * {@link #generators} and reused for the rest of the session.
     */
    public Map<String, GeneratorController> loadGenerators(World world) {
        if (this.presets.isEmpty()) {
            return this.generators; // i.e. never load them.
        }
        if (this.generators.isEmpty()) {
            this.presets.forEach((name, preset) -> {
                if (preset.enabled()) {
                    world.rand.setSeed(world.getSeed());
                    this.generators.put(name, GeneratorController.from(preset, world));
                }
            });
        }
        return this.generators;
    }

}
