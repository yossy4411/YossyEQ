package com.yossy4411.yossyeq.test;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.yossy4411.yossyeq.colorConverter.convertShindoToColor;
import static com.yossy4411.yossyeq.colorConverter.convertShindoToString;

public class CSVReader extends Application {
    long startTime = System.currentTimeMillis();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private void changeColor(Circle shape){
        long elapse = (System.currentTimeMillis() - startTime) / 100;
        int[] Shindo = {0,10,20,30,40,45,50,55,60,70};
        shape.setFill(convertShindoToColor(Shindo[(int) Math.floor(elapse) % Shindo.length]));
    }
    public static void main(String[] args) {
        launch(args);
    }
    public void start(Stage stage)  {
        Group root = new Group();
        Scene scene = new Scene(root,400,300);
        Circle shape = new Circle();
        Text text = new Text();
        shape.setCenterX(200);
        shape.setCenterY(150);
        shape.setRadius(50);
        root.getChildren().add(shape);
        text.setFont(Font.font("Arial", FontPosture.ITALIC, 20));
        text.setX(200);
        text.setY(150);
        text.setText("7");
        text.setFill(Color.WHITE);
        root.getChildren().add(text);
        stage.setTitle("マップ");
        stage.setScene(scene);
        stage.show();

        System.out.println(convertShindoToString(50));
        scheduler.scheduleAtFixedRate(() -> changeColor(shape), 0, 100, TimeUnit.MILLISECONDS);
    }
}
