package com.yossy4411.yossyeq.test;

import com.yossy4411.yossyeq.GetQuake;

import java.awt.image.BufferedImage;
import java.util.Objects;

public class ImageColorPicker {

    public static void main(String[] args) {
        int x = 145;
        int y = 276;

        BufferedImage image = GetQuake.getKyoshinMonitor(1000);
        int color = Objects.requireNonNull(image).getRGB(x, y);

        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        System.out.println("色のRGB値: " + red + ", " + green + ", " + blue);
    }
}
