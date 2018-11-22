package com.beyondscreen.mazecar.home.v3.car.utils;


public class Command {
	public String    type;   //行走类型
	public int       arg;    //行走参数
	/**
	 * @value 3左转  9右转    4左/西  5上/北  7下/南   8右/东
	 * */
	public int       buttonX;//底图上的指令x坐标

	public boolean isComplete;

	public Command(String t,int a,int x) {
		type=t;
		arg=a;
		buttonX=x;
		isComplete = false;
	}
}
