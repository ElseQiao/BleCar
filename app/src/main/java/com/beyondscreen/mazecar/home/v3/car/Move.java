package com.beyondscreen.mazecar.home.v3.car;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.beyond.card.CardEvent;
import com.beyondscreen.mazecar.common.LED;
import com.beyondscreen.mazecar.common.MusicUtil;
import com.beyondscreen.mazecar.common.ValueUtil;
import com.beyondscreen.mazecar.home.v3.car.utils.Command;
import com.beyondscreen.mazecar.home.v3.car.utils.Point;
import com.beyondscreen.mazecar.home.v3.car.road.AStarH;
import com.beyondscreen.mazecar.home.v3.car.road.AStarL;
import com.beyondscreen.mazecar.home.v3.car.road.AStarM;
import com.beyondscreen.mazecar.home.v3.car.btutils.BeyondCarOrder;
import com.beyondscreen.mazecar.home.v3.car.interfaces.BeyondCarStateListener;
import com.beyondscreen.mazecar.home.v3.car.btutils.BeyondScanBle;
import com.beyondscreen.mazecar.home.v3.car.utils.ButtonBreath;
import com.beyondscreen.mazecar.home.v3.car.utils.ThreadLight;
import com.beyondscreen.mazecar.resource.Position;
import com.beyondscreen.mazecar.resource.S;

public class Move implements BeyondCarStateListener,BeyondCarOrder.OnCompletionListener{
	private String TAG = "test";
	private MainActivity context;
	public int status = 0;
	public int STATUS_DIFFCULT = 1;
	public int STATUS_DEAL = 2;
	public int STATUS_MOVE = 3;
	/**
	 * 是否可以点击开始按键进入游戏
	 * */
	private boolean canStart = false;
	/**
	 * 是否可以执行指令，只有在小车连接下才可以执行
	 * */

	/** 怪兽摆放记录 */
	private Set<Integer> monster = new HashSet<>();
	/** 赋值怪兽 */
	private List<String> cardList = new ArrayList<>();
	/** 用户指令记录 */
	private LinkedList<Command> commands = new LinkedList<Command>();
	/**
	 *记录点亮的路径，当棋子放在点亮过的路径上时不再规划路线
	 * */
	private Set<Integer> lightSets = new HashSet<>();
	/** 小车的当前坐标 */
	// private int[] carCoordinate = { 1, 11 };
	private MediaPlayer playerT;
	private MediaPlayer playerV;
	private SoundPool soundPool;
	private ThreadLight threadLight;
	private Handler sonHandler;
	private BeyondScanBle beyondScanBle;
	private ButtonBreath buttonBreath;

	private int currBoardX = 1;// 贝板坐标(触摸坐标系)
	private int currBoardY = 11;
	private int currDir = BeyondCarOrder.DIR_N;// 上北下南左西右东
	private Point[][] touch2PosMap = new Point[14][14];
	private int xpos[] = { -1, 37, 50, 64, 77, 90, 103, 116, 130, 143, 156,
			169, 182, -1 };
	private int ypos[] = { -1, -1, 49, 62, 75, 88, 101, 114, 127, 141, 154,
			167, -1, -1 };

	private int putLight = 0x333333;
	/** 延时消息处理 */
	// private final int LIGHT_FLASH = 0;// 闪烁某个位置灯
	private static final int MOVE_CONTINUE = 1;// 捕捉怪兽灯效展示完毕后继续指令
	private final int LIGHT_SUCCESS = 2;// 成功答题等效
	private static final int COMMAND_LIGHT_FLASH = 3;// 输入提醒命令
	private static final int MONSTER_LIGHT_FLASH = 4;// 摆放怪兽提醒灯效
	private static final int START_GAME = 5;// 进入游戏
	private static final int START_LED = 6;// 开始平铺灯效
	private static final int LIGHT_ERROR = 7;// 错误的灯光
	private static final int START_LED2 = 8;// 开始平铺灯效
	private static final int FINSH_DELAY = 9;// 灯光效果完成后执行完成指令
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case MOVE_CONTINUE:
					if (msg.arg2 == 2) {
						resetLed();
					}
					looperCommands();
					break;
				case LIGHT_SUCCESS:
					ledOn(msg.arg1);
					break;
				case MONSTER_LIGHT_FLASH:
					LED.set(1, 12, msg.arg1 == 0 ? putLight : 0xffffff);
					break;
				case COMMAND_LIGHT_FLASH:
					LED.ledDrawLine(4, 12, 8, 12, msg.arg1 == 0 ? 0xffffff
							: putLight);
					break;
				case START_GAME:
					name();
					break;
				case START_LED:
					if (msg.arg1 == 0) {
						startLed();
					} else {
						LED.startLedShowAll(msg.arg2);
					}
					break;
				case START_LED2:
					if (msg.arg1 == 0) {
						startLedDong();
						LED.ledFillBox(0, 0, 13, 13, 0xffffff);
					} else {
						LED.startLedShowNew(msg.arg2);
					}
					break;
				case LIGHT_ERROR:
					if (msg.arg2 == 14) {
						resetLed();
						// finsh();
					} else {
						LED.ledErrorAnim(msg.arg1, msg.arg2);
					}
					break;
				case FINSH_DELAY:
					finsh();
					break;
				default:
					break;
			}
		}
	};


	public Move(MainActivity context) {
		this.context = context;
		LED.ledFillBox(0, 0, 13, 13, 0);
		threadLight = new ThreadLight("Light_Move");
		threadLight.start();
		sonHandler = new Handler(threadLight.getLooper(), new WorkCallback());
		initMap();
		initCar();
	}

	// 初始化贝板坐标系与底图点读笔坐标系影射
	private void initMap() {
		for (int i = 0; i < 14; i++)
			for (int j = 0; j < 14; j++) {
				touch2PosMap[i][j] = new Point(xpos[i], ypos[j]);
			}
	}

	private void initCar() {
		beyondScanBle=new BeyondScanBle(context.getApplicationContext());
		beyondScanBle.setBeyondCarStateListener(this);//小车状态修改
		//小车指令有些回调，需要哪些注册哪些，怪兽里只用到了指令完成的监听
		beyondScanBle.getBeyondCarOrder().setOnCompletionListener(this);
	}

	/**SoundPool 按键点击声*/
	int TONE_CLICK;
	int TONE_RIGHT;
	int v_conflict;
	public void startMove() {
		playerT = new MediaPlayer();
		playerV = new MediaPlayer();
		soundPool=new SoundPool.Builder().setMaxStreams(1).build();
		buttonBreath = new ButtonBreath();
		buttonBreath.startButtonBreath();
		handler.sendEmptyMessageDelayed(START_GAME, 8000);
		// dong音效灯光
		Message m = Message.obtain();
		m.what = START_LED2;
		m.arg1 = 0;
		handler.sendMessageDelayed(m, 4800);
		// 平铺音效灯光
		Message m1 = Message.obtain();
		m1.what = START_LED;
		m1.arg1 = 0;
		handler.sendMessageDelayed(m1, 6000);
		// 彩灯轮转
		LED.roundCorlorMini(-1, -1, 85);
		TONE_CLICK = MusicUtil.loadPool(context, soundPool, S.tones[S.TONE_CLICK]);
		TONE_RIGHT = MusicUtil.loadPool(context, soundPool, S.tones[S.TONE_RIGHT]);
		v_conflict= MusicUtil.loadPool(context, soundPool,"l15s029.m4a");//"a03v064.m4a",
		playT("f10m001.m4a");
		scanCar(5000);
	}

	private void scanCar(int delayTime) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				buttonBreath.setButtonStatus(0, 13, ButtonBreath.LED_FLASH,
						LED.COLOR_L_BLUE);
				beyondScanBle.startScan(true, new BeyondScanBle.ScanResultListener() {
					@Override
					public void noCarFind(int reason) {
						//没有扫描到设备，点击按钮重新扫描
						canClick=true;
						buttonBreath.setButtonStatus(0, 13, ButtonBreath.LED_FLASH,
								LED.COLOR_RED);
					}

					@Override
					public void findCar() {

					}
				});
			}
		}, delayTime);
	}

	public void stopMove() {
		if (playerT != null) {
			playerT.stop();
			playerT.release();
		}
		if (playerV != null) {
			playerV.stop();
			playerV.release();
		}
		if(soundPool!=null){
			soundPool.release();
		}
		if (buttonBreath != null) {
			buttonBreath.stopButtonBreath();
		}
		if (threadLight != null) {
			threadLight.quit();
		}

		if(beyondScanBle!=null){
			beyondScanBle.releaseBle();
		}

		// start = false;
		handler.removeCallbacksAndMessages(null);
	}

	protected void name() {
		playT(S.voices[S.v_name1]);
		playerT.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				playerT.setOnCompletionListener(null);
				// go();
				selectDegreeTip();
			}
		});
	}

	/** 难度选择 */
	protected void selectDegreeTip() {
		status = STATUS_DIFFCULT;
		playV(S.voices[S.IDX_VOICE_SELECT_DIFF]);
		setDegreeLight(true);
	}

	private int mDegree = 0;

	protected void selectDegree(int degree) {
		// degree 2,3,4
		if (status > STATUS_DIFFCULT) {
			resetValue();
		}
		setDegreeLight(false);
		mDegree = degree - 2;
		playV(S.vDiffcult[mDegree]);
		LED.ledFillBox(1, 1, 12, 11, 0);
		LED.set(degree, 0, 0xffffff);
		switch (mDegree) {
			case 0:
				LED.ledDrawLine(1, 5, 7, 5, 0xfff);
				LED.ledDrawLine(7, 11, 7, 5, 0xfff);
				AStarL.cleanSnooker();
				break;
			case 1:
				LED.ledDrawLine(1, 3, 9, 3, 0xfff);
				LED.ledDrawLine(9, 11, 9, 3, 0xfff);
				AStarM.cleanSnooker();
				break;
			case 2:
				AStarH.cleanSnooker();
				break;
			default:
				break;
		}
		buttonBreath.setButtonStatus(5, 0, ButtonBreath.LED_FLASH, 0xffffff);
		canStart = true;
		playerV.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				playerV.setOnCompletionListener(null);
				playV(S.voices[S.IDX_VOICE_INTOGAME]);
			}
		});
	}

	/** 重置参数 */
	private void resetValue() {
		status = STATUS_DIFFCULT;
		canStart = false;
		threadLight.removeAll();
		handler.removeCallbacksAndMessages(null);
		commands.clear();
		lightSets.clear();
		monster.clear();
		closeDealButton();
	}

	// 按键点击
	public void btClick(int x) {

		if (status < STATUS_DIFFCULT) {
			return;
		}
		switch (x) {
			case 2:
			case 3:
			case 4:
				selectDegree(x);
				break;
			case 5:
				if (canStart) {
					canStart = false;
					buttonBreath.setButtonStatus(5, 0, ButtonBreath.LED_COLSE,
							0xffffff);
					//playV(S.tones[S.TONE_RIGHT]);
					if(playerV.isPlaying()){
						playerV.stop();
					}
					soundPool.play(TONE_RIGHT, 1, 1, 1, 0, 1);
					// go();
					checkPieceOnboard();
				}
				break;
			default:
				break;
		}

	}

	private void go() {
		status = STATUS_DEAL;
		LED.set(1, 12, 0xfff);
		// 操作键灯光
		lightDealButton();
		// playT(S.voices[S.v_pieces]);
	}

	/** 怪兽放入 */
	public void monsterCardDown(int action, int x, int y, byte[] uid) {

		if (x > 29 || y > 29) {
			return;
		}

		if (status < STATUS_DEAL)
			return;

		if (outSide(x, y)) {
			playT(S.tones[S.TONE_CARDERROR]);
			// 非卡片区域
			return;
		}

		String cardValue = ValueUtil.BinaryToHexString(uid);
		if (x == 1 && y == 12) {
			if(status != STATUS_DEAL){
				return;
			}

			if (action == CardEvent.ACTION_DOWN) {
				// 不限制数量
				if (cardList.contains(cardValue)) {
					playT(S.tones[S.TONE_MAKEMONSTER]);
					return;
				}
				cardList.add(cardValue);
				playT(S.tones[S.TONE_MAKEMONSTER]);
			}
			return;
		}

		if (action == CardEvent.ACTION_DOWN) {
			if (!cardList.contains(cardValue)) {
				// 卡片没有认证
				playT(S.tones[S.TONE_CARDERROR]);
				return;
			}
			monster.add(Position.getIndex(x, y));
			drawRoad(x, y);
			playT(S.tones[S.TONE_BELLOW]);
			Log.d("test", "down::" + Position.getIndex(x, y));
		}

		if (action == CardEvent.ACTION_UP) {
			int p = Position.getIndex(x, y);
			if (monster.contains(p)) {
				monster.remove(p);
				reDrawAllRoad(x, y);
			}
		}
	}

	/** 路径动画的起点，以12,11为起点，当有怪兽放置时起点变更 */
	private int animalStart[] = { 1, 11 };
	/**
	 * 画出两点之间的路径
	 * */
	private void drawRoad(int x, int y) {
		// 如果当前点不再已经规划的路径上，则规划路径，并展示动画
		if (!lightSets.contains(Position.getIndex(x, y))) {
			ArrayList<Integer> list = getRoad(animalStart[0], animalStart[1],
					x, y);
			if (list.size() > 1) {
				// 设置新的起点,因为路径包含终点，所以下一次开始要从倒数第二个点开始
				int[] newP = Position.getCoordinate(list.get(1));
				animalStart[0] = newP[0];
				animalStart[1] = newP[1];
			}
			if (list.size() > 0) {
				// 动画
				threadLight.lightRoadAnimal(list);
			}
			// 记录点亮路径集合
			for (int i = 0; i < list.size(); i++) {
				if (i == 0 || i == list.size() - 1) {
					// 终点和起点不计入
					continue;
				}
				lightSets.add(list.get(i));
			}
		}
		// 当前位置设置为障碍点
		setSnooker(x, y);
	}

	/**
	 * 画出两点之间的路径
	 * */
	private void reDrawAllRoad(int x, int y) {
		// 如果当前点在已经点亮的路径上，则只需要移除障碍即可
		if (lightSets.contains(Position.getIndex(x, y))) {
			delSnooker(x, y);// 删除当前障碍
			return;
		}
		// 重新规划路线
		delSnooker(x, y);// 删除当前障碍
		animalStart[0] = 1;
		animalStart[1] = 11;
		lightSets.clear();
		LED.ledFillBox(1, 1, 12, 11, 0);
		drawDegreeL();
		for (int p : monster) {
			int[] endXy = Position.getCoordinate(p);
			delSnooker(endXy[0], endXy[1]);// 先把当前点设置为非障碍点,否则寻路走不到这个点
			drawRoad(endXy[0], endXy[1]);
		}
	}

	public void controlButton(int action, int x) {

		if (x == 11) {
			if (status == STATUS_DEAL || status == STATUS_MOVE)
				// 恢复起始位置
				resetIndex();
			return;

		}

		if (status != STATUS_DEAL)
			return;

		switch (x) {
			case 3:
			case 4:
			case 5:
			case 7:
			case 8:
			case 9:
				// recordDeal(x);
				recordMoveCommand(x);
				break;
			case 6:
				// 喇叭
				// recordDeal(x);
				recordMoveCommand(x);
				break;
			case 11:
				// 恢复起始位置
				//resetIndex();
				break;
			case 12:
				// 确认
				wellDone();
				break;
			default:
				break;
		}
	}

	private void recordMoveCommand(int x) {
		// x 3左转 4左 5上 7下 8右 9右转
		//playT(S.tones[S.TONE_RIGHT]);
		soundPool.play(TONE_RIGHT, 1, 1, 1, 0, 1);
		if (commands.size() == 0) {
			// 第一个命令，初始方向车头向上
			commands.offer(new Command("init", 0, 0));
		}

		if (x == 6) {
			commands.offer(new Command("catch", 0, 0));
			return;
		}

		if (x == 3) {
			commands.offer(new Command("turnLeft", 0, 3));
			return;
		}

		if (x == 9) {
			commands.offer(new Command("turnRight", 0, 9));
			return;
		}

		// 判断和之前指令是否一致，一致则直接添加移动距离，否则添加新指令
		Command c = commands.getLast();
		if (c.buttonX == x) {
			c.arg++;
		} else {
			commands.offer(new Command("forward", 1, x));
		}

		Log.d("test", "commands size....." + commands.size());
		for (Command cs : commands) {
			Log.d("test", cs.type + "-----step:" + cs.arg + "-----preBt:"
					+ cs.buttonX);
		}
	}

	/** 是否第一步，如果第一步就无法移动，则检查是否已经在该点周围布置了道路。如果没有则添加提示 */
	// private boolean isFristStep = true;

	private void wellDone() {
		status = STATUS_MOVE;
		soundPool.play(TONE_RIGHT, 1, 1, 1, 0, 1);
		// TODO 蓝牙状态没搜到和没有连接
		if (!beyondScanBle.isDeviceConnected()) {
//            buttonBreath.setButtonStatus(0, 13, ButtonBreath.LED_FLASH,
//                    LED.COLOR_YELLOW);
			finsh();// 小车没有连接蓝牙
			return;
		}

		//int[] xy = MonsterCar.getCarCoordinate();
		int[] xy={ 1, 11 };
		if (xy == null) {
			finsh();// 小车没有连接蓝牙或者没有在贝板上
			return;
		}
		// TODO　－－－－
		// if(carIsOutSide()){
		// finsh();//小车在界外
		// return;
		// }

		// 更新当前小车位置
		// carCoordinate[0]=xy[0];
		// carCoordinate[1]=xy[1];

		//playT(S.tones[S.TONE_RIGHT]);
		closeDealButton();
		// 放入子线程执行
		sonHandler.sendEmptyMessage(0);
	}

	class WorkCallback implements Handler.Callback {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
				case 0:
					looperCommands();
					break;
			}
			return true;
		}
	}



	public void looperCommands() {
		Command command = commands.poll();

		if (command == null) {
			finsh();
			Log.e("finish", "no command....");
			return;
		}
		if (command.type.equals("catch")) {
			monsterSpeak();
			// finsh();结束在mosterSpeak()中
			return;
		}
		boolean result = false;
		switch (command.type) {
			case "init":
				//TODO　初始化方向先取消
				result = beyondScanBle.getBeyondCarOrder().setAngle(90);// 方向朝北

				currBoardX = 1;
				currBoardY = 11;
				currDir = BeyondCarOrder.DIR_N;
				//result=sonHandler.sendEmptyMessage(0);
				break;
			case "forward":
				// 先判断车身方向是否正确
				if (!isRightDir(command.buttonX)) {
					errorDir();
					return;
				}

				switch (currDir) {
					case BeyondCarOrder.DIR_E:// 东
						currBoardX += command.arg;
						break;
					case BeyondCarOrder.DIR_S:// 南
						currBoardY += command.arg;
						break;
					case BeyondCarOrder.DIR_W:// 西
						currBoardX -= command.arg;
						break;
					case BeyondCarOrder.DIR_N:// 北
						currBoardY -= command.arg;
						break;
				}

				if (carIsOutSide(currBoardX, currBoardY)) {
					Log.e("test", "Command Q is exec Error,the end!!! currBoardX="
							+ currBoardX + ",currBoardY=" + currBoardY);
					canNotArrive();
					return;
				}

				Point p = new Point(touch2PosMap[currBoardX][currBoardY].x,
						touch2PosMap[currBoardX][currBoardY].y);
				// coordinateCalibration(p,currDir);//由于笔头不是在车的中心点，所以需要坐标校准，每款车的算法不尽相同

				if (p.x < 0 || p.y < 0) {
					Log.e(TAG, "pos is invailed,the end!!! cx=" + p.x + ",cy="
							+ p.y);
					canNotArrive();
					return;
				}
				Log.e(TAG, "move to cx=" + p.x + ",cy=" + p.y);
				result = beyondScanBle.getBeyondCarOrder().moveTo(p.x, p.y, true);
				if(result){
					carEffect("forward");
				}
				break;
			case "back":

				break;
			case "turnRight":
				result = beyondScanBle.getBeyondCarOrder().turnRight();

				switch (currDir) {
					case BeyondCarOrder.DIR_E:// 东
						currDir = BeyondCarOrder.DIR_S;
						break;
					case BeyondCarOrder.DIR_S:// 南
						currDir = BeyondCarOrder.DIR_W;
						break;
					case BeyondCarOrder.DIR_W:// 西
						currDir = BeyondCarOrder.DIR_N;
						break;
					case BeyondCarOrder.DIR_N:// 北
						currDir = BeyondCarOrder.DIR_E;
						break;
				}
				if(result){
					carEffect("turnRight");
				}
				break;
			case "turnLeft":
				result = beyondScanBle.getBeyondCarOrder().turnLeft();
				switch (currDir) {
					case BeyondCarOrder.DIR_E:// 东
						currDir = BeyondCarOrder.DIR_N;
						break;
					case BeyondCarOrder.DIR_S:// 南
						currDir = BeyondCarOrder.DIR_E;
						break;
					case BeyondCarOrder.DIR_W:// 西
						currDir = BeyondCarOrder.DIR_S;
						break;
					case BeyondCarOrder.DIR_N:// 北
						currDir = BeyondCarOrder.DIR_W;
						break;
				}
				if(result){
					carEffect("turnLeft");
				}
				break;
			default:
				Log.e(TAG, "looperCommands() default.......");
				break;
		}
		if (!result) {
			finsh();
			Log.e(TAG, "looperCommands() return false.......");
		}

	}


	/**
	 * 判断车身方向和当前指令方向是否一致。 一致返回true buttonX {@link Command#buttonX}
	 * */
	private boolean isRightDir(int buttonX) {
		if (currDir == BeyondCarOrder.DIR_N && buttonX == 5)
			return true;
		if (currDir == BeyondCarOrder.DIR_S && buttonX == 7)
			return true;
		if (currDir == BeyondCarOrder.DIR_E && buttonX == 8)
			return true;
		if (currDir == BeyondCarOrder.DIR_W && buttonX == 4)
			return true;
		return false;
	}

	//小车接口，指令完成回调
	@Override
	public void onCompletion(String cmd) {
		if(playerT.isPlaying())playerT.stop();
		//TODO 根据电量显示
		//car.led(1, 1, 65535);
		if (!commands.isEmpty()) {
			sonHandler.sendEmptyMessage(0);
		} else {
			finsh();
		}
	}

	/** 行动状态1 无法抵达（越界或者有障碍） */
	private void canNotArrive() {
		playT(S.tones[S.TONE_ERROR]);
		finsh();
	}

	/** 行动状态2 方向错误，未转向无法抵达 */
	private void errorDir() {
		playT(S.tones[S.TONE_ERROR]);
		finsh();
	}

	/** 捕捉怪兽 */
	private void monsterSpeak() {
		int monsterP = catchPosition();
		if (monsterP != -1) {
			playT(S.tones[S.TONE_SPEAK]);
			int[] light = Position.getCoordinate(monsterP);
			int time = threadLight.catchMonsterLight(light[0], light[1]);
			// finsh();
			// 正确后继续
			if (!commands.isEmpty()) {
				Message m = Message.obtain();
				m.what = MOVE_CONTINUE;
				m.arg2 = 2;
				handler.sendMessageDelayed(m, time);
			} else {
				handler.sendEmptyMessageDelayed(FINSH_DELAY, time);
				// finsh();
			}
		} else {
			playT(S.tones[S.TONE_GETERROR]);
			ledError();
			// 错误后继续
			if (!commands.isEmpty()) {
				Message m = Message.obtain();
				m.what = MOVE_CONTINUE;
				handler.sendMessageDelayed(m, 1700);
			} else {
				handler.sendEmptyMessageDelayed(FINSH_DELAY, 1700);
				// finsh();
			}
		}
	}

	private int catchPosition() {
		if (monster.contains(Position.getIndex(currBoardX - 1, currBoardY))) {
			return Position.getIndex(currBoardX - 1, currBoardY);
		}
		if (monster.contains(Position.getIndex(currBoardX + 1, currBoardY))) {
			return Position.getIndex(currBoardX + 1, currBoardY);
		}
		if (monster.contains(Position.getIndex(currBoardX, currBoardY - 1))) {
			return Position.getIndex(currBoardX, currBoardY - 1);
		}
		if (monster.contains(Position.getIndex(currBoardX, currBoardY + 1))) {
			return Position.getIndex(currBoardX, currBoardY + 1);
		}
		return -1;
	}

	protected void ledOn(int arg1) {
		if (arg1 == 7) {
			LED.ledDrawLine(0, 2, 0, 11, 0);
			LED.ledDrawLine(13, 2, 13, 11, 0);
		} else {
			LED.ledDrawLine(0, 2, 0, 11, LED.mapColors[arg1]);
			LED.ledDrawLine(13, 2, 13, 11, LED.mapColors[arg1]);
		}
	}

	private void finsh() {
		status = STATUS_DEAL;
		beyondScanBle.getBeyondCarOrder().stop();
		lightDealButton();
		commands.clear();
		// 熄灭路径
		switch (mDegree) {
			case 0:
				LED.ledFillBox(1, 6, 6, 11, 0);
				break;
			case 1:
				LED.ledFillBox(1, 4, 8, 11, 0);
				break;
			case 2:
				LED.ledFillBox(1, 1, 12, 11, 0);
				break;
			default:
				break;
		}
	}

	private void resetIndex() {
		if (status == STATUS_MOVE) {
			finsh();
		}
		//playT(S.tones[S.TONE_CLICK]);
		if(playerT.isPlaying()){
			playerT.stop();
		}
		soundPool.play(TONE_CLICK, 1, 1, 1, 0, 1);
		beyondScanBle.getBeyondCarOrder().stop();
		commands.clear();
		currBoardX = 1;
		currBoardY = 11;
		LED.set(currBoardX, currBoardY, 0xffffff);
	}

	/** 操作按键正常灯光显示 */
	private void lightDealButton() {
		LED.ledDrawLine(3, 12, 9, 12, 0x222222);
		LED.set(11, 12, 0xffffff);
		LED.set(12, 12, 0xffffff);
		LED.set(7, 0, 0xffffff);
		LED.set(1, 12, 0xffffff);
	}

	private void closeDealButton() {
		LED.ledDrawLine(3, 12, 12, 12, 0);
		LED.set(7, 0, 0);
		LED.set(11, 12, 0x333333);
	}


	private void carEffect(String type) {
		playerT.setOnCompletionListener(null);
		String s="run.mp3";
		boolean loop=false;
		switch (type) {
			case "forward":
				s="run.mp3";
				loop=true;
				//car.led(2, 4, 65535);
				break;
			case "turnRight":
				s=S.voices[S.v_right];
				break;
			case "turnLeft":
				s=S.voices[S.v_left];
				break;
		}
		//playT("run.mp3");

		MusicUtil.playMusic(context, playerT, s, loop);
	}

	private void playT(String string) {
		playerT.setOnCompletionListener(null);
		MusicUtil.playMusic(context, playerT, string, false);
	}

	private void playV(String string) {
		playerV.setOnCompletionListener(null);
		MusicUtil.playMusic(context, playerV, string, false);
	}

	public void conflict() {
		//MusicUtil.playMusic(context, playerT, "l15s029.m4a", false);
		soundPool.play(v_conflict, 1, 1, 1, 0, 1);
	}

	public void tipHelp() {
		if (status != STATUS_DEAL)
			return;
		// if (positions.size() == 0) {
		// playT(S.voices[S.v_pieces]);
		// return;
		// }
		if (monster.size() == 0) {
			playT(S.voices[S.v_monsters]);
			monstersLed();
			return;
		}
		playT(S.voices[S.v_command]);
		commandLed();
	}

	private void monstersLed() {
		handler.removeMessages(MONSTER_LIGHT_FLASH);
		for (int i = 0; i < 8; i++) {
			Message m = Message.obtain();
			m.what = MONSTER_LIGHT_FLASH;
			m.arg1 = i % 2;
			handler.sendMessageDelayed(m, i * 300);
		}
	}

	private void commandLed() {
		handler.removeMessages(COMMAND_LIGHT_FLASH);
		for (int i = 0; i < 8; i++) {
			Message m = Message.obtain();
			m.what = COMMAND_LIGHT_FLASH;
			m.arg1 = i % 2;
			handler.sendMessageDelayed(m, i * 300);
		}
	}

	private void startLed() {
		for (int i = 1; i < 28; i++) {
			Message m = Message.obtain();
			m.what = START_LED;
			m.arg1 = 2;
			m.arg2 = i;
			handler.sendMessageDelayed(m, i * 90);
		}
	}

	private void startLedDong() {
		for (int i = 0; i < 7; i++) {
			Message m = Message.obtain();
			m.what = START_LED2;
			m.arg1 = 2;
			m.arg2 = i;
			handler.sendMessageDelayed(m, 80 + i * 80);
		}
	}

	/** 恢复等级线 */
	private void drawDegreeL() {
		switch (mDegree) {
			case 0:
				LED.ledDrawLine(1, 5, 7, 5, 0xfff);
				LED.ledDrawLine(7, 11, 7, 5, 0xfff);
				break;
			case 1:
				LED.ledDrawLine(1, 3, 9, 3, 0xfff);
				LED.ledDrawLine(9, 11, 9, 3, 0xfff);
				break;
			case 2:
				break;
			default:
				break;
		}
	}

	/** 恢复动画前灯光 */
	protected void resetLed() {
		LED.ledFillBox(0, 0, 13, 13, 0x000000);
		for (int p : lightSets) {
			int x = p % 12 + 1;
			int y = p / 12 + 1;
			LED.set(x, y, putLight);
		}
		LED.ledDrawLine(2, 0, 4, 0, 0xffffff);
	}

	private Random random = new Random();

	/** 恢复动画前灯光 */
	private void ledError() {
		int baseTime = 100;
		LED.ledDrawLine(0, 0, 13, 13, 0xffffff);
		LED.ledDrawLine(0, 13, 13, 0, 0xffffff);
		for (int i = 0; i < 15; i++) {
			Message m = Message.obtain();
			m.what = LIGHT_ERROR;
			m.arg1 = 0;
			m.arg2 = i;
			handler.sendMessageDelayed(m, i * 120);
			if (i < 13) {
				Message m1 = Message.obtain();
				m1.what = LIGHT_ERROR;
				m1.arg2 = i + 1;
				m1.arg1 = 1;
				// handler.sendMessageDelayed(m1,
				// i*(baseTime-20+random.nextInt(20)));
				handler.sendMessageDelayed(m1,
						i * baseTime + random.nextInt(100));
			}
			if (i < 12) {
				Message m1 = Message.obtain();
				m1.what = LIGHT_ERROR;
				m1.arg2 = i + 2;
				m1.arg1 = 2;
				// handler.sendMessageDelayed(m1,
				// i*(baseTime-15+random.nextInt(30)));
				handler.sendMessageDelayed(m1,
						i * (baseTime + 20) + random.nextInt(120));
			}
			if (i < 11) {
				Message m1 = Message.obtain();
				m1.what = LIGHT_ERROR;
				m1.arg2 = i + 3;
				m1.arg1 = 3;
				handler.sendMessageDelayed(m1,
						i * (baseTime + 25) + random.nextInt(125));
			}
			if (i < 10) {
				Message m1 = Message.obtain();
				m1.what = LIGHT_ERROR;
				m1.arg2 = i + 4;
				m1.arg1 = 4;
				handler.sendMessageDelayed(m1,
						i * (baseTime + 40) + random.nextInt(140));
			}
			if (i < 9) {
				Message m1 = Message.obtain();
				m1.what = LIGHT_ERROR;
				m1.arg2 = i + 5;
				m1.arg1 = 5;
				handler.sendMessageDelayed(m1,
						i * (baseTime + 40) + random.nextInt(140));
			}
			if (i < 8) {
				Message m1 = Message.obtain();
				m1.what = LIGHT_ERROR;
				m1.arg2 = i + 6;
				m1.arg1 = 6;
				handler.sendMessageDelayed(m1,
						i * (baseTime + 60) + random.nextInt(160));
			}
		}
	}

	/** 统一控制难度按键 */
	private void setDegreeLight(boolean b) {
		buttonBreath.setButtonStatus(2, 0, b ? ButtonBreath.LED_FLASH
				: ButtonBreath.LED_COLSE, 0xffffff);
		buttonBreath.setButtonStatus(3, 0, b ? ButtonBreath.LED_FLASH
				: ButtonBreath.LED_COLSE, 0xffffff);
		buttonBreath.setButtonStatus(4, 0, b ? ButtonBreath.LED_FLASH
				: ButtonBreath.LED_COLSE, 0xffffff);
	}

	// ------------------------------难度区分处理区域------------------------------
	private ArrayList<Integer> getRoad(int x, int y, int x1, int y1) {

		switch (mDegree) {
			case 0:
				return AStarL.getRoad(x, y, x1, y1);
			case 1:
				return AStarM.getRoad(x, y, x1, y1);
			case 2:
				return AStarH.getRoad(x, y, x1, y1);
		}
		return AStarL.getRoad(x, y, x1, y1);
	}

	private void setSnooker(int x, int y) {
		switch (mDegree) {
			case 0:
				AStarL.setSnooker(x, y);
				break;
			case 1:
				AStarM.setSnooker(x, y);
				break;
			case 2:
				AStarH.setSnooker(x, y);
				break;
			default:
				break;
		}

	}

	private void delSnooker(int x, int y) {
		switch (mDegree) {
			case 0:
				AStarL.delSnooker(x, y);
				break;
			case 1:
				AStarM.delSnooker(x, y);
				break;
			case 2:
				AStarH.delSnooker(x, y);
				break;
			default:
				break;
		}
	}

	private void cleanSnooker() {
		switch (mDegree) {
			case 0:
				AStarL.cleanSnooker();
				break;
			case 1:
				AStarM.cleanSnooker();
				break;
			case 2:
				AStarH.cleanSnooker();
				break;
			default:
				break;
		}
	}

	/** 超过怪兽摆放边界 */
	private boolean outSide(int x, int y) {
		//

		if (x == 1 && y == 12) {
			return false;
		}

		if (x < 1 || x > 12 || y < 1 || y > 11) {
			return true;
		}

		// 低级区域限制
		if (mDegree == 0) {
			if (y < 6 || x > 5) {
				return true;
			}
		}
		// 中级区域限制
		if (mDegree == 1) {
			if (y < 4 || x > 8) {
				return true;
			}
		}

		return false;
	}

	/** 小车在界外 */
	private boolean carIsOutSide(int x, int y) {
		if (x < 1 || y < 1 || y > 11 || x > 12) {
			return true;
		}

		// 低级区域限制
		if (mDegree == 0) {
			if (y < 6 || x > 5) {
				return true;
			}
		}
		// 中级区域限制
		if (mDegree == 1) {
			if (y < 4 || x > 8) {
				return true;
			}
		}
		return false;
	}

	private Timer mCheckTimer;
	private TimerTask mCheckTimerTask;
	private int checkTimes = 0;
	private boolean isChecking = false;

	/**
	 * 检查贝板上是否还存在棋子(应用于下一局)
	 */
	public void checkPieceOnboard() {
		if (isChecking)
			return;
		checkTimes = 0;
		mCheckTimer = new Timer();
		mCheckTimerTask = new TimerTask() {
			@Override
			public void run() {
				if (context.card.count() == 0 && context.touch.count() < 3) {// &&
					go();
					mCheckTimer.cancel();
					isChecking = false;
				} else {
					// 语音提示：清空贝板，才能开始游戏
					if (checkTimes % 10 == 0) {// 10次播放一次语音
						if (playerV == null) {
							mCheckTimer.cancel();
							return;
						}
						playV(S.voices[S.IDX_VOICE_CLEAN]);
					}
					checkTimes++;
				}
			}
		};
		// 开始一个定时任务 1s内扫描游戏标签
		mCheckTimer.schedule(mCheckTimerTask, 100, 1000);
		isChecking = true;
	}

	@Override
	public void carDisconnect() {
		buttonBreath.setButtonStatus(0, 13, ButtonBreath.LED_FLASH,
				LED.COLOR_YELLOW);
	}

	@Override
	public void carConnect() {
		canClick =false;
		Log.d(TAG, "carConnect: 小车连接成功回调");
		//已连接
		buttonBreath.setButtonStatus(0, 13, ButtonBreath.LED_BREATH,
				LED.COLOR_WIHTE);
	}

	/**
	 * 开机自动扫描，如果扫描失败就可以设置为true,如果连接成功就设置为false,用户 不可以 在点击。所有处理有小车zi
	 * 自动连接
	 */
	private boolean canClick =false;
	/** 蓝牙键点击 */
	public void blueBtClicked() {
		if(!canClick){
			return;
		}

		if (beyondScanBle.isDeviceConnected()) {
			//连接
			canClick =false;
			return;
		}
		canClick=false;
		beyondScanBle.startScan(true, new BeyondScanBle.ScanResultListener() {
			@Override
			public void noCarFind(int reason) {
				canClick=true;
				buttonBreath.setButtonStatus(0, 13, ButtonBreath.LED_FLASH,
						LED.COLOR_RED);
			}
			@Override
			public void findCar() {

			}
		});
		buttonBreath.setButtonStatus(0, 13, ButtonBreath.LED_FLASH,
				LED.COLOR_L_BLUE);
	}



//	int testX = 0;
//	int testY = 0;
//    boolean testB=false;
//	public void testCar(int x) {
//		if (!context.isDeviceConnected())
//			return;
//		switch (x) {
//		case 9:
//			context.getPos(new MiniCarActivity.OnGetPosListener() {
//				@Override
//				public void onGetPos(int x, int y) {
//					Log.d(blueToothTag, "OnGetPosListener---------" + x + "..."
//							+ y);
//					testX = x;
//					testY = y;
//				}
//			});
//			break;
//		case 10:
//			context.moveTo(testX, testY, true);
//			break;
//		case 11:
//			context.stop();
//			Log.d(blueToothTag, "stop---------");
//			break;
//		case 12:
//			if(testB){
//				context.led(1, 1, 65535);
//				testB=false;
//			}else{
//				context.led(2, 4, 65535);
//				testB=true;
//			}
//			break;
//		default:
//			break;
//		}
//	}
}
