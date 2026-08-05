package com.personthecat.cleancaves.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

import java.util.Optional;

import static com.personthecat.cleancaves.util.CommonMethods.runEx;
import static com.personthecat.cleancaves.util.CommonMethods.runExF;

/**
 * The client side. Reading a preset in chat is much easier with a taller, wider chat window, so
 * {@code /cave ch} and {@code /cave cw} adjust it without leaving the game.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public Optional<String> chatHeight(String[] args) {
        final Minecraft mc = Minecraft.getMinecraft();
        final GameSettings cfg = mc.gameSettings;
        if (args.length == 0) {
            return Optional.of("Current chat height: " + cfg.chatHeightFocused);
        }
        final float possible = ((mc.displayHeight / (float) cfg.guiScale) - 20.0F) / 180.0F;
        final float scale = "max".equals(args[0]) ? possible : parseChatHeight(mc, cfg, possible, args[0]);
        cfg.chatHeightFocused = scale;
        cfg.saveOptions();
        return Optional.of("Updated chat height: " + scale);
    }

    @Override
    public Optional<String> chatWidth(String[] args) {
        final Minecraft mc = Minecraft.getMinecraft();
        final GameSettings cfg = mc.gameSettings;
        if (args.length == 0) {
            return Optional.of("Current chat width: " + cfg.chatWidth);
        }
        final float possible = mc.displayWidth / (float) cfg.guiScale / 320.0F;
        final float scale = "max".equals(args[0]) ? possible : parseChatWidth(mc, cfg, possible, args[0]);
        cfg.chatWidth = scale;
        cfg.saveOptions();
        return Optional.of("Updated chat width: " + scale);
    }

    private static float parseChatHeight(Minecraft mc, GameSettings cfg, float possible, String arg) {
        final float scale = parseScale(arg);
        if (scale < 0.25 || scale > 5.0) {
            throw runEx("<scale> should be a range of 0.25 ~ 5.0");
        }
        if ((scale * 180.0F + 20.0F) * cfg.guiScale > mc.displayHeight) {
            throw runExF("Max size is {} with your window and gui scale.", possible);
        }
        return scale;
    }

    private static float parseChatWidth(Minecraft mc, GameSettings cfg, float possible, String arg) {
        final float scale = parseScale(arg);
        if (scale < 0.25 || scale > 3.0) {
            throw runEx("<scale> should be a range of 0.25 ~ 3.0");
        }
        if (scale * 320.0F * cfg.guiScale > mc.displayWidth) {
            throw runExF("Max size is {} with your window and gui scale", possible);
        }
        return scale;
    }

    private static float parseScale(String arg) {
        try {
            return Float.parseFloat(arg);
        } catch (final NumberFormatException ignored) {
            throw runEx("Not a number: " + arg);
        }
    }
}
