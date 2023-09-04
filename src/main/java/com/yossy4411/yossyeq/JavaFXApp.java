package com.yossy4411.yossyeq;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;

public class JavaFXApp extends Application {


    @Override
    public void start(Stage primaryStage) throws IOException {
        // FXMLファイルから要素をロード
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        // グループにロードした要素を追加
        Circle point = new Circle();
        point.setRadius(500);
        // シーンを作成してグループをセット
        Scene scene = new Scene(loader.load(), 800, 600);

        // ステージにシーンをセットして表示
        primaryStage.setScene(scene);
        primaryStage.setTitle("yee");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
