
package oop.project.grouppe.y2022;

// an a* search implementation for this project
// using an abstract grid based by 2d solid mask array (i forgor what it called)

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

class AStar {
	private byte[][] mapTiles;
	public AStar(byte[][] mapTiles) {
		this.mapTiles = mapTiles;
	}
	
	public class Point {
		int x, y;
		float prio = 0.0f;
		Point prev = null;
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private final int[][] neighbors = new int[][] {
		{1, 0}, {0, 1}, {-1, 0}, {0, -1},
		//{-1, -1}, {1, 1}, {-1, 1}, {1, -1},
	};
	
	public float estCost(Point a, Point b) {
		return (float) ( // the euclidean distance of two points
			Math.sqrt((b.y - a.y) * (b.y - a.y) + (b.x - a.x) * (b.x - a.x))
		);
	}
	
	public float heu(Point a, Point b) { // heuristic, "should i walk to this point ?"
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y); // simply the manhattan distance
	}
	
	private class prioCom implements Comparator<Point> {
		public int compare(Point a, Point b) {
			return (a.prio < b.prio) ? -1 : ((a.prio > b.prio) ? 1 : 0);
		}
	}
	
	public Point findNearestWalkable(int x, int y) {
		// just a crappy breadth-first search algorithm that you've learned in the dsa class
		LinkedList<Point> list = new LinkedList<>();
		list.add(new Point(x, y));
		
		byte[][] looked = new byte[mapTiles.length][mapTiles[0].length];
		
		while (!list.isEmpty()) {
			Point curr = list.pollFirst();
			for (int i = 0; i < neighbors.length; i++) {
				int nx = curr.x + neighbors[i][0];
				int ny = curr.y + neighbors[i][1];
				
				if (
					nx < 0 || ny < 0 ||
					nx >= mapTiles.length || ny >= mapTiles[0].length
				) continue;
				
				if (looked[nx][ny] == 1) continue;
				
				if (
					mapTiles[nx][ny] == 0
				) {
					// we made it !
					return new Point(nx, ny);
				}
				
				looked[nx][ny] = 1;
				list.add(new Point(nx, ny));
			}
		}
		return null; // this poor sap point shouldn't be here . . .
	}
	
	public Point getPath(int x, int y, int gx, int gy) {
		PriorityQueue<Point> queue = new PriorityQueue<>(1, new prioCom());
		Point first = new Point(x, y);
		Point goal = new Point(gx, gy);
		
		if (
			x < 0 || y < 0 ||
			x >= mapTiles.length || y >= mapTiles[0].length
		) return null;
		
		if (
			mapTiles[x][y] != 0
		) first = findNearestWalkable(x, y);
		if (first == null) { return new Point(x, y); }
		
		if (
			mapTiles[gx][gy] != 0
		) goal = findNearestWalkable(gx, gy);
		if (goal == null) {
			return new Point(gx, gy);
		}
		
		
		queue.add(first);
		
		float[][] costs = new float[mapTiles.length][mapTiles[0].length];
		for (int i = 0; i < mapTiles.length; i++) {
			for (int j = 0; j < mapTiles[0].length; j++) {
				costs[i][j] = -1;
			}
		}
		costs[x][y] = 0.0f;
		
		while (!queue.isEmpty()) {
			Point curr = queue.poll();
			if (curr.x == goal.x && curr.y == goal.y) {
				goal = curr;
				break;
			}
				
			
			for (int i = 0; i < neighbors.length; i++) {
				int nx = curr.x + neighbors[i][0];
				int ny = curr.y + neighbors[i][1];
				if (
					nx < 0 || ny < 0 ||
					nx >= mapTiles.length || ny >= mapTiles[0].length
				) continue;
				if (
					mapTiles[nx][ny] != 0
				) continue; // solid
				
				Point n = new Point(nx, ny);
				float cost = costs[curr.x][curr.y] + estCost(curr, n);
				if (costs[nx][ny] == -1) {
					costs[nx][ny] = cost;
					float prio = cost + heu(n, goal);
					n.prio = prio;
					queue.add(n);
					n.prev = curr;
				}
			}
		}
		
		return (goal.prev != null) ? goal : null;
	}
}
