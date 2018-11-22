package com.beyondscreen.mazecar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.beyond.app.SysUtils;
import com.beyondscreen.mazecar.common.Background;
import com.beyondscreen.mazecar.common.IntentBeyond;
import com.beyondscreen.mazecar.common.ResolutionUtil;

public class MainActivity extends Activity {
	static {
		try {
			if(ResolutionUtil.ROCKCHIP_CPU.equals(ResolutionUtil.getCpu())) {
				String lib = SysUtils.getNativeLibName();
				System.out.println("Try to loadLibrary " + lib + " in apk launcher");
				System.loadLibrary(lib);
				System.out.println("loadLibrary success");
			}
		} catch (UnsatisfiedLinkError e) {
			System.out.println(e.getMessage());
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_main);
		autoEnterGame();
	}

	/**
	 * 根据参数信息进入指定的游戏版本
	 * home: pro v2 v3 mini; edu:pro v2 v3 mini (无);  mobi
	 */
	private void autoEnterGame() {
		Intent intent = getIntent();
		if (intent != null) {
			String mapVersion = intent.getStringExtra(Background.MAP_VERSION);// （目前的可能的值：null,"map_v2","map_v3"）
			//test
			//mapVersion = Background.BG_V3_B3;
			Log.d("test", "mapVersion: "+mapVersion);
			if (mapVersion == null) {
				onStartGameResult(-1);
				finish();
				return;
			}
			String boardModel = ResolutionUtil.getBoardModel();
			String gameType = IntentBeyond.gameType;

			// gameType=Background.SysVersion.VER_EDU;

			Class<? extends Activity> gameEntrance = null;
			if (gameType.equals(Background.SysVersion.VER_HOME)) {
				if (Background.SysBoardModel.MODEL_PRO.equals(boardModel)) {
					// 旗舰-家庭包
				} else if (Background.SysBoardModel.MODEL_MINI
						.equals(boardModel)) {
					// mini-
					if (Background.BG_V3_B3.equals(mapVersion)) {
						gameEntrance = com.beyondscreen.mazecar.home.v3.car.MainActivity.class;
					}
				} else {
					if (Background.BG_V2.equals(mapVersion)) {
						// 标准-家庭v2包
					} else if (Background.BG_V3.equals(mapVersion)) {
						// 标准-家庭v3包
					}
				}
			} else if (gameType.equals(Background.SysVersion.VER_EDU)) {


			} else if (gameType.equals(Background.SysVersion.VER_MOBI)) {
				// 标准-教育-魔比包
			}
			if (gameEntrance!= null) {
				Intent gameIntent = new Intent(MainActivity.this, gameEntrance);
				startActivity(gameIntent);
				onStartGameResult(1);
			} else {
				onStartGameResult(-1);
			}
		}
	}

	/**
	 * 游戏启动是否成功
	 * @param result
	 * 1成功 -1失败
	 */
	private void onStartGameResult(int result) {
		Intent intent = new Intent();
		String packageName = getPackageName();
		if (packageName == null || packageName.length() == 0) {
			packageName = "com.beyondscreen.mazecar";
		}
		intent.setAction(IntentBeyond.ACTION_BROADCAST_START_FEEDBACK);
		intent.putExtra("pkg", packageName);// 游戏包名
		intent.putExtra("code", result);// 错误码，小于0表示游戏启动失败，大于0成功
		sendBroadcast(intent);
		Log.e("TagMerge", "TagMerge100:-----------sendBroadcast:"+packageName);
		if(result == -1) {
			System.exit(0);
		} else {
			finish();
		}
	}
}
