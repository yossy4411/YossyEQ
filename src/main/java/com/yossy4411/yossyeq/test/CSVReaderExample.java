package com.yossy4411.yossyeq.test;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;

public class CSVReaderExample {

    public static void main(String[] args) {
        String filePath = "path/to/your/csvfile.csv";
        double targetValue = 2.0;

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // 1番目の配列の値を取得（文字列から数値に変換）
                double firstValue = Double.parseDouble(nextLine[0]);

                if (firstValue == targetValue) {
                    // 2番目の配列の値を取得（文字列から数値に変換）
                    double secondValue = Double.parseDouble(nextLine[1]);
                    System.out.println("Target value found: " + secondValue);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }
}
