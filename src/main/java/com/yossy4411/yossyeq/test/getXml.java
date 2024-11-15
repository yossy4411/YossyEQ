package com.yossy4411.yossyeq.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class getXml {
    public static void main(String[] args) {
        try {
            // URLを作成
            String urlStr = "https://www.data.jma.go.jp/developer/xml/feed/eqvol.xml";
            URL url = new URL(urlStr);

            // HTTPS接続を行うための設定
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // レスポンスを受け取る
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            reader.close();

            // XMLを解析する
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(url.openStream());

            // <entry>の中から<title>が「震源・震度に関する情報」というものを取り出す
            NodeList entryList = doc.getElementsByTagName("entry");
            for (int i = 0; i < entryList.getLength(); i++) {
                Element entry = (Element) entryList.item(i);
                String title = entry.getElementsByTagName("title").item(0).getTextContent();
                if ("震源・震度に関する情報".equals(title)) {
                    // <id>要素の内容を取得して表示
                    String id = entry.getElementsByTagName("id").item(0).getTextContent();
                    System.out.println("Found entry with title: " + title);
                    System.out.println("Entry content: " + entry.getTextContent());
                    System.out.println("ID: " + id);

                    // 別のXMLファイルへのURLにリクエストを送信して、結果を表示
                    URL newUrl = new URL(id);
                    HttpURLConnection newConn = (HttpURLConnection) newUrl.openConnection();
                    newConn.setRequestMethod("GET");

                    BufferedReader newReader = new BufferedReader(new InputStreamReader(newConn.getInputStream()));
                    StringBuilder newResponseStr = new StringBuilder();
                    while ((line = newReader.readLine()) != null) {
                        newResponseStr.append(line);
                    }
                    newReader.close();

                    System.out.println("Response from the new URL:");
                    System.out.println(newResponseStr);

                    break; // 見つかったらループを終了
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
