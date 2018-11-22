package com.beyondscreen.mazecar.resource;

import com.beyondscreen.mazecar.common.IntentBeyond;


public class S {

	public static int v_name1=0;
	public static int v_up=1;
	public static int v_dowm=2;
	public static int v_left=3;
	public static int v_right=4;
	public static int v_pieces=5;//放置路径卡
	public static int v_monsters=6;
	public static int v_command=7;
	public static int v_name2=8;
	public static int v_limit=9;
	public static int v_noroad=10;
	public static int v_nomonster=11;
	public static int IDX_VOICE_SELECT_DIFF=12;
	public static int IDX_VOICE_CLEAN=13;
	/**点击开始进入游戏 */
	public static final int IDX_VOICE_INTOGAME= 14;//
	/**放入身份卡 */
	public static final int IDX_VOICE_PUTID= 15;//
	/**移除身份卡 */
	public static final int IDX_VOICE_REMOVEID= 16;//
	public static String[] voices={
			"n10v01.m4a",
			"n10v02.m4a",
			"n10v03.m4a",
			"n10v04.m4a",
			"n10v05.m4a",
			"n10v06.m4a",
			"n10v07.m4a",
			"n10v08.m4a",
			"n10v09.m4a",
			"n10v10.m4a",
			"n10v11.m4a",
			"n10v12.m4a",
			"n10v001.m4a",
			"l17v010.m4a",
			"l15v073.m4a",
			"a03v063.m4a",
			"a03v064.m4a",
	};

	public static int net_welcom=0;
	public static int net_good=1;
	public static int net_diamond=2;
	public static int net_exit=3;
	public static String[] voicesNet={
			"net/net_welcom.m4a",
			"net/net_good.m4a",
			"net/net_diamond.m4a",
			"net/net_exit.m4a",
	};

	public static String[] numV={
			"net/l16v017.m4a",
			"net/l16v018.m4a",
			"net/l16v019.m4a",
			"net/l16v020.m4a",
			"net/l16v021.m4a",
			"net/l16v022.m4a",
	};

	public static String[] vDiffcult={
			"n10v002.m4a",
			"n10v003.m4a",
			"n10v004.m4a",
	};


	public static int TONE_RIGHT=0;
	public static int TONE_ERROR=1;
	public static int TONE_CLICK=2;
	public static int TONE_SPEAK=3;
	public static int TONE_WELLDONE=4;
	public static int TONE_CARDPUT=5;
	public static int TONE_CARDERROR=6;
	//-----
	public static int TONE_GETERROR=7;
	public static int TONE_MAKEDOG=8;
	public static int TONE_MAKEMONSTER=9;
	public static int TONE_MERGE=10;
	public static int TONE_BELLOW=11;
	public static int TONE_CARDPUT2=12;

	public static String[] tones={
			"f10s001.m4a",
			"f10s002.m4a",
			"n10s007.m4a",
			"n10s008.m4a",
			"f10s005.m4a",
			"n10s002.m4a",
			"f10s007.m4a",//
			"n10s003.m4a",
			"n10s004.m4a",
			"n10s005.m4a",
			"n10s006.m4a",
			"n10s009.m4a",
			"f10s006.m4a",//
	};

	public static String[] service_tip = {
			"edu/K01V001.m4a",//进如游戏
			"edu/K01V002.m4a",//开始游戏
			"edu/K01V003.m4a",//
			"edu/K01V004.m4a",//结束
	};
	static {
		if(IntentBeyond.language != null && "en".toLowerCase().equals(IntentBeyond.language.trim().toLowerCase())) {
			voices=EngBg.voices;
			service_tip=EngBg.service_tip;
		}
	}
}
