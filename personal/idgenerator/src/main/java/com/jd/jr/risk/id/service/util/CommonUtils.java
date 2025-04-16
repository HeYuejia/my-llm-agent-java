package com.jd.jr.risk.id.service.util;


import java.util.Arrays;

public class CommonUtils {
    public static String[] SWITCH_ON_EXP = new String[]{"ON", "TRUE", "on", "true"};

    public static boolean isPropKeyOn(String key) {

        String prop = System.getProperty(key);

        if (Arrays.asList(SWITCH_ON_EXP).contains(prop)) {
            return true;
        }

        return false;
    }
}