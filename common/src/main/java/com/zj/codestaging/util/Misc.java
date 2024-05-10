package com.zj.codestaging.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Misc {
    public static String getCurrentDate(){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(cal.getTime());
    }
}
