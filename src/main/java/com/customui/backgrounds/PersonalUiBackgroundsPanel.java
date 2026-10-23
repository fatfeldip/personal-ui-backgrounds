package com.customui.backgrounds;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

/** Simple browse buttons and thumbnails; other settings live in RuneLite configuration. */
final class PersonalUiBackgroundsPanel extends PluginPanel
{
    private final ConfigManager configManager;
    private final PersonalUiBackgroundsPlugin plugin;
    private final JLabel inventoryPreview = new JLabel();
    private final JLabel chatboxPreview = new JLabel();
    private final JLabel status = new JLabel();

    PersonalUiBackgroundsPanel(ConfigManager configManager, PersonalUiBackgroundsPlugin plugin)
    {
        this.configManager = configManager;
        this.plugin = plugin;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 10, 10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("YOUR GAME, YOUR IMAGES");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(5));
        JLabel explainer = new JLabel("<html>Select PNG/JPG files on your PC. "
            + "Images stay on your computer.</html>");
        explainer.setAlignmentX(LEFT_ALIGNMENT);
        content.add(explainer);
        content.add(Box.createVerticalStrut(20));
        addImagePicker(content, "Inventory / side panel", "inventoryImagePath", inventoryPreview, 201, 276);
        content.add(Box.createVerticalStrut(20));
        addImagePicker(content, "Chatbox", "chatboxImagePath", chatboxPreview, 255, 75);
        content.add(Box.createVerticalStrut(14));

        JLabel note = new JLabel("<html>Adjust strength and cropping in the plugin's "
            + "configuration (wrench icon). On resizable modern chat, "
            + "the transparent UI may be unaffected.</html>");
        note.setAlignmentX(LEFT_ALIGNMENT);
        content.add(note);
        content.add(Box.createVerticalStrut(12));
        status.setAlignmentX(LEFT_ALIGNMENT);
        content.add(status);
        add(content, BorderLayout.NORTH);
        refresh();
    }

    private void addImagePicker(JPanel parent, String title, String configKey,
                                JLabel preview, int thumbnailWidth, int thumbnailHeight)
    {
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        parent.add(heading);
        parent.add(Box.createVerticalStrut(7));

        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setPreferredSize(new Dimension(240, Math.min(160, thumbnailHeight)));
        preview.setMaximumSize(new Dimension(240, Math.min(160, thumbnailHeight)));
        preview.setBorder(BorderFactory.createLineBorder(new Color(98, 98, 98)));
        preview.setAlignmentX(LEFT_ALIGNMENT);
        parent.add(preview);
        parent.add(Box.createVerticalStrut(8));

        JButton choose = new JButton("Browse for image...");
        choose.setAlignmentX(LEFT_ALIGNMENT);
        choose.addActionListener(e ->
        {
            JFileChooser picker = new JFileChooser();
            picker.setDialogTitle("Select " + title + " image");
            picker.setFileFilter(new FileNameExtensionFilter("PNG or JPG images", "png", "jpg", "jpeg"));
            picker.setAcceptAllFileFilterUsed(false);
            String old = configManager.getConfiguration(PersonalUiBackgroundsConfig.GROUP, configKey);
            if (old != null && !old.isEmpty() && new File(old).getParentFile() != null)
            {
                picker.setCurrentDirectory(new File(old).getParentFile());
            }
            if (picker.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            {
                configManager.setConfiguration(PersonalUiBackgroundsConfig.GROUP,
                    configKey, picker.getSelectedFile().getAbsolutePath());
                refresh();
            }
        });
        parent.add(choose);

        JButton clear = new JButton("Clear image");
        clear.setAlignmentX(LEFT_ALIGNMENT);
        clear.addActionListener(e ->
        {
            configManager.setConfiguration(PersonalUiBackgroundsConfig.GROUP, configKey, "");
            refresh();
        });
        parent.add(Box.createVerticalStrut(4));
        parent.add(clear);
    }

    void refresh()
    {
        setPreview(inventoryPreview, configManager.getConfiguration(PersonalUiBackgroundsConfig.GROUP,
            "inventoryImagePath"));
        setPreview(chatboxPreview, configManager.getConfiguration(PersonalUiBackgroundsConfig.GROUP,
            "chatboxImagePath"));
        String error = plugin.getLastError();
        status.setText(error.isEmpty() ? "<html>Choose an image to begin.</html>"
            : "<html><font color='#FF7777'>" + escapeHtml(error) + "</font></html>");
    }

    private static void setPreview(JLabel target, String path)
    {
        target.setIcon(null);
        if (path == null || path.trim().isEmpty())
        {
            target.setText("No image selected");
            return;
        }
        File file = new File(path);
        if (!file.isFile() || file.length() > 20L * 1024L * 1024L)
        {
            target.setText("File missing or too large");
            return;
        }
        try
        {
            BufferedImage source = ImageIO.read(file);
            if (source == null)
            {
                target.setText("Invalid image");
                return;
            }
            int maxW = 225;
            int maxH = 150;
            double scale = Math.min(maxW / (double) source.getWidth(), maxH / (double) source.getHeight());
            int width = Math.max(1, (int) Math.round(scale * source.getWidth()));
            int height = Math.max(1, (int) Math.round(scale * source.getHeight()));
            BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumbnail.createGraphics();
            try
            {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(0, 0, width, height);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source, 0, 0, width, height, null);
            }
            finally
            {
                g.dispose();
                source.flush();
            }
            target.setText("");
            target.setIcon(new ImageIcon(thumbnail));
        }
        catch (IOException ex)
        {
            target.setText("Can't preview image");
        }
    }

    private static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static BufferedImage createIcon()
    {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(62, 47, 31));
            g.fillRoundRect(2, 3, 28, 26, 5, 5);
            g.setColor(new Color(229, 205, 151));
            g.drawRoundRect(2, 3, 27, 25, 5, 5);
            g.setColor(new Color(159, 215, 232));
            g.fillRect(6, 7, 20, 17);
            g.setColor(new Color(58, 126, 88));
            g.fillPolygon(new int[]{6, 12, 17, 25, 25, 6}, new int[]{24, 15, 20, 10, 24, 24}, 6);
            g.setColor(new Color(255, 217, 107));
            g.fillOval(19, 10, 5, 5);
        }
        finally
        {
            g.dispose();
        }
        return image;
    }
}
