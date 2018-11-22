package com.beyondscreen.mazecar.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

public class DTLogger {

	private static final File LOG_DIR = new File(Environment.getExternalStorageDirectory() + "/beiboard/log/");
	final static String TAG = "FILogger";
	private static boolean openlog = true;

	public static void d(String desc) {
		if (openlog)
			Log.d(TAG, desc);
	}
	
	public static void i(String desc) {
		if (openlog)
			Log.i(TAG, desc);
	}

	public static void e(String desc) {
		if (openlog)
			Log.e(TAG, desc);
	}

	public static void saveLog(String message) {
		if (openlog) {
			if (!LOG_DIR.exists()) {
				LOG_DIR.mkdirs();
			}
			File file = new File(LOG_DIR.getAbsolutePath(), "log_"
					+ System.currentTimeMillis() + ".txt");
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			FileOutputStream fOut = null;
			try {
				fOut = new FileOutputStream(file);
				fOut.write(message.getBytes());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				try {
					fOut.flush();
					fOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
