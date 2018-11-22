package com.beyondscreen.mazecar.home.v3.car.utils;

import com.beyondscreen.mazecar.common.LED;



public class ButtonBreath {
	public static final int LED_COLSE = 0;
	public static final int LED_BREATH = 1;
	public static final int LED_FLASH = 2;
	public static final int LED_LIGHT = 3;

	private boolean keepBreath = false;
	private int light[] = { 0x000000, 0x333333, 0x666666, 0x999999, 0xcccccc,
			0xfffffff, 0xcccccc, 0x999999, 0x666666, 0x333333 };
	public int[][] buttons = { 
			{2, 0,0,0xffffff},//junior
			{3, 0,0,0xffffff},//normal
			{4, 0,0,0xffffff},//senior
			{5, 0,0,0xffffff},//start
			{0, 13,0,0xffffff},//Bluetooth
	};
	
	private int[] point={0,0,0};
	

	public void startButtonBreath() {
		keepBreath = true;
		new Thread(BreathRunnable).start();
	}

	public void stopButtonBreath() {
		keepBreath = false;
	}

	/**
	 * @param status
	 * */
	public void setButtonStatus(int x, int y, int status,int color) {
		 for (int i = 0; i < buttons.length; i++) {
				if(buttons[i][0]==x&&buttons[i][1]==y){
					buttons[i][2] = status;
					buttons[i][3] = color;
				}
		}
		
		switch (status) {
		case LED_COLSE:
			LED.led.set(x, y, 0);//
			break;
		case LED_LIGHT:
			LED.led.set(x, y, 0xffffff);//
			break;
		default:
			break;
		}
	}

	
	
	public void setPointStatus(int x, int y, int status) {
		point[0]=x;
		point[1]=y;
		point[2]=status;
		switch (status) {
		case LED_COLSE:
			LED.led.set(x, y, 0);//
			break;
		case LED_LIGHT:
			LED.led.set(x, y, 0xffffff);//
			break;
		default:
			break;
		}
	}
	
	private int lightNow = 0;
	private Runnable BreathRunnable = new Runnable() {
		@Override
		public void run() {
			while (keepBreath){
				for (int i = 0; i < buttons.length; i++) {
					if (buttons[i][2] == LED_BREATH) {
						LED.led.set(buttons[i][0], buttons[i][1], light[lightNow]);
					} else if (buttons[i][2] == LED_FLASH) {
						LED.led.set(buttons[i][0], buttons[i][1],
								lightNow % 2 == 0 ? buttons[i][3] : 0);
					}
				}
				
				
				if(point[2]==LED_FLASH){
					LED.led.set(point[0], point[1],
							lightNow % 2 == 0 ? 0xffffff : 0);
				}
				
				lightNow++;
				if (lightNow > light.length - 1) {
					lightNow = 0;
				}
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

	
	public void resetButtonStatus(){
		 for (int i = 0; i < buttons.length; i++) {
			 buttons[i][2] = LED_COLSE;
		}
	}
}
