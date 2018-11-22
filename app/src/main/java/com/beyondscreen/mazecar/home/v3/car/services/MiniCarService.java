/*
 * Copyright © 2018 Beyond Screen Inc.
 * All Rights Reserved.
 * author : linx
 * e-mail : liling295@163.com
 * date   : 2018/10/19
 * desc   :
 * version: 1.0.0
 */
package com.beyondscreen.mazecar.home.v3.car.services;

import com.beyondscreen.mazecar.home.v3.car.interfaces.UARTInterface;
import com.beyondscreen.mazecar.home.v3.car.interfaces.UARTManagerCallbacks;
import com.beyondscreen.mazecar.home.v3.car.btutils.UARTManager;

import android.bluetooth.BluetoothDevice;

import no.nordicsemi.android.ble.BleManager;

/**
 * MiniCarService集成读写交互
 * */

public class MiniCarService extends BleProfileService implements UARTManagerCallbacks {
    private final static String TAG = MiniCarService.class.getSimpleName();


    /**继承blemanager,实现读写功能与远程设备交互*/
    private UARTManager mManager;
    private final LocalBinder mBinder = new UARTBinder();

    public class UARTBinder extends LocalBinder implements UARTInterface {
        @Override
        public void send(final byte[] cmd) {
            mManager.send(cmd);
        }

        public MiniCarService getService() {
            return MiniCarService.this;
        }
    }

    @Override
    public void onDestroy() {
        callback=null;
        super.onDestroy();
    }

    @Override
    protected LocalBinder getBinder() {
        return mBinder;
    }

    @Override
    protected BleManager<UARTManagerCallbacks> initializeManager() {
        return mManager = new UARTManager(this);
    }

    private Callback callback;
    public interface Callback {
        void onDataReceived(String data);
    }
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onDataReceived(BluetoothDevice device, String data) {
        if (callback!=null)
            callback.onDataReceived(data);
    }

    @Override
    public void onDataSent(BluetoothDevice device, String data) {

    }


}
