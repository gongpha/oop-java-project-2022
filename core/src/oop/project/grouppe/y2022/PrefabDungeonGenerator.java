package oop.project.grouppe.y2022;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

// generate a dungeon from premade rooms

public class PrefabDungeonGenerator extends Thread implements DungeonGenerator {
	public static final String[] prefabs = new String[] {
		"_entrance",
		"leftroom", "centerroom", "rightroom",
		"hallwayhorizontal", "hallwayvertical",
		"corner1", "corner2", "corner3", "corner4",
		"corner5"
	};

	public TiledMap getMap() {
		if (done) return map;
		return null;
	}

	public Vector2[] getPaperSpawns() {
		return new Vector2[] {
			new Vector2(16, 16)
		};
	}

	public Vector2[] getPowerSpawns() {
		return new Vector2[] {
			new Vector2(17, 17)
		};
	}

	public float getSpawnPointX() {
		return 2;
	}

	public float getSpawnPointY() {
		return 2;
	}

	public float getEnemySpawnPointX() {
		return 64;
	}

	public float getEnemySpawnPointY() {
		return 64;
	}

	public byte[][] getColTiles2DArray() {
		return coltiles;
	}
	
	private class RoomPrefab {
		TiledMap tiledMap;
		int sizeX;
		int sizeY;
		boolean special;
	}
	
	private class RoomDoor {
		RoomPrefab from;
		/*
		0 : LEFT
		1 : TOP
		2 : RIGHT
		3 : BOTTOM
		*/
		int direction;
		int thickness;
		
		// global pos
		int posX;
		int posY;
		// local pos
		int poslocalX;
		int poslocalY;
		
		public RoomDoor clone() {
			RoomDoor newobj = new RoomDoor();
			newobj.from = from;
			newobj.direction = direction;
			newobj.thickness = thickness;
			newobj.posX = posX;
			newobj.posY = posY;
			newobj.poslocalX = poslocalX;
			newobj.poslocalY = poslocalY;
			return newobj;
		}
		
		public int getRoomX() {
			return posX - poslocalX;
		}
		public int getRoomY() {
			return posY - poslocalY;
		}
		public Rectangle getRoomRectangle() {
			MapProperties props = from.tiledMap.getProperties();
			return new Rectangle(
				getRoomX(), getRoomY(),
				props.get("width", Integer.class),
				props.get("height", Integer.class)
			);
		}
		public Rectangle getRectangleOfNeighborDoor(RoomDoor door) {
			int roomX = 0;
			int roomY = 0;
			//TiledMap thatTiledMap = door.from;
			
			int doorMarginX = poslocalX - door.poslocalX;
			int doorMarginY = poslocalY - door.poslocalY;
			
			switch (direction) {
			case 0: // LEFT
				roomX = getRoomX() - door.from.sizeX;
				roomY = getRoomY() + doorMarginY;
				break;
			case 1: // TOP
				roomX = getRoomX() + doorMarginX;
				roomY = getRoomY() + from.sizeY;
				break;
			case 2: // RIGHT
				roomX = getRoomX() + from.sizeX;
				roomY = getRoomY() + doorMarginY;
				break;
			case 3: // BOTTOM
				roomX = getRoomX() + doorMarginX;
				roomY = getRoomY() - door.from.sizeY;
				break;
			}
			return new Rectangle(roomX, roomY, door.from.sizeX, door.from.sizeY);
		}
		
		public boolean equals(RoomDoor obj) {
			return (
				from == obj.from &&
				poslocalX == obj.poslocalX &&
				poslocalY == obj.poslocalY
			);
		}
	}
	
	private long seed;
	private int maxRoomCount;
	private final HashMap<String, RoomPrefab> loadedTiledMaps;
	private final HashMap<Integer, ArrayList<RoomDoor>> doorsByDirection;
	private final HashMap<RoomPrefab, ArrayList<RoomDoor>> doorsByPrefab;
	
	private LinkedList<Room> placedRooms;
	private Rectangle dungeonBound;
	
	private final TiledMap map;
	private byte coltiles[][];
	private boolean done = false;
	
	public PrefabDungeonGenerator(long seed, int maxRoomCount) {
		this.seed = seed;
		this.maxRoomCount = maxRoomCount;
		
		loadedTiledMaps = new HashMap<>();
		doorsByDirection = new HashMap<>();
		doorsByPrefab = new HashMap<>();
		
		map = new TiledMap();
		placedRooms = new LinkedList<Room>();
	}
	
	public long getSeed() { return seed; }
	
	public void startGenerate() {
		start();
	}
	
	private int getDirection(String dir) {
		if (dir.equals("LEFT")) return 0;
		if (dir.equals("TOP")) return 1;
		if (dir.equals("RIGHT")) return 2;
		if (dir.equals("BOTTOM")) return 3;
		return -1;
	}
	
	public void run() {
		ResourceManager rm = ResourceManager.instance();
		for (String name : prefabs) {
			TiledMap m = (TiledMap) rm.get("prefab__" + name);
			
			RoomPrefab pre = new RoomPrefab();
			MapProperties rprops = m.getProperties();
			
			pre.tiledMap = m;
			pre.sizeX = rprops.get("width", Integer.class);
			pre.sizeY = rprops.get("height", Integer.class);
			pre.special = name.startsWith("_");
			
			loadedTiledMaps.put(name, pre);
			
			// scan all doors
			MapObjects objs = m.getLayers().get("HINT").getObjects();
			for (MapObject o : objs) {
				MapProperties props = o.getProperties();
				int direction = getDirection(props.get("for", "", String.class));
				
				RoomDoor door = new RoomDoor();
				door.from = pre;
				door.poslocalX = (int)((Float)props.get("x")).floatValue() / 32;
				door.poslocalY = (int)((Float)props.get("y")).floatValue() / 32;
				door.thickness = props.get("size", 0, Integer.class);
				door.direction = direction;
				
				ArrayList<RoomDoor> subdoors;
				if (doorsByDirection.containsKey(direction)) {
					subdoors = doorsByDirection.get(direction);
				} else {
					subdoors = new ArrayList<>();
					doorsByDirection.put(direction, subdoors);
				}
				subdoors.add(door);
				
				if (doorsByPrefab.containsKey(pre)) {
					subdoors = doorsByPrefab.get(pre);
				} else {
					subdoors = new ArrayList<>();
					doorsByPrefab.put(pre, subdoors);
				}
				subdoors.add(door);
			}
		}
		
		
		Room root = placeNewRoom(loadRoom("_entrance"));
		Random r = new Random(seed);
		
		LinkedList<RoomDoor> pendingDoors = new LinkedList<>();
		root.readDoors(pendingDoors);
		while (!pendingDoors.isEmpty() && placedRooms.size() <= maxRoomCount) {
			RoomDoor door = pendingDoors.pollFirst();
			
			// choose a door for placing
			ArrayList<RoomDoor> list = doorsByDirection.get(getInvertedDirection(door.direction));
			int index = Math.abs(r.nextInt());
			int tryCount = 0;
			boolean placed = false;
			
			while (tryCount < list.size()) {
				/*
				System.out.println((index));
				System.out.println((tryCount));
				System.out.println((index + tryCount));
				System.out.println((index + tryCount) % list.size());
				System.out.println("##########################");*/
				tryCount += 1;
				RoomDoor targetDoor = list.get((index + tryCount) % list.size());
				if (targetDoor.from == door.from) continue; // must be different
				if (targetDoor.thickness != door.thickness) continue;
				if (targetDoor.from.special) continue; // special room. ignore
				
				RoomPrefab owner = targetDoor.from;
				//MapProperties props = owner.getProperties();
				Rectangle rect = door.getRectangleOfNeighborDoor(targetDoor);
				
				// check overlaps
				boolean overlaped = false;
				
				for (Room thatRoom : placedRooms) {
					if (rect.overlaps(thatRoom.getRect())) {
						// oh no
						overlaped = true;
						break;
					}
				}
				if (!overlaped) {
					// PLACEEEEE
					Room newRoom = placeNewRoom(owner, rect);
					newRoom.readDoors(pendingDoors, targetDoor);
					break;
				}
			}
			if (!placed) {
				// oof
			}
		}
		
		// merge all rooms
		int boundX = (int)dungeonBound.width; 
		int boundY = (int)dungeonBound.height;
		coltiles = new byte[boundX][boundY];
		
		TiledMapTileLayer newLayer = new TiledMapTileLayer(boundX, boundY, 32, 32);
		
		for (Room thatRoom : placedRooms) {
			RoomPrefab rp = thatRoom.roomPrefab;
			for (MapLayer l : rp.tiledMap.getLayers()) {
				if (l instanceof TiledMapTileLayer) {
					for (int x = 0; x < rp.sizeX; x++) {
						for (int y = 0; y < rp.sizeY; y++) {
							int posX = thatRoom.roomX + x - (int)dungeonBound.x;
							int posY = thatRoom.roomY + y - (int)dungeonBound.y;
							Cell c = ((TiledMapTileLayer) l).getCell(x, y);
							if (c == null) continue;
							newLayer.setCell(posX, posY, c);
							if (!c.getTile().getProperties().containsKey("free")) {
								//System.out.println(posX);
								//System.out.println(posY);
								//System.out.println("##############");
								coltiles[posX][posY] = 1;
							}
						}
					}
				}
			}
		}
		
		map.getLayers().add(newLayer);
		
		done = true;
	}
	
	private Room placeNewRoom(RoomPrefab roomPrefab, Rectangle onRect) {
		Room room = new Room(roomPrefab, (int)onRect.x, (int)onRect.y);
		placedRooms.add(room);
		if (dungeonBound == null) {
			dungeonBound = room.getRect();
		} else {
			dungeonBound = dungeonBound.merge(room.getRect());
		}
		
		return room;
	}
	
	private Room placeNewRoom(RoomPrefab roomPrefab) {
		return placeNewRoom(roomPrefab, new Rectangle(0, 0, 0, 0));
	}
	
	private RoomPrefab loadRoom(String room) {
		return loadedTiledMaps.get(room);
	}
	
	private class Room {
		RoomPrefab roomPrefab;
		int roomX;
		int roomY;
		
		public Room(RoomPrefab room, int roomX, int roomY) {
			roomPrefab = room;
			this.roomX = roomX;
			this.roomY = roomY;
		}
		
		public void readDoors(LinkedList<RoomDoor> pendingDoors) {
			readDoors(pendingDoors, null);
		}
		
		public void readDoors(LinkedList<RoomDoor> pendingDoors, RoomDoor targetDoor) {
			ArrayList<RoomDoor> list = doorsByPrefab.get(roomPrefab);
			for (RoomDoor d : list) {
				if (targetDoor != null) {
					if (targetDoor.equals(d)) {
						continue;
					}
				}
				RoomDoor newDoor = d.clone();
				newDoor.posX = newDoor.poslocalX + roomX;
				newDoor.posY = newDoor.poslocalY + roomY;
				pendingDoors.add(newDoor);
			}
		}
		
		public Rectangle getRect() {
			return new Rectangle(
				roomX, roomY,
				roomPrefab.sizeX, roomPrefab.sizeY
			);
		}
	}
	
	public static int getInvertedDirection(int dir) {
		return dir + (dir > 1 ? -2 : 2);
	}
	
}
