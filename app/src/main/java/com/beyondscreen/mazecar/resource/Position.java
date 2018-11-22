package com.beyondscreen.mazecar.resource;

public class Position {

	/**返回当前坐标在格子中的下标*/
	public static int getIndex(int x, int y) {
		return (y-1)*12+(x-1);
	}

	/**返回某个值得坐标*/
	public static int[] getCoordinate(int p) {
		int x=p%12+1;
		int y=p/12+1;
		int[] c={x,y};
		return c;
	}

	/**返回当前坐标在格子中的下标*/
	public static int getIndexForSub(int x, int y) {
		return (y-2)*12+(x-1);
	}

	/**返回某个值得坐标*/
	public static int[] getCoordinateSub(int p) {
		int x=p%12+1;
		int y=p/12+2;
		int[] c={x,y};
		return c;
	}
}
