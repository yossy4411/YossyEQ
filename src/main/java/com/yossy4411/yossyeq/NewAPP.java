package com.yossy4411.yossyeq;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Affine;
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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.*;
import java.util.stream.IntStream;

public class NewAPP extends Application {

    private double scaleFactor = 2; // 初期拡大率
    private double offsetX, offsetY;
    private Pane shape;
    private Scene scene;
    private double translateX,translateY;
    private final List<List<String>> Names = new ArrayList<>();
    private Transition translateAnimation;
    private final int[] MapScale = new int[]{900,800};//7:6
    List<List<List<List<Double>>>> buf = new ArrayList<>();
    HashMap<Point2D, UUID> NodeOfSegments = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) throws IOException {
        calculatePolygon("src/main/resources/com/yossy4411/yossyeq/world/ne_10m_country.shp");
        calculatePolygon("src/main/resources/com/yossy4411/yossyeq/jp-report/地震情報／細分区域.shp");
        List<List<List<Double[]>>> buffermap = new ArrayList<>();
        buffermap.add(changeDetail(1f, 0));
        buffermap.add(changeDetail(100f, 1));

        shape = new Pane();
        resetPane(shape,buffermap);
        //redraw(buf);
        Pane mapRoot =new StackPane(shape);
        shape.setMaxSize(MapScale[0],MapScale[1]);
        Pane root = new StackPane(mapRoot);
        scene = new Scene(root, 1001.2, 700);
        scene.widthProperty().addListener((obs,oldValue,newValue) -> {
            mapRoot.setTranslateX(Math.min(newValue.doubleValue(), MapScale[0]) / 2 * scaleFactor);
            System.out.println(Math.min(newValue.doubleValue(), MapScale[0]) / 2 * scaleFactor);
            TransitionMapping(shape.getScaleX(), shape.getTranslateX(), shape.getTranslateY());
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
                    scaleFactor = Math.max(scene.getWidth() / MapScale[0], scene.getHeight() / MapScale[1]);

                }
                double b = shape.getScaleX();
                delta *= scaleFactor/b/delta;
                // アニメーションを使ってズーム
                double translateX = ((-event.getX())*b-shape.getTranslateX())*(delta-1);
                double translateY = ((-event.getY())*b-shape.getTranslateY())*(delta-1);
                animateZoom(scaleFactor, shape.getTranslateX() * delta + translateX, shape.getTranslateY() * delta + translateY, 300);
            }});
        // マウスドラッグでペインの移動
        shape.setOnMousePressed((MouseEvent event) -> {

            if (translateAnimation != null) {
                translateAnimation.stop();
                translateAnimation = null;
            }
            offsetX = event.getSceneX() - shape.getTranslateX();
            offsetY = event.getSceneY() - shape.getTranslateY();
        });

        shape.setOnMouseDragged((MouseEvent event) -> {
            translateX = event.getSceneX() - offsetX;
            translateY = event.getSceneY() - offsetY;
            redraw(buffermap);
        });


        scene.setFill(Color.gray(0.2));
        primaryStage.setTitle("Scale Pane Example");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void resetPane(Pane shape, List<List<List<Double[]>>> buffermap) {
        shape.getChildren().clear();
        for(List<List<Double[]>> s:buffermap) {
            Group state = new Group();
            for (List<Double[]> t : s) {
                Group prefecture = new Group();
                for (Double[] r : t) {
                    Polygon polygon = new Polygon();
                    polygon.getPoints().addAll(r);
                    prefecture.getChildren().add(polygon);
                }
                state.getChildren().add(prefecture);
            }
            shape.getChildren().add(state);
        }
    }

    private void redraw(List<List<List<Double[]>>> buffer){
        for (int i2 = 0; i2 < buffer.size(); i2++) {
            List<List<Double[]>> a = buffer.get(i2);
            Group state = new Group();
            for (int j = 0; j < a.size(); j++) {
                List<Double[]> b = a.get(j);
                Group prefecture = new Group();
                for (int k = 0; k < b.size(); k++) {
                    Double[] c = b.get(k);
                    if (shape.getChildren().get(i2) instanceof Group s) if (s.getChildren().get(j) instanceof Group t) if (t.getChildren().get(k) instanceof Polygon r){
                        r.getPoints().setAll(IntStream.range(0, c.length)
                                .mapToObj(i -> i % 2 == 0 ? c[i] + translateX : c[i] + translateY)
                                .toArray(Double[]::new));
                    }
                }
                state.getChildren().add(prefecture);
            }
        }
    }

    private void animateZoom(double scaleFactor, double translateX, double translateY, int duration) {
        if (translateAnimation != null) {
            translateAnimation.stop();
            translateAnimation = null;
        }
        ScaleTransition scaleTransition = new ScaleTransition    (Duration.millis(duration), shape);
        TranslateTransition translateTransition = new TranslateTransition(Duration.millis(duration), shape);
        Affine a = new Affine();
        a.appendTranslation(translateX,translateY);
        a.appendScale(scaleFactor,scaleFactor);
        scaleTransition.setToX(scaleFactor);
        scaleTransition.setToY(scaleFactor);
        translateTransition.setToX(translateX);
        translateTransition.setToY(translateY);
        ParallelTransition parallelTransition = new ParallelTransition(translateTransition, scaleTransition);
        parallelTransition.setOnFinished(event -> translateAnimation = null);
        parallelTransition.setInterpolator(Interpolator.TANGENT(Duration.millis(50),20));
        parallelTransition.setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                return Math.sqrt(2*t);
            }
        });


        // 現在のアニメーションを保存
        translateAnimation = parallelTransition;
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

    private List<List<Double[]>> changeDetail(float LOD, int index) {
        if (index<0||index>=buf.size()){
            return null;
        }
        List<List<List<Double>>> g = buf.get(index);
        float i1 = 1 / LOD;
        HashMap<Point2D,Integer> table = new HashMap<>();
        List<List<Double[]>> res = new ArrayList<>();
        for (List<List<Double>> pref:g){
            List<Double[]> newPref = new ArrayList<>();
            for (int j = 0; j < pref.size(); j++) {
                List<Double> polygon = pref.get(j);
                Point2D d = new Point2D(polygon.get(0), polygon.get(1));
                int i = 2;
                int count = 0;
                while (i < polygon.size()) {
                    Point2D point2D = new Point2D(polygon.get(i), polygon.get(i + 1));

                    boolean[] edge = {NodeOfSegments.containsKey(point2D), table.containsKey(point2D)};
                    if (edge[0] || (!(d.distance(point2D) < i1) || edge[1])) {
                        table.put(point2D, table.size());
                        Point2D delete = new Point2D(polygon.get(i - 2), polygon.get(i - 1));
                        if (!(NodeOfSegments.containsKey(delete)) && point2D.distance(delete) < i1) {
                            polygon.remove(i - 2);
                            polygon.remove(i - 2);
                            table.remove(delete);
                        } else {
                            i += 2;
                            count++;
                        }
                        d = point2D;
                    } else {
                        polygon.remove(i);
                        polygon.remove(i);
                    }
                }
                newPref.add(polygon.toArray(new Double[0]));
                /*if (count < 3&&((Group) pref).getChildren().size()>1) {
                    children.remove(j);
                    j--;
                }*/
            }
            res.add(newPref);
        }
        return res;
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
                List<List<List<Double>>> geoGroup = new ArrayList<>();
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    attribute.add((String) feature.getAttribute("name"));

                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    List<List<Double>> prefecture = new ArrayList<>();
                    if (geometry instanceof org.locationtech.jts.geom.Polygon) {
                        int index = -1;
                        org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) geometry;
                        Coordinate[] coordinates = polygon.getCoordinates();
                        Point2D buffer = null;
                        List<Double> poly = new ArrayList<>();
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
                            poly.add(point.getX());//座標を格納する
                            poly.add(point.getY());//座標を格納する
                        }
                        prefecture.add(poly);
                    } else if (geometry instanceof MultiPolygon multiPolygon) {

                        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                            int index = -1;

                            org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) multiPolygon.getGeometryN(i);
                            Coordinate[] coordinates = polygon.getCoordinates();
                            List<Double> poly = new ArrayList<>();
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
                                poly.add(point.getX());
                                poly.add(point.getY());
                            }
                            prefecture.add(poly);

                        }
                        geoGroup.add(prefecture);
                    }
                }
                buf.add(geoGroup);
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
        if (screenY>MapScale[1]/2.0){screenY = MapScale[1]/2.0;}

        return new Point2D(screenX, screenY);
    }
}