package com.beyondscreen.mazecar.home.v3.car.interfaces;

import com.beyondscreen.mazecar.home.v3.car.btutils.BeyondScanBle;

/**
 * Created by Else on 2018/11/20.
 * 这里只需要根据小车的连接状态设置灯光提示用户即可，小车会自动连接所以不需要特殊操作
 * 蓝牙状态太多，多数无用，如果需要添加更多监听可以参考{@link BeyondScanBle#mCommonBroadcastReceiver}自定义
 * 扩展
 */

public interface BeyondCarStateListener {
    /**与小车连接失败了*/
    void carDisconnect();
    /**连接上小车了*/
    void carConnect();
}
