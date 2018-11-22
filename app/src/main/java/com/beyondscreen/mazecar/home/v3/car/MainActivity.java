package com.beyondscreen.mazecar.home.v3.car;



import com.beyond.app.BeyondActivity;
import com.beyond.app.SysUtils;
import com.beyond.card.CardEvent;
import com.beyond.touch.TouchEvent;
import com.beyondscreen.mazecar.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class MainActivity extends BeyondActivity {
	/** exit game app */ 
	private final String ACTION_BROADCAST_EXIT_APP = "com.beyondscreen.broadcast.exitapp"; 
	private final BroadcastReceiver stopReceiver = new ReceiverForStop();
	private Move move;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    	SysUtils.enableSleepMode(false);//???????
        IntentFilter filter = new IntentFilter(); 
		filter.addAction(ACTION_BROADCAST_EXIT_APP);
		registerReceiver(stopReceiver, filter);
        init();
        move=new Move(this);
        move.startMove();
    }


	private void init() {
		card.reset();
		card.setMatchStrategy(com.beyond.card.Card.MAP_LIST_MATCH);
		card.initMapList(getCardArea());
		//card.setFastMode(true);
		// card.setMask(2, 2, new byte[] { '1', '1', '1', '1' });
		//card.setItemFilter(0, com.beyond.card.Card.FILTER_USERID);
		startTouch();
	}

	private int[] getCardArea() {
		int[] data = new int[133*4];
		int i = 0;
		for (int y = 1; y < 12; y++) {
			for (int x = 1; x < 13; x++) {
				data[i * 4 + 0] = x;
				data[i * 4 + 1] = y;
				data[i * 4 + 2] = 1;
				data[i * 4 + 3] = 1;
				i++;
			}
		}
		
		data[132 * 4 + 0]= 1;
		data[132 * 4 + 1] = 12;
		data[132 * 4 + 2] = 1;
		data[132 * 4 + 3] = 1;
		return data;
	}

	/**???????????*/
	private int sigleClick=-1;
	private boolean isLongClick=false;
	@Override
    public void onBeyondTouch(int action, TouchEvent e) {
    	super.onBeyondTouch(action, e);
    	if(move==null)return;
    	if(e.x>29||e.y>29)return;
    	//TODO????????
    	if(e.x==13&&e.y==13&&action==TouchEvent.ACTION_DOWN){
    		//stopGame();
    	}
    	
    	if(e.y==0&&e.x>1&&e.x<6){
    		if(!isLongClick&&touch.isLongPress(e)){
				isLongClick=true;
				move.btClick(e.x);
			}

			if(action==TouchEvent.ACTION_UP){
				isLongClick=false;
			}
			return;
    	}
    	
    	if(e.y==0&&e.x>1&&e.x>8&&action==TouchEvent.ACTION_DOWN){
    		//move.testCar(e.x);
    		return;
    	}
    	
    	if(e.y==13&&e.x==0){
    		if(!isLongClick&&touch.isLongPress(e)){
				isLongClick=true;
				move.blueBtClicked();
			}

			if(action==TouchEvent.ACTION_UP){
				isLongClick=false;
			}
			return;
    	}
    	
    	if(action==TouchEvent.ACTION_PRESSING)return;
    	
    	if(e.x==7&&e.y==0&&action==TouchEvent.ACTION_DOWN){
    		//move.tipHelp();
    	}
    	//&&!touch.isFingerClick(e)
     	//Log.e("test", e.x+"-------"+e.y);
//    	if(e.x>0&&e.y>0&&e.x<13&&e.y<12){
//    		move.shapeCardDown(action,e.x,e.y);
//    		return;
//    	}
   
    	if(e.y==12&&e.x>2&&e.x<13){
    		if(e.x==10)return;
    		if(sigleClick==-1&&action==TouchEvent.ACTION_DOWN){
    			sigleClick=e.x;
    			move.controlButton(action,e.x);
    			if(move.status==move.STATUS_DEAL){
    				led.set(e.x, e.y, 0xffffff);
    			}
    		}
    		
    		if(sigleClick==e.x&&action==TouchEvent.ACTION_UP){
    			sigleClick=-1;
    			if(move.status==move.STATUS_DEAL){
    				if(e.x!=11&&e.x!=12){
        				led.set(e.x, e.y, 0x222222);
        			}
    			}
    		}
    	}
    }
	
	@Override
	public void onBeyondCard(int action, CardEvent e) {
		super.onBeyondCard(action, e);
		if(e.x>29||e.y>29)return;
		if(move==null)return;
		
		if(action==CardEvent.ACTION_CONFLICT){
			move.conflict();
			return;
		}
		move.monsterCardDown(action, e.x, e.y,e.uid);
	}
	
	public class ReceiverForStop extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction()==ACTION_BROADCAST_EXIT_APP){
				stopGame();
			}
		}
	}

	public void stopGame() {
		SysUtils.enableSleepMode(true);
		unregisterReceiver(stopReceiver);
		//endGame();
		if(move!=null){
			move.stopMove();
		}
		finish();
		System.exit(0);
	}


	//--------------------------blue deal--------------------------------------------

}
