package com.yossy4411.yossyeq.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

public class RoundedLineExample extends Application {
    public void start(Stage stage) {
        // ペインを作成
        Pane pane = new Pane();

        // Polygonを作成してペインに追加
        Polygon polygon = new Polygon();
        polygon.getPoints().addAll(
                50.0, 100.0,
                100.0, 50.0,
                150.0, 100.0
        );
        polygon.setStroke(Color.BLACK);
        polygon.setStrokeWidth(5);
        polygon.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND); // 角を丸くする
        polygon.setFill(null);
        pane.getChildren().add(polygon);

        // シーンを作成
        Scene scene = new Scene(pane, 200, 200);

        // ステージにシーンを追加して表示
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
