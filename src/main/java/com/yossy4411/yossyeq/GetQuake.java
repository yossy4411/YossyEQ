package com.yossy4411.yossyeq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class GetQuake {
    public static JsonNode getQuakeInfoList(String urlStr){
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                String responseBody = response.body();
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    return objectMapper.readTree(responseBody);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static List<JsonNode> sortJson(JsonNode jsonData){

        try {
            // JSONデータをJSONNodeのリストに変換
            List<JsonNode> nodeList = new ArrayList<>();
            jsonData.forEach(nodeList::add);

            // scale順に並び替え
            nodeList.sort(Comparator.comparingInt(node -> node.get("scale").asInt()));

            // 並び替えた結果を表示
            return nodeList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static BufferedImage getKyoshinMonitor(long offsetMilli) {
        //引数"timestamp"には、巻き戻す時間をミリ秒形式で指定してください
        LocalDateTime now = LocalDateTime.now().minus(offsetMilli+260, ChronoUnit.MILLIS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedTime = now.format(formatter);
        formatter =  DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = now.format(formatter);
        try {
            return ImageIO.read(URI.create("http://www.kmoni.bosai.go.jp/data/map_img/RealTimeImg/jma_s/"+formattedDate+"/"+formattedTime+".jma_s.gif").toURL());
        } catch (IIOException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static BufferedImage getPredict(long offsetMilli) {
        //引数"timestamp"には、巻き戻す時間をミリ秒形式で指定してください
        LocalDateTime now = LocalDateTime.now().minus(offsetMilli, ChronoUnit.MILLIS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedTime = now.format(formatter);
        formatter =  DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = now.format(formatter);
        try {
            return ImageIO.read(URI.create("https://smi.lmoniexp.bosai.go.jp/data/map_img/EstShindoImg/eew/20220316/20220316233651.eew.gif").toURL());
        } catch (IIOException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static long waitforKmoni(long timedelay){
        int d = (int) timedelay;
        if(getKyoshinMonitor(d) == null){
            for (int i = 0; i < 40; i++) {
                d += 100;
                if (getKyoshinMonitor(d) != null) {
                    for (i = 0; i < 5; i++) {
                        d -=20;
                        if (getKyoshinMonitor(d)==null){
                            return d+20;
                        }
                    }
                }
            }
        }else {
            for (int i = 0; i < 40; i++) {
                d -= 100;
                if (getKyoshinMonitor(d) != null) {
                    for (i = 0; i < 5; i++) {
                        d +=20;
                        if (getKyoshinMonitor(d)==null){
                            return d-20;
                        }
                    }
                }
            }
        }
        return -1;
    }
    public static BufferedImage getPGA(long offsetMilli) {
        //引数"timestamp"には、巻き戻す時間をミリ秒形式で指定してください
        LocalDateTime now = LocalDateTime.now().minus(offsetMilli, ChronoUnit.MILLIS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedTime = now.format(formatter);
        formatter =  DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = now.format(formatter);
        try {
            return ImageIO.read(URI.create("https://smi.lmoniexp.bosai.go.jp/data/map_img/RealTimeImg/acmap_s/"+formattedDate+"/"+formattedTime+".acmap_s.gif").toURL());
        } catch (IIOException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static long getLatestTime(){
        //ミリ秒単位でインターネット上の時間を取得できます。
        NTPUDPClient client = new NTPUDPClient();
        try {
            client.open();
            InetAddress hostAddr = InetAddress.getByName("time.cloudflare.com"); // NTPサーバーのアドレス
            TimeInfo timeInfo = client.getTime(hostAddr);
            return timeInfo.getReturnTime();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            client.close();
        }
    }
    public static Color pickColor(BufferedImage image, int x, int y){
        int color = image.getRGB(x, y);
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        if (red + green + blue ==0){return null;}
        return Color.rgb(red,green,blue);
    }
    public static class utils {
        public static List<Double> sortedIndex(List<Double> list) {
            // ソート前のインデックスを保持するオブジェクトのリストを作成
            List<IndexedValue> indexedList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                indexedList.add(new utils.IndexedValue(i, list.get(i)));
            }

            // 値でソートするComparatorを定義
            Comparator<IndexedValue> valueComparator = Comparator.comparingDouble(iv -> iv.value);

            // オブジェクトのリストを値でソート
            indexedList.sort(valueComparator);

            List<Double> n = new ArrayList<>();
            for (IndexedValue iv : indexedList) {
                n.add((double) iv.index);
            }
            return n;
        }
        static class IndexedValue {
            int index; // ソート前のインデックス
            Double value; // 値

            IndexedValue(int index, Double value) {
                this.index = index;
                this.value = value;
            }
        }
        public static Point2D ConvertCoordinateToPosition(double latitude,double longitude){
            long x,y;
            if(latitude<30){
                x = Math.round(60.5+(longitude - 125.5)*20);
                y = Math.round(162+(25.6-latitude)*25.7);
            }else{
                x = Math.round(5+(longitude-128.8433)*20.393);
                y = Math.round(21+(45.4883-latitude)*24.582);
            }
            return new Point2D(x,y);
        }

    }

}
