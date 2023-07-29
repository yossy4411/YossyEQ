package com.yossy4411.yossyeq;

import javafx.scene.paint.Color;

public class colorConverter {
    /// <summary>
    /// スケールを震度に変換します。
    /// </summary>
    /// <param name="scale">変換前のスケール</param>
    /// <returns></returns>
    public static double ConvertToIntensity(double scale) {
        return (scale * 10 - 3);
    }

    /// <summary>
    /// スケールをPGA(最大加速度)に変換します。
    /// </summary>
    /// <param name="scale">変換前のスケール</param>
    /// <returns></returns>
    public static double ConvertToPgaFromScale(double scale) {
        return Math.pow(10, 5 * scale - 2);
    }

    /// <summary>
    /// スケールをPGV(最大速度)に変換します。
    /// </summary>
    /// <param name="scale">変換前のスケール</param>
    /// <returns></returns>
    public static double ConvertToPgvFromScale(double scale) {
        return Math.pow(10, 5 * scale - 3);
    }

    /// <summary>
    /// スケールをPGD(最大変位)に変換します。
    /// </summary>
    /// <param name="scale">変換前のスケール</param>
    /// <returns></returns>
    public static double ConvertToPgdFromScale(double scale){
			return Math.pow(10,5*scale -4);

    }
    /// <summary>
    /// 多項式補完を使用して色をスケールに変換します。
    /// from: https://qiita.com/NoneType1/items/a4d2cf932e20b56ca444
    /// </summary>
    /// <param name="color">変換元の色</param>
    /// <returns>変換後のスケール</returns>
    public static double ConvertToScale(Color color){
			return Math.max(0, ConvertToScaleAtPolynomialInterpolationInternal(color));
    }
    private static double ConvertToScaleAtPolynomialInterpolationInternal(Color color)
    {
        // Input : color in hsv space
        double h,s,v;
        h = color.getHue();
        s = color.getSaturation();
        v = color.getBrightness();
        h /= 360;

        // Check if the color belongs to the scale
        if (v <= 0.1 || s <= 0.75)
            return 0;

        if (h > 0.1476)
            return 280.31 * Math.pow(h, 6) - 916.05 * Math.pow(h, 5) + 1142.6 * Math.pow(h, 4) - 709.95 * Math.pow(h, 3) + 234.65 * Math.pow(h, 2) - 40.27 * h + 3.2217; //280.31 * h^6 - 916.05 * h^5 + 1142.6 * h^4 - 709.95 * h^3 + 234.65 * h^2 - 40.27 * h + 3.2217
        else if (h > 0.001)
            return 151.4 * Math.pow(h, 4) - 49.32 * Math.pow(h, 3) + 6.753 * Math.pow(h, 2) - 2.481 * h + 0.9033; //151.4 * h^4 - 49.32 *h^3 + 6.753 *h^2 - 2.481 * h + 0.9033
        else
            return -0.005171 * Math.pow(v, 2) - 0.3282 * v + 1.2236;
    }

    public static Color ConvertToColorFromScale(double scale) {
        double h, s, v;

        if (scale <= 0)
            return Color.BLACK;

        if (scale >= 1)
            return Color.WHITE;

        if (scale <= 0.221)
            h = (scale - 0.9033) / -0.005171;
        else if (scale <= 0.1476)
            h = Math.pow((scale - 0.9033) / 151.4 + 2.481, 0.5);
        else
            h = Math.pow((scale - 3.2217) / 280.31 + 40.27, 0.16666666666666666);

        s = (scale - 0.9033) / 6.753 / Math.pow(h, 2) + 2.481;
        v = (scale - 3.2217) / 234.65 / Math.pow(h, 5) - 709.95 * Math.pow(h, 3) - 916.05 * Math.pow(h, 5) + 280.31 * Math.pow(h, 6) + 1142.6 * Math.pow(h, 4);

        h *= 360;
        v = Math.max(0, Math.min(1, v));
        s = Math.max(0, Math.min(1, s));

        return Color.hsb(h, s, v);
    }

}