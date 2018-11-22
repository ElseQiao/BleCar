package com.beyondscreen.mazecar.common;

import java.io.File;

/**
 * Created on 2017/8/10.
 */

public class ConfigPathUtil {

    public static final int SAMSUNG_4418=0x0001;
    public static final int ROCK_3218=0x0002;

    public static String getPath(){
        String sdPath="/storage/sdcard0/config";
        String dataPath="/data/config";
        File sdFile=new File(sdPath);
        File daFile=new File(dataPath);

        if (sdFile.exists()){
            return sdPath;
        }else if (daFile.exists()){
            return dataPath;
        }else {
            return null;
        }
    }



}
