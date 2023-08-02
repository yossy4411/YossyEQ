package com.yossy4411.yossyeq.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KyoshinMonitor {
    public static void main(String[] args) {
        String urlStr = "https://www.lmoni.bosai.go.jp/monitor/webservice/hypo/eew/20230729193455.json";

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
                System.out.println(responseBody);
            } else {
                System.out.println("Error: " + statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
