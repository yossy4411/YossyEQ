package com.yossy4411.yossyeq;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.*;

public class NewAPP extends Application {

    private double scaleFactor = 1; // 初期拡大率
    private double offsetX, offsetY;
    private Group shape;
    private Scene scene;
    private final List<Group> Polygons = new ArrayList<>();
    private final List<List<String>> Names = new ArrayList<>();
    private double[] mapOffset = new double[]{0,0};
    private Transition transition;
    private final int[] MapScale = new int[]{900,800};//7:6
    HashMap<Point2D, UUID> NodeOfSegments = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        shape = new Group();

        Pane mapRoot =new StackPane(shape);
        mapRoot.setMaxSize(MapScale[0],MapScale[1]);
        Pane root = new StackPane(mapRoot);
        scene = new Scene(root, 1001.2, 700);

        calculatePolygon("src/main/resources/com/yossy4411/yossyeq/world/ne_10m_country.shp");
        calculatePolygon("src/main/resources/com/yossy4411/yossyeq/jp-report/地震情報／細分区域.shp");
        changeDetail(1f, 0);
        changeDetail(100f, 1);
        scene.widthProperty().addListener((obs,oldValue,newValue) -> {
            mapRoot.setTranslateX(Math.min(newValue.doubleValue(), MapScale[0]) / 2 * scaleFactor);
            System.out.println(Math.min(newValue.doubleValue(), MapScale[0]) / 2 * scaleFactor);
            //TransitionMapping(shape.getScaleX(), shape.getTranslateX(), shape.getTranslateY());
            //TODO:ウィンドウを小さくするときにずれる
        });

        scene.heightProperty().addListener((obs,oldValue,newValue) -> {
            mapRoot.setTranslateY(Math.min(newValue.doubleValue(), MapScale[1]) / 2 * scaleFactor);
            TransitionMapping(shape.getScaleX(), shape.getTranslateX(), shape.getTranslateY());
        });
        shape.scaleXProperty().addListener((obs, oldValue, newValue) -> {

            mapRoot.setTranslateX(Math.min(scene.getWidth(),MapScale[0]) /2*newValue.doubleValue());
            mapRoot.setTranslateY(Math.min(scene.getHeight(),MapScale[1])/2*newValue.doubleValue());
            TransitionMapping(newValue.doubleValue(), shape.getTranslateX(), shape.getTranslateY());
        });
        shape.setScaleX(scaleFactor);
        shape.setScaleY(scaleFactor);

        //TODO:ラグが発生します 移動後のスケール変更.

        shape.setOnScroll((ScrollEvent event) -> {
            if(!shape.isPressed()){
                double delta = event.getDeltaY() / 32;
                double zoomFactor = Math.pow(1.2, delta);

                scaleFactor *= zoomFactor;
                if (scaleFactor < scene.getWidth() / MapScale[0] || scaleFactor < scene.getHeight() / MapScale[1]) {
                    zoomFactor /= scaleFactor / Math.max(scene.getWidth() / MapScale[0], scene.getHeight() / MapScale[1]);
                    scaleFactor = Math.max(scene.getWidth() / MapScale[0], scene.getHeight() / MapScale[1]);

                }

                // アニメーションを使ってズーム
                animateZoom(shape, scaleFactor, zoomFactor, event.getX(), event.getY());
            }});
        // マウスドラッグでペインの移動
        shape.setOnMousePressed((MouseEvent event) -> {

            if (transition != null) {
                transition.stop();
                transition = null;
            }
            offsetX = event.getSceneX() - shape.getTranslateX();
            offsetY = event.getSceneY() - shape.getTranslateY();
        });

        shape.setOnMouseDragged((MouseEvent event) -> {
            System.out.println("translation");
            double newX = event.getSceneX() - offsetX;
            double newY = event.getSceneY() - offsetY;
            shape.setTranslateX(newX);
            shape.setTranslateY(newY);
            TransitionMapping(shape.getScaleX(), shape.getTranslateX(), shape.getTranslateY());
            System.out.println("translated");
        });

        scene.setFill(Color.gray(0.2));
        primaryStage.setTitle("Scale Pane Example");
        primaryStage.setScene(scene);
        primaryStage.show();
    }



    private void animateZoom(@NotNull Node pane, double scaleFactor, double delta, double mouseX, double mouseY) {
        if (transition != null) {
            transition.stop();
            transition = null;
        }
        double b = pane.getScaleX();
        delta *= scaleFactor/b/delta;
        double translateX = ((-mouseX)*b-pane.getTranslateX())*(delta-1);
        double translateY = ((-mouseY)*b-pane.getTranslateY())*(delta-1);
        ScaleTransition scaleTransition = new ScaleTransition    (Duration.millis(300), pane);
        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(300), pane);
//        pane.setScaleX(scaleFactor);
//        pane.setScaleY(scaleFactor);
//        pane.setTranslateX((pane.getTranslateX()) * delta + translateX);
//        pane.setTranslateY((pane.getTranslateY()) * delta + translateY);
        scaleTransition.setToX(scaleFactor);
        scaleTransition.setToY(scaleFactor);
        translateTransition.setToX((pane.getTranslateX()) * delta + translateX);
        translateTransition.setToY((pane.getTranslateY()) * delta + translateY);
        // マウス位置を考慮した移動アニメーションを作成
        ParallelTransition parallelTransition = new ParallelTransition(translateTransition, scaleTransition);
        // アニメーションの完了時に現在のアニメーションをクリア
        parallelTransition.setOnFinished(event -> transition = null);
        parallelTransition.setInterpolator(Interpolator.TANGENT(Duration.millis(50),20));


        parallelTransition.play();

        // 現在のアニメーションを保存
        transition = parallelTransition;
    }
    private void TransitionMapping(double scale, double translateX, double translateY){

        if (MapScale[0]*0.5*scale-Math.abs(translateX) <scene.getWidth()/2) {
            if (translateX > 0) {
                shape.setTranslateX(MapScale[0] * 0.5 * scale - scene.getWidth() / 2);
            } else {
                shape.setTranslateX(-MapScale[0] * 0.5 * scale + scene.getWidth() / 2);
            }

        }
        if (MapScale[1]*0.5*scale-Math.abs(translateY)<scene.getHeight()/2) {
            if(translateY > 0){
                shape.setTranslateY( MapScale[1]*0.5*scale-scene.getHeight()/2);
            }else{
                shape.setTranslateY(-MapScale[1]*0.5*scale+scene.getHeight()/2);
            }
        }
    }

    private void changeDetail(float LOD, int index) {
        if (index<0||index>=Polygons.size()){
            return;
        }
        Group g = Polygons.get(index);
        float i1 = 1 / LOD;
        HashMap<Point2D,Integer> table = new HashMap<>();
        for (Node pref:g.getChildren()){
            ObservableList<Node> children = ((Group) pref).getChildren();
            for (int j = 0; j < children.size(); j++) {
                Node node = children.get(j);
                Polygon polygon = (Polygon) node;
                Point2D d = new Point2D(polygon.getPoints().get(0), polygon.getPoints().get(1));
                int i = 2;
                int count = 0;
                while (i < polygon.getPoints().size()) {
                    Point2D point2D = new Point2D(polygon.getPoints().get(i), polygon.getPoints().get(i + 1));

                    boolean[] edge = {NodeOfSegments.containsKey(point2D), table.containsKey(point2D)};
                    if (edge[0] || (!(d.distance(point2D) < i1) || edge[1])) {
                        table.put(point2D, table.size());
                        Point2D delete = new Point2D(polygon.getPoints().get(i - 2), polygon.getPoints().get(i - 1));
                        if (!(NodeOfSegments.containsKey(delete)) && point2D.distance(delete) < i1) {
                            polygon.getPoints().remove(i - 2);
                            polygon.getPoints().remove(i - 2);
                            table.remove(delete);
                        } else {
                            i += 2;
                            count++;
                        }
                        d = point2D;
                    } else {
                        polygon.getPoints().remove(i);
                        polygon.getPoints().remove(i);
                    }
                }
                if (count < 3&&((Group) pref).getChildren().size()>1) {
                    children.remove(j);
                    j--;
                }
            }
        }
        shape.getChildren().add(g);
    }
    private void calculatePolygon(String filePath) throws IOException, ArrayIndexOutOfBoundsException {
        Map<Point2D,Integer> Array = new HashMap<>();
        boolean isJP = true;
        File file = new File(filePath);
        DataStore dataStore = FileDataStoreFinder.getDataStore(file);
        String[] typeNames = dataStore.getTypeNames();
        for (String typeName : typeNames) {
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            SimpleFeatureCollection collection = source.getFeatures();
            try (SimpleFeatureIterator iterator = collection.features()) {
                List<String> attribute = new ArrayList<>();
                Group state = new Group();
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    attribute.add((String) feature.getAttribute("name"));

                    Geometry geometry = (Geometry) feature.getDefaultGeometry();

                    Group pref = new Group();
                    if (geometry instanceof org.locationtech.jts.geom.Polygon) {
                        int index = -1;
                        org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) geometry;
                        Coordinate[] coordinates = polygon.getCoordinates();
                        Point2D buffer = null;
                        Polygon polygon1 = new Polygon();
                        for (Coordinate coordinate : coordinates) {
                            Point2D point = convertLatLngToScreen(coordinate.getY(), coordinate.getX());

                            if (Array.containsKey(point)) {//もし地物が被っているなら
                                int obj = Array.get(point);
                                if (index == -1) {
                                    NodeOfSegments.put(point, UUID.randomUUID());
                                } else if (obj != index) {
                                    NodeOfSegments.put(point, UUID.randomUUID());
                                }
                                buffer = point;
                                index = obj;
                            } else {
                                if (index != -1) {
                                    NodeOfSegments.put(buffer, UUID.randomUUID());
                                    index = -1;

                                }

                            }
                            Array.put(point, 0);
                            polygon1.getPoints().addAll(point.getX(), point.getY());//座標を格納する
                        }
                        polygon1.setStroke(Color.GRAY);
                        polygon1.setStrokeWidth(0.01);
                        polygon1.setFill(Color.WHITESMOKE);
                        pref.getChildren().add(polygon1);
                    } else if (geometry instanceof MultiPolygon multiPolygon) {

                        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                            int index = -1;

                            org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) multiPolygon.getGeometryN(i);
                            Coordinate[] coordinates = polygon.getCoordinates();
                            Polygon polygon1 = new Polygon();
                            Point2D buffer = null;
                            for (Coordinate coordinate : coordinates) {
                                Point2D point = convertLatLngToScreen(coordinate.getY(), coordinate.getX());

                                if (Array.containsKey(point)) {//もし地物が被っているなら
                                        int obj = Array.get(point);
                                        if (index == -1) {
                                            NodeOfSegments.put(point, UUID.randomUUID());
                                        } else if (obj != index) {
                                            NodeOfSegments.put(point, UUID.randomUUID());
                                        }
                                        buffer = point;
                                        index = obj;
                                } else {
                                    if (index != -1) {
                                        NodeOfSegments.put(buffer, UUID.randomUUID());
                                        index = -1;

                                    }

                                }
                                Array.put(point, i);
                                polygon1.getPoints().addAll(point.getX(), point.getY());//座標を格納する
                            }
                            polygon1.setStroke(Color.GRAY);
                            polygon1.setStrokeWidth(0.01);
                            polygon1.setFill(Color.WHITESMOKE);
                            pref.getChildren().add(polygon1);

                        }
                        state.getChildren().add(pref);
                    }
                }
                Polygons.add(state);
                if (isJP) {
                    Names.add(attribute);
                }
            }
        }
    }

    private Point2D convertLatLngToScreen(double latitude, double longitude) {
        double mercatorY = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(latitude) / 2));
        double screenX = longitude /360 * MapScale[0];
        double screenY = -(mercatorY * MapScale[1] / (2 * Math.PI));

        return new Point2D(screenX, screenY);
    }
}