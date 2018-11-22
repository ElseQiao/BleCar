package com.beyondscreen.mazecar.home.v3.car.utils;

import java.util.ArrayList;
import java.util.Arrays;

import com.beyondscreen.mazecar.common.LED;
import com.beyondscreen.mazecar.resource.Position;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class ThreadLight extends HandlerThread {
	/**捉怪兽灯效消息*/
	private final int LIGHT_MONSTER_CATCH = 0;
	/**彩灯一圈灯效消息*/
	private static final int LIGHT_ROUND = 1;
	/**捉怪兽灯效结束消息通知*/
	private static final int MONSTER_END = 2;
	/**怪兽路径连接动画*/
	private static final int LIGHT_ROAD_ANIMAL=3;

	/**捉怪兽灯效间隔*/
	private final int TIME_MONSTER_CATCH = 100;
	/**彩灯轮转默认间隔*/
	private static final int ROUND_INTERVALTIME =20;

	public ThreadLight(String name) {
		super(name);
	}

	private Handler handler;
	@Override
	protected void onLooperPrepared() {
		handler = new Handler(getLooper()) {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
					case LIGHT_MONSTER_CATCH:
						Log.e("test", "LIGHT_MONSTER_CATCH----------"+currentThread().getName());
						int[] p = (int[]) msg.obj;
						if (p != null) {
							if (msg.arg2 == 1) {
								LED.ledFillBox(p[0], p[1], p[2], p[3], msg.arg1);
							} else {
								LED.ledDrawBox(p[0], p[1], p[2], p[3], msg.arg1);
							}
						}
						break;
					case LIGHT_ROUND:
						Log.e("test", "LIGHT_ROUND");
						int[] xy= (int[]) msg.obj;
						if(xy!=null){
							LED.set(xy[0], xy[1], msg.arg1);
						}
						break;
					case MONSTER_END:
						LED.ledDrawBox(0, 0, 13, 13, 0);
						Log.e("test", "MONSTER_END  light compelte ,please back to game by interface");
						break;
					case LIGHT_ROAD_ANIMAL:
						int[] xy1=Position.getCoordinate(msg.arg1);
						if(xy1!=null){
							LED.set(xy1[0], xy1[1], 0x333333);
						}
						break;
					default:
						break;
				}
			};
		};

		super.onLooperPrepared();
	}

	public void lightRoadAnimal(ArrayList<Integer> positions){
		int time=1000/(positions.size());
		int lenth=positions.size();
		for (int i = 0; i < lenth; i++) {
			Message msg = Message.obtain();
			msg.what = LIGHT_ROAD_ANIMAL;
			msg.arg1 = positions.get(lenth-i-1);
			handler.sendMessageDelayed(msg, i*time);
		}
	}

	/**
	 *捕捉怪兽的动画效果
	 * @return
	 * */
	public int catchMonsterLight(int x, int y) {
		// catchMonsterP[0]=x;
		// catchMonsterP[1]=y;
		int h = y > 7 ? y : 13 - y;
		int l = x > 7 ? x : 13 - x;
		int length = Math.min(h, l);

		// 收缩点亮
		for (int i = length; i > 0; i--) {
			if (i == length) {
				int round[] = new int[4];
				round[0] = x - (length - 1) > 0 ? x - (length - 1) : 0;
				round[1] = y - (length - 1) > 0 ? y - (length - 1) : 0;
				round[2] = x + (length - 1) < 14 ? x + (length - 1) : 13;
				round[3] = y + (length - 1) < 14 ? y + (length - 1) : 13;
				Log.d("test", Arrays.toString(round));
				LED.ledFillBox(0, 0, 13, 13, 0xffffff);
				LED.ledFillBox(round[0], round[1], round[2], round[3], 0);
				continue;
			}
			int round[] = new int[4];
			round[0] = x - i > 0 ? x - i : 0;
			round[1] = y - i > 0 ? y - i : 0;
			round[2] = x + i < 14 ? x + i : 13;
			round[3] = y + i < 14 ? y + i : 13;
			Log.d("test", Arrays.toString(round));
			Message msg = Message.obtain();
			msg.what = LIGHT_MONSTER_CATCH;
			msg.arg1 = 0xffffff;
			msg.obj = round;
			handler.sendMessageDelayed(msg, (length - i) * TIME_MONSTER_CATCH);
		}
		// 扩展熄灭
		for (int i = 1; i < length + 1; i++) {
			if (i == length) {
				int round[] = new int[4];
				round[0] = 0;
				round[1] = 0;
				round[2] = 13;
				round[3] = 13;
				Log.d("test", Arrays.toString(round));
				Message msg = Message.obtain();
				msg.what = LIGHT_MONSTER_CATCH;
				msg.arg1 = 0;
				msg.arg2 = 1;
				msg.obj = round;
				handler.sendMessageDelayed(msg, (length + i)
						* TIME_MONSTER_CATCH);
				continue;
			}
			int round[] = new int[4];
			round[0] = x - i > 0 ? x - i : 0;
			round[1] = y - i > 0 ? y - i : 0;
			round[2] = x + i < 14 ? x + i : 13;
			round[3] = y + i < 14 ? y + i : 13;
			Log.d("test", Arrays.toString(round));
			Message msg = Message.obtain();
			msg.what = LIGHT_MONSTER_CATCH;
			msg.arg1 = 0;
			msg.obj = round;
			handler.sendMessageDelayed(msg, (length + i) * TIME_MONSTER_CATCH);
		}

		// 彩灯转一圈
		ledRound(2*length*TIME_MONSTER_CATCH+100,ROUND_INTERVALTIME);
		//灯效消耗的总时间
		int totalTime=2*length*TIME_MONSTER_CATCH+52*ROUND_INTERVALTIME+300;
		handler.sendEmptyMessageDelayed(MONSTER_END, 2*length*TIME_MONSTER_CATCH+52*ROUND_INTERVALTIME+300);
		return totalTime;
	}


	private void ledRound(int delayTime,int intervalTime) {
		for (int i = 0; i < 52; i++) {
			int x = i / 13;
			int y = i % 13;
			switch (x) {
				case 0:
					x = y;
					y = 0;
					break;
				case 1:
					x = 13;
					break;
				case 2:
					x = 13 - y;
					y = 13;
					break;
				case 3:
					x = 0;
					y = 13 - y;
					break;
				default:
					break;
			}

			int round[] = new int[2];
			round[0] =x;
			round[1] =y;
			Message msg = Message.obtain();
			msg.what = LIGHT_ROUND;
			msg.arg1 = mapColors[i % 7];
			msg.obj=round;
			handler.sendMessageDelayed(msg, delayTime+(i*intervalTime));
		}
	}

	public static final int[] mapColors = { LED.COLOR_GREEN, LED.COLOR_BLUE,
			LED.COLOR_L_BLUE, LED.COLOR_RED, LED.COLOR_YELLOW, LED.COLOR_PURPLE, LED.COLOR_WIHTE };

	public void removeAll() {
		handler.removeCallbacksAndMessages(null);
	}
}