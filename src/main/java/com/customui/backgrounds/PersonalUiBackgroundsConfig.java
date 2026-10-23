package com.customui.backgrounds;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(PersonalUiBackgroundsConfig.GROUP)
public interface PersonalUiBackgroundsConfig extends Config
{
    String GROUP = "personalUiBackgrounds";

    @ConfigSection(name = "Inventory", description = "Image behind inventory items and other side tabs", position = 0)
    String INVENTORY = "inventory";

    @ConfigSection(name = "Chatbox", description = "Image behind the chat text", position = 1)
    String CHATBOX = "chatbox";

    @ConfigItem(keyName = "inventoryEnabled", name = "Enable inventory image", description = "Apply a custom image to the side panel background", section = INVENTORY, position = 0)
    default boolean inventoryEnabled()
    {
        return true;
    }

    @ConfigItem(keyName = "inventoryImagePath", name = "Inventory image file", description = "Full path to a PNG or JPEG file (or use the plugin's Browse panel)", section = INVENTORY, position = 1)
    default String inventoryImagePath()
    {
        return "";
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "inventoryOpacity", name = "Inventory image strength", description = "Percent image blended onto the game's original background", section = INVENTORY, position = 2)
    default int inventoryOpacity()
    {
        return 85;
    }

    @ConfigItem(keyName = "inventoryScale", name = "Inventory image scaling", description = "How to resize your image", section = INVENTORY, position = 3)
    default ScalingMode inventoryScale()
    {
        return ScalingMode.CROP_TO_FILL;
    }

    @ConfigItem(keyName = "chatboxEnabled", name = "Enable chatbox image", description = "Apply a custom image behind chat text", section = CHATBOX, position = 0)
    default boolean chatboxEnabled()
    {
        return true;
    }

    @ConfigItem(keyName = "chatboxImagePath", name = "Chatbox image file", description = "Full path to a PNG or JPEG file (or use the plugin's Browse panel)", section = CHATBOX, position = 1)
    default String chatboxImagePath()
    {
        return "";
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "chatboxOpacity", name = "Chatbox image strength", description = "Lower values make chat text easier to read", section = CHATBOX, position = 2)
    default int chatboxOpacity()
    {
        return 40;
    }

    @ConfigItem(keyName = "chatboxScale", name = "Chatbox image scaling", description = "How to resize your image", section = CHATBOX, position = 3)
    default ScalingMode chatboxScale()
    {
        return ScalingMode.CROP_TO_FILL;
    }
}
