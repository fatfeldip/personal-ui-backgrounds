package com.customui.backgrounds;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PersonalUiBackgroundsPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(PersonalUiBackgroundsPlugin.class);
        RuneLite.main(args);
    }
}
