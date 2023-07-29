package com.yossy4411.yossyeq;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static javafx.application.Application.launch;

public class CSVReader extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    public void start(Stage stage)  {
        String csvFile = "src/main/resources/ShindoColor.csv";
        String delimiter = ",";
        double targetValue = 2.1;
        Group root = new Group();
        Scene scene = new Scene(root,400,300);
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            Polyline redline = new Polyline();
            Polyline greenline = new Polyline();
            Polyline blueline = new Polyline();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);
                double firstColumnValue = Double.parseDouble(values[0]);
                int red = Integer.parseInt(values[1]);
                int green = Integer.parseInt(values[2]);
                int blue = Integer.parseInt(values[3]);
                Circle point = new Circle();

                point.setRadius(5);
                point.setFill(Color.rgb(red,green,blue));
                point.setCenterX((firstColumnValue + 3 )* 40);
                point.setCenterY(250);
                redline.getPoints().addAll((firstColumnValue + 3 )* 40, 270 - (double) red);
                greenline.getPoints().addAll((firstColumnValue + 3 )* 40, 270-(double) green);
                blueline.getPoints().addAll((firstColumnValue + 3 )* 40, 270-(double) blue);
                root.getChildren().add(point);
            }
            redline.setStroke(Color.RED);
            greenline.setStroke(Color.GREEN);
            blueline.setStroke(Color.BLUE);
            root.getChildren().add(redline);
            root.getChildren().add(greenline);
            root.getChildren().add(blueline);
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.setTitle("マップ");
        stage.setScene(scene);
        stage.show();
    }
}
