package com.yossy4411.yossyeq;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import org.json.JSONArray;
import com.jayway.jsonpath.JsonPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class Map extends Application{
    private double windowWidth;
    private double windowHeight;
    private double zoomFactor;
    private List<List<List<Point2D>>> geometryPolygons = new ArrayList<>();
    private Group root;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) throws IOException {

        windowWidth = 800;//ウィンドウ横幅
        windowHeight = 600;//ウィンドウ縦幅
        calculatePolygons("src/main/resources/com/yossy4411/yossyeq/World.geojson", "src/main/resources/com/yossy4411/yossyeq/World_Geo.json");

        root = new Group();
        Scene scene = new Scene(root, windowWidth, windowHeight);

        drawPolygons();

        scene.setOnScroll(this::handleScroll);
        scene.setFill(Color.WHITE);//背景色
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    private void drawPolygons() {
        for (List<Point2D> polygonPoints : geometryPolygons.get(0)){
            Polygon polygon = new Polygon();
            for (Point2D point : polygonPoints) {
                polygon.getPoints().addAll(point.getX(), point.getY());
            }
            // ポリゴンのスタイルを設定
            polygon.setFill(Color.DARKGRAY);
            polygon.setStroke(Color.BLACK);
            polygon.setStrokeWidth(1.0);
            root.getChildren().add(polygon);
        }
    }

    private void calculatePolygons(String GeometryPath, String arrayPath) throws IOException {
        List<List<Point2D>> polygons = new ArrayList<>();//ポリゴンを収納する
        Object data = parse(GeometryPath);
        Object array = parse(arrayPath);
        int countries = JsonPath.read(array, "$.count");//地物の数を取得
        for (int geometry = 0; geometry < countries; geometry ++){
            int groupCount = getGroupCount(array, geometry);//国の中の地形の数を取得
            for (int terrain = 0; terrain < groupCount; terrain ++){
                JSONArray positions = new JSONArray(getJson(data, geometry, terrain));//地形の座標数を取得
                List<Point2D> polygonPoints = new ArrayList<>();
                for (int position = 0; position < positions.length(); position++){
                    JSONArray point = positions.getJSONArray(position);
                    Point2D pos = convertLatLngToScreen(point.getDouble(1), point.getDouble(0));
                    polygonPoints.add(pos);
                }
                polygons.add(polygonPoints);
            }
        }
        geometryPolygons.add(polygons);
    }
    private void handleScroll(ScrollEvent event) {
        double scrollDeltaY = event.getDeltaY();

        // ホイール操作による拡大縮小
        double zoomDelta = 2;
        if (scrollDeltaY > 0) {
            zoomFactor *= zoomDelta;
        } else {
            zoomFactor /= zoomDelta;
        }


        root.setScaleX(zoomFactor);
        root.setScaleY(zoomFactor);
        redrawPolygons();
    }


    //ここから下はいじらないでください
    private List<Double> getJson(Object parsedJson, int geo, int array) { //JSONデータを取得する
        try {
            // JsonPathを使用してデータをクエリ
            String xCoordinatesPath = "$.features[" + geo + "].geometry.coordinates[" + array + "][0][*]";
            return JsonPath.read(parsedJson, xCoordinatesPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void redrawPolygons() {
        root.getChildren().clear();
        drawPolygons();
    }


    private int getGroupCount(Object parsedJson, int geo) {
        // JsonPathを使用してデータをクエリ
        String xCoordinatesPath = "$.features[" + geo + "]";
        return JsonPath.read(parsedJson, xCoordinatesPath);
    }

    // メルカトル図法による緯度経度から画面上の座標への変換
    private Point2D convertLatLngToScreen(double latitude, double longitude) {
        double mercatorY = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(latitude) / 2));

        // スクリーン上の座標に変換
        double centerX = windowWidth / 2;
        double centerY = windowHeight / 2;
        double screenX = longitude * windowWidth / 360 + centerX;
        double screenY = -(mercatorY * windowHeight / (2 * Math.PI)) + centerY;

        return new Point2D(screenX, screenY);
    }

    private Object parse(String filePath) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(filePath)));
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Object.class);
    }
}
