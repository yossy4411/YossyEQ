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
import javafx.scene.shape.Polyline;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.*;

import static com.yossy4411.yossyeq.colorConverter.*;

public class Map extends Application {

    private double windowWidth,windowHeight;
    private double mouseX;
    private double mouseY;
    private final int mapSizeX = 800;//※マップの本来の比率は8:7です
    private final int mapSizeY = 700;
    private double zoomFactor = 1.0;
    private final float roughness = 1f;
    private double zoomFactor2;
    private final List<String> Config = new ArrayList<>();
    private Point2D scroll = new Point2D(0,0);

    private final List<List<List<List<Point2D>>>> geometryPolygons = new ArrayList<>();
    private final List<List<Point2D>> Points = new ArrayList<>();
    private final List<List<String>> Names = new ArrayList<>();
    private final List<List<Color>> Colors = new ArrayList<>();
    private final List<Circle> kyoshinMonitor = new ArrayList<>();
    private final List<Point2D> kyoshinImages = new ArrayList<>();
    private Group map,world,monitor,monitorText,japan,tsunami,quakeInfo;
    private final String[] configArray = {"darkmode"};

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) throws IOException {

        windowWidth = 800; // ウィンドウ横幅
        windowHeight = 600; // ウィンドウ縦幅
        readProperties();
        addPoint("src/main/resources/kyoshinMonitorPlaces.json","/Location/Latitude","/Location/Longitude","/Name", new String[]{"/IsSuspended","Point/X","Point/Y"});

        calcFromShapefile("src/main/resources/com/yossy4411/yossyeq/WorldMq/ne_50m_land.shp");
        calcFromShapefile("src/main/resources/com/yossy4411/yossyeq/jp1/地震情報／細分区域.shp");
        calcFromShapefile("src/main/resources/com/yossy4411/yossyeq/jp1/地震情報／都道府県等.shp");
        System.out.println((double)Math.round(ConvertToIntensity(ConvertToScale(Color.valueOf("#001cdf"))) *10)/10);
        calcTsunami();

        addPoint("src/main/resources/stations.json","/lat","/lon","/name",new String[]{"/code"});
        map = new Group();//**以下のものをまとめているグループ**
        world = new Group();//世界のマップ
        japan = new Group();//日本のマップ
        tsunami = new Group();//津波予報
        monitor = new Group();//強震モニタ
        quakeInfo = new Group();//地震情報
        monitorText = new Group();//観測点
        Group root = new Group();//mapグループの大元

        root.getChildren().add(map);
        map.getChildren().add(tsunami);
        map.getChildren().add(world);
        map.getChildren().add(japan);
        map.getChildren().add(monitor);
        map.getChildren().add(monitorText);
        map.getChildren().add(quakeInfo);
        addKyoshinMonitor();
        Scene scene = new Scene(root, windowWidth, windowHeight);
        addColor();
        zoomMapping();
        zoomFactor  =9.5;
        Point2D home = convertLatLngToScreen(35.5,-135);
        map.setTranslateX(home.getX() - windowWidth / 2);
        map.setTranslateY(windowHeight / 2 - home.getY());
        drawTsunami();
        drawPolygons();
        changeKyoshinMonitor();
        drawQuakeInfo();

        translateMapping();

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
                changeKyoshinMonitor();

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

        scene.setFill(Color.WHITE);
        if(Objects.equals(getProperties("darkmode"), "true")){scene.setFill(Color.valueOf("#1a1a1a"));}
        stage.setTitle("マップ");
        stage.setScene(scene);
        stage.show();
    }
    private void readProperties(){
        Properties properties = new Properties();

        try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
            properties.load(input);
            for (String s : configArray) {
                Config.add(properties.getProperty(s));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String getProperties(String array){
        int index = Arrays.asList(configArray).indexOf(array);
        return Config.get(index);
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
                    polygon.setFill(Color.valueOf("9D9D9DFF"));
                    if(Objects.equals(getProperties("darkmode"), "true")){polygon.setFill(Color.GRAY);}
                    polygon.setStroke(Color.GRAY);
                    if(Objects.equals(getProperties("darkmode"), "true")){polygon.setStroke(Color.DARKGRAY);}
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
                    boolean outWindow = Math.abs(point.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * roughness || Math.abs(point.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight *roughness;//ウィンドウ外かどうか
                    if (!outWindow&&(point.distance(distPoint) > 2 || distPoint.equals(new Point2D(1800, 900)))) {//ポリゴンを簡略化
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
                    Color color = Colors.get(0).get(i);
                    polygon.setFill(color);
                    polygon.setStroke(color.darker());
                    if(Objects.equals(getProperties("darkmode"), "true")){polygon.setStroke(color.brighter());}
                    polygon.setStrokeWidth(zoomFactor2);
                    japan.getChildren().add(polygon);
                }
            }
        }
        geometries = geometryPolygons.get(2);//日本の緊急地震速報の配列
        Group strokes = new Group();
        int i = 0;
        for (List<List<Point2D>> geometry : geometries) {//地物を取得

            for (List<Point2D> point2DS : geometry) {//シェイプを取得
                Polyline polygon = new Polyline();
                Point2D distPoint = new Point2D(1800, 900);
                int outCount = 0;
                for (Point2D point : point2DS) {//ポリゴンを取得
                    point = convertLatLngToScreen(point.getX(), point.getY());//画面上の座標に変換
                    boolean outWindow = Math.abs(point.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * roughness || Math.abs(point.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight * roughness;//ウィンドウ外かどうか
                    if (!outWindow && (point.distance(distPoint) > 1 || distPoint.equals(new Point2D(1800, 900)))) {//ポリゴンを簡略化
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                    } else if (outWindow && point.distance(distPoint) > 200) {
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                        outCount++;
                    }
                }
                if (polygon.getPoints().size() > 2 && outCount < polygon.getPoints().size()) {//完全に画面外または線or点になるものを描画しない
                    //ポリゴンのスタイルを設定

                    polygon.setStroke(Colors.get(4).get(i));
                    polygon.setStrokeWidth(zoomFactor2 * Math.sqrt(Math.sqrt(zoomFactor))*1.3);
                    polygon.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

                    if (Colors.get(4).get(i)==null){
                        polygon.setStroke(Color.DIMGRAY);
                        if (Objects.equals(getProperties("darkmode"), "true")) {
                            polygon.setStroke(Color.WHITESMOKE);
                        }
                        polygon.setStrokeWidth(zoomFactor2 * Math.sqrt(Math.sqrt(zoomFactor)) / 2);
                    }



                    strokes.getChildren().add(polygon);
                }

            }
            i++;
        }
        japan.getChildren().add(strokes);
    }
    private void drawTsunami(){
        tsunami.getChildren().clear();
        List<List<List<Point2D>>> geometries = geometryPolygons.get(3);
        Group strokes = new Group();
        for (int i = 0;i<geometries.size();i++) {//地物を取得
            List<List<Point2D>> geometry = geometries.get(i);
            for (List<Point2D> point2DS : geometry) {//シェイプを取得
                Polyline polygon = new Polyline();
                Point2D distPoint = new Point2D(1800, 900);
                int outCount = 0;
                for (Point2D point : point2DS) {//ポリゴンを取得
                    point = convertLatLngToScreen(point.getX(), point.getY());//画面上の座標に変換
                    boolean outWindow = Math.abs(point.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * roughness || Math.abs(point.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight * roughness;//ウィンドウ外かどうか
                    if (!outWindow && (point.distance(distPoint) > 1 || distPoint.equals(new Point2D(1800, 900)))) {//ポリゴンを簡略化
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                    } else if (outWindow && point.distance(distPoint) > 200) {
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                        outCount++;
                    }
                }
                if (polygon.getPoints().size() > 2 && outCount < polygon.getPoints().size()) {//完全に画面外または線or点になるものを描画しない
                    //ポリゴンのスタイルを設定
                    Color color = Colors.get(1).get(i);
                    if (!(color == null)){
                        Circle circle = new Circle();
                        Point2D point = convertLatLngToScreen(point2DS.get(1).getX(),point2DS.get(1).getY());
                        circle.setCenterX(point.getX());
                        circle.setCenterY(point.getY());
                        circle.setRadius(Math.sqrt(zoomFactor) * Math.sqrt(3));
                        circle.setFill(color);
                        polygon.setStroke(color);
                        polygon.setStrokeWidth(Math.sqrt(zoomFactor) * 3);
                        polygon.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                        strokes.getChildren().add(circle);
                        circle = new Circle();
                        point = convertLatLngToScreen(point2DS.get(point2DS.size()-1).getX(),point2DS.get(point2DS.size()-1).getY());
                        circle.setCenterX(point.getX());
                        circle.setCenterY(point.getY());
                        circle.setRadius(Math.sqrt(zoomFactor) * Math.sqrt(3));
                        circle.setFill(color);
                        strokes.getChildren().add(circle);
                        strokes.getChildren().add(polygon);
                    }
                }
            }
        }
        tsunami.getChildren().add(strokes);
    }
    private void drawQuakeInfo(){
        quakeInfo.getChildren().clear();
        Group pointsGroup = new Group();
        List<Point2D> points = Points.get(1);
        for (int i= 0; i < points.size(); i ++) {
            Point2D point2D = points.get(i);
            Circle point = new Circle();
            Point2D position = convertLatLngToScreen(point2D.getY(), point2D.getX());
            point.setCenterX(position.getX());
            point.setCenterY(position.getY());
            Color color = Colors.get(2).get(i);
            if (color == null) {color = Color.rgb(0,0,0);}
            point.setFill(color);
            point.setStroke(color.darker());
            point.setStrokeWidth(Math.sqrt(zoomFactor) / 7);
            Text text = new Text(Names.get(5).get(i));
            text.setFont((Font.font("Arial", 10)));
            text.setX(position.getX() + Math.sqrt(zoomFactor)/1.2);
            text.setY(position.getY() + Math.sqrt(zoomFactor)/2.4);
            text.setFill(Color.grayRgb(20));
            if (Objects.equals(getProperties("darkmode"), "true")) {text.setFill(Color.grayRgb(220));point.setStroke(color.brighter());}
            point.setRadius(Math.sqrt(zoomFactor) / 1.2);

            boolean inWindow = Math.abs(point.getCenterX() + map.getTranslateX() - windowWidth / 2) < windowWidth / 2 + point.getRadius() && Math.abs(point.getCenterY() + map.getTranslateY() - windowHeight / 2) < windowHeight / 2 + point.getRadius();
            if (inWindow && !(Colors.get(2).get(i) == null)) {
                quakeInfo.getChildren().add(point);
                if (zoomFactor > 300) {
                    pointsGroup.getChildren().add(text);
                }
            }
        }
        quakeInfo.getChildren().add(pointsGroup);
    }
    private void addKyoshinMonitor(){
        monitor.getChildren().clear();
        List<Point2D> points = Points.get(0);
        for (int i= 0; i < points.size(); i ++) {
            Circle point = new Circle();
            kyoshinMonitor.add(point);
        }
        monitor.getChildren().addAll(kyoshinMonitor);
    }

    private void changeKyoshinMonitor() {
        monitorText.getChildren().clear();
        List<Point2D> points = Points.get(0);
        for (int i= 0; i < points.size(); i ++) {
            Circle point = kyoshinMonitor.get(i);
            Point2D point2D = points.get(i);
            Point2D position = convertLatLngToScreen(point2D.getY(), point2D.getX());
            point.setCenterX(position.getX());
            point.setCenterY(position.getY());
            Text text = new Text(Names.get(0).get(i));
            text.setFont((Font.font("Arial", 20)));
            text.setX(position.getX() + 10);
            text.setY(position.getY() + Math.sqrt(zoomFactor)/3);
            text.setFill(Color.grayRgb(20));
            if (Objects.equals(getProperties("darkmode"), "true")) {text.setFill(Color.grayRgb(220));}

            point.setRadius(Math.sqrt(zoomFactor)/1.5);
            point.setFill(Colors.get(3).get(i));
            boolean inWindow = Math.abs(point.getCenterX() + map.getTranslateX() - windowWidth / 2) < windowWidth / 2 + point.getRadius()&& Math.abs(point.getCenterY() + map.getTranslateY() - windowHeight / 2) < windowHeight /2+ point.getRadius();
            if (inWindow && Names.get(1).get(i).equals("false")/*休止中かどうか*/ ) {
                if (zoomFactor > 300){monitorText.getChildren().add(text);}}
            else{point.setFill(null);}
        }

    }
    private void calcFromShapefile(String filePath) throws IOException {
        boolean isJP = 1 == geometryPolygons.size() || 2 == geometryPolygons.size();
        File file = new File(filePath);
        DataStore dataStore = FileDataStoreFinder.getDataStore(file);
        String[] typeNames = dataStore.getTypeNames();
        List<List<List<Point2D>>> output= new ArrayList<>();
        for (String typeName : typeNames) {
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            SimpleFeatureCollection collection = source.getFeatures();
            try (SimpleFeatureIterator iterator = collection.features()) {
                List<String> attribute = new ArrayList<>();
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    if (isJP){attribute.add((String) feature.getAttribute("name"));}

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
                if (isJP) {
                    Names.add(attribute);
                }
            }
        }
        geometryPolygons.add(output);
    }
    private void calcTsunami() throws IOException {
        File file = new File("src/main/resources/com/yossy4411/yossyeq/tsunami/津波予報区.shp");
        DataStore dataStore = FileDataStoreFinder.getDataStore(file);
        String[] typeNames = dataStore.getTypeNames();
        List<List<List<Point2D>>> output = new ArrayList<>();
        for (String typeName : typeNames) {
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            SimpleFeatureCollection collection = source.getFeatures();

            try (SimpleFeatureIterator iterator = collection.features()) {
                List<String> attribute = new ArrayList<>();
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    attribute.add((String) feature.getAttribute("name"));

                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    if (geometry instanceof org.locationtech.jts.geom.LineString) {
                        org.locationtech.jts.geom.LineString lineString = (org.locationtech.jts.geom.LineString) geometry;
                        Coordinate[] coordinates = lineString.getCoordinates();
                        List<Point2D> points = new ArrayList<>();
                        for (Coordinate coordinate : coordinates) {
                            points.add(new Point2D(coordinate.getY(), coordinate.getX()));//座標を格納する
                        }
                        List<List<Point2D>> lineSegments = new ArrayList<>();
                        lineSegments.add(points);
                        output.add(lineSegments);
                    } else if (geometry instanceof org.locationtech.jts.geom.MultiLineString multiLineString) {
                        List<List<Point2D>> lineSegments = new ArrayList<>();
                        for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
                            org.locationtech.jts.geom.LineString lineString = (org.locationtech.jts.geom.LineString) multiLineString.getGeometryN(i);
                            Coordinate[] coordinates = lineString.getCoordinates();
                            List<Point2D> points = new ArrayList<>();
                            for (Coordinate coordinate : coordinates) {
                                points.add(new Point2D(coordinate.getY(), coordinate.getX()));//座標を格納する
                            }
                            lineSegments.add(points);

                        }
                        output.add(lineSegments);
                    }
                }
                Names.add(attribute);
            }
        }
        geometryPolygons.add(output);
    }


    private void addColor (){
        Colors.clear();
        List<Color> colorList = new ArrayList<>();
        for (int i = 0;i<geometryPolygons.get(1).size(); i++) {
            Color color = Color.grayRgb(200);
            if(Objects.equals(getProperties("darkmode"), "true")){color = Color.valueOf("#404040FF");}
            if (Objects.equals(Names.get(2).get(i), "滋賀県南部")){color = Color.valueOf("#32b464");}
            if (Objects.equals(Names.get(2).get(i), "滋賀県北部")){color = Color.valueOf("#e1e05d");}
            colorList.add(color);
        }
        Colors.add(colorList);
        colorList = new ArrayList<>();
        for (int i = 0;i<geometryPolygons.get(3).size(); i++){
            Color color = null;
            String tsunamiArray = "";
            if (tsunamiArray.contains(Names.get(4).get(i))){color = Color.valueOf("#eb0fff");}
            colorList.add(color);
        }
        Colors.add(colorList);
        colorList = new ArrayList<>();
        for (int i = 0;i<Points.get(1).size(); i++){
            Color color = null;
            String tsunamiArray = "大阪此花区春日出北";
            if (tsunamiArray.contains(Names.get(5).get(i))){color = Color.valueOf("#32b464");}
            colorList.add(color);
        }
        Colors.add(colorList);
        colorList = new ArrayList<>();
        for (int i = 0;i<Points.get(0).size(); i++){
            Color color = Color.BLUE;
            String tsunamiArray = "大津";
            if (tsunamiArray.contains(Names.get(0).get(i))){color = com.yossy4411.yossyeq.colorConverter.convertToColor(0.3);}
            colorList.add(color);
        }
        Colors.add(colorList);
        colorList = new ArrayList<>();
        for (int i = 0;i<geometryPolygons.get(2).size(); i++){
            Color color = null;
            String tsunamiArray = "滋賀県 大阪府 京都府 兵庫県 三重県 和歌山県 奈良県 愛知県 福井県 香川県 徳島県 岐阜県";
            if (tsunamiArray.contains(Names.get(3).get(i))){color = Color.RED;}
            if (color == null) {colorList.add(color);}
            else{colorList.add(color.deriveColor(0, 1, 1, 0.7));}
        }
        Colors.add(colorList);
    }
    private void setKyoshinColor(){
        List<Color> colorList = new ArrayList<>();
        for (int i = 0;i<Points.get(0).size(); i++){
            Color color = Color.BLUE;
            String tsunamiArray = "大津";
            if (tsunamiArray.contains(Names.get(0).get(i))){color = com.yossy4411.yossyeq.colorConverter.convertToColor(0.3);}
            colorList.add(color);
        }
        Colors.set(3,colorList);
    }
    private void addPoint (String filePath,String latitudePath,String longitudePath,String namePath,String[] otherPath) {
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
        int index = Names.size() - 1;
        Points.add(new ArrayList<>());
        int index2 = Points.size() - 1;
        for (int i = 0;i<jsonNode.size();i++) {
            if(!Objects.equals(jsonNode.at("/" + i + "/Point").asText(), "null")) {
                Points.get(index2).add(new Point2D(jsonNode.at("/" + i + longitudePath).asDouble(), jsonNode.at("/" + i + latitudePath).asDouble()));
                Names.get(index - 1).add(jsonNode.at("/" + i + namePath).asText());
                Names.get(index).add(jsonNode.at("/" + i + otherPath[0]).asText());
                if (otherPath.length > 1) {
                    kyoshinImages.add(new Point2D(jsonNode.at("/" + i + otherPath[1]).asDouble(), jsonNode.at("/" + i + otherPath[2]).asDouble()));
                }
            }
        }
    }

    /*
    ここから下は
    いじるとバグります
    いじる場合は自己責任で...
     */
    private void handleScroll(ScrollEvent event) {
        double scrollDeltaY = event.getDeltaY() /32;

        // ホイール操作による拡大縮小
        double zoomDelta = 1 + Math.abs(scrollDeltaY) * 0.5;
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
        if (Math.abs(map.getTranslateX() - scroll.getX()) > windowWidth / 2 * roughness || Math.abs(map.getTranslateY() - scroll.getY()) > windowHeight/2* roughness){redrawPolygons();}//スクロールしすぎたときに再描画する
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
        drawTsunami();
        drawPolygons();
        changeKyoshinMonitor();
        drawQuakeInfo();
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
