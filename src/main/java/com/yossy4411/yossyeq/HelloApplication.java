package com.yossy4411.yossyeq;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

public class ShapeFilePolygonExample extends Application {

    @Override
    public void start(Stage primaryStage) {
        // シェープファイルから座標データを取得する処理
        // ここでは、仮の座標値として直接指定します
        double[] polygonCoordinates = {50, 50, 150, 50, 150, 150, 50, 150};

        // ポリゴンを作成
        Polygon polygon = new Polygon();
        for (int i = 0; i < polygonCoordinates.length; i += 2) {
            polygon.getPoints().add(polygonCoordinates[i]);
            polygon.getPoints().add(polygonCoordinates[i + 1]);
        }

        // ポリゴンのスタイルを設定
        polygon.setFill(Color.GREEN);
        polygon.setStroke(Color.BLACK);

        // グループにポリゴンを追加
        Group root = new Group();
        root.getChildren().add(polygon);

        // シーンを作成
        Scene scene = new Scene(root, 300, 200);

        // ステージにシーンを設定して表示
        primaryStage.setScene(scene);
        primaryStage.setTitle("Shape File Polygon Example");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
