package com.customui.backgrounds;

public enum ScalingMode
{
    CROP_TO_FILL("Crop to fill"),
    FIT("Fit entire image"),
    STRETCH("Stretch to fit");

    private final String label;

    ScalingMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
