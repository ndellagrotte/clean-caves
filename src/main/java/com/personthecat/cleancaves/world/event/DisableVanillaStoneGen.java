package com.personthecat.cleancaves.world.event;

import com.personthecat.cleancaves.config.ConfigFile;

import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Presets usually place their own stone variants, so vanilla's are off by default. */
public final class DisableVanillaStoneGen {

    private DisableVanillaStoneGen() { }

    /** Disables generation of vanilla stone veins. */
    @SubscribeEvent
    public static void onDisableVanillaStoneGen(GenerateMinable event) {
        // Ensure that this feature is indicated in the config file.
        if (!ConfigFile.enableVanillaStoneClusters) {
            switch (event.getType()) {
                case ANDESITE, DIORITE, GRANITE -> event.setResult(Result.DENY);
                default -> { }
            }
        }
    }

}
