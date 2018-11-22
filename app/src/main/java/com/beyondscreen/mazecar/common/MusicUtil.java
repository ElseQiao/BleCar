package com.beyondscreen.mazecar.common;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class MusicUtil {
	/**
	 * IO����
	 * */
	public static void playMusic(Context context, MediaPlayer player,
			String fileName, boolean isLoop) {
		try {
			AssetManager assetManager = context.getAssets();
			AssetFileDescriptor fileDescriptor = assetManager.openFd(fileName);
			if (fileDescriptor == null) {
				return;
			}
			player.reset();
			player.setDataSource(fileDescriptor.getFileDescriptor(),
					fileDescriptor.getStartOffset(), fileDescriptor.getLength());
			fileDescriptor.close();
			player.setLooping(isLoop);
			player.prepare();
			player.setVolume(1.0f, 1.0f);
			player.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void initMusic(Context context, MediaPlayer player,
			String fileName, boolean isLoop) {
		try {
			AssetManager assetManager = context.getAssets();
			AssetFileDescriptor fileDescriptor = assetManager.openFd(fileName);
			if (fileDescriptor == null) {
				return;
			}
			player.reset();
			player.setDataSource(fileDescriptor.getFileDescriptor(),
					fileDescriptor.getStartOffset(), fileDescriptor.getLength());
			fileDescriptor.close();
			player.setLooping(isLoop);
			player.prepare();
			player.setVolume(1.0f, 1.0f);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public static int loadPool(Context context, SoundPool soundPool,String fileName) {
		try {
			AssetManager assetManager = context.getAssets();
			AssetFileDescriptor fileDescriptor = assetManager.openFd(fileName);
			if (fileDescriptor == null) {
				return -1;
			}
			int id=soundPool.load(fileDescriptor, 1);
			fileDescriptor.close();
			return id;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public static void releaseMusic(MediaPlayer player) {
		if (player != null) {
			player.release();
			player = null;
		}
	}

	public static void stopMusic(MediaPlayer player) {
		if (player != null && player.isPlaying()) {
			player.stop();
		}
	}
}
