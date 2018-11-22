package com.beyondscreen.mazecar.common;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class ResolutionUtil {
    public final static String BY1="30x30";
    public final static String BY2="18x18";
    public final static String BY3="14x14";
    
    public final static String ROCKCHIP_CPU	= "rk3128";
    public final static String SAMSUNG_CPU	= "sp4418";


    //set SystemProperties as you want
 	 private final static String version = getProperty("ro.build.version.main");
 	 
 	 public static void setProperty(String key, String value) {    
         try {    
             Class<?> c = Class.forName("android.os.SystemProperties");  
             Method set = c.getMethod("set", String.class, String.class);
             set.invoke(c, key, value );
         } catch (Exception e) {
             Log.d("SysUtils", "setProperty====exception=");
             e.printStackTrace();
         }
     }

 	public static String getProperty(String key) {   
 		String value = "";
         try {
             Class<?> c = Class.forName("android.os.SystemProperties");  
             Method get = c.getMethod("get", String.class);
             value = (String)get.invoke(c, key);
         } catch (Exception e) {
             Log.d("SysUtils", "getProperty====exception=");
             e.printStackTrace();
         } 
 		return value;
     }
 	
    /**
     * ��ȡ����ķֱ��ʣ�30*30   18*18   14*14��
     * @return
     */
    public static String getSystemResolution() {
        try {
            if (version.contains("by1")) {
                return "30x30";
            } else if (version.contains("by2")) {
                return "18x18";
            } else if (version.contains("by3")) {
                return "14x14";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
            
        return "18x18";// default       
    }

    /**
     * ��ȡ���������(by1 or by2)
     * @return
     */
    public static String getBoardModel() {
        if (version.contains("by1")) {
            return "by1";
        } else if (version.contains("by2")) {
            return "by2";
        } else if (version.contains("by3")) {
            return "by3";
        }
        return "by2";//default
    }
    
    public static String getCpu() {
		String cpu = "";
		try {
			if (version.contains("3128")) {
				cpu = "rk3128";
			} else if (version.contains("4418")) {
				cpu = "sp4418";
			}			
		} catch (Exception e) {
			e.printStackTrace();			
			cpu = "";	
		}

		return cpu;// beyond_jni_rk3128_18x18
	}

    /**
     * ��ȡϵͳ�汾��
     * @return ϵͳ�汾��
     */
    public static String sysVersion() {
        int[] iversion={0, 0, 0};
        try {
//            String ver= SystemProperties.get("ro.build.version.main");
            Pattern pattern = Pattern.compile("by2_(\\d+)_v(\\d+).(\\d+).(\\d+)_(\\d+)\\.(\\d+)");
            Matcher matcher = pattern.matcher(version);
            if(matcher.find()){
            	iversion[0]= Integer.parseInt(matcher.group(2));
            	iversion[1]= Integer.parseInt(matcher.group(3));
            	iversion[2]= Integer.parseInt(matcher.group(4));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return iversion[0]+"."+iversion[1]+"."+iversion[2];
    }
    
    public static int sysIntVersion() {
        int[] iversion={0,0,0};
        try {
            Pattern pattern = Pattern.compile("by2_(\\d+)_v(\\d+).(\\d+).(\\d+)_(\\d+)\\.(\\d+)");
            Matcher matcher = pattern.matcher(version);
            if(matcher.find()){
                iversion[0]= Integer.parseInt(matcher.group(2));
                iversion[1]= Integer.parseInt(matcher.group(3));
                iversion[2]= Integer.parseInt(matcher.group(4));
            }

        }catch (Exception e){
//            e.printStackTrace();
        }
        return Integer.valueOf(iversion[0]+""+iversion[1]+""+iversion[2]).intValue();
    }
}
