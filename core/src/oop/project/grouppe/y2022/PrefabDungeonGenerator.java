package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

// generate a dungeon from premade rooms
// for room creation, I have written all instructions in the discord chat.

public class PrefabDungeonGenerator extends Thread implements DungeonGenerator {
	public TiledMap getMap() {
		if (done) return map;
		return null;
	}

	public Vector2[] getPaperSpawns() {
		return (Vector2[]) papersSpawnpoints.toArray(new Vector2[papersSpawnpoints.size()]);
	}
	
	public Vector2[] getMedkitSpawns() {
		return (Vector2[]) medkitsSpawnpoints.toArray(new Vector2[medkitsSpawnpoints.size()]);
	}

	public Vector2[] getPowerSpawns() {
		return (Vector2[]) powersSpawnpoints.toArray(new Vector2[powersSpawnpoints.size()]);
	}

	public Vector2[] getSpawnPoints() {
		return (Vector2[]) spawnpoints.toArray(new Vector2[spawnpoints.size()]);
	}
	
	public Vector2[] getEnemySpawnPoints() {
		return (Vector2[]) enemySpawnpoints.toArray(new Vector2[enemySpawnpoints.size()]);
	}

	public byte[][] getColTiles2DArray() {
		return coltiles;
	}
	
	private class RoomPrefab {
		TiledMap tiledMap;
		int sizeX;
		int sizeY;
		boolean special;
		
		ArrayList<Vector2> papers;
		ArrayList<Vector2> medkits;
		ArrayList<Vector2> powers;
		ArrayList<Vector2> players;
		ArrayList<Vector2> enemies;
		public RoomPrefab() {
			papers = new ArrayList<>();
			medkits = new ArrayList<>();
			powers = new ArrayList<>();
			players = new ArrayList<>();
			enemies = new ArrayList<>();
		}
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
		
		public boolean isHorizontal() {
			// 0, 1, 2, 3
			// L, T, R, B
			// H  V  H  V
			return direction % 2 == 0;
		}
		
		public boolean isDirectionNegativeAxis() {
			// Left and Bottom (0, 3)
			return direction % 3 == 0;
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
	
	private final boolean isLobby;
	private final long seed;
	private final int maxRoomCount;
	private final HashMap<String, RoomPrefab> loadedTiledMaps;
	private final HashMap<Integer, ArrayList<RoomDoor>> doorsByDirection;
	private final HashMap<RoomPrefab, ArrayList<RoomDoor>> doorsByPrefab;
	
	private final LinkedList<Room> placedRooms;
	private Rectangle dungeonBound;
	
	private final TiledMap map;
	private byte coltiles[][];
	private boolean done = false;
	
	private final StaticTiledMapTile barrierVert;
	private final StaticTiledMapTile barrierHori;
	
	private final ArrayList<Vector2> spawnpoints; // players
	private final ArrayList<Vector2> enemySpawnpoints;
	private final ArrayList<Vector2> papersSpawnpoints;
	private final ArrayList<Vector2> medkitsSpawnpoints;
	private final ArrayList<Vector2> powersSpawnpoints;
	private RectangleMapObject entranceArea;
	
	public PrefabDungeonGenerator(long seed, int maxRoomCount, boolean isLobby) {
		this.seed = seed;
		this.maxRoomCount = maxRoomCount;
		
		loadedTiledMaps = new HashMap<>();
		doorsByDirection = new HashMap<>();
		doorsByPrefab = new HashMap<>();
		
		map = new TiledMap();
		placedRooms = new LinkedList<>();
		
		Texture items = (Texture) ResourceManager.instance().get("items");
		barrierVert = new StaticTiledMapTile(new TextureRegion(items, 0, 64, 32, 32));
		barrierHori = new StaticTiledMapTile(new TextureRegion(items, 0, 96, 32, 32));
		
		spawnpoints = new ArrayList<>();
		enemySpawnpoints = new ArrayList<>();
		papersSpawnpoints = new ArrayList<>();
		medkitsSpawnpoints = new ArrayList<>();
		powersSpawnpoints = new ArrayList<>();
		this.isLobby = isLobby;
	}
	
	public PrefabDungeonGenerator(long seed, int maxRoomCount) {
		this(seed, maxRoomCount, false);
	}
	
	// starts as lobby
	public PrefabDungeonGenerator() {
		this(0, 1, true);
	}
	
	public long getSeed() { return seed; }
	
	public void startGenerate() {
		start();
	}
	
	public void startGenerateInstant() {
		run();
	}
	
	private int getDirection(String dir) {
		if (dir.equals("LEFT")) return 0;
		if (dir.equals("TOP")) return 1;
		if (dir.equals("RIGHT")) return 2;
		if (dir.equals("BOTTOM")) return 3;
		return -1;
	}
	
	private void importPointsFromLayer(MapLayer layer, ArrayList<Vector2> output) {
		if (layer == null) return;
		MapObjects layerObjects = layer.getObjects();
		for (MapObject o : layerObjects) {
			MapProperties props = o.getProperties();
			Vector2 v = new Vector2(
				(int)((Float)props.get("x")).floatValue() / 32,
				(int)((Float)props.get("y")).floatValue() / 32
			);
			output.add(v);
		}
	}
	
	public void run() {
		ResourceManager rm = ResourceManager.instance();
		for (String name : Customization.ROOMS) {
			TiledMap m = (TiledMap) rm.get("prefab__" + name);
			RoomPrefab pre = new RoomPrefab();
			MapProperties rprops = m.getProperties();
			
			
			if (isLobby != name.equals("__lobby")) continue;
			
			pre.tiledMap = m;
			pre.sizeX = rprops.get("width", Integer.class);
			pre.sizeY = rprops.get("height", Integer.class);
			pre.special = name.startsWith("_");
			
			loadedTiledMaps.put(name, pre);
			
			// scan all doors
			
			// get all points in the layers
			MapLayers layers = m.getLayers();
			importPointsFromLayer(layers.get("PAPERS"), pre.papers);
			importPointsFromLayer(layers.get("MEDKITS"), pre.medkits);
			importPointsFromLayer(layers.get("POWERS"), pre.powers);
			importPointsFromLayer(layers.get("PLAYERS"), pre.players);
			importPointsFromLayer(layers.get("ENEMIES"), pre.enemies);
			
			MapLayer ml = layers.get("ENTRANCE");
			MapObjects layerObjects = null;
			
			if (ml != null) {
				layerObjects = ml.getObjects();
				for (MapObject o : layerObjects) {
					if (o instanceof RectangleMapObject) {
						Rectangle r = ((RectangleMapObject) o).getRectangle();
						entranceArea = new RectangleMapObject(
							r.x, r.y, r.width, r.height
						);
						break;
					}
				}
			}
			if (entranceArea == null) {
				// the entrance didn't exist
				CoreGame.instance().getConsole().print("The entrance area didn't exist.");
				entranceArea = new RectangleMapObject(0, 0, 0, 0); // avoid errors
			}
			
			
			// get all connections
			ml = layers.get("HINT");
			if (ml != null) {
				layerObjects = ml.getObjects();
				for (MapObject o : layerObjects) {
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
		}
		
		Random r = new Random(seed);
		Room root = placeNewRoom(loadRoom(isLobby ? "__lobby" : "_entrance"), r);
		
		LinkedList<RoomDoor> pendingDoors = new LinkedList<>();
		LinkedList<RoomDoor> toPutBarriers = new LinkedList<>();
		
		root.readDoors(pendingDoors);
		while (!pendingDoors.isEmpty() && placedRooms.size() <= maxRoomCount) {
			RoomDoor door = pendingDoors.pollFirst();
			
			// choose a door for placing
			ArrayList<RoomDoor> list = doorsByDirection.get(getInvertedDirection(door.direction));
			int index = Math.abs(r.nextInt());
			int tryCount = 0;
			boolean placed = false;
			
			while (tryCount < list.size()) {
				tryCount += 1;
				RoomDoor targetDoor = list.get((index + tryCount) % list.size());
				if (targetDoor.from == door.from) continue; // must be different
				if (targetDoor.thickness != door.thickness) continue;
				if (targetDoor.from.special) continue; // special room. ignored
				
				RoomPrefab owner = targetDoor.from;
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
					Room newRoom = placeNewRoom(owner, rect, r);
					newRoom.readDoors(pendingDoors, targetDoor);
					placed = true;
					break;
				}
			}
			if (!placed) {
				// put barriers
				toPutBarriers.add(door);
			}
		}
		
		// merge all rooms
		int boundX = (int)dungeonBound.width; 
		int boundY = (int)dungeonBound.height;
		coltiles = new byte[boundX][boundY];
		
		// add position to points
		Vector2 neg = new Vector2((int)dungeonBound.x, (int)dungeonBound.y);
		neg.scl(-32.0f);
		addPositionPoints(papersSpawnpoints, neg);
		addPositionPoints(medkitsSpawnpoints, neg);
		addPositionPoints(powersSpawnpoints, neg);
		addPositionPoints(spawnpoints, neg);
		addPositionPoints(enemySpawnpoints, neg);
		entranceArea.getRectangle().x -= (int)dungeonBound.x * 32;
		entranceArea.getRectangle().y -= (int)dungeonBound.y * 32;
		
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
								coltiles[posX][posY] = 1;
							}
						}
					}
				}
			}
		}
		
		map.getLayers().add(newLayer);
		
		if (!toPutBarriers.isEmpty()) {
			// deadend hallways
			TiledMapTileLayer barrierLayer = new TiledMapTileLayer(boundX, boundY, 32, 32);
			Cell bVert = new Cell();
			bVert.setTile(barrierVert);
			Cell bHori = new Cell();
			bHori.setTile(barrierHori);

			for (RoomDoor barrier : toPutBarriers) {
				for (int i = 0; i < barrier.thickness; i++) {
					int posX, posY;
					Cell toPlace;
					if (barrier.isHorizontal()) {
						posX = barrier.posX;
						posY = barrier.posY - i;
						toPlace = bVert;
						if (!barrier.isDirectionNegativeAxis()) {
							posX -= 1;
						}
					} else {
						posX = barrier.posX + i;
						posY = barrier.posY;
						toPlace = bHori;
						if (barrier.isDirectionNegativeAxis()) {
							posY += 1;
						}
					}
					posX -= (int)dungeonBound.x;
					posY -= (int)dungeonBound.y + 1;
					barrierLayer.setCell(posX, posY, toPlace);
					coltiles[posX][posY] = 1;
				}
			}
			map.getLayers().add(barrierLayer);
		}
		
		
		done = true;
	}
	
	private Room placeNewRoom(RoomPrefab roomPrefab, Rectangle onRect, Random rand) {
		Room room = new Room(roomPrefab, (int)onRect.x, (int)onRect.y);
		placedRooms.add(room);
		if (dungeonBound == null) {
			dungeonBound = room.getRect();
		} else {
			dungeonBound = dungeonBound.merge(room.getRect());
		}
		
		// take points
		room.importPoints(rand);
		
		return room;
	}
	
	private void addPositionPoints(ArrayList<Vector2> list, Vector2 v) {
		for (Vector2 d : list) {
			d.add(v);
		}
	}
	
	private Room placeNewRoom(RoomPrefab roomPrefab, Random rand) {
		return placeNewRoom(roomPrefab, new Rectangle(0, 0, 0, 0), rand);
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
			if (list == null) return;
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
		
		private void importPoints(Random rand, ArrayList<Vector2> from, ArrayList<Vector2> to, float chance) {
			Vector2 roomPos = new Vector2(roomX, roomY);
			for (Vector2 v : from) {
				if (rand.nextFloat() > chance) continue;
				to.add(v.cpy().add(roomPos).scl(32.0f));
			}
		}
		
		public void importPoints(Random rand) {
			importPoints(rand, roomPrefab.papers, papersSpawnpoints, 0.5f);
			importPoints(rand, roomPrefab.medkits, medkitsSpawnpoints, 0.33f);
			importPoints(rand, roomPrefab.powers, powersSpawnpoints, 0.5f);
			importPoints(rand, roomPrefab.players, spawnpoints, 1.0f);
			importPoints(rand, roomPrefab.enemies, enemySpawnpoints, 1.0f);
		}
	}
	
	public static int getInvertedDirection(int dir) {
		return dir + (dir > 1 ? -2 : 2);
	}
	
	public RectangleMapObject getEntranceRect() {
		return entranceArea;
	}
}
