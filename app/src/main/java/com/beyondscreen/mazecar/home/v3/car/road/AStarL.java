package com.beyondscreen.mazecar.home.v3.car.road;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.beyondscreen.mazecar.resource.Position;

/**
 * 低级难度路径工具
 * */
public class AStarL {

	public static final int[][] NODES = {
			{ 0, 0, 0, 0, 0, 0},
			{ 0, 0, 0, 0, 0, 0},
			{ 0, 0, 0, 0, 0, 0},
			{ 0, 0, 0, 0, 0, 0},
			{ 0, 0, 0, 0, 0, 0},
			{ 0, 0, 0, 0, 0, 0},
	};

	public static final int STEP = 10;

	private ArrayList<Node> openList = new ArrayList<Node>();
	//private Query<Node> openList = new PriorityQueue<Node>();
	private ArrayList<Node> closeList = new ArrayList<Node>();

	public Node findMinFNodeInOpneList() {
		Node tempNode = openList.get(0);
		for (Node node : openList) {
			if (node.F < tempNode.F) {
				tempNode = node;
			}
		}
		return tempNode;
	}

	public ArrayList<Node> findNeighborNodes(Node currentNode) {
		ArrayList<Node> arrayList = new ArrayList<Node>();
		// 只考虑上下左右，不考虑斜对角
		int topX = currentNode.x;
		int topY = currentNode.y - 1;
		if (canReach(topX, topY) && !exists(closeList, topX, topY)) {
			arrayList.add(new Node(topX, topY));
		}
		int bottomX = currentNode.x;
		int bottomY = currentNode.y + 1;
		if (canReach(bottomX, bottomY) && !exists(closeList, bottomX, bottomY)) {
			arrayList.add(new Node(bottomX, bottomY));
		}
		int leftX = currentNode.x - 1;
		int leftY = currentNode.y;
		if (canReach(leftX, leftY) && !exists(closeList, leftX, leftY)) {
			arrayList.add(new Node(leftX, leftY));
		}
		int rightX = currentNode.x + 1;
		int rightY = currentNode.y;
		if (canReach(rightX, rightY) && !exists(closeList, rightX, rightY)) {
			arrayList.add(new Node(rightX, rightY));
		}
		return arrayList;
	}

	public boolean canReach(int x, int y) {
		if (x >= 0 && x < NODES[0].length && y >= 0 && y < NODES.length) {
			return NODES[y][x] == 0;// 以横向为x轴，纵向为y轴，则（0,3）对应的值是NODES[3][0]
		}
		return false;
	}

	public Node findPath(Node startNode, Node endNode) {
		//用前先清空
		openList.clear();
		closeList.clear();
		// 把起点加入 open list
		openList.add(startNode);

		while (openList.size() > 0) {
			// 遍历 open list ，查找 F值最小的节点，把它作为当前要处理的节点
			Node currentNode = findMinFNodeInOpneList();
			// 从open list中移除
			openList.remove(currentNode);
			// 把这个节点移到 close list
			closeList.add(currentNode);
			// 找出当前方格附近所有可以移动到的格子（不包含已经走过的closeList中的格子）
			ArrayList<Node> neighborNodes = findNeighborNodes(currentNode);
			for (Node node : neighborNodes) {
				if (exists(openList, node)) {
					// 如果已经存在，检查是否路径是否比之前更近，是则修改父节点
					foundPoint(currentNode, node);
				} else {
					// 如果openList不含这个格子，放进去，并把当前格子currentNode设为他的父节点用来最后寻找路径用
					// 并计算F/G/H
					notFoundPoint(currentNode, endNode, node);
				}
			}
			if (find(openList, endNode) != null) {
				return find(openList, endNode);
			}
		}

		return find(openList, endNode);
	}

	private void foundPoint(Node tempStart, Node node) {
		int G = calcG(tempStart, node);
		if (G < node.G) {
			node.parent = tempStart;
			node.G = G;
			node.calcF();
		}
	}

	private void notFoundPoint(Node tempStart, Node end, Node node) {
		node.parent = tempStart;
		node.G = calcG(tempStart, node);
		node.H = calcH(end, node);
		node.calcF();
		openList.add(node);
	}

	/** STEP=10是个基数，也就是F=G+H都是10的倍数计数 */
	private int calcG(Node start, Node node) {
		int G = STEP;
		int parentG = node.parent != null ? node.parent.G : 0;
		return G + parentG;
	}

	private int calcH(Node end, Node node) {
		int step = Math.abs(node.x - end.x) + Math.abs(node.y - end.y);
		return step * STEP;
	}


	public static Node find(List<Node> nodes, Node point) {
		for (Node n : nodes)
			if ((n.x == point.x) && (n.y == point.y)) {
				return n;
			}
		return null;
	}

	public static boolean exists(List<Node> nodes, Node node) {
		for (Node n : nodes) {
			if ((n.x == node.x) && (n.y == node.y)) {
				return true;
			}
		}
		return false;
	}

	public static boolean exists(List<Node> nodes, int x, int y) {
		for (Node n : nodes) {
			if ((n.x == x) && (n.y == y)) {
				return true;
			}
		}
		return false;
	}

	public static class Node {
		public Node(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int x;
		public int y;

		public int F;// 格子价值评估，也就是当前道路在无障碍时所需总步数
		public int G;// 从起点到当前位置已经走过的步数
		public int H;// 从当前位置到终点还需要的步数

		public void calcF() {
			this.F = this.G + this.H;
		}

		public Node parent;
	}

	//----------------------------------------------以下为配合游戏使用的方法-------------------------
	private static AStarL aStarH;
	private static AStarL getInstance(){
		if(aStarH==null){
			aStarH=new AStarL();
		}
		return aStarH;
	}
	/**
	 * 得到两个点间的路径，会返回底图上的下标{@link Position}
	 * 根据回溯法，返回顺序是从终点到起点
	 * 本路径中NODES的坐标和底图的坐标相差为1，不完全对应
	 * 返回的点包含起始点和终点[start,end]
	 * */
	public static ArrayList<Integer> getRoad(int fromX,int fromY,int toX,int toY){
		Node startNode = new Node(fromX-1, fromY-6);
		Node endNode = new Node(toX-1, toY-6);
		Node parent =getInstance().findPath(startNode, endNode);
		ArrayList<Integer> arrayList = new ArrayList<Integer>();
		while (parent != null) {
			//arrayList.add(new Node(parent.x, parent.y));
			//System.out.println(parent.x+"---"+ parent.y);

			arrayList.add(exchange(parent.x,parent.y));
			parent = parent.parent;
		}
		return arrayList;
	}

	private static Integer exchange(int x, int y) {
		Log.d("test", x+"--exchange--"+ y);
		return Position.getIndex(x+1, y+6);//加1是为了对应底图坐标
	}

	/**设置某个位置为障碍点*/
	public static void setSnooker(int x,int y){
		NODES[y-6][x-1]=1;
	}
	/**删除某个位置为障碍点*/
	public static void delSnooker(int x,int y){
		NODES[y-6][x-1]=0;
	}
	/**清空障碍点*/
	public static void cleanSnooker(){
		for (int i = 0; i < NODES.length; i++) {
			for (int j = 0; j < NODES[i].length; j++) {
				NODES[i][j]=0;
			}
		}
	}
}
