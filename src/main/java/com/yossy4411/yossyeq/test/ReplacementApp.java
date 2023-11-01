package com.yossy4411.yossyeq.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yossy4411.yossyeq.GetQuake;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class ReplacementApp extends Application {
    private Point2D f = new Point2D(0,0);
    @Override
    public void start(Stage primaryStage) {
        // レイアウトの作成
        Pane root = new Pane();
        //画像の読み込み
        root.getChildren().add(new ImageView(new Image("file:src/main/resources/com/yossy4411/yossyeq/basemap.png")));
        Image image = new Image("file:src/main/resources/com/yossy4411/yossyeq/kmoni.png");
        ImageView imageview = new ImageView(image);
        imageview.setSmooth(false);
        imageview.setOpacity(0.5);
        root.getChildren().add(imageview);

        addPoint(root);
        // シーンの作成
        Scene scene = new Scene(root, image.getWidth(), image.getHeight());
        scene.setOnMousePressed(event -> f = new Point2D(event.getX()-root.getTranslateX(),event.getY()- root.getTranslateY()));
        scene.setOnScroll(event -> {
            double delta = Math.pow(1.01,event.getDeltaY());
            root.setScaleX(root.getScaleX() * delta);
            root.setScaleY(root.getScaleY() * delta);
            root.setTranslateX(root.getTranslateX()*delta);
            root.setTranslateY(root.getTranslateY()*delta);
        });
        scene.setOnMouseDragged(event -> {
            root.setTranslateX( event.getX()-f.getX());
            root.setTranslateY(event.getY()-f.getY());
        });
        // ステージの設定
        primaryStage.setTitle("Circle Placement");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void addPoint(Pane root) {

        // ファイルからJSON文字列を読み取る
        String jsonString;
        try {
            jsonString = new String(Files.readAllBytes(Paths.get("src/main/resources/kyoshinMonitorPlaces.json")));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // JSON文字列を解析してJsonNodeオブジェクトに変換
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        for (int i = 0; i < jsonNode.size(); i++) {
            if (!(Objects.equals(jsonNode.at("/" + i + "/Point").asText(), "null")||jsonNode.at("/"+i+"/IsSuspended").asBoolean())) {
                Point2D poi = GetQuake.utils.ConvertCoordinateToPosition(jsonNode.at("/" + i + "/Location/Latitude").asDouble(),jsonNode.at("/" + i + "/Location/Longitude").asDouble());
                root.getChildren().add(new Rectangle(jsonNode.at("/" + i + "/Point/X").asInt(),jsonNode.at("/" + i + "/Point/Y").asInt(),1,1));
                Rectangle rectangle = new Rectangle(poi.getX(),poi.getY(),1,1);
                rectangle.setFill(new Color(1,0,0,0.5));
                root.getChildren().add(rectangle);
            }
        }

    }
}
