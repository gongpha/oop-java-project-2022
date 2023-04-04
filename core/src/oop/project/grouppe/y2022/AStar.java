
package oop.project.grouppe.y2022;

// haha finding

import java.util.Comparator;
import java.util.PriorityQueue;


class AStar {
	private char[][] mapTiles;
	public AStar(char[][] mapTiles) {
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
	
	public float estCost(Point a, Point b) {
		return (float) (
			Math.sqrt((b.y - a.y) * (b.y - a.y) + (b.x - a.x) * (b.x - a.x))
		);
	}
	
	public float heu(Point a, Point b) {
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
	}
	
	private class prioCom implements Comparator<Point> {
		public int compare(Point a, Point b) {
			return (a.prio < b.prio) ? -1 : ((a.prio > b.prio) ? 1 : 0);
		}
	}
	
	public Point getPath(int x, int y, int gx, int gy) {
		PriorityQueue<Point> queue = new PriorityQueue<>(1, new prioCom());
		Point first = new Point(x, y);
		Point goal = new Point(gx, gy);
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
			if (curr.x == gx && curr.y == gy) {
				goal = curr;
				break;
			}
				
			
			final int[][] neighbors = new int[][] {
				{1, 0}, {0, 1}, {-1, 0}, {0, -1},
				//{-1, -1}, {1, 1}, {-1, 1}, {1, -1},
			};
			for (int i = 0; i < neighbors.length; i++) {
				int nx = curr.x + neighbors[i][0];
				int ny = curr.y + neighbors[i][1];
				if (
					nx < 0 || ny < 0 ||
					nx >= mapTiles.length || ny >= mapTiles.length
				) continue;
				if (
					mapTiles[nx][ny] == 1	
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
