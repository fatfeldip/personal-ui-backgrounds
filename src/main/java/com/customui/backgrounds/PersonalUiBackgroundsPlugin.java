package com.customui.backgrounds;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SpriteID;
import net.runelite.api.SpritePixels;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

/**
 * Replaces only interface background sprites. Inventory icons and chat text
 * remain RuneScape-rendered elements ABOVE the custom backgrounds.
 * This plugin does not alter any game data or send images anywhere.
 */
@PluginDescriptor(
    name = "Personal UI Backgrounds",
    internalName = "personal-ui-backgrounds",
    description = "Choose local images for your inventory and chatbox backgrounds",
    tags = {"inventory", "chatbox", "custom", "background", "image"}
)
@SuppressWarnings("deprecation") // SpriteID names remain stable aliases in RuneLite.
public class PersonalUiBackgroundsPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(PersonalUiBackgroundsPlugin.class);
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;
    private static final int MAX_IMAGE_DIMENSION = 8192;

    private static final int[] INVENTORY_SPRITES = {
        SpriteID.RS2_SIDE_PANEL_BACKGROUND,
        SpriteID.FIXED_MODE_SIDE_PANEL_BACKGROUND,
        SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND
    };
    private static final int[] CHATBOX_SPRITES = {
        SpriteID.RS2_CHATBOX_BACKGROUND,
        SpriteID.CHATBOX
    };

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configManager;
    @Inject private SpriteManager spriteManager;
    @Inject private ClientToolbar clientToolbar;
    @Inject private PersonalUiBackgroundsConfig config;

    private PersonalUiBackgroundsPanel panel;
    private NavigationButton navigationButton;
    private volatile boolean started;
    private volatile String lastError = "";

    // All maps below are accessed exclusively from the RuneLite client thread.
    private final Map<Integer, SpritePixels> installedSprites = new HashMap<>();
    private final Map<Integer, SpritePixels> replacedOverrides = new HashMap<>();
    private final Set<Integer> previouslyUnmapped = new HashSet<>();
    private final Map<Integer, BufferedImage> originalArtwork = new HashMap<>();

    @Provides
    PersonalUiBackgroundsConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(PersonalUiBackgroundsConfig.class);
    }

    @Override
    protected void startUp()
    {
        started = true;
        SwingUtilities.invokeLater(() ->
        {
            if (!started)
            {
                return;
            }
            panel = new PersonalUiBackgroundsPanel(configManager, this);
            navigationButton = NavigationButton.builder()
                .tooltip("Personal UI Backgrounds")
                .icon(PersonalUiBackgroundsPanel.createIcon())
                .priority(7)
                .panel(panel)
                .build();
            clientToolbar.addNavigation(navigationButton);
        });
        clientThread.invokeLater(this::reloadSprites);
    }

    @Override
    protected void shutDown()
    {
        started = false;
        clientThread.invokeLater(() ->
        {
            removeOurOverrides();
            originalArtwork.clear();
            replacedOverrides.clear();
            previouslyUnmapped.clear();
        });
        SwingUtilities.invokeLater(() ->
        {
            if (navigationButton != null)
            {
                clientToolbar.removeNavigation(navigationButton);
                navigationButton = null;
            }
            panel = null;
        });
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!PersonalUiBackgroundsConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }
        clientThread.invokeLater(this::reloadSprites);
        SwingUtilities.invokeLater(() ->
        {
            if (panel != null)
            {
                panel.refresh();
            }
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            clientThread.invokeLater(this::reloadSprites);
        }
    }

    public String getLastError()
    {
        return lastError;
    }

    private void reloadSprites()
    {
        if (!started)
        {
            return;
        }

        removeOurOverrides();
        lastError = "";

        // The whole image is read once per config change, not on every frame.
        if (config.inventoryEnabled() && !config.inventoryImagePath().trim().isEmpty())
        {
            try
            {
                BufferedImage image = readLocalImage(config.inventoryImagePath());
                for (int spriteId : INVENTORY_SPRITES)
                {
                    applySprite(spriteId, image, config.inventoryOpacity(), config.inventoryScale(), false);
                }
            }
            catch (IOException | IllegalArgumentException ex)
            {
                recordError("Inventory image: " + ex.getMessage());
            }
        }
        if (config.chatboxEnabled() && !config.chatboxImagePath().trim().isEmpty())
        {
            try
            {
                BufferedImage image = readLocalImage(config.chatboxImagePath());
                for (int spriteId : CHATBOX_SPRITES)
                {
                    applySprite(spriteId, image, config.chatboxOpacity(), config.chatboxScale(), true);
                }
            }
            catch (IOException | IllegalArgumentException ex)
            {
                recordError("Chatbox image: " + ex.getMessage());
            }
        }

        client.getWidgetSpriteCache().reset();
        SwingUtilities.invokeLater(() ->
        {
            if (panel != null)
            {
                panel.refresh();
            }
        });
    }

    private void recordError(String message)
    {
        log.warn(message);
        lastError = lastError.isEmpty() ? message : lastError + " | " + message;
    }

    private static BufferedImage readLocalImage(String path) throws IOException
    {
        File file = new File(path.trim());
        if (!file.isFile())
        {
            throw new IOException("File not found: " + path);
        }
        if (Files.size(file.toPath()) > MAX_FILE_SIZE_BYTES)
        {
            throw new IOException("Image is over 20 MB; please use a smaller PNG/JPG");
        }
        BufferedImage photo = ImageIO.read(file);
        if (photo == null)
        {
            throw new IOException("This file isn't a supported image (use PNG or JPG)");
        }
        if (photo.getWidth() > MAX_IMAGE_DIMENSION || photo.getHeight() > MAX_IMAGE_DIMENSION)
        {
            photo.flush();
            throw new IOException("Image is larger than 8192 px; resize it first");
        }
        return photo;
    }

    private void applySprite(int id, BufferedImage photo, int strength, ScalingMode scaleMode, boolean chatbox)
    {
        final int width;
        final int height;
        if (!chatbox)
        {
            // Resizable inventory is originally a small tiled sprite. Replacing it
            // with a 201x276 sprite prevents personal photos from repeating as tiles.
            width = id == SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND ? 201 : 190;
            height = id == SpriteID.RESIZEABLE_MODE_SIDE_PANEL_BACKGROUND ? 276 : 261;
        }
        else
        {
            // Capture actual dimensions so classic and fixed gameframe artwork
            // keeps its intended size, including the decorative frame/borders.
            BufferedImage original = getOriginal(id);
            width = original != null ? original.getWidth() : (id == SpriteID.CHATBOX ? 519 : 479);
            height = original != null ? original.getHeight() : (id == SpriteID.CHATBOX ? 165 : 150);
        }

        BufferedImage original = getOriginal(id);
        int borderX = chatbox ? Math.min(10, width / 16) : 0;
        int borderTop = chatbox ? Math.min(8, height / 16) : 0;
        int borderBottom = chatbox ? Math.min(23, height / 6) : 0;
        BufferedImage output = BackgroundImageRenderer.render(original, photo, width, height,
            borderX, borderTop, borderX, borderBottom, strength, scaleMode,
            chatbox ? new Color(213, 193, 157) : new Color(59, 52, 38));
        int[] argb = output.getRGB(0, 0, width, height, null, 0, width);
        SpritePixels pixels = client.createSpritePixels(argb, width, height);
        if (pixels == null)
        {
            return;
        }

        Map<Integer, SpritePixels> overrides = client.getSpriteOverrides();
        if (!replacedOverrides.containsKey(id) && !previouslyUnmapped.contains(id))
        {
            if (overrides.containsKey(id))
            {
                replacedOverrides.put(id, overrides.get(id));
            }
            else
            {
                previouslyUnmapped.add(id);
            }
        }
        installedSprites.put(id, pixels);
        overrides.put(id, pixels);
    }

    private BufferedImage getOriginal(int id)
    {
        BufferedImage old = originalArtwork.get(id);
        if (old != null)
        {
            return old;
        }

        SpritePixels existing = replacedOverrides.get(id);
        if (existing == null && !previouslyUnmapped.contains(id))
        {
            existing = client.getSpriteOverrides().get(id);
        }
        if (existing != null)
        {
            old = existing.toBufferedImage();
        }
        else
        {
            // Cache source (archive, file 0). May be null before the login screen loads.
            old = spriteManager.getSprite(id, 0);
        }
        if (old != null && old.getWidth() > 0 && old.getHeight() > 0)
        {
            originalArtwork.put(id, old);
            return old;
        }
        return null;
    }

    private void removeOurOverrides()
    {
        if (installedSprites.isEmpty())
        {
            return;
        }
        Map<Integer, SpritePixels> overrides = client.getSpriteOverrides();
        for (Map.Entry<Integer, SpritePixels> entry : installedSprites.entrySet())
        {
            int id = entry.getKey();
            // Don't overwrite a different plugin's changes made after ours.
            if (overrides.get(id) != entry.getValue())
            {
                continue;
            }
            if (replacedOverrides.containsKey(id))
            {
                overrides.put(id, replacedOverrides.get(id));
            }
            else
            {
                overrides.remove(id);
            }
        }
        installedSprites.clear();
        client.getWidgetSpriteCache().reset();
    }
}
