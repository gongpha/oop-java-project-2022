package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import java.util.Random;

// a dungeon generator powered by binary space partitioning (may be related to dsa)
// i feel stupido

public class BSPDungeonGenerator extends Thread {
	private long seed;
	private int sizeX;
	private int sizeY;
	private int scale = 5;
	private TiledMap map;
	private TiledMapTileLayer layerRoom;
	private MapLayer layerObj;
	
	private final StaticTiledMapTile wall;
	private final StaticTiledMapTile wallTop;
	private final StaticTiledMapTile floor;
	private final Cell Cwall;
	private final Cell CwallTop;
	private final Cell Cfloor;
	
	private final TextureMapObject entrance;
	
	private Room bottomRoom;
	private int bottomRoomY;
	
	private char[][] tiles;
	private boolean done = false;
	
	private float spawnPointX;
	private float spawnPointY;
	
	public final static String[] tilesets = {
		"dun1",
	};
	
	public BSPDungeonGenerator(long seed, int sizeX, int sizeY, Texture tileset) {
		this.seed = seed;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		
		bottomRoom = null;
		bottomRoomY = sizeY;
		
		wall = new StaticTiledMapTile(new TextureRegion(tileset, 0, 0, 32, 32));
		wallTop = new StaticTiledMapTile(new TextureRegion(tileset, 32, 0, 32, 32));
		floor = new StaticTiledMapTile(new TextureRegion(tileset, 64, 0, 32, 32));
		
		wall.getProperties().put("col", true);
		wallTop.getProperties().put("col", true);
		
		Cwall = new Cell();
		CwallTop = new Cell();
		Cfloor = new Cell();
		Cwall.setTile(wall);
		CwallTop.setTile(wallTop);
		Cfloor.setTile(floor);
		
		tiles = new char[sizeX][sizeY];
		map = new TiledMap();
		layerRoom = new TiledMapTileLayer(sizeX * scale, sizeY * scale, 32, 32);
		layerObj = new MapLayer();
		
		entrance = new TextureMapObject();
		entrance.setTextureRegion(new TextureRegion(tileset, 0, 32, 128, 32));
		layerObj.getObjects().add(entrance);
		
		layerRoom.setName("tiles");
		map.getLayers().add(layerRoom);
		layerObj.setName("objs");
		map.getLayers().add(layerObj);
	}
	
	public void startGenerate() {
		start();
	}
	
	public void run() {
		Room root = new Room(1, 1, sizeX - 2, sizeY - 2);
		Random r = new Random(seed);
		root.split(r, 6);
		printTiles();
		
		entrance.setX(bottomRoom.X * layerRoom.getTileWidth() * scale);
		entrance.setY(bottomRoom.Y * layerRoom.getTileHeight() * (scale - 1));
		spawnPointX = entrance.getX() + layerRoom.getTileWidth();
		spawnPointY = entrance.getY() + layerRoom.getTileHeight();
		
		done = true;
	}
	
	public TiledMap getMap() {
		if (done) return map;
		return null;
	}
	
	public float getSpawnPointX() {
		return spawnPointX;
	}
	public float getSpawnPointY() {
		return spawnPointY;
	}
	
	public void printTiles() {
		for (int X = 0; X < layerRoom.getWidth(); X++) {
			for (int Y = 0; Y < layerRoom.getHeight(); Y++) {
				/////////////
				int M;
				for (M = -1 + X; M <= 1 + X; M++) {
					for (int N = -1 + Y; N <= 1 + Y; N++) {
						if (M < 0 || N < 0) continue;
						if (M >= layerRoom.getWidth() || N >= layerRoom.getHeight()) continue;
						int MM = M / scale;
						int NN = N / scale;
						if (tiles[MM][NN] == 1) {
							// HAHA WALL
							//plotStaticWall(X, Y);
							layerRoom.setCell(X, Y, CwallTop);
							M = X + 3; // exit M
							break;
						}
					}
				}
				if (M != X + 3) {
					if (tiles[X / scale][Y / scale] == 1)
						layerRoom.setCell(X, Y, Cfloor);
						//plotStaticFloor(X, Y);
				}
				///////////
			}	
		}
	}
	
	public class Room {
		int sizeX;
		int sizeY;
		int X;
		int Y;
		float hv = 0.5f;
		
		Room left;
		Room right;
		
		public float lerp(float a, float b, float w)
		{
			return a * (1.0f - w) + (b * w);
		}
		
		public Room(int X, int Y, int sX, int sY) {
			this.X = X;
			this.Y = Y;
			this.sizeX = sX;
			this.sizeY = sY;
			left = null;
			right = null;
		}
		
		public void printToLayer() {
			for (int XX = 0; XX < sizeX; XX++) {
				for (int YY = 0; YY < sizeY; YY++) {
					tiles[XX + X][YY + Y] = 1;
				}
			}
		}
		
		public void shrink(Random rand) {
			int x = (int)lerp(0, sizeX / 2, rand.nextFloat());
			int y = (int)lerp(0, sizeY / 2, rand.nextFloat());
			X += x;
			Y += y;
			sizeX -= lerp(0, sizeX / 2, rand.nextFloat()) + x;
			sizeY -= lerp(0, sizeY / 2, rand.nextFloat()) + y;
			
			if (Y < bottomRoomY) {
				bottomRoom = this;
				bottomRoomY = Y;
			}
		}
		
		public void split(Random rand, int iteration) {
			float ratio = lerp(0.45f, 0.55f, rand.nextFloat());
			int ratioAt;
			boolean vert = rand.nextFloat() < hv;
			if (vert) {
				ratioAt = (int)lerp(0, sizeY, ratio);
				left = new Room(X, Y, sizeX, ratioAt);
				right = new Room(X, Y + ratioAt, sizeX, sizeY - ratioAt);
				left.hv = 0.0125f;
				right.hv = 0.0125f;
			} else {
				ratioAt = (int)lerp(0, sizeX, ratio);
				left = new Room(X, Y, ratioAt, sizeY);
				right = new Room(X + ratioAt, Y, sizeX - ratioAt, sizeY);
				left.hv = 0.9875f;
				right.hv = 0.9875f;
			}
			
			// draw corridors
			printCorridor(vert, ratioAt);
			
			if (iteration == 0) {
				left.shrink(rand);
				right.shrink(rand);
				left.printToLayer();
				right.printToLayer();
				return;
			}
			left.split(rand, iteration - 1);
			right.split(rand, iteration - 1);
		}
		
		public void printCorridor(boolean vert, int ratioAt) {
			if (vert) {
				int x = sizeX / 2 + X;
				int y1 = Y + ratioAt / 2;
				int y2 = Y + (sizeY - ratioAt) / 2 + ratioAt;
				for (int y = y1; y <= y2; y++) {
					tiles[x][y] = 1;
				}
			} else {
				int y = sizeY / 2 + Y;
				int x1 = X + ratioAt / 2;
				int x2 = X + (sizeX - ratioAt) / 2 + ratioAt;
				for (int x = x1; x <= x2; x++) {
					tiles[x][y] = 1;
				}
			}
		}
	}
}
