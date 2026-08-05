package com.personthecat.cleancaves.world.event;

import com.personthecat.cleancaves.config.ConfigFile;
import com.personthecat.cleancaves.world.generator.EarlyCaveHook;
import com.personthecat.cleancaves.world.generator.NoGeneration;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Swaps this mod's carvers in for vanilla's. Runs at the lowest priority so that any other cave
 * mod has already had its say -- if one has, its generator is wrapped rather than discarded.
 */
public class ReplaceVanillaCaveGen {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInitMapGen(InitMapGenEvent event) {
        final boolean isOriginalGen = event.getNewGen().equals(event.getOriginalGen());
        switch (event.getType()) {
            case CAVE -> event.setNewGen(hook(event, isOriginalGen));
            case RAVINE -> {
                if (isOriginalGen) {
                    event.setNewGen(NoGeneration.getInstance());
                }
            }
            case NETHER_CAVE -> {
                if (ConfigFile.netherGenerate) {
                    event.setNewGen(hook(event, isOriginalGen));
                }
            }
            case MINESHAFT -> {
                if (!ConfigFile.enableMineshafts && isOriginalGen) {
                    event.setNewGen(NoGeneration.getInstance());
                }
            }
            default -> { }
        }
    }

    /** Wraps whatever generator is already in place, unless it is still vanilla's. */
    private static EarlyCaveHook hook(InitMapGenEvent event, boolean isOriginalGen) {
        return new EarlyCaveHook(isOriginalGen ? null : event.getNewGen());
    }
}
