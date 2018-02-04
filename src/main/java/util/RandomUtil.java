package main.java.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {
    private final static Random RAND = new Random(System.currentTimeMillis());
    
    private static String hash(String str){
        MessageDigest mdAlgorithm = null;
        try {
            mdAlgorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return str;
        }
        mdAlgorithm.update(str.getBytes());
        byte[] digest = mdAlgorithm.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            str = Integer.toHexString(0xFF & digest[i]);
            if (str.length() < 2) {
                str = "0" + str;
            }
            hexString.append(str);
        }
        return hexString.toString();
    }

    public static long getRandom(long max){
        return ThreadLocalRandom.current().nextLong(max);
    }
    
    public static String genRandomStr(){
        return hash(String.valueOf(RAND.nextLong()));
    }
}
