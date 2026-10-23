package com.customui.backgrounds;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BackgroundImageRendererTest
{
    @Test
    public void allModesPreserveMarginsAndScale()
    {
        BufferedImage picture = solid(80, 120, Color.GREEN);
        BufferedImage background = solid(10, 10, Color.RED);
        for (ScalingMode mode : ScalingMode.values())
        {
            BufferedImage result = BackgroundImageRenderer.render(background, picture, 201, 276,
                7, 5, 7, 5, 100, mode, Color.GRAY);
            assertEquals(201, result.getWidth());
            assertEquals(276, result.getHeight());
            assertEquals(Color.RED.getRGB(), result.getRGB(1, 1));
            assertEquals(Color.GREEN.getRGB(), result.getRGB(100, 120));
        }
    }

    @Test
    public void imageStrengthZeroKeepsOriginalBackground()
    {
        BufferedImage result = BackgroundImageRenderer.render(solid(20, 20, Color.RED),
            solid(20, 20, Color.GREEN), 20, 20, 0, 0, 0, 0, 0, ScalingMode.CROP_TO_FILL, Color.GRAY);
        assertEquals(Color.RED.getRGB(), result.getRGB(10, 10));
    }

    private static BufferedImage solid(int width, int height, Color color)
    {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }
}
