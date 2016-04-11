package scu.miomin.com.lrucachedemo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by 晓勇 on 2015/7/20 0020.
 */
public class MD5Tools {

    /**
     * @param key
     * @return
     * 对key进行MD5加密并返回加密过的散列值
     */
    public static String decodeString(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
