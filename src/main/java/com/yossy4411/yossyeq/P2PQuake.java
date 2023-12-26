package com.yossy4411.yossyeq;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.opengis.metadata.identification.CharacterSet.SHIFT_JIS;

public class P2PQuake{
    private final Thread serverThread;
    private final Server server;

    private class Server implements Runnable{
        private final String[] serverURLs;
        protected int availablePort = -1;
        private final int tryIndex;
        private final int regionCode = 501;
        protected Consumer<P2PMessage> onMessage;
        protected Consumer<Exception> onError;
        private final int timeout = 3000;
        Socket serverSocket;
        private Server(String[] serverURLs, int tryURL){
            this.serverURLs = serverURLs;
            tryIndex = tryURL;
        }
        @Override
        public void run() {
            try {
                serverSocket = new Socket();
                serverSocket.connect(new InetSocketAddress(serverURLs[tryIndex], 6910), timeout);
            } catch (IOException e) {
                boolean connected = false;
                for (int i = 0; i < serverURLs.length; i++) {
                    if (i != tryIndex) {
                        if (TryConnectToServer(serverURLs[i])) {
                            connected = true;
                            break;
                        }
                    }
                }
                if (!connected) {
                    if (onError != null) onError.accept(e);

                }

            }

            String[][] reply = new String[][]{{"211 1", "131 1 0.36:yossyEQ:Beta0.0.1"}, {"292", "119 1"}, {"236","117 1 "}, {"237","127 1"}, {"295","127 1"}, {"247","118 1"},{"238","119 1"}};
            List<Object[]> peers = new ArrayList<>();
            try {

                BufferedReader reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), SHIFT_JIS.toCharset()));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(server.serverSocket.getOutputStream(), SHIFT_JIS.toCharset()), true);
                int peerId = 0;
                while (serverSocket.isConnected()) {
                    try {
                        String message = reader.readLine();
                        P2PMessage parsedMessage = new P2PMessage(message==null?new String[0]:message.split(" "));
                        if (onMessage != null) {
                            onMessage.accept(parsedMessage);
                        }

                        String response = Arrays.stream(reply)
                                .filter(row -> row[0].contains(" ")?row.length >= 2 && Objects.requireNonNull(message).equals(row[0]):parsedMessage.getCode()==Integer.parseInt(row[0]))
                                .findFirst()
                                .map(row -> row[1])
                                .orElse(null);
                        if (response != null) {
                            writer.println(response);
                        }else if (parsedMessage.getCode()==212){
                            if (Double.parseDouble(parsedMessage.getData()[0]) > 0.36) {
                                writer.println("192 1");
                                serverSocket.close();
                            }
                            else writer.println("113 1");
                        } else if (parsedMessage.getCode()==233) {
                            peerId = Integer.parseInt((parsedMessage.getData()[0]));
                            writer.println(availablePort == -1?"115 1 "+peerId:"114 1 "+peerId+":"+ availablePort);
                        } else if (parsedMessage.getCode()==234&&parsedMessage.getData()[0].equals("0")) {
                             availablePort = -1;writer.println("115 1 "+peerId);
                        } else if (parsedMessage.getCode()==239){
                            serverSocket.close();
                        } else if (parsedMessage.getCode()==235){
                            String[] data = parsedMessage.getData();
                            for (int i = 0; i < data.length; i++) {
                                Socket peerSocket = new Socket();
                                try {
                                    peerSocket = new Socket();
                                    String[] address = data[i].split(",");
                                    peerSocket.connect(new InetSocketAddress(address[0], Integer.parseInt(address[1])), 5000);
                                    Object[] obj = new Object[]{peerSocket, address[2],
                                            new BufferedReader(new InputStreamReader(peerSocket.getInputStream(), SHIFT_JIS.toCharset())),
                                            new PrintWriter(new OutputStreamWriter(peerSocket.getOutputStream(), SHIFT_JIS.toCharset()), true)};
                                    if(peerSocket.isConnected()) peers.add(obj);


                                } catch (IOException e) {
                                    System.out.println("connection failed to peer " + i);
                                    peerSocket.close();
                                }
                            }
                            StringBuilder send = new StringBuilder("155 1 ");
                            for (Object[] peer :peers){
                                send.append(peer[1].toString()).append(":");
                            }
                            send.deleteCharAt(send.length()-1);
                            writer.println(send);
                            writer.println( "116 1 "+peerId+":"+(availablePort==-1?"6911":availablePort)+":"+ regionCode +":"+ peers.size() +((availablePort==-1)?":0":"10"));
                            reply[2][1] += peerId;

                        }else if (parsedMessage.getCode()==237){
                            parsedMessage.getData();
                        }

                    }catch (IOException e){
                        break;
                    }
                }
                System.out.println("Connection Closed");
                while (true){
                    try {
                        PeerConversation(peers);
                    }catch (Exception e){
                        break;
                    }

                }
            } catch (IOException e) {
                if (onError != null) onError.accept(e);
            }
        }
        private void PeerConversation(List<Object[]> peers){
            try {
                for (Object[] peer : peers) {
                    Socket peersocket = (Socket)peer[0];
                    BufferedReader reader = (BufferedReader) peer[2];
                    PrintWriter writer = (PrintWriter) peer[3];
                    P2PMessage a = new P2PMessage(reader.readLine().split(" "));
                    if (onMessage != null) onMessage.accept(a);
                }
            }catch (IOException e) {
                e.printStackTrace();
                if (onError != null) onError.accept(e);
            };
        }
        private boolean TryConnectToServer(String url){
            try {
                serverSocket = new Socket();
                serverSocket.connect(new InetSocketAddress(url, 6910), timeout);
                return true;
            }catch (IOException e){
                return false;
            }
        }

        public void setPort(int i) {
            availablePort = i;
        }
    }
    public static class P2PMessage {
        private final String[] content;

        P2PMessage(String[] content) {
            this.content = content;
        }
        public String[] getData() {
            return content[2].split(":");
        }
        public int getCode() {
            return Integer.parseInt(content[0]);
        }
        public String[] getRaw() {
            return content;
        }
        
    }
    private P2PQuake(String[] serverUrls, int firstTry) {
        server = new Server(serverUrls, firstTry);
        serverThread = new Thread(server);
    }
    public P2PQuake(){
        this(new String[]{"p2pquake.info", "www.p2pquake.net", "p2pquake.xyz", "p2pquake.ddo.jp"},1);
    }
    public void setOnMassage(Consumer<P2PMessage> message){
        server.onMessage = message;
    }
    public void connect(){
        serverThread.start();
    }
    public void setPort(int a){
        server.setPort(a);
    }
}

