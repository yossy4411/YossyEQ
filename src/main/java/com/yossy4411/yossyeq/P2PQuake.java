package com.yossy4411.yossyeq;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PQuake {
    private static int peerId = -1;
    private static final Logger LOGGER = Logger.getLogger(P2PQuake.class.getName());
    private static final List<Integer> ConnectedPeers = new ArrayList<>();
    private static List<String> Keys = new ArrayList<>();
    private static LocalDateTime KeyEffective = LocalDateTime.now();
    private static final List<String> sendData = new ArrayList<>();
    private static int i = 0;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static void main(String[] args) throws IOException {
        LOGGER.setLevel(Level.INFO);
        connectToServer();
        scheduler.scheduleAtFixedRate(P2PQuake::serverEcho, 10, 10, TimeUnit.MINUTES);
    }
    private static void serverEcho() {
        if (i > 0) {
            String[] allAddress = {"p2pquake.ddo.jp", "www.p2pquake.net", "p2pquake.info", "p2pquake.xyz"};
            String ipAddress = allAddress[new Random().nextInt(allAddress.length)]; // ランダムにIPアドレスを選択
            int port = 6910; // 接続先のポート番号

            try (Socket socket = new Socket(ipAddress, port);
                 OutputStream outputStream = socket.getOutputStream();
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, Charset.forName("SHIFT-JIS")), true);
                 InputStream inputStream = socket.getInputStream();
                 BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("SHIFT-JIS")))) {

                // サーバーからの応答を受信
                String response;
                while ((response = in.readLine()) != null) {
                    String[] parts = response.split(" ");
                    int code = Integer.parseInt(response.split(" ")[0]);
                    if (code == 211) {
                        out.println("131 1 0.32:YossyEQ:0.0.1a");
                    } else if (code == 212) {
                        out.println("123 1 " + peerId + ":" + ConnectedPeers.size());
                    } else if (code == 243) {
                        if (ConnectedPeers.size() < 2) {
                            String[] peers = parts[2].split(":");
                            for (String peer : peers) {
                                String[] peerData = peer.split(",");
                                CompletableFuture.runAsync(() -> {
                                    connectToPeer(peerData[0], Integer.parseInt(peerData[1]), Integer.parseInt(peerData[2])); // 任意のアドレスとポートを指定
                                });
                            }
                            StringBuilder str = new StringBuilder();
                            try {
                                Thread.sleep(5000);

                                for (int p : ConnectedPeers) {
                                    str.append(p).append(":");
                                }
                                if (str.length() == 0) {
                                    System.out.println("どのピアとも接続できませんでした。");
                                } else {
                                    str.deleteCharAt(str.length() - 1);
                                }
                                out.println("155 1 " + str);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (Keys == null) {
                            throw (new RuntimeException("鍵が取得できませんでした。しばらく時間をおいてからもう一度お試しください。")) ;
                        } else if (KeyEffective.plus(30, ChronoUnit.MINUTES).isBefore(LocalDateTime.now())){
                            out.println("124 1 "+peerId +":"+ Keys.get(0));
                        }

                    } else if (code == 237 || code == 244) {
                        try {
                            Keys.add(parts[2].split(":")[0]);
                            Keys.add(parts[2].split(":")[2]);
                            Keys.add(parts[2].split(":")[3]);
                            KeyEffective = LocalDateTime.parse(parts[2].split(":")[1], DateTimeFormatter.ofPattern("yyyy/MM/dd HH-mm-ss"));
                            saveKey(Keys,KeyEffective);
                        } catch (ArrayIndexOutOfBoundsException ignore) {
                            readKey();
                        }
                        out.println("119 1");
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        i++;

    }
    private static void connectToPeer(String address, int port, int peerID) {
        LocalDateTime receivedTime = LocalDateTime.now();
        try (Socket socket = new Socket(address, port);

             OutputStream outputStream = socket.getOutputStream();
             PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, Charset.forName("SHIFT-JIS")), true);
             InputStream inputStream = socket.getInputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("SHIFT-JIS")))) {
            int n= 0;
            // ピアからの応答を受信

            String response;
            while ((response = in.readLine()) != null) {

                System.out.println(response + "をピアから受信しました。(" + peerID + ")");

                if (n == 0) {
                    ConnectedPeers.add(peerID);
                    CompletableFuture.runAsync(() -> {
                        try {
                            relay(peerID, out);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    n = 1;
                }
                if(receivedTime.plus(20, ChronoUnit.MINUTES).isBefore(LocalDateTime.now())){
                    socket.close();
                    ConnectedPeers.remove((Integer) peerID);
                    System.out.println("ピアとの接続が確認できなかったため、" + peerID + "への接続を切断しました。");
                }
                receivedTime = LocalDateTime.now();
                int code = Integer.parseInt(response.split(" ")[0]);
                if (code == 614) {
                    out.println("634 1 0.32:YossyEQ:0.0.1a");
                    System.out.println("ピアにシステムバージョンを送信しました。");
                } else if (code == 612) {
                    out.println("632 1" + peerId);
                    System.out.println("ピアにIDを送信しました。");
                } else if (code == 611) {
                    out.println("631 1");
                    System.out.println("ピアとのエコーを実施しました。");
                } else if (code == 561||code == 555||code == 615||code == 551||code == 552) {
                    relayOther(response, peerID);
                }

            }
        } catch (IOException ignored) {}
    }
    static void relayOther(String data, int peerID){
        sendData.clear();
        sendData.add(String.valueOf(peerID));
        sendData.add(data);
        String buffer = data.split(" ")[2];
        sendData.add(buffer);
    }
    private static void relay(int peerID,PrintWriter out) throws IOException {
        List<String> buffers = new ArrayList<>();
        List<Long> bufferTime = new ArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            System.out.println("ピアへの送信の準備が完了しました。");
            boolean error = false;
            while (!error) {
                try {
                    if (!sendData.isEmpty()) {
                        if (!buffers.contains(sendData.get(2))) {
                            buffers.add(sendData.get(2));
                            bufferTime.add(System.currentTimeMillis());
                            if (Integer.parseInt(sendData.get(0)) != peerID) {
                                out.println(sendData.get(1));
                                LOGGER.info(sendData.get(1) + "を" + peerID + "に送信しました。");
                            }
                            for(int i = 0; i < bufferTime.size(); i++){
                                long t = bufferTime.get(i);
                                if (System.currentTimeMillis() - t > 10000){
                                    bufferTime.remove(i);
                                    buffers.remove(i);

                                }
                            }
                        }
                    }
                    Thread.sleep(10); // 適宜適切な待ち時間を設定する
                } catch (Exception e) {
                    System.err.println("エラーが発生しました：" + e.getMessage());
                    error = true;
                }
            }
        });
    }
    private static void connectToServer(){
        String[] allAddress = {"p2pquake.ddo.jp", "www.p2pquake.net", "p2pquake.info", "p2pquake.xyz"};
        String ipAddress = allAddress[new Random().nextInt(allAddress.length)]; // ランダムにIPアドレスを選択
        int port = 6910; // 接続先のポート番号


        try (Socket socket = new Socket(ipAddress, port);
             OutputStream outputStream = socket.getOutputStream();
             PrintWriter out = new PrintWriter(new OutputStreamWriter(outputStream, Charset.forName("SHIFT-JIS")), true);
             InputStream inputStream = socket.getInputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("SHIFT-JIS")))) {

            // サーバーからの応答を受信
            String response;
            int progress = 0;
            boolean portHeld = false;
            int CanUsePort;
            while ((response = in.readLine()) != null) {
                System.out.println("サーバーからの応答：" + response);

                // レスポンスのコードを抽出
                String[] parts = response.split(" ");
                if (parts.length >= 1) {
                    int code = Integer.parseInt(parts[0]);

                    // コードが211の場合、特定のデータを送信
                    if (code == 211) {
                        out.println("131 1 0.32:YossyEQ:Beta");
                        System.out.println("ソフトウェアバージョンを送信します。");
                        progress = 1;
                    }else if (progress == 1){
                        if(code == 212){
                            System.out.println("認証が完了しました。ピアIDを取得します。");
                            out.println( "113 1");
                            progress = 2;
                        } else {
                            System.out.println("プロコトルのバージョンが非対応なようです。\n" +
                                    "ソフトウェアを更新してください。Error1");
                        }
                    }else if (progress == 2) {
                        if (code == 233){
                            peerId = Integer.parseInt(parts[2]);
                            CanUsePort = 6911;
                            System.out.println("ピアIDが発行されました:" + peerId +
                                    "\n" + CanUsePort + "番ポートの開放状態を調べています。");
                            out.println("114 1 " + peerId + ":" + CanUsePort);
                            progress = 3;
                        }else{System.out.println("ピアIDを取得できませんでした。Error P-1");}
                    }else if(progress == 3){
                        if (Integer.parseInt(parts[2]) == 1){
                            System.out.println("ポートは開放されています。");
                            portHeld = true;
                        }else {
                            System.out.println("ポートが開放されていません。\n詳しくはこちら:https://github.io");
                        }
                        progress = 4;
                        out.println( "115 1 " + peerId);
                    }else if(progress == 4){
                        String[] peers = parts[2].split(":");
                        for (String peer : peers) {
                            String[] peerData = peer.split(",");
                            CompletableFuture.runAsync(() -> {
                                connectToPeer(peerData[0], Integer.parseInt(peerData[1]), Integer.parseInt(peerData[2])); // 任意のアドレスとポートを指定
                            });
                        }
                        StringBuilder str = new StringBuilder();
                        try {
                            Thread.sleep(5000);

                            for (int p : ConnectedPeers) {
                                str.append(p).append(":");
                            }
                            if (str.length() == 0){System.out.println("どのピアとも接続できませんでした。");}
                            else {str.deleteCharAt(str.length() -1);}
                            out.println("155 1 "+str);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("ピアとの接続が完了しました。接続ピア:"+str);
                        out.println("116 1 " + peerId + ":6911:501:"+ConnectedPeers.size()+":0");
                        progress = 5;
                    }else if (progress == 5){
                        System.out.println("現在のピア総数:" + parts[2]);
                        out.println("117 1 " + peerId);
                        progress = 6;
                    }else if (progress == 6){
                        if(code == 237){
                            String[] key =parts[2].split(":");
                            Keys.clear();
                            Keys.add(key[0]);
                            Keys.add(key[1]);
                            key =parts[3].split(":");
                            Keys.add(key[1]);
                            KeyEffective = LocalDateTime.parse(parts[2].split(":")[2] +" "+ key[0], DateTimeFormatter.ofPattern("yyyy/MM/dd HH-mm-ss"));
                            saveKey(Keys,KeyEffective);
                        }else{readKey();System.out.println("鍵は既に割り当て済みのようです。");}

                        progress = 7;
                        out.println("127 1");
                    }
                    else if (progress == 7){
                        String[] areaPeers = parts[2].split(";");
                        out.println("119 1");
                        progress = 8;
                    }else if (progress == 8){
                        System.out.println("P2Pネットワークへの接続を確立しました。");
                        progress = 9;
                    }
                }
                if (Integer.parseInt(parts[0]) == 293){System.out.println("引数に間違いがあります。");}
            }
        } catch (IOException e) {
            System.err.println("エラーが発生しました: " + e.getMessage());
        }
    }
    private static void saveKey(List<String> Data, LocalDateTime time){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/com/yossy4411/yossyeq/keys.txt"))) {
            for (String data : Data) {
                writer.write(data);
                writer.newLine(); // 改行でデータを区切る
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH-mm-ss");
            writer.write(time.format(formatter));
            System.out.println("データをファイルに書き込みました。");
        } catch (IOException e) {
            System.err.println("ファイルの書き込みエラー: " + e.getMessage());
        }
    }
    private static void readKey (){
        System.out.println("データをファイルから読み込みました。");
        if (Keys == null){Keys = new ArrayList<>();}else{Keys.clear();}

        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/com/yossy4411/yossyeq/keys.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Keys.add(line); // 1行ずつ読み込んでリストに追加
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH-mm-ss");
            KeyEffective = LocalDateTime.parse(Keys.get(Keys.size() -1), formatter);
            Keys.remove(Keys.size() -1);

        } catch (IOException e) {
            System.err.println("ファイルの読み込みエラー: " + e.getMessage());
        }
    }
}