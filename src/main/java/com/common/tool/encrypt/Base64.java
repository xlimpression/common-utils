package com.common.tool.encrypt;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Base64 {

    /**
     * decode
     */
    public static byte[] decode(String key) throws Exception {
        return (new BASE64Decoder()).decodeBuffer(key);
    }

    /**
     * encode
     */
    public static String encode(byte[] key) throws Exception {
        return (new BASE64Encoder()).encodeBuffer(key);
    }

}
