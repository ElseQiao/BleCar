package com.beyondscreen.mazecar.home.v3.car.test;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.beyond.app.BeyondActivity;
import com.beyond.app.SysUtils;
import com.beyond.touch.TouchEvent;
import com.beyondscreen.mazecar.R;
import com.beyondscreen.mazecar.common.ResolutionUtil;
import com.beyondscreen.mazecar.home.v3.car.btutils.BeyondScanBle;
import com.beyondscreen.mazecar.home.v3.car.interfaces.BeyondCarStateListener;


public class TestActivity extends BeyondActivity implements View.OnClickListener,BeyondCarStateListener {
    private static final String TAG = "TestActivity";
    private Button b1;
    private Button b2;
    private Button b3;
    private Button b4;
    private Button b5;
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

    private BeyondScanBle beyondScanBle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        initView();

        led.fillBox(0,0,13,13,0);
        led.set(0,0,0x00ffff);
        led.set(2,0,0xff00ff);
        led.set(4,0,0xffff00);
        beyondScanBle=new BeyondScanBle(this.getApplicationContext());
        beyondScanBle.setBeyondCarStateListener(this);
        startTouch();
    }

    private void initView() {
        b1=findViewById(R.id.button1);
        b2=findViewById(R.id.button2);
        b3=findViewById(R.id.button3);
        b4=findViewById(R.id.button4);
        b5=findViewById(R.id.button5);
        b1.setOnClickListener(this);
        b2.setOnClickListener(this);
        b3.setOnClickListener(this);
        b4.setOnClickListener(this);
        b5.setOnClickListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: -----");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: -----");
        if(beyondScanBle!=null){
            beyondScanBle.releaseBle();
        }
        super.onDestroy();
    }

    private boolean canStart=true;
    @Override
    public void onBeyondTouch(int action, TouchEvent e) {
        super.onBeyondTouch(action, e);
        if(action==TouchEvent.ACTION_DOWN&&e.y==0){
            switch (e.getX()){
                case 0:
                   // if(!canStart)return;
                    canStart=false;
                    beyondScanBle.startScan(true, new BeyondScanBle.ScanResultListener() {
                        @Override
                        public void noCarFind(int reason) {
                            canStart=true;
                        }
                        @Override
                        public void findCar() {

                        }
                    });
                    break;
                case 2:
                    beyondScanBle.testClose();
                    break;
                case 4:
                    beyondScanBle.releaseBle();
                    break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button1:
                break;
            case R.id.button2:
                break;
            case R.id.button3:
                break;
            case R.id.button4:
                break;
            case R.id.button5:
                finish();
                break;
        }

    }


    @Override
    public void carDisconnect() {
        led.set(13,13,0xff0000);
        Log.e(TAG, "carDisconnect: 连接成功了");
    }

    @Override
    public void carConnect() {
        led.set(13,13,0x00ff00);
    }
}