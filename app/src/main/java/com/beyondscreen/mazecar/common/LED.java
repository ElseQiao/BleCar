package com.beyondscreen.mazecar.common;

import android.util.Log;

import com.beyond.led.Led;

public class LED {
	/** 单色 */
	public static final int COLOR_SINGLE = 0xffffff;
	/** 黑色(也是关闭LED) */
	public static final int COLOR_BLACK = 0x000000;

	/** 绿色 */
	public static final int COLOR_GREEN = 0x00ff00;
	/** 蓝色 */
	public static final int COLOR_BLUE = 0x0000ff;
	/** 浅蓝 */
	public static final int COLOR_L_BLUE = 0x00ffff;
	/** 红色 */
	public static final int COLOR_RED = 0xff0000;
	/** 黄色 */
	public static final int COLOR_YELLOW = 0xffff00;
	/** 紫色 */
	public static final int COLOR_PURPLE = 0xff00ff;
	/** 白色 */
	public static final int COLOR_WIHTE = 0xffffff;
	/** 20%白色 */
	public static final int COLOR_WIHTE_20 = 0x333333;
	public static Led led = new Led();

	public LED() {
	}

	public static void ledDrawLine(int x1, int y1, int x2, int y2, int color) {
		led.drawLine(x1, y1, x2, y2, color);
	}

	public static void ledDrawBox(int x1, int y1, int x2, int y2, int color) {
		led.drawBox(x1, y1, x2, y2, color);
	}

	public static void ledFillBox(int x1, int y1, int x2, int y2, int color) {
		led.fillBox(x1, y1, x2, y2, color);
	}

	public static void set(int x, int y, int color) {
		led.set(x, y, color);
	}

	public static final int[] mapColors = { COLOR_GREEN, COLOR_BLUE,
			COLOR_L_BLUE, COLOR_RED, COLOR_YELLOW, COLOR_PURPLE, COLOR_WIHTE };

	public static boolean gameOver = false;

	/**
	 * 周围彩灯
	 * */
	public static void roundCorlor(final int time) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 68; i++) {
					if (gameOver)
						return;// 游戏结束�???

					int x = i / 17;
					int y = i % 17;
					switch (x) {
					case 0:
						x = y;
						y = 0;
						break;
					case 1:
						x = 17;
						break;
					case 2:
						x = 17 - y;
						y = 17;
						break;
					case 3:
						x = 0;
						y = 17 - y;
						break;
					default:
						break;
					}
					set(x, y, mapColors[i % 7]);
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
						ledDrawBox(0, 0, 17, 17, 0);
						e.printStackTrace();
					}
				}
				ledDrawBox(0, 0, 17, 17, 0);
			}
		}).start();
	}

	/**
	 * 周围彩灯依次 areaX areaY 表示区域的按键，因数独的区域按键在彩灯区域，以操作彩灯后要恢复彩灯位置的亮灯情况
	 * */
	public static void roundCorlorMini(final int areaX, final int areaY,
			final int time) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 52; i++) {
					if (gameOver)
						return;// 游戏结束，停�????
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
					set(x, y, mapColors[i % 7]);
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
						ledDrawBox(0, 0, 13, 13, 0);
						e.printStackTrace();
					}
				}
				ledDrawBox(0, 0, 13, 13, 0);

				// if(areaX==0&&areaY==3){
				// //点亮模式按键
				// set(areaX, areaY, 0xffffff);
				// };

			}
		}).start();
	}

	public static void startLedShow(int i) {
		LED.ledFillBox(1, 1, 12, 11, 0);

		if (i == 23) {
			return;
		}

		if (i == 0) {
			LED.set(1, 11, 0xfff);
			LED.ledDrawLine(1, 10, 2, 11, 0xfff);
			LED.ledDrawLine(1, 9, 3, 11, 0xfff);
			return;
		}

		if (i < 10) {
			LED.ledDrawLine(1, 12 - i, i, 11, 0xfff);
			LED.ledDrawLine(1, 11 - i, i + 1, 11, 0xfff);
			LED.ledDrawLine(1, 10 - i, i + 2, 11, 0xfff);
			return;
		}

		if (i == 10) {
			LED.ledDrawLine(1, 12 - i, i, 11, 0xfff);
			LED.ledDrawLine(1, 11 - i, i + 1, 11, 0xfff);
			LED.ledDrawLine(2, 1, 12, 11, 0xfff);
			return;
		}

		if (i == 11) {
			LED.ledDrawLine(1, 12 - i, i, 11, 0xfff);
			LED.ledDrawLine(2, 1, 12, 11, 0xfff);
			LED.ledDrawLine(3, 1, 12, 10, 0xfff);
			return;
		}

		if (i < 20) {
			LED.ledDrawLine(i - 10, 1, 12, 23 - i, 0xfff);
			LED.ledDrawLine(i - 9, 1, 12, 22 - i, 0xfff);
			LED.ledDrawLine(i - 8, 1, 12, 21 - i, 0xfff);
			return;
		}

		if (i == 20) {
			LED.ledDrawLine(i - 10, 1, 12, 23 - i, 0xfff);
			LED.ledDrawLine(i - 9, 1, 12, 22 - i, 0xfff);
			LED.set(12, 1, 0xfff);
			return;
		}
		if (i == 21) {

			LED.ledDrawLine(11, 1, 12, 2, 0xfff);
			LED.set(12, 1, 0xfff);
			return;
		}
		if (i == 22) {
			LED.set(12, 1, 0xfff);
			return;
		}

	}

	public static void startLedShowSub(int i) {
		LED.ledFillBox(1, 2, 12, 11, 0);

		if (i == 22) {
			return;
		}

		if (i == 0) {
			LED.set(1, 11, 0xfff);
			LED.ledDrawLine(1, 10, 2, 11, 0xfff);
			LED.ledDrawLine(1, 9, 3, 11, 0xfff);
			return;
		}

		if (i < 9) {
			LED.ledDrawLine(1, 12 - i, i, 11, 0xfff);
			LED.ledDrawLine(1, 11 - i, i + 1, 11, 0xfff);
			LED.ledDrawLine(1, 10 - i, i + 2, 11, 0xfff);
			return;
		}

		if (i == 9) {
			LED.ledDrawLine(1, 12 - i, i, 11, 0xfff);
			LED.ledDrawLine(1, 11 - i, i + 1, 11, 0xfff);
			LED.ledDrawLine(2, 2, 11, 11, 0xfff);
			return;
		}

		if (i == 10) {
			LED.ledDrawLine(1, 2, 10, 11, 0xfff);
			LED.ledDrawLine(2, 2, 11, 11, 0xfff);
			LED.ledDrawLine(3, 2, 12, 11, 0xfff);
			return;
		}
		
		if (i == 11) {
			LED.ledDrawLine(2, 2, 11, 11, 0xfff);
			LED.ledDrawLine(3, 2, 12, 11, 0xfff);
			LED.ledDrawLine(4, 2, 12, 10, 0xfff);
			return;
		}

		if (i < 19) {
			LED.ledDrawLine(i - 9, 2, 12, 23 - i, 0xfff);
			LED.ledDrawLine(i - 8, 2, 12, 22 - i, 0xfff);
			LED.ledDrawLine(i - 7, 2, 12, 21 - i, 0xfff);
			return;
		}

		if (i == 19) {
			LED.ledDrawLine(10, 2, 12, 4, 0xfff);
			LED.ledDrawLine(11, 2, 12, 3, 0xfff);
			LED.set(12, 2, 0xfff);
			return;
		}
		if (i == 20) {

			LED.ledDrawLine(11, 2, 12, 3, 0xfff);
			LED.set(12, 2, 0xfff);
			return;
		}
		if (i == 21) {
			LED.set(12, 2, 0xfff);
			return;
		}
	}
	
	public static void startLedShowAll(int i) {
		LED.ledFillBox(0, 0,13, 13, 0);

		if (i == 27) {
			return;
		}

		if (i == 0) {
			LED.set(1, 11, 0xfff);
			LED.ledDrawLine(0, 12, 1, 13, 0xffffff);
			LED.ledDrawLine(0, 11, 2, 13, 0xffffff);
			return;
		}

		if (i < 12) {
			LED.ledDrawLine(0, 13 - i, i, 13, 0xffffff);
			LED.ledDrawLine(0, 12 - i, i + 1, 13, 0xffffff);
			LED.ledDrawLine(0, 11 - i, i + 2, 13, 0xffffff);
			return;
		}

		if (i == 12) {
			LED.ledDrawLine(0, 13 - i, i, 13, 0xffffff);
			LED.ledDrawLine(0, 12 - i, i + 1, 13, 0xffffff);
			LED.ledDrawLine(1, 0, 13, 12, 0xffffff);
			return;
		}

		if (i<24) {
			LED.ledDrawLine(i - 13, 0, 13, 26 - i, 0xffffff);
			LED.ledDrawLine(i - 12, 0, 13, 25 - i, 0xffffff);
			LED.ledDrawLine(i - 11, 0, 13, 24 - i, 0xffffff);
			return;
		}
		
		

		if (i ==24) {
			LED.ledDrawLine(11, 0, 13, 2, 0xffffff);
			LED.ledDrawLine(12, 0, 13, 1, 0xffffff);
			LED.set(13,0,0xfff);
			return;
		}

		if (i == 25) {
			LED.ledDrawLine(12, 0, 13, 1, 0xffffff);
			LED.set(13,0,0xfff);
			return;
		}
		if (i == 26) {
			LED.set(13,0,0xffffff);
			return;
		}
	}
	/**开场dong音效灯光*/
	public static void startLedShowNew(int arg2) {
		int p=6-arg2;
		ledDrawBox(p, p, p+(2*arg2+1), p+(2*arg2+1), 0x000000);
	};

	
	/**熄灭传入的坐标，并点亮该坐标的下一个位置*/
	public static void ledErrorAnim(int x,int y) {
		//13-(13+(-13+i))
//		if(x==1){
//			ledFillBox(0, 0, 13, y, 0);
//		}
		Log.d("test", "input---"+x+"-----"+y);
		set(x, y, 0);
		set(x, y+1, 0xffffff);
		set(13-x, y, 0);
		set(13-x, y+1, 0xffffff);
		Log.e("test", "out---"+x+"-----"+y);
	}

}
