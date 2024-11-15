package com.yossy4411.yossyeq.test;

import java.io.FileOutputStream;
import java.util.Base64;

public class KeySave {

    public static void main(String[] args) throws Exception {
        String key = "MIIBCwIBADANBgkqhkiG9w0BAQEFAASB9jCB8wIBAAIxAKRIrFUQLHMS1z8gJeV/yrQ6LSIklK4dzBrnjRbFs5aAGi+4blfDGhDypoW6G4nQ6QIDAQABAjAZyTO41k+5Gt1Lkebg3ZdvhSzNuHgt1uHlrEWMuq/7nhAcAFbOafxfbh1AGv4RIxECGQDnlzzB+alZ6sozEXGpNR5ix0mNkZEE4kMCGQC1mV3xtZdWBCXC1oAUs9/FqrL9EDKd22MCGQCvcXMlv+z7oVLsvRpOBV+vlSmzhcNG3+ECGFTzk/D4o/0HH6sPsQb+RlYvzl937lXORQIZAKFpiDpItHCTjY/gBTUmcMQz02qJps3qLQ==:MEwwDQYJKoZIhvcNAQEBBQADOwAwOAIxAKRIrFUQLHMS1z8gJeV/yrQ6LSIklK4dzBrnjRbFs5aAGi+4blfDGhDypoW6G4nQ6QIDAQAB:2023/11/21, 23-21-11:NeKI5wWKhdJeD3yYTQUXQHNykgjtWAnbj5CJAXdRJ2mdnX7omF7I36zDeecGBbrNyyJGFOxpSU1Ru0jxS+G9QM6yNuBTIr65rXbDRDaZF/fz/tk2r9eELIq0xqQcoXRgqFPmjQvhCTpk4lNUUHmwzDlwQGY5qZXdyl8OuVYBIQU=";
        String[] keys = key.split(":");
        byte[] privatekey = Base64.getDecoder().decode(keys[0]);
        byte[] publickey =  Base64.getDecoder().decode(keys[1]);
        saveBytesToFile(new byte[][]{publickey, privatekey});
    }
    private static void saveBytesToFile(byte[][] data) {
        try (FileOutputStream outputStream = new FileOutputStream("src/main/resources/com/yossy4411/yossyeq/key.dat")) {
            for (byte[] datum : data) {
                outputStream.write(datum);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
