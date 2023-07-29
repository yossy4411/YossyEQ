package com.yossy4411.yossyeq.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class ColorEditor extends Application {
    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();

        // 色相（hue）と明るさ（brightness）を指定
        double hue = 0.5; // 色相（0.0～1.0の間で指定）
        double brightness = 1.0; // 明るさ（0.0～1.0の間で指定）

        // 色をHSB値で作成
        Color color = Color.hsb(hue * 360, 1.0, brightness);

        Circle circle = new Circle(50, color);
        root.getChildren().add(circle);

        Scene scene = new Scene(root, 200, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
