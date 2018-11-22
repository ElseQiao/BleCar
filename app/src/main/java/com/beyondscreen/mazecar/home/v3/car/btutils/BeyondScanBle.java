package com.beyondscreen.mazecar.home.v3.car.btutils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.beyondscreen.mazecar.home.v3.car.interfaces.BeyondCarStateListener;
import com.beyondscreen.mazecar.home.v3.car.services.BleProfileService;
import com.beyondscreen.mazecar.home.v3.car.services.MiniCarService;

import java.util.List;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.Logger;

/**
 * Created by Else on 2018/11/20.
 * 针对贝板编辑的扫描类
 * 贝板扫描特点：
 * 1.相对于手机来讲贝板扫描不灵敏，所以扫描时可以采用短周期，高频率的方式。每次扫描时间3s；
 *   四次扫描12S为一个周期
 * 2.连续扫描多次后可能导致无法扫描到设备的问题，表现为callback()无返回，这时候需要重启蓝牙
 *
 *  实现功能：扫描+连接，其中连接状态可通过实现{@link BeyondCarStateListener}
 *  使用注意：直接调用{@link #startScan(boolean, ScanResultListener)}可开始扫描，实现接口可监听扫描结果
 *           退出贝板前记得调用{@link #releaseBle()} 释放BLE相关资源
 */

public class BeyondScanBle {
    private static final String TAG = "TEST";
    private Context mContext;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner mScanner;
    private Handler mHandler= new Handler(Looper.getMainLooper());

    /**扫描mDevice记录*/
    private volatile BluetoothDevice mDevice;
    /**连接mDevice记录*/
    private BluetoothDevice mBluetoothDevice;
    private ILogSession mLogSession;
    private MiniCarService.UARTBinder uARTBinder=null;

    /**是否正在扫描*/
    private boolean mIsScanning=false;
    private String mName = "MINI_BYCAR01";
    /**小车扫描结果*/
    private ScanResultListener scanResultListener;
    /**小车连接结果*/
    private BeyondCarStateListener beyondCarStateListener;

    private BeyondCarOrder beyondCarOrder;
    private BeyondScanBle(){

    }

    public BeyondScanBle(Context mContext){
        this.mContext=mContext;
        initBlue();
        beyondCarOrder=new BeyondCarOrder();
    }

    private void initBlue() {
        final BluetoothManager manager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mContext.registerReceiver(mBluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        bluetoothAdapter = manager.getAdapter();
        Log.e(TAG, "initBlue: bluetoothAdapter is null"+bluetoothAdapter.isEnabled());
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
        mScanner= bluetoothAdapter.getBluetoothLeScanner();

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mCommonBroadcastReceiver,
                makeIntentFilter());
    }


    /**释放启用的资源，退出游戏时调用*/
    public void releaseBle(){
        if(beyondCarOrder!=null){
            beyondCarOrder.releaseAll();
        }
        //释放扫描
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mCommonBroadcastReceiver);
        mContext.unregisterReceiver(mBluetoothStateBroadcastReceiver);
        if(mIsScanning){
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
        mHandler.removeCallbacksAndMessages(null);
        //释放连接
        if(uARTBinder!=null){
            //这里会关闭蓝牙连接服务，并关闭服务
            final Intent service = new Intent(mContext, MiniCarService.class);
            mContext.stopService(service);
        }
        //关闭蓝牙
        if(bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
        }
        bluetoothAdapter=null;
        mHandler=null;
        beyondCarStateListener=null;
        scanResultListener=null;
        mContext=null;
    }


    public void testClose(){
        if(uARTBinder!=null){
            //这里会关闭蓝牙连接服务，并关闭服务
            final Intent service = new Intent(mContext, MiniCarService.class);
            mContext.stopService(service);
        }
    }


    /**设备是否连接中*/
    public boolean isDeviceConnected() {
        return uARTBinder != null && uARTBinder.isConnected();
    }
    //过滤字段的方式
    //ParcelUuid parcelUuid=new ParcelUuid(UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"));
    //ScanFilter sf=new ScanFilter.Builder().setServiceUuid(parcelUuid).build();
    /**当蓝牙关闭时记录扫描次数，超过5次没开启则停止扫描*/
    private int delayTime=0;
    /**
     * 开启扫描，虽然方法已经对多次请求做了处理，会避免重复调用，但是调用时最好实现接口
     * 并在本次扫描结束前将按键设为不可点击状态。
     *
     * @param needCallBack false不设置回调,监听传null；true设置回调，当扫描结束未扫描到设备的话会
     *                     进行返回，这时可以设置相应按键再次启动扫描。
     *                     目前这种方式小车在范围内都会被发现，主要针对没有开启小车的情况
     *
     * eg:根据目前贝板的需求，暂时没有游戏中更换连接设备的要求，所以小车一旦连接后就没必要再次调用startScan();
     *    所以在小车已经发现或连接后，可以禁止再次扫描方法。虽然接口提供了findCar()方法提示小车被发现，但为
     *    防止连接失败的情况最好，最好通过监听小车连接成功来设置
     * */
    public void startScan(boolean needCallBack,ScanResultListener scanResultListener) {
        if(mIsScanning){
            //如果正在扫描，返回
            return;
        }

        if(needCallBack){
           this.scanResultListener=scanResultListener;
        }

        if(isDeviceConnected()){
            if(scanResultListener!=null){
                scanResultListener.noCarFind(CASE_CAR_ISCONNECT);
            }
            return;
        }

        //如果蓝牙未开启，开启蓝牙并稍等500ms
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(delayTime>5){
                        if(scanResultListener!=null){
                            scanResultListener.noCarFind(CASE_BT_ISCLOSE);
                        }
                        return;
                    }
                    delayTime++;
                    startScan(false,null);
                }
            },500);
            return;
        }
        delayTime=0;
        //
        if(mScanner==null){
            mScanner=bluetoothAdapter.getBluetoothLeScanner();
            if(mScanner==null){
                if(scanResultListener!=null){
                    scanResultListener.noCarFind(CASE_BLUETOOTHLESCANNER_NULL);
                }
                return;
            }
        }

        Log.d(TAG, "startScan...");
        haveResult=false;
        mIsScanning = true;
        mDevice=null;
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
        mHandler.postDelayed(stopScanTask, 3000);
    }


    /**记录scanCallback是否有回调结果。贝板在多次扫描后就不再返回结果，此时需要重启蓝牙*/
    private boolean haveResult=false;
    /**记录scanCallback的没有回调结果的次数*/
    private int noResultCounter=0;
    /**记录自动扫描的次数，每次主动扫描程序都拆分为最多四次扫描，缩短扫描时间，提高扫描次数以适应贝板的情况*/
    private int scanCounter=0;
    private Runnable stopScanTask=()->stopScan(false);
    public void stopScan(boolean realClose) {
        if(!mIsScanning){
            return;
        }
        //基本处理
        mHandler.removeCallbacks(stopScanTask);
        Log.w(TAG, "stopScan!!! ");
        mIsScanning = false;
        mScanner.stopScan(scanCallback);
        //
        if(!realClose&&scanCounter<4){
            if(!haveResult){
                //本次扫描无结果
                noResultCounter++;
            }
            //说明没有扫描到需要的设备,根据贝板的设备，扫描结果反馈不全，所以采用扫描时间短，多次扫描方式来优
            //优化这里
            scanCounter++;
            if(noResultCounter>=3){
                //连续两次扫描不到，则重启蓝牙，再扫描
                noResultCounter=0;
                reSetBlueTooth();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startScan(false,null);
                    }
                },500);
            }else{
                startScan(false,null);
            }
        }else{
            if(scanResultListener!=null){
                if(realClose){
                    scanResultListener.findCar();
                }else{
                    scanResultListener.noCarFind(CASE_DISCOVER_NULL);
                }
            }
            scanCounter=0;
            noResultCounter=0;
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private void reSetBlueTooth() {
        if (bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.enable();
            }
        },200);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // TODO 自动生成的方法存根
            if(result.getDevice().getName()!=null){
                Log.d("test", "onScanResult ads:" + result.getDevice().getAddress()+"---name:"
                        +result.getDevice().getName());
            }else{
                Log.d("test", "onScanResult list:" + result.getDevice().getAddress());
            }
            haveResult=true;
            String devName = result.getDevice().getName();

            if (devName != null && devName.equalsIgnoreCase(mName) && mDevice == null) {
                //mDevice==null可以防止同一次扫描多次进入连接，但是如果同时多次调用startScan()可能会导致多次
                //连接，所以如果想要严谨点可以在连接时调用{@link uARTBinder.disconnect();}先断开连接，不过
                //贝板这里并不需要   by Else
                stopScan(true);
                mDevice = result.getDevice();
                onDeviceSelected(mDevice, mName);
                Log.w(TAG, "Car(" + mName + ") connected!!!");

            }
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            // 当设置uuid过滤时结果会返回这里
            super.onBatchScanResults(results);
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Car("  + ") onScanFailed errorCode=" + errorCode);

        }
    };

    /**小车连接方法，连接过程在服务中处理*/
    private void onDeviceSelected(final BluetoothDevice device, final String name) {
        final int titleId = getLoggerProfileTitle();
        if (titleId > 0) {
            mLogSession = Logger.newSession(mContext.getApplicationContext(), mContext.getString(titleId), device.getAddress(), name);
            // If nRF Logger is not installed we may want to use local logger
            if (mLogSession == null && getLocalAuthorityLogger() != null) {
                mLogSession = LocalLogSession.newSession(mContext.getApplicationContext(), getLocalAuthorityLogger(), device.getAddress(), name);
            }
        }
        mBluetoothDevice = device;
        //mDeviceName = name;

        // The device may not be in the range but the service will try to connect to it if it reach it
        Logger.d(mLogSession, "Creating service...");
        final Intent service = new Intent(mContext, MiniCarService.class);
        service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
        service.putExtra(BleProfileService.EXTRA_DEVICE_NAME, name);
        if (mLogSession != null)
            service.putExtra(BleProfileService.EXTRA_LOG_URI, mLogSession.getSessionUri());
        mContext.startService(service);
        Logger.d(mLogSession, "Binding to the service...");
        Log.d(TAG,"Binding to the service...");
        mContext.bindService(service, mServiceConnection, 0);
        //eg by Else:startService()后bindService可以保证该服务解绑后不被销毁，直到stopService（flags为0
        //时调用直到stopService会解绑所有绑定）
    }

    /**
     * Returns the title resource id that will be used to create logger session. If 0 is returned (default) logger will not be used.
     *
     * @return the title resource id
     */
    protected int getLoggerProfileTitle() {
        return 0;
    }
    /**
     * This method may return the local log content provider authority if local log sessions are supported.
     *
     * @return local log session content provider URI
     */
    protected Uri getLocalAuthorityLogger() {
        return null;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            final MiniCarService.UARTBinder bleService = (MiniCarService.UARTBinder) service;
            mBluetoothDevice = bleService.getBluetoothDevice();
            mLogSession = bleService.getLogSession();
            Logger.d(mLogSession, "Activity bound to the service");
            uARTBinder=bleService;
            onW_RListen(uARTBinder);
            // Update UI
            //mDeviceName = bleService.getDeviceName();

            // And notify user if device is connected
            if (bleService.isConnected()) {
                Log.e(TAG, "onServiceConnected: bleService.isConnected()-----");
            } else {
                // If the device is not connected it means that either it is still connecting,
                // or the link was lost and service is trying to connect to it (autoConnect=true).
                Log.e(TAG, "onServiceConnected: bleService.connecting-----");
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            // Note: this method is called only when the service is killed by the system,
            // not when it stops itself or is stopped by the activity.
            // It will be called only when there is critically low memory, in practice never
            // when the activity is in foreground.
            Logger.d(mLogSession, "Activity disconnected from the service");
            //mDeviceName = null;
            mBluetoothDevice = null;
            mLogSession = null;
            onW_RListenUnbinded();
            uARTBinder=null;
        }
    };


    public BeyondCarOrder getBeyondCarOrder(){
        return beyondCarOrder;
    }

    /**
     * MiniCarService会接受远程设备的消息，onWrListen通过实现接口获取该消息
     */
    protected void onW_RListen(final MiniCarService.UARTBinder binder) {
        beyondCarOrder.initCallbackWhenBind(binder);
    }

    /**
     * Called when activity unbinds from the service. You may no longer use this binder because the sensor was disconnected. This method is also called when you
     * leave the activity being connected to the sensor in the background.
     */
    protected void onW_RListenUnbinded() {
        beyondCarOrder.doUnbind();
    }

    /**
     * Checks the {@link BleProfileService#EXTRA_DEVICE} in the given intent and compares it with the connected BluetoothDevice object.
     * @param intent intent received via a broadcast from the service
     * @return true if the data in the intent apply to the connected device, false otherwise
     */
    protected boolean isBroadcastForThisDevice(final Intent intent) {
        final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BleProfileService.EXTRA_DEVICE);
        return mBluetoothDevice != null && mBluetoothDevice.equals(bluetoothDevice);
    }
    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleProfileService.BROADCAST_CONNECTION_STATE);
        intentFilter.addAction(BleProfileService.BROADCAST_SERVICES_DISCOVERED);
        intentFilter.addAction(BleProfileService.BROADCAST_DEVICE_READY);
        intentFilter.addAction(BleProfileService.BROADCAST_BOND_STATE);
        intentFilter.addAction(BleProfileService.BROADCAST_BATTERY_LEVEL);
        intentFilter.addAction(BleProfileService.BROADCAST_ERROR);
        return intentFilter;
    }

    /**广播类，和服务通信，接受来自service的消息更新*/
    private final BroadcastReceiver mCommonBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Check if the broadcast applies the connected device
            if (!isBroadcastForThisDevice(intent))
                return;

            final BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BleProfileService.EXTRA_DEVICE);
            final String action = intent.getAction();
            switch (action) {
                case BleProfileService.BROADCAST_CONNECTION_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);

                    switch (state) {
                        case BleProfileService.STATE_CONNECTED: {
                           // mDeviceName = intent.getStringExtra(BleProfileService.EXTRA_DEVICE_NAME);
                            Log.d(TAG, "onReceive: ------STATE_CONNECTED");
                            if(beyondCarStateListener!=null){
                                beyondCarStateListener.carConnect();
                            }
                            break;
                        }
                        case BleProfileService.STATE_DISCONNECTED: {
                            Log.d(TAG, "onReceive: ------STATE_STATE_DISCONNECTED-手动断开连接与服务");
                            //mDeviceName = null;
                            break;
                        }
                        case BleProfileService.STATE_LINK_LOSS: {
                            if(beyondCarStateListener!=null){
                                beyondCarStateListener.carDisconnect();
                            }
                            Log.d(TAG, "onReceive: ------STATE_LINK_LOSS-失去连接");
                            break;
                        }
                        case BleProfileService.STATE_CONNECTING: {
                            Log.d(TAG, "onReceive: ------STATE_CONNECTING");
                            break;
                        }
                        case BleProfileService.STATE_DISCONNECTING: {
                            Log.d(TAG, "onReceive: ------STATE_CONNECTING");
                            break;
                        }
                        default:
                            // there should be no other actions
                            break;
                    }
                    break;
                }
                case BleProfileService.BROADCAST_SERVICES_DISCOVERED: {
                    final boolean primaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_PRIMARY, false);
                    final boolean secondaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_SECONDARY, false);

                    if (primaryService) {
                        Log.d(TAG, "onReceive: ------onServicesDiscovered");
                    } else {
                        Log.d(TAG, "onReceive: ------onDeviceNotSupported");
                    }
                    break;
                }
                case BleProfileService.BROADCAST_DEVICE_READY: {
                    //onDeviceReady(bluetoothDevice);
                    Log.d(TAG, "onReceive: ------DEVICE_READY");
                    break;
                }
                case BleProfileService.BROADCAST_BOND_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    switch (state) {
                        case BluetoothDevice.BOND_BONDING:
                            Log.d(TAG, "onReceive: ------BOND_BONDING");
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            Log.d(TAG, "onReceive: ------BOND_BONDED");
                            break;
                    }
                    break;
                }
                case BleProfileService.BROADCAST_BATTERY_LEVEL: {
                    final int value = intent.getIntExtra(BleProfileService.EXTRA_BATTERY_LEVEL, -1);
                    if (value > 0)
                        Log.d(TAG, "onReceive: ------BROADCAST_BATTERY_LEVEL");
                    break;
                }
                case BleProfileService.BROADCAST_ERROR: {
                    final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
                    final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
                    // onError(bluetoothDevice, message, errorCode);
                    Log.d(TAG, "onReceive: ------BROADCAST_ERROR");
                    break;
                }
            }
        }
    };

    public void setBeyondCarStateListener(BeyondCarStateListener beyondCarStateListener){
        this.beyondCarStateListener=beyondCarStateListener;
    }

    public final int CASE_DISCOVER_NULL=0;
    public final int CASE_BLUETOOTHLESCANNER_NULL=1;
    public final int CASE_BT_ISCLOSE=2;
    public final int CASE_CAR_ISCONNECT=3;
    /**扫描情况返回接口*/
    public interface ScanResultListener{
        void noCarFind(int reason);
        void findCar();
    }



    /**蓝牙状态监听，调试用*/
    private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

            final String stateString = "[Broadcast] Action received: " + BluetoothAdapter.ACTION_STATE_CHANGED ;
            Logger.d(mLogSession, stateString);

            switch (state) {
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_OFF:
                    Log.e(TAG, "onReceive: STATE_OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.e(TAG, "onReceive: STATE_ON");
                    break;
            }
        }
    };
}