package com.yossy4411.yossyeq;

import javafx.scene.paint.Color;

import java.util.Arrays;

public class colorConverter {

    public static class Scale{
        private final double scale;
        public Scale(double scale){
            this.scale = scale;
        }
        /// スケールと震度に変換します。
        public double toIntensity() {
            return (scale * 10 - 3);
        }

        /// スケールとPGA(最大加速度)に変換します。
        public double toPga() {
            return Math.pow(10, 5 * scale - 2);
        }

        /// スケールとPGV(最大速度)に変換します。
        /// 速度応答にも使用できます。
        public double toPgv() {
            return Math.pow(10, 5 * scale - 3);
        }

        /// スケールとPGD(最大変位)に変換します。
        public double toPgd(){
            return Math.pow(10,5 * scale - 4);
        }
    }


    /// 多項式補完を使用して色をスケールに変換します。
    /// from: https://qiita.com/NoneType1/items/a4d2cf932e20b56ca444
    public static double ConvertColorToScale(Color color){
        if (color == null) {return -1;}else{
			return Math.max(0, ConvertToScaleAtPolynomialInterpolationInternal(color));}
    }
    private static double ConvertToScaleAtPolynomialInterpolationInternal(Color color)
    {
        // Input : color
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


    public static Color convertToColor(double scale) {
        int index = (int) Math.round(scale * 100);
        if(index<0 || 99<index){return null;}
        int[] red = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 12, 18, 25, 31, 37, 44, 50, 56, 63, 75, 88, 100, 113, 125, 138, 151, 163, 176, 189, 195, 202, 208, 215, 222, 228, 235, 241, 248, 255, 254, 254, 254, 254, 255, 254, 255, 254, 255, 255, 254, 254, 254, 254, 255, 254, 255, 254, 255, 255, 254, 254, 254, 254, 255, 254, 255, 254, 255, 255, 254, 253, 252, 251, 250, 249, 248, 247, 246, 245, 238, 230, 223, 215, 208, 200, 192, 185, 177, 170};
        int[] green = {0, 7, 14, 21, 28, 36, 43, 50, 57, 64, 72, 85, 99, 112, 126, 140, 153, 167, 180, 194, 208, 212, 216, 220, 224, 228, 233, 237, 241, 245, 250, 250, 250, 251, 251, 252, 252, 253, 253, 254, 255, 254, 254, 254, 254, 255, 254, 255, 254, 255, 255, 251, 248, 244, 241, 238, 234, 231, 227, 224, 221, 213, 205, 197, 190, 182, 174, 167, 159, 151, 144, 136, 128, 121, 113, 106, 98, 90, 83, 75, 68, 61, 54, 47, 40, 33, 27, 20, 13, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] blue = {205, 209, 214, 218, 223, 227, 231, 236, 240, 245, 250, 238, 227, 216, 205, 194, 183, 172, 161, 150, 139, 130, 121, 113, 104, 96, 88, 79, 71, 62, 54, 49, 45, 41, 37, 33, 28, 24, 20, 16, 12, 10, 9, 8, 7, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        return Color.rgb(red[index], green[index], blue[index]);
    }
    public static int convertIntensityToShindo(double Intensity){
        int result = (int) Math.round(Intensity / 2) * 2;
        if (7<=result || result < 5){result = Math.round(result/10f)*10;}
        return result;
    }
    public static String convertShindoToString(int shindo){
        float scale = shindo / 10f;
        String result;
        if(4 < scale && scale<=6) {
            if (scale >  Math.floor(scale)){
                result = (int)Math.ceil(scale) + "弱";
            }else {
                result = (int)scale + "強";
            }
        }else{result = String.valueOf((int)scale);}
        return result;
    }

    public static Color convertShindoToColor(int shindo){
        int[] Shindo = {0,10,20,30,40,45,50,55,60,70};
        Arrays.sort(Shindo);
        int index = Arrays.binarySearch(Shindo,shindo);
        String[] ShindoColor = {"#818188","#38add7","#5bd08c","#ffd85f","#f1a418","#ea0f0f","#b60606","#b10d58","#9a0e8e","#491971"};
        if (index < 0){return null;}
        return Color.valueOf(ShindoColor[index]);
    }
}