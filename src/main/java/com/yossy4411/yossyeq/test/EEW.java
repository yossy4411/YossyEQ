package com.yossy4411.yossyeq.test;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class EEW {
    public static void main(String[] args) {
        WebSocketClient cli2 = new WebSocketClient(URI.create("wss://telegram.projectbs.cn/jmaeewws")) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                System.out.println("Connected to server");
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        send("ping");
                    }
                },1000,60000);
            }

            @Override
            public void onMessage(String s) {
                System.out.println(s);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                System.out.println("Connection closed. Code: " + i + ", Reason: " + s);
            }

            @Override
            public void onError(Exception e) {
                System.err.println("Error occurred");
                e.printStackTrace();
            }

        };
        cli2.connect();
        WebSocketClient cli1 = new WebSocketClient(URI.create("wss://ws-api.wolfx.jp/jma_eew")) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                System.out.println("Connected to server");
            }

            @Override
            public void onMessage(String s) {
                System.out.println(s);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                System.out.println("Connection closed. Code: " + i + ", Reason: " + s);
            }

            @Override
            public void onError(Exception e) {
                System.err.println("Error occurred");
                e.printStackTrace();
            }

        };
        cli1.connect();
    }
}
