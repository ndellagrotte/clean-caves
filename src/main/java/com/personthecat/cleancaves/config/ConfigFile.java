package com.personthecat.cleancaves.config;

import com.personthecat.cleancaves.Reference;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RequiresMcRestart;
import net.minecraftforge.common.config.Config.RequiresWorldRestart;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Mod-wide settings, as opposed to the per-preset settings under {@code config/cleancaves}. Forge
 * discovers and syncs these by reflecting over the annotated fields; no {@code Configuration}
 * object is constructed anywhere.
 */
@Mod.EventBusSubscriber
@Config(modid = Reference.MOD_ID)
public final class ConfigFile {

    @Comment({
                    "Whether vanilla stone clusters--including andesite,",
                    "diorite, and granite--should spawn in the world."
            }) @RequiresMcRestart public static boolean enableVanillaStoneClusters = false;

    @Comment("Whether to enable vanilla water lakes underground.") @RequiresMcRestart
    public static boolean enableWaterLakes = false;

    @Comment("Whether to enable vanilla lava lakes underground.") @RequiresMcRestart
    public static boolean enableLavaLakes = false;

    @Comment("Whether to enable vanilla mineshafts underground.") @RequiresMcRestart
    public static boolean enableMineshafts = true;

    @Comment({
                    "Whether this mod will attempt to run simultaneously",
                    "with one other cave generation mod, such as Worley's", "Caves or Yung's Better Caves."
            }) @RequiresMcRestart public static boolean otherGeneratorEnabled = false;

    @Comment({
                    "When this field is set to true, PresetTester reports the",
                    "problems it finds in a preset as warnings and errors instead",
                    "of burying them at the info and debug levels. It never stops",
                    "the game. Users who are more serious about creating cleaner",
                    "and more efficient presets should consider enabling this field",
                    "to make sure that nothing slips by."
            }) public static boolean strictPresets = false;

    @Comment({
                    "When this field is set to true, PresetReader will skip over",
                    "any preset that cannot be parsed or read and simply not load",
                    "it. Make sure to check your log to determine if any preset",
                    "erred. Errors in the imports folder still stop the game,",
                    "because every preset may depend on them."
            }) public static boolean ignoreInvalidPresets = false;

    @Comment({
                    "A list of dimensions where HeightMapLocator will check for the",
                    "surface to avoid spawning caverns in water. Disable this in your",
                    "dimension if you don't have regular oceans spawning."
            }) @RequiresWorldRestart public static int[] heightMapDims = { 0 };

    @Comment("The chunk search range for tunnel and ravine features.") public static int mapRange = 8;

    @Comment({ "The range in chunks to read biomes for features that use", "distance-based biome testing." })
    public static int biomeRange = 2;

    @Comment("Whether to override and replace caverns in the nether.") @RequiresMcRestart
    public static boolean netherGenerate = false;

    @Comment({
                    "Whether to automatically generate preset files inside of",
                    "cleancaves/generated. This will help you see how your",
                    "variables are getting expanded every time you reload your", "presets."
            }) public static boolean autoGenerate = true;

    @Comment({
                    "Whether the pure parts of cave generation--the noise that",
                    "decides where caves go--may be computed on helper threads.",
                    "Caves are always written to the chunk on the server thread,",
                    "in the same order, so the generated terrain is unchanged.",
                    "Disable this to keep everything on the server thread."
            }) public static boolean parallelGeneration = true;

    @Comment({
                    "Helper threads used for parallel cave generation.",
                    "0 = automatic: CPU threads minus a reserve for the server,",
                    "the client renderer and Pulsar, capped at 8 (the server",
                    "thread always works too). A positive value overrides the", "automatic choice, up to 64."
            }) @RequiresMcRestart public static int maxGeneratorThreads = 0;

    private ConfigFile() { }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Reference.MOD_ID)) {
            ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
        }
    }

}
