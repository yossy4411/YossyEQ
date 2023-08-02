package com.yossy4411.yossyeq.test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WriteListToFileExample {
    private static int a = 0;
    private static List<Integer> list= new ArrayList<>();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static void main(String[] args) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                relay();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        );
        scheduler.scheduleAtFixedRate(() -> list.add(1), 0, 1, TimeUnit.SECONDS);
    }
    private static void relay() throws InterruptedException {
        while (true) {
            try
            {
                System.out.println(list);
                Thread.sleep(5000);
            }catch (Exception e){System.err.println(e.getMessage());}
        }
    }
}
