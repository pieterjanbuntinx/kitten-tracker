package com.kittentracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class KittenPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(KittenPlugin.class);
        RuneLite.main(args);
    }
}