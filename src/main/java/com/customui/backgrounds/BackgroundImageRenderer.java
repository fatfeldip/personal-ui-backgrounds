package com.customui.backgrounds;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** A small, independent renderer so image scaling can be unit tested without RuneLite. */
final class BackgroundImageRenderer
{
    private BackgroundImageRenderer()
    {
    }

    /**
     * Renders a complete opaque sprite at the target dimensions. The image is
     * alpha-blended over the original interface sprite (when available).
     * Borders and chat tabs are intentionally not replaced.
     */
    static BufferedImage render(BufferedImage original, BufferedImage photo, int width, int height,
                                int insetLeft, int insetTop, int insetRight, int insetBottom,
                                int opacityPercent, ScalingMode scaling, Color fallback)
    {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        try
        {
            // Retain native sprite transparency (especially in modern gameframes).
            // Only use a solid fallback when the client artwork has not loaded yet.
            if (original == null)
            {
                g.setColor(fallback);
                g.fillRect(0, 0, width, height);
            }
            if (original != null)
            {
                // If the native sprite tiles (e.g. 32x32 resizable inventory),
                // tile it under the personal image so empty edges remain intact.
                for (int y = 0; y < height; y += original.getHeight())
                {
                    for (int x = 0; x < width; x += original.getWidth())
                    {
                        g.drawImage(original, x, y, null);
                    }
                }
            }

            int x = insetLeft;
            int y = insetTop;
            int w = Math.max(1, width - insetLeft - insetRight);
            int h = Math.max(1, height - insetTop - insetBottom);
            g.setClip(x, y, w, h);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                Math.min(100, Math.max(0, opacityPercent)) / 100f));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (scaling == ScalingMode.STRETCH)
            {
                g.drawImage(photo, x, y, w, h, null);
            }
            else
            {
                double ratioX = w / (double) photo.getWidth();
                double ratioY = h / (double) photo.getHeight();
                double ratio = scaling == ScalingMode.FIT ? Math.min(ratioX, ratioY) : Math.max(ratioX, ratioY);
                int scaledW = Math.max(1, (int) Math.round(photo.getWidth() * ratio));
                int scaledH = Math.max(1, (int) Math.round(photo.getHeight() * ratio));
                g.drawImage(photo, x + (w - scaledW) / 2, y + (h - scaledH) / 2, scaledW, scaledH, null);
            }
        }
        finally
        {
            g.dispose();
        }
        return output;
    }
}
