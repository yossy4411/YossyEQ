package com.yossy4411.yossyeq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;


public class Map extends Application {
    private double windowWidth,windowHeight;
    private double mouseX;
    private double mouseY;
    private final int mapSizeX = 1000;//※マップの本来の比率は3:4です
    private final int mapSizeY = 800;
    private double zoomFactor = 1.0;
    private final float roughness = 1f;
    private double zoomFactor2;
    private Point2D scroll = new Point2D(0,0);

    private final List<List<List<List<Point2D>>>> geometryPolygons = new ArrayList<>();
    private final List<List<Point2D>> Points = new ArrayList<>();
    private final List<List<String>> Names = new ArrayList<>();
    private final List<List<Color>> Colors = new ArrayList<>();
    private Group map,world,monitor,japan;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) throws IOException {

        windowWidth = 800; // ウィンドウ横幅
        windowHeight = 600; // ウィンドウ縦幅

        calcFromShapefile("src/main/resources/com/yossy4411/yossyeq/WorldMq/ne_50m_land.shp");
        calcFromShapefile("src/main/resources/com/yossy4411/yossyeq/jp1/地震情報／細分区域.shp");
        addPoint("src/main/resources/kyoshinMonitorPlaces.json");


        map = new Group();
        monitor = new Group();
        japan = new Group();
        world = new Group();

        Group root = new Group();
        root.getChildren().add(map);
        map.getChildren().add(world);
        map.getChildren().add(japan);
        map.getChildren().add(monitor);
        Scene scene = new Scene(root, windowWidth, windowHeight);
        addColor();
        zoomMapping();
        drawPolygons();
        drawKyoshinMonitor();

        // マウスのドラッグ操作ハンドリング
        scene.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                mouseX = event.getX();
                mouseY = event.getY();
            }
        });
        scene.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                double deltaX = event.getX() - mouseX;
                double deltaY = event.getY() - mouseY;

                // グループを移動
                map.setTranslateX(map.getTranslateX() + deltaX);
                map.setTranslateY(map.getTranslateY() + deltaY);
                translateMapping();
                drawKyoshinMonitor();

                mouseX = event.getX();
                mouseY = event.getY();
            }
        });
        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
            windowWidth = newValue.doubleValue();
            translateMapping();
            redrawPolygons();
        });
        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            windowHeight = newValue.doubleValue();
            translateMapping();
            redrawPolygons();
        });


        scene.setOnScroll(this::handleScroll);
        scene.setFill(Color.valueOf("#0B0C1C")); // 背景色
        scene.setFill(Color.WHITE);
        stage.setTitle("マップ");
        stage.setScene(scene);
        stage.show();
    }

    private void drawPolygons() {
        scroll = new Point2D(map.getTranslateX(), map.getTranslateY());
        List<List<List<Point2D>>> geometries = geometryPolygons.get(0);
        for (List<List<Point2D>> geometry : geometries) {//地物を取得
            for (List<Point2D> point2DS : geometry) {//シェイプを取得
                Polygon polygon = new Polygon();
                Point2D distPoint = new Point2D(1800, 900);
                int outCount = 0;

                for (Point2D point : point2DS) {//ポリゴンを取得
                    point = convertLatLngToScreen(point.getX(), point.getY());//画面上の座標に変換
                    boolean outWindow = Math.abs(point.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * (0.5+ roughness) || Math.abs(point.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight * (0.5+ roughness);//ウィンドウ外かどうか
                    if (!outWindow&&(point.distance(distPoint) > 4 || distPoint.equals(new Point2D(1800, 900)))) {//ポリゴンを簡略化
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                    } else if (outWindow && point.distance(distPoint) > 120){
                            polygon.getPoints().addAll(point.getX(), point.getY());
                            distPoint = point;
                            outCount ++;
                    }
                }
                if (polygon.getPoints().size() > 2 && outCount < polygon.getPoints().size()) {//完全に画面外または線点になるものを描画しない
                    //ポリゴンのスタイルを設定
                    polygon.setFill(Color.DARKGRAY);
                    polygon.setStroke(Color.GRAY);
                    polygon.setStrokeWidth(zoomFactor2);
                    world.getChildren().add(polygon);
                }
            }
        }
        drawJapanPolygons();
    }
    private void drawJapanPolygons() {
        japan.getChildren().clear();
        scroll = new Point2D(map.getTranslateX(), map.getTranslateY());
        List<List<List<Point2D>>> geometries = geometryPolygons.get(1);
        for (int i = 0; i <geometries.size(); i++) {//地物を取得
            List<List<Point2D>> geometry = geometries.get(i);
            for (List<Point2D> point2DS : geometry) {//シェイプを取得
                Polygon polygon = new Polygon();
                Point2D distPoint = new Point2D(1800, 900);
                int outCount = 0;
                for (Point2D point : point2DS) {//ポリゴンを取得
                    point = convertLatLngToScreen(point.getX(), point.getY());//画面上の座標に変換
                    boolean outWindow = Math.abs(point.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * (0.5+ roughness) || Math.abs(point.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight * (0.5+roughness);//ウィンドウ外かどうか
                    if (!outWindow&&(point.distance(distPoint) > 1 || distPoint.equals(new Point2D(1800, 900)))) {//ポリゴンを簡略化
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                    } else if (outWindow && point.distance(distPoint) > 200){
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                        outCount ++;
                    }
                }
                if (polygon.getPoints().size() > 2 && outCount < polygon.getPoints().size()) {//完全に画面外または線or点になるものを描画しない
                    //ポリゴンのスタイルを設定
                    polygon.setFill(Colors.get(0).get(i));
                    polygon.setStroke(Color.BLACK);
                    polygon.setStrokeWidth(zoomFactor2);
                    japan.getChildren().add(polygon);
                }
            }
        }
    }

    private void drawKyoshinMonitor() {
        monitor.getChildren().clear();
        Group pointsGroup = new Group();
        List<Point2D> points = Points.get(0);
        for (int i= 0; i < points.size(); i ++) {
            Point2D point2D = points.get(i);
            Circle point = new Circle();
            Point2D position = convertLatLngToScreen(point2D.getY(), point2D.getX());
            point.setCenterX(position.getX());
            point.setCenterY(position.getY());
            Text text = new Text(Names.get(0).get(i));
            text.setFont((Font.font("Arial", 20)));
            text.setX(position.getX());
            text.setY(position.getY());
            point.setRadius(Math.sqrt(zoomFactor)/1.5);
            point.setFill(Color.BLUE);
            boolean inWindow = Math.abs(point.getCenterX() + map.getTranslateX() - windowWidth / 2) < windowWidth / 2 + point.getRadius()&& Math.abs(point.getCenterY() + map.getTranslateY() - windowHeight / 2) < windowHeight /2+ point.getRadius();
            if (inWindow && Objects.equals(Names.get(1).get(i), "false")) {monitor.getChildren().add(point);
                if (zoomFactor > 300){pointsGroup.getChildren().add(text);}}
        }
        monitor.getChildren().add(pointsGroup);
    }
    private void calcFromShapefile(String filePath) throws IOException {
        File file = new File(filePath);
        DataStore dataStore = FileDataStoreFinder.getDataStore(file);
        String[] typeNames = dataStore.getTypeNames();
        List<List<List<Point2D>>> output= new ArrayList<>();
        for (String typeName : typeNames) {
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            SimpleFeatureCollection collection = source.getFeatures();
            try (SimpleFeatureIterator iterator = collection.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();

                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    List<List<Point2D>> Polygon = new ArrayList<>();
                    if (geometry instanceof org.locationtech.jts.geom.Polygon) {
                        org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) geometry;
                        Coordinate[] coordinates = polygon.getCoordinates();
                        List<Point2D> points = new ArrayList<>();
                        for (Coordinate coordinate : coordinates) {
                            points.add(new Point2D(coordinate.getY(),coordinate.getX()));//座標を格納する
                        }
                        Polygon.add(points);
                    } else if (geometry instanceof MultiPolygon multiPolygon) {

                        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                            org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) multiPolygon.getGeometryN(i);
                            Coordinate[] coordinates = polygon.getCoordinates();
                            List<Point2D> points = new ArrayList<>();
                            for (Coordinate coordinate : coordinates) {
                                points.add(new Point2D(coordinate.getY(),coordinate.getX()));//座標を格納する
                            }
                            Polygon.add(points);
                        }
                    }
                    output.add(Polygon);
                }
            }
        }
        geometryPolygons.add(output);
    }
    private void addColor (){
        List<Color> colorList = new ArrayList<>();
        for (int i = 0;i<geometryPolygons.get(1).size(); i++) {
            Random random = new Random();
            colorList.add(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
        }
        Colors.add(colorList);
    }
    private void addPoint (String filePath) {
        // ファイルからJSON文字列を読み取る
        String jsonString;
        try {
            jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
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
        Names.add(new ArrayList<>());
        Names.add(new ArrayList<>());
        Points.add(new ArrayList<>());
        for (int i = 0;i<jsonNode.size();i++){
        Points.get(0).add(new Point2D(jsonNode.at("/"+i+"/Location/Longitude").asDouble(),jsonNode.at("/"+i+"/Location/Latitude").asDouble()));
        Names.get(0).add(jsonNode.at("/" + i + "/Name").asText());
            Names.get(1).add(jsonNode.at("/" + i + "/IsSuspended").asText());
        }
    }

    /*
    ここから下は
    いじるとバグります
    いじる場合は自己責任で...
     */
    private void handleScroll(ScrollEvent event) {
        double scrollDeltaY = event.getDeltaY();

        // ホイール操作による拡大縮小
        double zoomDelta = 1.3;
        int maxZoom = 1000;
        double minZoom = Math.max(windowWidth / mapSizeX, windowHeight / mapSizeY);

        if (scrollDeltaY > 0) {
            zoomFactor *= zoomDelta;
            if (zoomFactor * zoomDelta > maxZoom){
                zoomDelta *= maxZoom / zoomFactor;
                zoomFactor = maxZoom;
            }
        } else {
            zoomFactor /= zoomDelta;
            if (zoomFactor / zoomDelta < minZoom){
                zoomDelta /= minZoom / zoomFactor;
                zoomFactor = minZoom;
            }
        }

        Point2D coordinate = new Point2D(event.getX() - windowWidth/2, event.getY()-windowHeight/2 );

        // カメラの移動量を計算
        double cameraTranslateX, cameraTranslateY;
        if (scrollDeltaY> 0 ) {
            cameraTranslateX = (coordinate.getX() - map.getTranslateX()) * (1 - zoomDelta);
            cameraTranslateY = (coordinate.getY() - map.getTranslateY()) * (1 - zoomDelta);
        } else {
            cameraTranslateX = (coordinate.getX() - map.getTranslateX()) * ((zoomDelta - 1) / zoomDelta);
            cameraTranslateY = (coordinate.getY() - map.getTranslateY()) * ((zoomDelta - 1) / zoomDelta);
        }
        // カメラの移動
        map.setTranslateX(map.getTranslateX() + cameraTranslateX);
        map.setTranslateY(map.getTranslateY() + cameraTranslateY);
        translateMapping();

        redrawPolygons();
    }
    private void translateMapping() {
        double translateX = map.getTranslateX() / zoomFactor;
        double translateY = map.getTranslateY() / zoomFactor;
        double windowX = windowWidth / 2;
        double windowY = windowHeight / 2;
        float mapX = (float) mapSizeX / 2;
        float mapY = (float) mapSizeY / 2;
        if (Math.abs(translateX) > mapX - windowX / zoomFactor){
            if (translateX > 0) {
                map.setTranslateX(mapX * zoomFactor - windowX);
            }
            else
            {
                map.setTranslateX(-mapX * zoomFactor + windowX);
            }
            if (Math.abs(map.getTranslateX() / zoomFactor) > mapX - windowX / zoomFactor){zoomMapping();}
        }
        if (Math.abs(map.getTranslateX() - scroll.getX()) > windowWidth / 4 * roughness || Math.abs(map.getTranslateY() - scroll.getY()) > windowHeight/4* roughness){redrawPolygons();}//スクロールしすぎたときに再描画する
        if (Math.abs(translateY) > mapY - windowY / zoomFactor){
            if (translateY > 0) {
                map.setTranslateY(mapY * zoomFactor - windowY);
            }
            else
            {
                map.setTranslateY(-mapY * zoomFactor + windowY);
            }
            if (Math.abs(map.getTranslateY() / zoomFactor) > mapY - windowY / zoomFactor){zoomMapping();}
        }
    }
    private void redrawPolygons() {
        world.getChildren().clear();
        drawPolygons();
        drawKyoshinMonitor();
    }


    private void zoomMapping(){
        zoomFactor = Math.max(windowWidth / mapSizeX, windowHeight / mapSizeY);
        zoomFactor2 = zoomFactor;
    }

    // メルカトル図法による緯度経度から画面上の座標への変換
    private Point2D convertLatLngToScreen(double latitude, double longitude) {
        double mercatorY = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(latitude) / 2));

        // スクリーン上の座標に変換
        double centerX = windowWidth / 2;
        double centerY = windowHeight / 2;
        double screenX = longitude * mapSizeX / 360 * zoomFactor + centerX;
        double screenY = -(mercatorY * mapSizeY / (2 * Math.PI)) * zoomFactor + centerY;

        return new Point2D(screenX, screenY);
    }


}
