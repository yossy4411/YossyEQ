package com.yossy4411.yossyeq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.yossy4411.yossyeq.GetQuake.*;
import static com.yossy4411.yossyeq.colorConverter.ConvertColorToScale;
import static com.yossy4411.yossyeq.colorConverter.convertToColor;

public class Map extends Application {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private double windowWidth, windowHeight;
    private double mouseX;
    private double mouseY;
    private final int mapSizeX = 800;//※マップの本来の比率は8:7です
    private final int mapSizeY = 750;
    private double zoomFactor = 1.0;
    private final float roughness = 1f;
    private double zoomFactor2;
    private final List<String> Config = new ArrayList<>();
    private Point2D scroll = new Point2D(0, 0);

    private final List<List<List<List<Point2D>>>> geometryPolygons = new ArrayList<>();
    private final List<List<Point2D>> Points = new ArrayList<>();
    private final List<List<String>> Names = new ArrayList<>();
    private final List<List<String>> Colors = new ArrayList<>();
    private final List<Circle> kyoshinMonitor = new ArrayList<>();
    private final List<List<Double>> Scales = new ArrayList<>();
    private final List<Point2D> kyoshinImages = new ArrayList<>();

    private final Pane map = new Pane();
    private Group world, monitor, monitorText, japan, tsunami, quakeInfo;
    private final List<Long> quakeIds = new ArrayList<>();
    VBox RightBox = new VBox();
    private final long timeDelay = getLatestTime() - System.currentTimeMillis();
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) throws IOException, InterruptedException {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // 非同期タスクを実行する処理
            return "非同期処理の結果";
        });
        windowWidth = 800; // ウィンドウ横幅
        windowHeight = 600; // ウィンドウ縦幅
        readProperties();

        /*ここから*/
        /**/ addPoint("src/main/resources/kyoshinMonitorPlaces.json", "/Location/Latitude", "/Location/Longitude", "/Name", new String[]{"/IsSuspended", "/Point/X", "/Point/Y"});
        /**/ calcFromShapefile("src/main/resources/com/yossy4411/yossyeq/world/ne_10m_country.shp");
        /**/ calcFromShapefile("src/main/resources/com/yossy4411/yossyeq/jp-report/地震情報／細分区域.shp");
        /**/ calcFromShapefile("src/main/resources/com/yossy4411/yossyeq/jp-eew/緊急地震速報／都道府県等.shp");
        /**/ calcTsunami();
        /**/ addPoint("src/main/resources/stations.json", "/lat", "/lon", "/name", new String[]{"/code"});
        /*ここまでは順番を変えるとバグります。*/

        Group root = new Group();//ルートグループ
        System.out.println(Config);
        //root.getStyleClass().add("dark-mode");
        Scene scene = new Scene(root, windowWidth, windowHeight);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
        scene.setFill(Color.WHITE);
        if (root.getStyleClass().contains("dark-mode")) {
            scene.setFill(Color.valueOf("#1a1a1a"));
        }
        world = new Group();//世界のマップ
        japan = new Group();//日本のマップ
        tsunami = new Group();//津波予報
        monitor = new Group();//強震モニタ
        quakeInfo = new Group();//地震情報
        monitorText = new Group();//観測点
        Group rightUI = new Group();
        ScrollPane f = new ScrollPane(RightBox);

        f.setPrefSize(200,370);
        f.setLayoutX(-f.getPrefWidth());
        rightUI.getChildren().add(f);
        rightUI.setLayoutX(windowWidth);
        rightUI.getStyleClass().add("rightUI");

        root.getChildren().add(map);
        root.getChildren().add(rightUI);
        map.getChildren().add(tsunami);
        map.getChildren().add(world);
        map.getChildren().add(japan);
        map.getChildren().add(monitor);
        map.getChildren().add(monitorText);
        map.getChildren().add(quakeInfo);
        addKyoshinMonitor();

        addColor();
        zoomMapping();
        zoomFactor = 9.5;
        Point2D home = convertLatLngToScreen(35.5, -135);
        map.setTranslateX(home.getX() - windowWidth / 2);
        map.setTranslateY(windowHeight / 2 - home.getY());
        drawTsunami();
        drawPolygons();
        changeKyoshinMonitor();
        drawQuakeInfo();

        translateMapping();
        scheduler.scheduleAtFixedRate(this::setKyoshinColor, 1240-(System.currentTimeMillis()+timeDelay)%1000, 1000, TimeUnit.MILLISECONDS);
        Thread.sleep(1240-(System.currentTimeMillis()+timeDelay)%1000);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> changeKyoshinMonitor())
        );
        timeline.setCycleCount(Timeline.INDEFINITE); // 繰り返し設定

        // Timelineの開始
        timeline.play();

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

                mouseX = event.getX();
                mouseY = event.getY();
            }
        });
        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
            windowWidth = newValue.doubleValue();
            rightUI.setLayoutX(windowWidth);
            translateMapping();
            redrawPolygons();
        });
        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            windowHeight = newValue.doubleValue();
            translateMapping();
            redrawPolygons();
        });


        scene.setOnScroll(event -> { if(map.isHover()){handleScroll(event);} });
        stage.setTitle("マップ");
        stage.setScene(scene);
        stage.show();
    }

    private void readProperties() {
        Properties properties = new Properties();

        try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
            properties.load(input);
            Enumeration<?> propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String propertyValue = properties.getProperty(propertyNames.nextElement().toString());
                Config.add(propertyValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    boolean outWindow = Math.abs(point.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * (0.5 + roughness) || Math.abs(point.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight * (0.5 + roughness);//ウィンドウ外かどうか
                    if (!outWindow && (point.distance(distPoint) > 4 || distPoint.equals(new Point2D(1800, 900)))) {//ポリゴンを簡略化
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                    } else if (outWindow && point.distance(distPoint) > 120) {
                        polygon.getPoints().addAll(point.getX(), point.getY());
                        distPoint = point;
                        outCount++;
                    }
                }
                if (polygon.getPoints().size() > 2 && outCount < polygon.getPoints().size()) {//完全に画面外または線点になるものを描画しない
                    //ポリゴンのスタイルを設定
                    polygon.getStyleClass().add("world");
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
        for (int i = 0; i < geometries.size(); i++) {//地物を取得
            List<List<Point2D>> geometry = geometries.get(i);
            for (List<Point2D> point2DS : geometry) {//シェイプを取得
                Polygon polygon = new Polygon();
                Point2D distPoint = new Point2D(1800, 900);
                int outCount = 0;
                for (Point2D point : point2DS) {//ポリゴンを取得
                    point = convertLatLngToScreen(point.getX(), point.getY());//画面上の座標に変換
                    boolean outWindow = Math.abs(point.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * roughness || Math.abs(point.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight * roughness;//ウィンドウ外かどうか
                    if (!outWindow && (point.distance(distPoint) > 2 || distPoint.equals(new Point2D(1800, 900)))) {//ポリゴンを簡略化
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
                    polygon.getStyleClass().add(Colors.get(0).get(i));
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

                    polygon.getStyleClass().add(Colors.get(3).get(i));
                    polygon.setStrokeWidth(zoomFactor2 * Math.sqrt(Math.sqrt(zoomFactor)) * 1.3);

                    if (Objects.equals(Colors.get(3).get(i), "non-eew")) {
                        polygon.setStrokeWidth(zoomFactor2 * Math.sqrt(Math.sqrt(zoomFactor)) / 2);
                    }


                    strokes.getChildren().add(polygon);
                }

            }
            i++;
        }
        japan.getChildren().add(strokes);
    }

    private void drawTsunami() {
        tsunami.getChildren().clear();
        List<List<List<Point2D>>> geometries = geometryPolygons.get(3);
        Group strokes = new Group();
        for (int i = 0; i < geometries.size(); i++) {//地物を取得
            List<List<Point2D>> geometry = geometries.get(i);
            for (List<Point2D> point2DS : geometry) {//シェイプを取得
                Polyline polygon = new Polyline();
                polygon.getStyleClass().add("tsunami");
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
                    if (!(Objects.equals(Colors.get(1).get(i), "clear"))) {
                        polygon.getStyleClass().add(Colors.get(1).get(i));
                        polygon.setStrokeWidth(Math.sqrt(zoomFactor) * 3);
                        strokes.getChildren().add(polygon);
                    }
                }
            }
        }
        tsunami.getChildren().add(strokes);
    }

    private void drawQuakeInfo() {
        quakeInfo.getChildren().clear();
        Group pointsGroup = new Group();
        List<Point2D> points = Points.get(1);
        for (int i = 0; i < points.size(); i++) {
            Point2D point2D = points.get(i);
            Circle point = new Circle();
            Point2D position = convertLatLngToScreen(point2D.getY(), point2D.getX());
            point.setCenterX(position.getX());
            point.setCenterY(position.getY());
            point.getStyleClass().add(Colors.get(2).get(i));
            point.setStrokeWidth(Math.sqrt(zoomFactor) / 7);
            Text text = new Text(Names.get(5).get(i));
            text.setX(position.getX() + Math.sqrt(zoomFactor) / 1.2);
            text.setY(position.getY() + Math.sqrt(zoomFactor) / 2.4);
            text.getStyleClass().add("qtext");
            point.setRadius(Math.sqrt(zoomFactor) / 1.2);

            boolean outWindow = Math.abs(position.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * roughness || Math.abs(position.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight * roughness;
            if (!outWindow && !(Colors.get(2).get(i) == null)) {
                quakeInfo.getChildren().add(point);
                if (zoomFactor > 300) {
                    pointsGroup.getChildren().add(text);
                }
            }
        }
        quakeInfo.getChildren().add(pointsGroup);
    }

    private void addKyoshinMonitor() {
        monitor.getChildren().clear();
        List<Point2D> points = Points.get(0);
        for (int i = 0; i < points.size(); i++) {
            kyoshinMonitor.add(new Circle());
            kyoshinMonitor.add(new Circle());
        }
        monitor.getChildren().addAll(kyoshinMonitor);
    }
    private void changeKyoshinMonitor() {
        monitorText.getChildren().clear();
        //System.out.println(Names.get(0).get((int)Math.floor(Scales.get(1).get(Scales.get(1).size() - 1))));
        monitor.getChildren().clear();
        List<Point2D> points = Points.get(0);
        for (int i = 0; i < Scales.get(1).size(); i++) {
            int index = (int)Math.floor(Scales.get(1).get(i));

            Circle point = new Circle();
            Point2D point2D = points.get(index);
            Point2D position = convertLatLngToScreen(point2D.getY(), point2D.getX());
            point.setCenterX(position.getX());
            point.setCenterY(position.getY());
            Label text = new Label(Names.get(0).get(index));
            text.getStyleClass().add("kmoni-text");
            text.setLayoutX(position.getX() + Math.sqrt(zoomFactor) / 1.5);
            text.setLayoutY(position.getY()-12);


            point.setRadius(Math.sqrt(zoomFactor) / 1.5);
            Color color = convertToColor(Scales.get(0).get(index));
            point.setFill(color);
            boolean outWindow = Math.abs(position.getX() + map.getTranslateX() - windowWidth / 2) > windowWidth * (0.5 + roughness) || Math.abs(position.getY() + map.getTranslateY() - windowHeight / 2) > windowHeight * (0.5 + roughness);//ウィンドウ外かどうか
            if (!outWindow && Names.get(1).get(index).equals("false")/*休止中かどうか*/ && color != null) {
                if (zoomFactor > 300) {
                    monitorText.getChildren().add(text);
                }
            }
            monitor.getChildren().add(point);
        }
    }

    private void calcFromShapefile(String filePath) throws IOException {
        boolean isJP = 1 == geometryPolygons.size() || 2 == geometryPolygons.size();
        File file = new File(filePath);
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
                    if (isJP) {
                        attribute.add((String) feature.getAttribute("name"));
                    }

                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    List<List<Point2D>> Polygon = new ArrayList<>();
                    if (geometry instanceof org.locationtech.jts.geom.Polygon) {
                        org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) geometry;
                        Coordinate[] coordinates = polygon.getCoordinates();
                        List<Point2D> points = new ArrayList<>();
                        for (Coordinate coordinate : coordinates) {
                            points.add(new Point2D(coordinate.getY(), coordinate.getX()));//座標を格納する
                        }
                        Polygon.add(points);
                    } else if (geometry instanceof MultiPolygon multiPolygon) {

                        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                            org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) multiPolygon.getGeometryN(i);
                            Coordinate[] coordinates = polygon.getCoordinates();
                            List<Point2D> points = new ArrayList<>();
                            for (Coordinate coordinate : coordinates) {
                                points.add(new Point2D(coordinate.getY(), coordinate.getX()));//座標を格納する
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
    private void EEW(){
        List<String> colorList = new ArrayList<>();
        for (int i = 0; i < geometryPolygons.get(1).size(); i++) {
            String color = "map";
            if (Objects.equals(Names.get(2).get(i), "滋賀県南部")) {
                color = "s70";
            }
            if (Objects.equals(Names.get(2).get(i), "滋賀県北部")) {
                color = "s60";
            }
            colorList.add(color);
        }
        Colors.set(0, colorList);
        drawJapanPolygons();
    }
    private void addColor() {
        Colors.clear();
        List<String> colorList = new ArrayList<>();
        for (int i = 0; i < geometryPolygons.get(1).size(); i++) {
            colorList.add("map");
        }
        Colors.add(colorList);

        colorList = new ArrayList<>();
        for (int i = 0; i < geometryPolygons.get(3).size(); i++) {
            String tsunamiArray = "愛知県外海 伊勢・三河湾 大阪府 愛媛県瀬戸内 静岡県 東京湾内湾";
            if (tsunamiArray.contains(Names.get(4).get(i))) {
                colorList.add("tsunami-3");
            }else{
                colorList.add("clear");
            }
        }
        Colors.add(colorList);

        List<Point2D> pointList = new ArrayList<>();
        colorList = new ArrayList<>();
        JsonNode node = Objects.requireNonNull(GetQuake.getQuakeInfoList("https://api.p2pquake.net/v2/history?codes=551&limit=100"));

        for (int i=node.size()-1; -1<i; i--){
            long timestamp = 0;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                Date date = sdf.parse(node.at("/" + i + "/earthquake/time").asText());

                // Dateオブジェクトをミリ秒単位のUNIXタイムスタンプに変換
                timestamp = date.getTime()/1000;
            }catch (ParseException e){
                e.printStackTrace();
            }
            if (!quakeIds.contains(timestamp)){
                Pane f = new Pane();
                Rectangle shindo = new Rectangle(40, 50);
                shindo.getStyleClass().add("shindo");
                shindo.getStyleClass().add("s" + node.at("/" + i + "/earthquake/maxScale").asInt());
                shindo.setX(2);
                Label shingen = new Label(node.at("/" + i + "/earthquake/hypocenter/name").asText());
                shingen.getStyleClass().add("text");
                shingen.setLayoutX(45);
                f.getChildren().addAll(shingen, shindo);
                String d = node.at("/" + i + "/earthquake/hypocenter/depth").asText();
                if (Objects.equals(d, "0")) {
                    d = "ごく浅い";
                } else {
                    d += "km";
                }
                shingen = new Label(d);
                shingen.getStyleClass().add("text");
                shingen.setLayoutX(45);
                shingen.setLayoutY(20);
                f.getChildren().add(shingen);
                shingen = new Label("M" + node.at("/" + i + "/earthquake/hypocenter/magnitude").asText());
                shingen.getStyleClass().add("magnitude");
                shingen.setLayoutX(113);
                shingen.setLayoutY(5);
                f.getChildren().add(shingen);
                RightBox.getChildren().add(0,f);
                if (!node.at("/" + i + "/issue/type").asText().equals("DetailScale")) {
                    quakeIds.add(timestamp);
                }
            }
        }
        List<String> typesList = new ArrayList<>();
        int dataIndex = 0;
        List<JsonNode> nodes = sortJson(node.at("/" + dataIndex + "/points"));
        for (JsonNode nodeaddr : Objects.requireNonNull(nodes)) {
            String type = nodeaddr.at("/addr").asText();
            colorList.add("s"+nodeaddr.at("/scale").asInt());
            int index = Names.get(5).indexOf(type);
            pointList.add(Points.get(1).get(index));
            typesList.add(type);
        }
        Names.set(5, typesList);
        Colors.add(colorList);
        Points.set(1, pointList);

        colorList = new ArrayList<>();
        for (int i = 0; i < geometryPolygons.get(2).size(); i++) {
            String EEW = "滋賀県 大阪府 京都府 兵庫県 三重県 和歌山県 奈良県 愛知県 福井県 香川県 徳島県 岐阜県";
            if (EEW.contains(Names.get(3).get(i))) {
                colorList.add("eew");
            } else {
                colorList.add("non-eew");
            }
        }
        Colors.add(colorList);
        for (int i = 0; i < 4; i++) {
            List<Double> object = new ArrayList<>();
            for (Point2D ignored : kyoshinImages) {
                object.add(0d);
            }
            Scales.add(object);
        }
    }

    private void setKyoshinColor() {
        BufferedImage kMoni = getKyoshinMonitor(-timeDelay);
        if (kMoni != null) {
            BufferedImage eew = getPredict(0);
            List<Double> intensity = new ArrayList<>();
            List<Double> realIntensity = new ArrayList<>();
            List<Double> preEEW = new ArrayList<>();
            for (int i = 0; i < kyoshinImages.size(); i++) {
                Point2D point = kyoshinImages.get(i);
                double obj = ConvertColorToScale(pickColor(Objects.requireNonNull(kMoni), (int) point.getX(), (int) point.getY()));
                double noneCount;
                preEEW.add(ConvertColorToScale(pickColor(Objects.requireNonNull(eew), (int) point.getX(), (int) point.getY())));
                if (obj == -1) {
                    noneCount = Scales.get(2).get(i) + 1;
                    if (noneCount < 3) {
                        obj = Scales.get(0).get(i);
                    }
                } else {
                    noneCount = 0;
                }
                intensity.add(obj);
                realIntensity.add(noneCount);
            }
            Scales.set(0, intensity);
            Scales.set(1, utils.sortedIndex(intensity));
            Scales.set(2, realIntensity);
            Scales.set(3, preEEW);
        }
    }

    private void addPoint(String filePath, String latitudePath, String longitudePath, String namePath, String[] otherPath) {
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
        for (int i = 0; i < jsonNode.size(); i++) {
            if (!Objects.equals(jsonNode.at("/" + i + "/Point").asText(), "null")) {
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
    どこかをいじると必ずバグります
    いじる場合は自己責任で...
     */
    private void handleScroll(ScrollEvent event) {
        double scrollDeltaY = event.getDeltaY() / 32;

        // ホイール操作による拡大縮小
        double zoomDelta = 1 + Math.abs(scrollDeltaY) * 0.5;
        int maxZoom = 1000;
        double minZoom = Math.max(windowWidth / mapSizeX, windowHeight / mapSizeY);

        if (scrollDeltaY > 0) {
            zoomFactor *= zoomDelta;
            if (zoomFactor * zoomDelta > maxZoom) {
                zoomDelta *= maxZoom / zoomFactor;
                zoomFactor = maxZoom;
            }
        } else {
            zoomFactor /= zoomDelta;
            if (zoomFactor / zoomDelta < minZoom) {
                zoomDelta /= minZoom / zoomFactor;
                zoomFactor = minZoom;
            }
        }

        Point2D coordinate = new Point2D(event.getX() - windowWidth / 2, event.getY() - windowHeight / 2);
        // カメラの移動量を計算
        double cameraTranslateX, cameraTranslateY;
        if (scrollDeltaY > 0) {
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
        EEW();
    }

    private void translateMapping() {
        double translateX = map.getTranslateX() / zoomFactor;
        double translateY = map.getTranslateY() / zoomFactor;
        double windowX = windowWidth / 2;
        double windowY = windowHeight / 2;
        float mapX = mapSizeX / 2f;
        float mapY = mapSizeY / 2f;
        if (Math.abs(translateX) > mapX - windowX / zoomFactor) {
            if (translateX > 0) {
                map.setTranslateX(mapX * zoomFactor - windowX);
            } else {
                map.setTranslateX(-mapX * zoomFactor + windowX);
            }
            //if (Math.abs(map.getTranslateX() / zoomFactor) > mapX - windowX / zoomFactor){zoomMapping();}
        }
        if (Math.abs(map.getTranslateX() - scroll.getX()) > windowWidth / 2 * roughness || Math.abs(map.getTranslateY() - scroll.getY()) > windowHeight / 2 * roughness) {
            redrawPolygons();
        }//スクロールしすぎたときに再描画する
        if (Math.abs(translateY) > mapY - windowY / zoomFactor) {
            if (translateY > 0) {
                map.setTranslateY(mapY * zoomFactor - windowY);
            } else {
                map.setTranslateY(-mapY * zoomFactor + windowY);
            }
            if (Math.abs(map.getTranslateY() / zoomFactor) > mapY - windowY / zoomFactor) {
                zoomMapping();
            }
        }
    }

    private void redrawPolygons() {
        world.getChildren().clear();
        drawTsunami();
        drawPolygons();
        changeKyoshinMonitor();
        drawQuakeInfo();
    }


    private void zoomMapping() {
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
