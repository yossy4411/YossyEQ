package com.yossy4411.yossyeq.test;

import com.yossy4411.yossyeq.P2PQuake;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class P2PQuakeExample {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        P2PQuake Quake = new P2PQuake();
        Quake.connect();
        Quake.setOnMassage((P2PQuake.P2PMessage a) -> System.out.println(Arrays.toString(a.getRaw())));
        Quake.setPort(6911);

    }


}
