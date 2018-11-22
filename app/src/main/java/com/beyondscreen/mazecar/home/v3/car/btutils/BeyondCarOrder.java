package com.beyondscreen.mazecar.home.v3.car.btutils;

import android.util.Log;

import com.beyondscreen.mazecar.home.v3.car.services.MiniCarService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Else on 2018/11/21.
 * 小车指令控制器，连接小车后调用
 * 实际使用中无需实例化，直接调用{@link BeyondScanBle#getBeyondCarOrder()}获取实力即可
 */

public class BeyondCarOrder implements MiniCarService.Callback{
    // north, south, east and west
    public static final int DIR_N = 0;// 北
    public static final int DIR_S = 4;// 南
    public static final int DIR_E = 6;// 东
    public static final int DIR_W = 2;// 西
    public static final int DIR_EN = 7;// 东北
    public static final int DIR_WN = 1;// 西北
    public static final int DIR_ES = 5;// 东南
    public static final int DIR_WS = 3;// 西南

    // * @param type 效果类型 0-关；1-开；2-呼吸；3-快闪
    public static final int LED_TYPE_CLOSE = 0;
    public static final int LED_TYPE_OPEN = 1;
    public static final int LED_TYPE_BREATH = 2;
    public static final int LED_TYPE_BLINK = 3;

    // * @param color 颜色值 0-关；1-绿；2-蓝；3-红；4-青；5-黄；6-紫；7-白
    public static final int LED_COLOR_BLACK = 0;
    public static final int LED_COLOR_GREEN = 1;
    public static final int LED_COLOR_BLUE = 2;
    public static final int LED_COLOR_RED = 3;
    public static final int LED_COLOR_CYAN = 4;
    public static final int LED_COLOR_YELLOW = 5;
    public static final int LED_COLOR_PURPLE = 6;
    public static final int LED_COLOR_WHITE = 7;

    public static final int KEY_ERROR = -1;
    public static final int KEY_PRESS = 1;
    public static final int KEY_DOUBLE_CLICK = 2;
    public static final int KEY_LONG_PRESS = 3;

    public static final int STATE_DISCONNECTED = 0;// 未连接
    public static final int STATE_DISCONNECTING = 1; // 正在连接
    public static final int STATE_CONNECTING = 2; // 正在连接
    public static final int STATE_CONNECTED = 3; // 已连接
    public static final int STATE_UNFOUND_DEV = 4; // 未找到设备
    public static final int STATE_LOW_POWER = 5; // 低电
    public static final int STATE_SCAN_FAILED = 6; // 扫描失败
    public static final int STATE_BT_OFF = 7; //蓝牙关闭
    public static final int STATE_BT_ON = 8; //蓝牙开启
    public static final int STATE_DEVICE_READY = 9;//蓝牙设备就绪
    public static final int STATE_LINK_LOSS = 10;//蓝牙连接丢失
    public static final int STATE_DEVICE_BONDING = 11; //服务连接中
    public static final int STATE_DEVICE_BONDED = 12;  //服务连接成功
    // 命令名
    public static final String CMD_TURN_RIGHT = "turnRight";
    public static final String CMD_TURN_LEFT = "turnLeft";
    public static final String CMD_TURN_RIGHTA = "turnRightA";
    public static final String CMD_TURN_LEFTA = "turnLeftA";
    public static final String CMD_ROTATE_LEFT = "rotateLeft";
    public static final String CMD_ROTATE_RIGHT = "rotateRight";
    public static final String CMD_SHAKE = "shake";
    public static final String CMD_SET_ANGLE = "setAngle";
    public static final String[] CMD_ARR = {CMD_TURN_RIGHT, CMD_TURN_LEFT, CMD_TURN_RIGHTA, CMD_TURN_LEFTA,
            CMD_ROTATE_LEFT, CMD_ROTATE_RIGHT, CMD_SHAKE, CMD_SET_ANGLE};

    private OnStateChangeListener mOnStateChangeListener = null;
    private OnCompletionListener mOnCompletionListener = null;
    private OnInterruptListener mOnInterruptListener = null;// 指令中断监听器
    private OnGetAngleListener mOnGetAngleListener = null;
    private OnGetPosListener mOnGetPosListener = null;
    private OnGetVerListener mOnGetVerListener = null;
    private OnGetBatListener mOnGetBatListener = null;
    private OnKeyEventListener mOnKeyEventListener = null;
    private OnPosChangeListener mOnPosChangeListener = null;
    private OnMaxLostCommandExceedListener mOnMaxLostCommandExceedListener = null;// //连续多条指令执行失败

    private static final String TAG = "BeyondCarOrder";
    private String mName = "MINI_BYCAR01";
    private boolean mRunning;
    private MiniCarService.UARTBinder mServiceBinder;


    public void releaseAll(){
        if(mRunning){
            stop();
        }
        if(mServiceBinder!=null){
            mServiceBinder.getService().setCallback(null);
        }
        mServiceBinder=null;
    }


    public void initCallbackWhenBind(MiniCarService.UARTBinder mServiceBinder){
        this.mServiceBinder=mServiceBinder;
        mServiceBinder.getService().setCallback(this);
    }

    public void doUnbind(){
        mServiceBinder=null;
    }


    @Override
    public void onDataReceived(String rx) {
        if (rx == null) {
            Log.e(TAG, "Car(" + mName + ") Receive nothing!!!");
            return;
        }

        if (rx.contains("END")) {// 完成
            mRunning = false;
            if (mOnCompletionListener != null) {
                String cmd = "CMD_UNKOWN";
                for (String c : CMD_ARR)
                    if (rx.contains(c))
                        cmd = c;
                mOnCompletionListener.onCompletion(cmd);
            }
        } else if (rx.contains("INT")) {// 中断
            mRunning = false;
            if (mOnInterruptListener != null) {
                String cmd = "CMD_UNKOWN";
                for (String c : CMD_ARR)
                    if (rx.contains(c))
                        cmd = c;
                mOnInterruptListener.onInterrupt(cmd);
            }
        } else if (rx.contains("dir")) {
            mRunning = false;
            if (mOnGetAngleListener != null) {
                int[] angleArr = getRxDataArray(rx);
                if (angleArr != null) {
                    mOnGetAngleListener.onGetAngle(angleArr[0]);
                } else {
                    Log.e(TAG, "Car(" + mName + ") get angle failed!!!");
                    mOnGetAngleListener.onGetAngle(-1);
                }
            }
        } else if (rx.contains("pos")) {
            if (mOnGetPosListener != null) {
                int[] posArr = getRxDataArray(rx);
                if (posArr != null && posArr.length >= 2) {
                    mOnGetPosListener.onGetPos(posArr[0], posArr[1]);
                } else {
                    Log.e(TAG, "Car(" + mName + ") get pos failed!!!");
                    mOnGetPosListener.onGetPos(-1, -1);
                }
            }
        } else if (rx.contains("car")) {
            if (mOnGetVerListener != null) {
                if (rx.length() == 19) {
                    mOnGetVerListener.onGetVer(rx);
                } else {
                    Log.e(TAG, "Car(" + mName + ") get ver failed!!!");
                }
            }
        } else if (rx.contains("bat")) {
            if (mOnGetBatListener != null) {
                int[] levelArr = getRxDataArray(rx);
                if (levelArr != null) {
                    mOnGetBatListener.onGetBat(levelArr[0]);
                    if (levelArr[0] < 10) {// 低于10% 报低电
                        if (mOnStateChangeListener != null) {
                            mOnStateChangeListener.OnStateChange(STATE_LOW_POWER);
                        }
                    }
                } else {
                    Log.e(TAG, "Car(" + mName + ") get bat failed!!!");
                    mOnGetBatListener.onGetBat(-1);
                }
            }
        } else if (rx.contains("TP")) {
            if (mOnKeyEventListener != null) {
                int[] levelArr = getRxDataArray(rx);
                if (levelArr != null) {
                    mOnKeyEventListener.onKeyEvent(levelArr[0]);
                } else {
                    Log.e(TAG, "Car(" + mName + ") get key event failed!!!");
                    mOnKeyEventListener.onKeyEvent(KEY_ERROR);
                }
            }
        } else if (rx.contains("gps")) {
            if (mOnPosChangeListener != null) {
                int[] gpsArr = getRxDataArray(rx);
                if (gpsArr != null && gpsArr.length >= 3) {
                    mOnPosChangeListener.onPosChange(gpsArr[0], gpsArr[1], gpsArr[2]);
                } else {
                    Log.e(TAG, "Car(" + mName + ") get gps failed!!!");
                    mOnPosChangeListener.onPosChange(-1, -1, -1);
                }
            }
        } else {
            // Log.e(TAG, "Car("+mName+") not process rx data!!!");
        }
    }


    /**
     * 前进
     */
    public void forward() {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do forward, please call stop() first");
            return;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[3];
            buff[0] = 'R';
            buff[1] = 'M';
            buff[2] = 'A';
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") forward=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") forward=" + status);

    }

    /**
     * 后退
     */
    public void back() {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do back, please call stop() first");
            return;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[3];
            buff[0] = 'R';
            buff[1] = 'M';
            buff[2] = 'B';
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") back=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") back=" + status);

    }

    /**
     * 左转
     */
    public boolean turnLeft() {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do turnLeft, please call stop() first");
            return false;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[3];
            buff[0] = 'R';
            buff[1] = 'M';
            buff[2] = 'L';
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") turnLeft=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") turnLeft=" + status);
        return status;
    }

    /**
     * 右转
     */
    public boolean turnRight() {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do turnRight, please call stop() first");
            return false;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[3];
            buff[0] = 'R';
            buff[1] = 'M';
            buff[2] = 'R';
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") turnRight=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") turnRight=" + status);
        return status;
    }

    /**
     * 左旋 度数可以超过360
     */
    public boolean rotateLeftByAngle(int angle) {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do rotateLeftByAngle(" + angle
                    + "), please call stop() first");
            return false;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[5];
            buff[0] = 'R';
            buff[1] = 'A';
            buff[2] = 'L';
            buff[3] = (byte) ((angle >> 8) & 0xFF);
            buff[4] = (byte) (angle & 0xFF);
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") rotateLeftByAngle(" + angle + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") rotateLeftByAngle(" + angle + ")=" + status);
        return false;
    }

    /**
     * 右旋 度数可以超过360
     */
    public void rotateRightByAngle(int angle) {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do rotateRightByAngle(" + angle
                    + "), please call stop() first");
            return;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[5];
            buff[0] = 'R';
            buff[1] = 'A';
            buff[2] = 'R';
            buff[3] = (byte) ((angle >> 8) & 0xFF);
            buff[4] = (byte) (angle & 0xFF);
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") rotateRightByAngle(" + angle + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") rotateRightByAngle(" + angle + ")=" + status);

    }

    /**
     * 左转 度数可以超过360
     */
    public void turnLeftByAngle(int angle) {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do turnLeftByAngle(" + angle
                    + "), please call stop() first");
            return;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[5];
            buff[0] = 'R';
            buff[1] = 'A';
            buff[2] = 'l';
            buff[3] = (byte) ((angle >> 8) & 0xFF);
            buff[4] = (byte) (angle & 0xFF);
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") turnLeftByAngle(" + angle + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") turnLeftByAngle(" + angle + ")=" + status);

    }

    /**
     * 右转 度数可以超过360
     */
    public void turnRightByAngle(int angle) {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do turnRightByAngle(" + angle
                    + "), please call stop() first");
            return;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[5];
            buff[0] = 'R';
            buff[1] = 'A';
            buff[2] = 'r';
            buff[3] = (byte) ((angle >> 8) & 0xFF);
            buff[4] = (byte) (angle & 0xFF);
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") turnRightByAngle(" + angle + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") turnRightByAngle(" + angle + ")=" + status);

    }

    /**
     * 设值档位 0-3
     */
    public void setSpeed(int speed) {
        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[3];
            buff[0] = 'S';
            buff[1] = 'S';
            if (speed == 0)
                buff[2] = 'S';
            else if (speed == 1)
                buff[2] = 'M';
            else if (speed == 2)
                buff[2] = 'H';
            else if (speed == 3)
                buff[2] = 'L';
            else
                buff[2] = 'M';

            send(buff);
            Log.w(TAG, "Car(" + mName + ") setSpeed(" + speed + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") setSpeed(" + speed + ")=" + status);

    }

    /**
     * 分别设值左右轮电机的转速 [-4 , 4],负数代表反转
     */
    public void setMotorSpeed(int ls, int rs) {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do setMotorSpeed(" + ls + "," + rs
                    + "), please call stop() first");
            return;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[4];
            buff[0] = 'S';
            buff[1] = 'M';
            buff[2] = (byte) ls;
            buff[3] = (byte) rs;
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") setMotorSpeed(" + ls + "," + rs + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") setSpeed(" + ls + "," + rs + ")=" + status);

    }

    /**
     * 摇摆，晃动
     */
    public void shake() {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do shake, please call stop() first");
            return;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[2];
            buff[0] = 'E';
            buff[1] = 'S';
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") shake=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") shake=" + status);

    }

    /**
     * 设值角度 0-359
     */
    public boolean setAngle(int angle) {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do setAngle(" + angle
                    + "), please call stop() first");
            return false;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[3];
            buff[0] = 'A';
            buff[1] = (byte) ((angle >> 8) & 0xFF);
            buff[2] = (byte) (angle & 0xFF);
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") setAngle(" + angle + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") setAngle(" + angle + ")=" + status);
        return status;
    }

    public void stop() {
        boolean status = (mServiceBinder != null);
        mRunning = false;
        if (status) {
            byte[] buff = new byte[3];
            buff[0] = 'R';
            buff[1] = 'M';
            buff[2] = 'S';
            send(buff);
            Log.w(TAG, "Car(" + mName + ") stop=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") stop=" + status);

    }

    /**
     * 小车led灯光控制
     *
     * @param color 灯光颜色 0-关；1-绿；2-蓝；3-红；4-青；5-黄；6-紫；7-白
     * @ms ms 显示时间（毫秒）
     */
    public void led(int color, int ms) {
        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[7];
            buff[0] = 'L';
            buff[1] = 'H';
            buff[2] = (byte) ('0' + color);
            buff[3] = '1';// open
            buff[4] = 'T';
            buff[5] = (byte) ((ms >> 8) & 0xFF);
            buff[6] = (byte) (ms & 0xFF);
            send(buff);
            Log.w(TAG, "Car(" + mName + ") led(" + color + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") led(" + color + ")=" + status);

    }

    /**
     * 小车led的效果控制（如：闪烁、颜色交替闪等）
     *
     * @param type  效果类型 0-关；1-开；2-呼吸；3-快闪
     * @param color 颜色值 0-关；1-绿；2-蓝；3-红；4-青；5-黄；6-紫；7-白
     * @ms ms 显示时间（毫秒）
     */
    public void led(int type, int color, int ms) {
        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[7];
            buff[0] = 'L';
            buff[1] = 'H';
            buff[2] = (byte) ('0' + color);
            buff[3] = (byte) ('0' + type);
            buff[4] = 'T';
            buff[5] = (byte) (ms >> 8);
            buff[6] = (byte) (ms & 0x00FF);
            send(buff);
            Log.w(TAG, "Car(" + mName + ") led(" + type + "," + color + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") led(" + type + "," + color + ")=" + status);

    }

    /**
     * 小车震动 type:0-关；1-开
     *
     * @param ms 震动时长(毫秒)
     */
    public void vibrate(boolean isOn, int ms) {
        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[5];
            buff[0] = 'V';
            buff[1] = (byte) (isOn ? '1' : '0');
            buff[2] = 'T';
            buff[3] = (byte) ((ms >> 8) & 0xFF);
            buff[4] = (byte) (ms & 0xFF);
            send(buff);
            Log.w(TAG, "Car(" + mName + ") vibrate(" + isOn + "," + ms + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") vibrate(" + isOn + "," + ms + ")=" + status);

    }

    /**
     * 寻线模式，小车自动走到指定的坐标
     *
     * @param x
     * @param y
     */
    public boolean moveTo(int x, int y, boolean isForward) {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do moveTo(" + x + "," + y + "," + isForward
                    + "), please call stop() first");
            return false;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[6];
            buff[0] = (byte) (isForward ? 'F' : 'B');
            buff[1] = 'P';
            buff[2] = (byte) ((x >> 8) & 0xFF);
            buff[3] = (byte) (x & 0xFF);
            buff[4] = (byte) ((y >> 8) & 0xFF);
            buff[5] = (byte) (y & 0xFF);
            mRunning = true;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") moveTo(" + x + "," + y + "," + isForward + ")=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") moveTo(" + x + "," + y + "," + isForward + ")=" + status);
        return status;
    }

    public void getVer(OnGetVerListener listener) {
        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[2];
            buff[0] = 'G';
            buff[1] = 'V';
            mOnGetVerListener = listener;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") getVer=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") getVer=" + status);
    }

    public void setDfuMode() {
        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[3];
            buff[0] = 'D';
            buff[1] = 'F';
            buff[2] = 'U';
            send(buff);
            Log.w(TAG, "Car(" + mName + ") setDfuMode=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") setDfuMode=" + status);
    }

    public void getPos(OnGetPosListener listener) {
        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[2];
            buff[0] = 'G';
            buff[1] = 'P';
            mOnGetPosListener = listener;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") getPos=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") getPos=" + status);
    }

    public void getAngle(OnGetAngleListener listener) {
        if (mRunning) {
            Log.e(TAG, "Car(" + mName + ") is running, if you need do getDirection, please call stop() first");
            return;
        }

        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[2];
            buff[0] = 'G';
            buff[1] = 'A';
            mRunning = true;
            mOnGetAngleListener = listener;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") getDirection=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") getDirection=" + status);
    }

    public void send(byte[] cmd) {
        if (mServiceBinder != null)
            mServiceBinder.send(cmd);
    }

    public void getBatteryPercent(OnGetBatListener listener) {
        boolean status = (mServiceBinder != null);
        if (status) {
            byte[] buff = new byte[2];
            buff[0] = 'G';
            buff[1] = 'E';
            mOnGetBatListener = listener;
            send(buff);
            Log.w(TAG, "Car(" + mName + ") getBatteryPercent=" + status);
        } else
            Log.e(TAG, "Car(" + mName + ") getBatteryPercent=" + status);
    }


    private int[] getRxDataArray(String rx) {
        String regEx = "[^0-9]";// 匹配指定范围内的数字

        // Pattern是一个正则表达式经编译后的表现模式
        Pattern p = Pattern.compile(regEx);

        // 一个Matcher对象是一个状态机器，它依据Pattern对象做为匹配模式对字符串展开匹配检查。
        Matcher m = p.matcher(rx);

        // 将输入的字符串中非数字部分用空格取代并存入一个字符串
        String string = m.replaceAll(" ").trim();

        if (string.equals(""))
            return null;

        // 以空格为分割符在讲数字存入一个字符串数组中
        String[] strArr = string.split(" ");

        if (strArr == null || strArr.length == 0)
            return null;
        int[] ret = new int[strArr.length];

        // 遍历数组转换数据类型输出
        for (int i = 0; i < strArr.length; ++i) {
            // System.out.println(Integer.parseInt(s));
            ret[i] = Integer.parseInt(strArr[i]);
        }
        return ret;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mOnStateChangeListener = listener;
    }

    public void setOnKeyEventListener(OnKeyEventListener listener) {
        mOnKeyEventListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void setOnInterruptListener(OnInterruptListener listener) {
        mOnInterruptListener = listener;
    }

    public void setOnPosChangeListener(OnPosChangeListener listener) {
        mOnPosChangeListener = listener;
    }

    public void setOnGetVersionListener(OnGetVerListener listener) {
        mOnGetVerListener = listener;
    }

    public void setOnMaxLostCommandExceedListener(OnMaxLostCommandExceedListener listener) {
        mOnMaxLostCommandExceedListener = listener;
    }

    public interface OnStateChangeListener {
        void OnStateChange(int status);
    }

    public interface OnCompletionListener {
        void onCompletion(String cmd);
    }

    public interface OnInterruptListener {
        void onInterrupt(String cmd);
    }

    // get angle
    public interface OnGetAngleListener {
        void onGetAngle(int dir);
    }

    public interface OnGetPosListener {
        void onGetPos(int x, int y);
    }

    public interface OnGetVerListener {
        void onGetVer(String ver);
    }

    public interface OnGetBatListener {
        void onGetBat(int level);
    }

    public interface OnKeyEventListener {
        void onKeyEvent(int event);
    }

    public interface OnPosChangeListener {
        void onPosChange(int x, int y, int angle);
    }

    // 连续多条指令执行失败
    public interface OnMaxLostCommandExceedListener {
        void OnMaxLostCommandExceed();
    }

}
