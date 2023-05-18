package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

// game world

public class World {
	private boolean markInit = false;
	private boolean ready = false;
	
	private OrthographicCamera camera;
	public OrthographicCamera getCamera() { return camera; }
	private Viewport viewport;
	private Stage stage;
	
	private int lastID = 0;
	
	private Client myClient;
	private final HashMap<Integer, Entity> entities; // < entID : Entity >
	public Entity[] dumpAllEntities() {
		return entities.values().toArray(new Entity[entities.size()]);
	}
	
	private final HashMap<Integer, Integer> clientCharacters; // < netID : entID >, included ourselves
	private final ArrayList<Character> clientCharacterList;
	public int getCharacterCount() { return clientCharacterList.size(); }
	public Character getCharacterByIndex(int i) { return clientCharacterList.get(i); }
	private final LinkedList<Integer> entitiesRemoveLater;
	
	private DungeonGenerator generator = null;
	public boolean isGenerating() { return generator != null; }
	private long generateSeed;
	public long getGameSeed() { return generateSeed; }
	
	private TiledMap worldMap = null;
	private WorldRenderer worldRenderer = null;
	private boolean waiting = false;
	private QuadTree mapQuadTree = null;
	private String currentLevelName = "The Nameless City"; // TODO : remove soon
	private byte[][] mapColTiles;
	private final LinkedList<Entity> currentLevelEntities;
	private static final int PAPERS_LIMIT = 100;
	
	private int paperCount;
	private int collectedPaperCount;
	private int collectedPaperCountPrevious;
	private int currentLevel = 0;
	public int getCurrentLevelNumber() { return currentLevel; }
	
	// usually for servers
	private boolean atLobby = false;
	public boolean isInLobby() { return atLobby; }
	private final float[] currentMapspawnPoint;
	
	// generating levels AND using for counting players at the entrance
	private final HashSet<Integer> pendingPlayer;
	
	private RectangleMapObject serverReadyArea;
	private boolean insideReadyArea = false;
	public boolean isInsideReadyArea() { return insideReadyArea; }
	
	// common
	private RectangleMapObject entranceArea;
	private boolean insideEntranceArea = false;
	public boolean isInsideEntranceArea() { return insideEntranceArea; }
	
	private final Texture overlay;
	private final Texture endBG;
	
	private HUD hud;
	public HUD getHUD() { return hud; }
	
	private boolean processing = false;
	
	private boolean drawQuadTree = false;
	public void toggleDrawQuadTree() { drawQuadTree = !drawQuadTree; }
	public boolean isDrawQuadTree() { return drawQuadTree; }
	public void setDrawQuadTree(boolean yes) { drawQuadTree = yes; }
	
	private boolean drawPath = false;

	//private final int DUNX = 48;
	//private final int DUNY = 48;
	//private final int DUNS = 5;
	
	private final boolean cameraSmooth = true;
	
	private float spectatingDelay = 0.0f; // after died
	private boolean spectatingDelayStarting = false;
	private int spectatingIndex = 0;
	private Character spectatingCharacter = null;
	
	private boolean gameEnd = false;
	public boolean isGameEnded() { return gameEnd; }
	
	private Music worldMusic;
	
	private int frameProcessed = 0;
	private boolean markRecording = false;
	private DemoWriter demoW;
	private DemoReader demoR;
	
	private final Color[] quadTreeColors;
	
	private boolean isDemoWorking(DemoHandler demhand) {
		if (demhand == null) return false;
		if (demhand.isDead()) {
			if (demoW == demhand) demoW = null;
			if (demoR == demhand) demoR = null;
			return false;
		};
		return true;
	}
	
	public boolean isDemoWriting() {
		return isDemoWorking(demoW);
	}
	
	public boolean isDemoReading() {
		return isDemoWorking(demoR);
	}
	
	public class WorldRenderer extends OrthogonalTiledMapRenderer {
		public WorldRenderer(TiledMap map) {
			super(map);
		}
		public void renderObject (MapObject object) {
			if (object instanceof TextureMapObject) {
				TextureMapObject t = (TextureMapObject)object;
				batch.draw(t.getTextureRegion(), t.getX(), t.getY());
			}
		}
	}
	
	public World() {
		this.quadTreeColors = new Color[] {
				Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA
		};
		//this.server = server;
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		currentMapspawnPoint = new float[]{0.0f, 0.0f};
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, w, h);
		
		viewport = new ExtendViewport(w, h, camera);
		stage = new Stage(viewport);
		camera.zoom = 1.0f;
		
		entities = new HashMap<>();
		clientCharacters = new HashMap<>();
		clientCharacterList = new ArrayList<>();
		entitiesRemoveLater = new LinkedList<>();
		
		hud = new HUD(this);
		
		overlay = (Texture) ResourceManager.instance().get("darkness1");
		endBG = (Texture) ResourceManager.instance().get("end");
		
		pendingPlayer = new HashSet<>();
		
		currentLevelEntities = new LinkedList();
		
		
		camera.update();
		
		/////////////////////
		
		
		//generateMap(System.nanoTime(), 0);
	}
	
	// playing the demo file instead of playing
	public World(String demoFileName) {
		this();
		demoR = new DemoReader(demoFileName, this);
	}
	
	public void startPlayDemo() {
		if (demoR == null) return;
		demoR.initializeDemo();
	}
	
	public void initWorldFromDemoFile(DataInputStream dis) throws IOException {
		generateSeed = dis.readLong();
		
		// read all entities
		int count = dis.readInt();
		for (int i = 0; i < count; i++) {
			int entID = dis.readInt();
			String entClass = dis.readUTF();
			try {
				Entity ent = (Entity) Class.forName(entClass).getDeclaredConstructor().newInstance();
				addEntity(ent, entID);
				ent.deserializeConstructor(dis);
			} catch (Exception e) {
				CoreGame.instance().getConsole().printerr("Cannot create an entity : " + e.getMessage());
				throw new IOException("Failed to add entities");
			}
		}
		
		// CREATE a fake client
		myClient = new Client();

		// read all player infos
		count = dis.readInt();
		for (int i = 0; i < count; i++) {
			int netID = dis.readInt();
			int entID = dis.readInt();

			Entity ent = entities.get(entID);
			if (ent == null) {
				CoreGame.instance().getConsole().printerr("Cannot get an entity " + entID + " for " + netID);
				return;
			}

			// create a fake player
			Player p = new Player();
			p.readStream(dis);
			if (ent instanceof Character) {
				((Character) ent).setupPlayer(p);
			}
			myClient.newPlayer(p);
			clientCharacters.put(p.getNetID(), ent.getID());
		}

		int myNetID = dis.readInt();
		Character myEntity = getCharacterByNetID(myNetID);
		spectatingCharacter = (Character) myEntity;
		myClient.setPlayer(spectatingCharacter.getPlayer());
		myClient.setCharacter(spectatingCharacter);

		frameProcessed = 0;

		// OK !
		generateMap(generateSeed, 0, 0); // tileindex 0, level 0. xd
	}
	
	public boolean isSpectating() {
		return spectatingCharacter != null;
	}
	
	public Character getSpectatingCharacter() {
		// returns the character that the camera is currently following
		// it never returns null !
		return spectatingCharacter == null ? myClient.getCharacter() : spectatingCharacter;
	}
	
	public void setLevelName(String name) {
		currentLevelName = name;
	}
	
	public int getPaperCount() {
		return paperCount;
	}
	
	public int getCollectedPaperCount() {
		return collectedPaperCount;
	}
	
	public int getPreviousCollectedPaperCount() {
		return collectedPaperCountPrevious;
	}
	
	// serverside
	public void addCollectedPaperCount(Character ch) {
		Server s = getMyClient().getServer();
		Player p = ch.getPlayer();
		
		Packet.SPlayerScoreAdd pa = new Packet.SPlayerScoreAdd();
		pa.ch = ch;
		pa.playerCurrentPaperCount = p.getScore() + 1;
		pa.currentPaperCount = collectedPaperCount + 1;
		s.broadcast(pa);
	}
	
	// serverside, debugging purpose
	public void setCollectedPaperCount(Character ch, int to) {
		Server s = getMyClient().getServer();
		Player p = ch.getPlayer();
		
		Packet.SPlayerScoreAdd pa = new Packet.SPlayerScoreAdd();
		pa.ch = ch;
		pa.playerCurrentPaperCount = p.getScore() + to;
		pa.currentPaperCount = to;
		s.broadcast(pa);
	}
	
	// serverside, PUNYA of Workpoint's BONUS (reviving)
	public void addRevingBonus(Character ch) {
		Server s = getMyClient().getServer();
		Player p = ch.getPlayer();
		
		Packet.SPlayerScoreAdd pa = new Packet.SPlayerScoreAdd();
		pa.ch = ch;
		pa.playerCurrentPaperCount = p.getScore() + 50;
		s.broadcast(pa);
	}
	
	// clientside
	public void addCollectedPaperCountPuppet(int netID, int playerCurrentPaperCount, int currentPaperCount) {
		if (currentPaperCount != -1)
			collectedPaperCount = currentPaperCount;
		
		Player p = getCharacterByNetID(netID).getPlayer();
		p.setScore(playerCurrentPaperCount);
		
		// sort players by scores
		// i lov lambdas
		clientCharacterList.sort(new Comparator<Character>() {
			public int compare(Character o1, Character o2) {
				int p1 = o1.getPlayer().getScore();
				int p2 = o2.getPlayer().getScore();
				return (p1 < p2) ? 1 : ((p1 > p2) ? -1 : 0);
			}
		});
		
		feedChat(-3,
			p.getUsername() + " collected a gold. (" + collectedPaperCount + "/" + paperCount + ")"
		, netID == getMyClient().getMyPlayer().getNetID());
		
		checkPaperCount();
	}
	
	public void checkPaperCount() {
		if (getMyClient().isServer()) {
			if (collectedPaperCount >= paperCount) {
				getMyClient().getServer().sendChat(-4, "All golds have been collected ! Come back to the entrance", -1);
			}
		}
	}
	
	public void createEntity(Entity ent) {
		int i = allocateID();
		ent.setID(i);
		addEntity(ent, i);
		
		Packet.SEntCreate p = new Packet.SEntCreate();
		p.ent = ent;
		myClient.getServer().broadcastExceptServer(p);
	}
	
	public void addEntities(Entity[] ents, boolean isLevelEntities) {
		for (int i = 0; i < ents.length; i++) {
			int id = allocateID();
			ents[i].setID(id);
			addEntity(ents[i], id);
		}
		
		if (isDemoReading()) return;
		
		Packet.SEntCreateMultiple p = new Packet.SEntCreateMultiple();
		p.ents = ents;
		p.isLevelEntities = isLevelEntities;
		myClient.getServer().broadcastExceptServer(p);
	}
	
	public void destroyEntity(Entity ent) {
		if (!isDemoReading()) {
			Packet.SEntDelete p = new Packet.SEntDelete();
			p.ent = ent;
			myClient.getServer().broadcastExceptServer(p);
		}
		deleteEntity(ent.getID());
	}
	
	private void addEntity(Entity ent, int ID) {
		ent.setWorld(this);
		stage.addActor(ent);
		entities.put(ID, ent);
		ent.setID(ID);
	}
	
	public void deleteEntity(int ID) {
		if (!entities.containsKey(ID)) return;
		
		Entity ent = entities.get(ID);
		ent.remove();
		if (processing) {
			// we cannot remove the item during iterating !
			// remove later
			entitiesRemoveLater.add(ID);
		} else {
			entities.remove(ID);
		}
		
		if (isDemoWriting()) {
			demoW.processRecordEntityDelete(frameProcessed, ID);
		}
	}
	
	public Entity getEntity(int ID) {
		return entities.get(ID);
	}
	
	public int allocateID() {
		lastID += 1;
		return lastID - 1;
	}
	
	public Entity createEntityAuthorized(int ID, String className, boolean isLevelEntities) {
		try {
			Entity ent = (Entity) Class.forName(className).getDeclaredConstructor().newInstance();
			addEntity(ent, ID);
			if (isLevelEntities)
				currentLevelEntities.add(ent);
			return ent;
		} catch (Exception e) {
			CoreGame.instance().getConsole().printerr("cannot create an entity : " + e.getMessage());
		}
		return null;
	}
	
	public TiledMap getWorldMap() {
		return worldMap;
	}
	
	public QuadTree getMapQuadTree() {
		return mapQuadTree;
	}
	
	public byte[][] getColMapTiles() {
		return mapColTiles;
	}
	
	public Character getCharacterByNetID(int netID) {
		return (Character) entities.get(clientCharacters.get(netID));
	}
	
	public HashMap<Integer, Integer> dumpCharactersEntID() {
		return clientCharacters;
	}
	
	public Character registerNewPlayer(int entID, Player p, boolean newConnect) {
		Character ent = (Character) createEntityAuthorized(entID,
			Character.class.getName(), false
		);
		ent.setupPlayer(p);
		
		clientCharacters.put(p.getNetID(), entID);
		clientCharacterList.add(ent);
		CoreGame.instance().getConsole().print("Created Player " + p.getNetID());
		
		Server server = CoreGame.instance().getServer();
		if (p.getNetID() == myClient.getMyPlayer().getNetID())
			myClient.setCharacter(ent);
		if (server != null) {
			// set on puppets too
			server.getClient(p.getNetID()).setCharacter(ent);
			
			// teleport to the spawn point
			if (worldMap != null) // map must be ready
				ent.teleport(currentMapspawnPoint[0], currentMapspawnPoint[1]);
		}
		
		// print to chat
		if (newConnect && p.getNetID() != myClient.getMyPlayer().getNetID()) {
			feedChat(-1, p.getUsername() + " joined the game", false);
		}
		
		return ent;
	}
	
	public void setMyClient(Client myClient) {
		this.myClient = myClient;
	}
	public Client getMyClient() {
		return myClient;
	}
	
	public void markReady() {
		markInit = true;
	}
	
	public void setMap(TiledMap map) {
		if (worldMap != null)
			worldMap.dispose();
		if (worldRenderer != null)
			worldRenderer.dispose();
		worldMap = map;
		worldRenderer = new WorldRenderer(map);
	}
	
	public void instantiateLobby() {
		removeOldLevelEnts();
		ensureQuadTreeCleaned();
		
		generator = new PrefabDungeonGenerator();
		generator.startGenerateInstant();
		
		if (myClient.isServer()) {
			currentMapspawnPoint[0] = generator.getSpawnPoints()[0].x;
			currentMapspawnPoint[1] = generator.getSpawnPoints()[0].y;
			serverReadyArea = generator.getEntranceRect();
		}
		mapColTiles = generator.getColTiles2DArray();
		
		// reteleport players to the spawn point (server) and revive them (both)
		for (int id : clientCharacters.values()) {
			Entity e = entities.get(id);
			e.revive();
			if (myClient.isServer())
				e.teleport(currentMapspawnPoint[0], currentMapspawnPoint[1]);
		}
		
		setMap(generator.getMap());
		generator = null; // instant gc
	}
	
	public void generateMap(long seed, int tilesetIndex, int level) {
		atLobby = false;
		
		ensureQuadTreeCleaned();
		removeOldLevelEnts();
		
		currentLevel = level;
		
		//Texture texture = (Texture) ResourceManager.instance().get("tileset__" + BSPDungeonGenerator.tilesets[tilesetIndex]);
		
		//generator = new BSPDungeonGenerator(seed, DUNX, DUNY, DUNS, texture);
		generator = new PrefabDungeonGenerator(seed, 200);
		generator.startGenerate();
		generateSeed = seed;
	}
	
	/////////////////////
	
	public boolean keyTyped (char c) {
		return hud.keyTyped(c);
	}
	
	public boolean keyDown(int i) {
		if (hud.keyDown(i)) return true;
		
		if (gameEnd && getMyClient().isServer() && i == Input.Keys.SPACE) {
			returnToLobbyRemote();
			return true;
		}
		
		if (getMyClient().isServer() && insideReadyArea) {
			if (i == Input.Keys.SPACE) {
				if (!insideReadyArea) return true;
				newLevel();
				insideReadyArea = false;
				return true;
			}
		}
		
		if (spectatingCharacter != null) {
			if (i == Input.Keys.SPACE) {
				spectate(1);
				return true;
			}
		}
		
		Character m = myClient.getCharacter();
		if (m != null) {
			return m.keyDown(i);
		}
		return false;
	}
	
	public boolean keyUp(int i) {
		if (hud.keyUp(i)) return true;
		
		Character m = myClient.getCharacter();
		if (m != null) {
			return m.keyUp(i);
		}
		
		return false;
	}
	
	public boolean handleEscapeKey() {
		return hud.handleEscapeKey();
	}
	
	// serverside
	public void newLevel() {
		atLobby = false;
		insideReadyArea = false;
		
		if (isDemoWriting()) {
			demoW.finishWriting();
		}
		
		Packet.SGenLevel p = new Packet.SGenLevel();
		p.mapName = currentLevelName;
		//p.seed = -977121143;
		p.seed = new Random().nextInt();
		p.level = currentLevel + 1;
		
		
		// add all players as pending
		for (HashMap.Entry<Integer, Integer> e : clientCharacters.entrySet()) {
			pendingPlayer.add(e.getKey());
		}
		
		int tilesetIndex = new Random().nextInt();
		p.tilesetIndex = Math.abs(tilesetIndex) % BSPDungeonGenerator.tilesets.length;
		CoreGame.instance().getConsole().print(">> " + p.seed + " : " + p.tilesetIndex);
		
		
		getMyClient().getServer().broadcast(p);	
	}
	
	public void pendingRemove(int netID) {
		pendingPlayer.remove(Integer.valueOf(netID));
	}
	
	private void ensureQuadTreeCleaned() {
		if (mapQuadTree != null) {
			// delete the old tree
			mapQuadTree.release();
			mapQuadTree = null;
		}
	}
	
	// serverside
	private void initGame() {
		// setup a quadtree
		
		ensureQuadTreeCleaned();
		mapQuadTree = new QuadTree(mapColTiles.length, mapColTiles[0].length);

		// create objects

		int ghostNumber = (int)(currentLevel / 3) + 1;
		
		Random rand = new Random();
		rand.setSeed(generator.getSeed());
		
		// choose the furthest enemy spawn point
		Vector2[] enemySpawnpoints = generator.getEnemySpawnPoints();
		final Vector2 p = new Vector2(entranceArea.getRectangle().x, entranceArea.getRectangle().y);
		Arrays.sort(enemySpawnpoints, new Comparator<Vector2>() {
			public int compare(Vector2 o1, Vector2 o2) {
				return p.dst(o1) > p.dst(o2) ? -1 : 1;
			}
		});
		
		// spawn nextbots
		Nextbot[] addedGhosts = new Nextbot[ghostNumber];
		for (int i = 0; i < ghostNumber; i++) {
			int ghostIndex = (int)(Math.abs(rand.nextInt()) % Customization.GHOSTS.length);
			Nextbot ghost = new Nextbot();
			ghost.setGhostIndex(ghostIndex);
			
			Vector2 enemySpawnpoint = enemySpawnpoints[
				i % enemySpawnpoints.length
			];
			ghost.setPosition(enemySpawnpoint.x, enemySpawnpoint.y);
			currentLevelEntities.add(ghost);
			addedGhosts[i] = ghost;
		}

		// items
		LinkedList<Item> items = new LinkedList<>();

		// papers (golds)
		Vector2[] spawns = generator.getPaperSpawns();
		ArrayList<Vector2> spawns_list = new ArrayList<Vector2>(Arrays.asList(spawns));
		Collections.shuffle(spawns_list, rand);
		List<Vector2> spawns_list_sub;
		if (spawns_list.size() < 100)
			spawns_list_sub = spawns_list;
		else
			spawns_list_sub = spawns_list.subList(0, PAPERS_LIMIT);
		for (Vector2 v : spawns_list_sub) {
			Gold m = new Gold();
			m.setPosition(v.x, v.y);
			currentLevelEntities.add(m);
			items.add(m);
		}
		paperCount = spawns_list_sub.size();
		collectedPaperCount = 0;
		
		// medkits
		spawns = generator.getMedkitSpawns();
		
		for (Vector2 v : spawns) {
			Medkit m = new Medkit();
			m.setPosition(v.x, v.y);
			currentLevelEntities.add(m);
			items.add(m);
		}

		// powers
		spawns = generator.getPowerSpawns();
		
		for (Vector2 v : spawns) {
			Item m = null;
			switch (rand.nextInt() % 4) {
			case 0 :
				m = new Protection();
				break;
			case 1 :
				m = new Faster();
				break;
			case 2 :
				m = new Invisible();
				break;
			case 3 :
				m = new Angel();
				break;
			}
			if (m == null) continue;
			m.setPosition(v.x, v.y);
			currentLevelEntities.add(m);
			items.add(m);
		}

		// remove items that placed at the border of the node

		for (Item i : items) {
			mapQuadTree.updatePos(i);
			/*QuadTree.Node n = i.getCurrentNode();
			if (n != null) {
				if (n.nodes[0] != null) {
					added.remove(i);
					i.setCurrentNode(null);
				}
			}*/
		}

		addEntities((Entity[]) currentLevelEntities.toArray(new Entity[currentLevelEntities.size()]), true);
		
		// setup astar
		for (int i = 0; i < addedGhosts.length; i++) {
			addedGhosts[i].setupAStar();
		}
		
		InitGamePost();
	}
	
	// locally delete all entities that are associated with this level
	public void removeOldLevelEnts() {
		for (Entity e : currentLevelEntities) {
			//if (e == null) continue;
			deleteEntity(e.getID());
		}
		currentLevelEntities.clear();
		
		ResourceManager.instance().stopAllSoundMusic();
		worldMusic = null;
	}
	
	// clientside
	public void initGamePuppet(int maxPaperCount) {
		paperCount = maxPaperCount;
		generator = null;
		InitGamePost();
	}
	
	private void InitGamePost() {
		frameProcessed = 0;
		
		// revive all players
		for (int id : clientCharacters.values()) {
			Entity e = entities.get(id);
			e.revive();
		}
		
		// stop spectating
		spectatingCharacter = null;
		
		if (markRecording) {
			demoW = new DemoWriter(this);
		}
	}
	
	private void clearProgress() {
		// reset scores and level
		for (Character ch : clientCharacterList) {
			Player player = ch.getPlayer();
			player.setScore(0);
		}
		currentLevel = 0;
		collectedPaperCountPrevious = 0;
	}
	
	private void returnToLobby() {
		clearProgress();
		atLobby = true;
		instantiateLobby();
		gameEnd = false;
		
		playMusic("m_lobby", false);
		
		hud.refreshIPList();
	}
	
	public void playMusic(String musicName, boolean stopAllMusics) {
		if (stopAllMusics) {
			ResourceManager.instance().stopAllSoundMusic();
		} else if (worldMusic != null) {
			ResourceManager.instance().stopMusic(worldMusic);
		}
		
		worldMusic = (Music) ResourceManager.instance().get(musicName);
		worldMusic.setLooping(true);
		ResourceManager.instance().playMusic(worldMusic);
	}
	
	// used to call "returnToLobby" in the different threads
	// becuz opengl sucks at it
	private boolean markedReturnToLobby = false;
	public void markReturnToLobby() {
		markedReturnToLobby = true;
	}
	
	// serverside
	public void returnToLobbyRemote() {
		Packet.SReturnToLobby p = new Packet.SReturnToLobby();
		myClient.getServer().broadcast(p);
	}
	
	public void render(float delta) {
		process(delta);
		frameProcessed += 1;
	}
	
	private void process(float delta) {
		if (!isDemoReading()) {
			if (markInit) {
				returnToLobby();
				markInit = false;
				ready = true;
			}

			if (markedReturnToLobby) {
				returnToLobby();
				markedReturnToLobby = false;
			}
			
			if (!ready) return;
		} else {
			if (demoR.readFrame(frameProcessed)) {
				// PLAYING FINISHED
				return;
			}
		}
		
		SpriteBatch batch = CoreGame.instance().getBatch();
		
		if (!gameEnd) {
			pollDungeonGenerator(batch);
			if (!isDemoReading()) {
				tickTimers(delta);
				processEntities(delta);
			}
			
			renderLevel(batch, delta);

			stage.act(delta);
			stage.draw();
			
			cameraUpdatePos(delta);

			batch.begin();
			batch.draw(overlay, 0.0f, 0.0f); // draw the darkness 0_0
		} else {
			batch.begin();
			batch.draw(endBG, 0, 0);
		}
		
		hud.draw(batch);
		
		batch.end();
	}
	
	private void pollDungeonGenerator(SpriteBatch batch) {
		if (generator != null) {
			// do not render anything when generating
			
			TiledMap map = generator.getMap();
			if (map != null) {
				if (!waiting) {
					setMap(map);
					
					// set our character location to the spawn points
					// randomized. can be overlapped
					Random rand = new Random();
					rand.setSeed(generator.getSeed());
					Vector2[] spawnpoints = generator.getSpawnPoints();
					Vector2 spawnpoint;
					if (spawnpoints.length == 0) {
						// empty ?
						spawnpoint = new Vector2(0, 0);
					} else {
						spawnpoint = spawnpoints[Math.abs(rand.nextInt()) % spawnpoints.length];
					}
					myClient.getCharacter().teleport(spawnpoint.x, spawnpoint.y);
					mapColTiles = generator.getColTiles2DArray();
					entranceArea = generator.getEntranceRect();
					
					if (!isDemoReading()) {
						if (myClient.isServer()) {
							// remove myself
							pendingRemove(1);
						} else {
							// tell the server
							Packet.CGenerateDone p = new Packet.CGenerateDone();
							myClient.send(p);
						}
					}
					
					waiting = true;
				} else {
					// await (for server)
					waiting = false;
					if (isDemoReading() || (myClient.isServer() && pendingPlayer.isEmpty())) {
						// ALRIGHT LETS GO
						if (!isDemoReading()) {
							initGame();
							Packet.SInitGame p = new Packet.SInitGame();
							p.maxPaperCount = paperCount;
							getMyClient().getServer().broadcastExceptServer(p);
						}
						generator = null;
					}
				}
			}
			
			return;
		}
	}
	
	private void renderLevel(SpriteBatch batch, float delta) {
		if (worldRenderer != null) {
			worldRenderer.setView(camera);
			worldRenderer.render();
		}
		
		batch.begin();
		
		if (drawQuadTree && mapQuadTree != null) {
			batch.end();
			ShapeRenderer s = new ShapeRenderer();
			s.setProjectionMatrix(camera.combined);
			s.begin(ShapeRenderer.ShapeType.Line);
			drawQuadTree(mapQuadTree.getRoot(), s, 0);
			s.end();
			batch.begin();
		}
		
		batch.end();
	}
	
	private void processEntities(float delta) {
		Character m = myClient.getCharacter();
		
		if (myClient.isServer()) {
			// process entities (server)
			processing = true;
			for (HashMap.Entry<Integer, Entity> e : entities.entrySet()) {
				Entity ent = e.getValue();
				if (ent.isDeleted()) continue;
				ent.process(delta);
				
				if (isDemoWriting()) {
					demoW.processRecordEntityProcess(frameProcessed, ent);
				}
			}
			processing = false;
			
			for (int j = entitiesRemoveLater.size() - 1; j >= 0; j--) {
				entities.remove(entitiesRemoveLater.get(j));
			}
			entitiesRemoveLater.clear();
		} else {
			// process your character (client) 
			if (m != null) {
				m.process(delta);
			}
		}
	}
	
	private void tickTimers(float delta) {
		// timers
		if (spectatingDelayStarting) {
			spectatingDelay -= delta;
			if (spectatingDelay <= 0.0f) {
				spectate(0);
				spectatingDelayStarting = false;
			}
		}
	}
	
	private void cameraUpdatePos(float delta) {
		if (generator != null) return; // generating. do not update
		Character m = myClient.getCharacter();
		
		Character followCharacter;
		if (spectatingCharacter != null) {
			followCharacter = spectatingCharacter;
		} else {
			followCharacter = m;
		}
		
		if (followCharacter != null) {
			if (cameraSmooth) {
				camera.position.set(
					new Vector3(camera.position.x, camera.position.y, 0.0f).lerp(
						new Vector3(followCharacter.getX() + 16.0f, followCharacter.getY() + 16.0f, 0.0f)
					, delta * 15.0f)
				);
			} else {
				camera.position.set(
					new Vector3(followCharacter.getX() + 16.0f, followCharacter.getY() + 16.0f, 0.0f)
				);
			}
			
			camera.update();
		}
		
		// poll the cam position to all entities
		for (HashMap.Entry<Integer, Entity> e : entities.entrySet()) {
			e.getValue().updateCameraPos(camera.position.x, camera.position.y);
		}
	}
	
	private void drawQuadTree(QuadTree.Node n, ShapeRenderer s, int index) {
		s.setColor(quadTreeColors[index % quadTreeColors.length]);
		float margin = index * 24.0f;
		s.rect(
			n.X * 32.0f + margin, n.Y * 32.0f + margin,
			n.W * 32.0f - (margin * 2.0f), n.H * 32.0f - (margin * 2.0f)
		);
		for (int i = 0; i < 4; i++) {
			QuadTree.Node nn = n.nodes[i];
			if (nn == null) continue;
			drawQuadTree(nn, s, index + 1);
		}
	}
	
	public boolean isDrawPathEnabled() {
		return drawPath;
	}
	
	public void setDrawPathEnabled(boolean yes) {
		drawPath = yes;
	}
	
	public void resize(int w, int h) {
		viewport.update(w, h, true);
		camera.update();
	}
	
	public void submitChat(int netID, String text) {
		if (!text.isEmpty()) {
			Packet.CSendChat p = new Packet.CSendChat();
			p.message = text;
			p.netID = netID;
			myClient.send(p);
		}
	}
	
	// add texts on the top left
	public void feedText(String text) {
		hud.feedTextItem(new ChatText(text));
	}
	
	public void removeDisconnectedClient(int netID) {
		if (!clientCharacters.containsKey(netID)) return;
		deleteEntity(clientCharacters.get(netID));
		clientCharacters.remove(netID);
		for (int i = 0; i < clientCharacterList.size(); i++) {
			if (clientCharacterList.get(i).getPlayer().getNetID() == netID) {
				clientCharacterList.remove(i);
				break;
			}
		}
	}
	
	public void feedChat(int netID, String text, boolean flash) {
		ChatText i = new ChatText(text);
		
		switch (netID) {
			case -1:
				i.setAuthorColor("YELLOW"); // system
				break;
			case -2:
				i.setAuthorColor("RED"); // cheat notify
				playSound("s_cheat");
				break;
			case -3:
				i.setAuthorColor("GREEN"); // paper collected notify
				playSound("s_paper");
				break;
			case -4:
				i.setAuthorColor("MAGENTA"); // cheerful notify
				playSound("s_completed");
				break;
			case -5:
				i.setAuthorColor("RED"); // death notify
				//ResourceManager.instance().playSound("s_completed");
				break;
			case -6:
				i.setAuthorColor("CYAN"); // revive notify
				playSound("s_revived");
				break;
			case -7:
				i.setAuthorColor("PINK"); // heal notify
				playSound("s_heal");
				break;
			default:
				Character c = getCharacterByNetID(netID);
				if (c.isDied()) i.setAuthorColor("RED"); // set to red (others)
				i.setAuthor(c.getPlayer().getUsername());
				playSound("s_chat");
				break;
		}
		
		if (netID == myClient.getMyPlayer().getNetID())
			i.setAuthorColor("YELLOW"); // your message
		hud.feedTextItem(i);
		
		if (flash)
			CoreGame.instance().flashScreen();
		
		if (isDemoWriting()) {
			demoW.processRecordChat(frameProcessed, i, flash);
		}
	}
	
	public void playSound(String sndName) {
		ResourceManager.instance().playSound(sndName);
		if (isDemoWriting()) {
			demoW.processRecordSoundEmit(frameProcessed, sndName);
		}
	}
	
	/////////////////////////////////
	
	public void tellPosChange(Entity ent) {
		// in tha lobby ?
		if (ent == getMyClient().getCharacter())
			if (atLobby) {
				// is it touching the ready area ?
				Rectangle entRect = ent.getRect();
				insideReadyArea = (serverReadyArea.getRectangle().overlaps(entRect));
			} else {
				// playing
				// inside the entrance radius ?
				boolean oldV = insideEntranceArea;
				Vector2 playerPoint = new Vector2(ent.getX() + 16, ent.getY() + 16);
				
				if (entranceArea == null) return;
				
				insideEntranceArea = entranceArea.getRectangle().contains(playerPoint);
				if (oldV != insideEntranceArea) {
					// lets they know !
					Packet.CUpdateAtTheEntrance p = new Packet.CUpdateAtTheEntrance();
					p.yes = insideEntranceArea;
					getMyClient().send(p);
					CoreGame.instance().getConsole().print("entr : " + insideEntranceArea);
				}

			}
	}
	
	public void tellCharacterLeave(Player player) {
		feedChat(-1, player.getUsername() + " left the game", false);
		checkPlayersEntrance();
	}
	
	// server only
	public void updateAtTheEntrance(int netID, boolean yes) {
		if (generator != null) return; // generating
		
		Integer i = Integer.valueOf(netID);
		if (yes) {
			pendingPlayer.add(i);
		} else {
			pendingPlayer.remove(i);
		}
		
		checkPlayersEntrance();
	}
	
	private boolean checkPlayersEntrance() {
		if (!myClient.isServer()) return false; // NOT A SERVER !!!!!1
		
		if (pendingPlayer.size() >= clientCharacters.size() && collectedPaperCount >= paperCount) {
			pendingPlayer.clear();
			collectedPaperCountPrevious = collectedPaperCount;
			newLevel(); // NEXT LEVEL
			CoreGame.instance().getConsole().print("! NEXT !");
			return true;
		}
		return false;
	}
	
	/////////////////////////////////
	
	public boolean attemptExit(Character ch) {
		if (paperCount < collectedPaperCount) return false; // nah
		
		return false;
	}
	
	public void dForceWin() {
		if (!getMyClient().isServer()) {
			CoreGame.instance().getConsole().printerr("This command isn't permitted on clients");
			return;
		}
		setCollectedPaperCount(getMyClient().getCharacter(), 666);
	}
	
	/////////////////////////////////
	
	// serverside
	public void healCharacter(Character ch, int heal) {
		if (getMyClient().getServer() == null) return; // not a server
		
		ch.heal(heal);
		
		Packet.SEntUpdateHealth p = new Packet.SEntUpdateHealth();
		p.netID = ch.getPlayer().getNetID();
		p.newHealth = ch.getHealth();
		getMyClient().getServer().broadcast(p);
	}
	
	// serverside
	public void setCharacterHealth(Character ch, int health) {
		if (getMyClient().getServer() == null) return; // not a server
		
		ch.setHealth(health);
		
		Packet.SEntUpdateHealth p = new Packet.SEntUpdateHealth();
		p.netID = ch.getPlayer().getNetID();
		p.newHealth = ch.getHealth();
		getMyClient().getServer().broadcast(p);
	}
	
	// serverside
	public void killCharacter(int netID) {
		if (getMyClient().getServer() == null) return; // not a server
		
		Packet.SCharacterDiedRevive p = new Packet.SCharacterDiedRevive();
		p.netID = netID;
		p.isRevive = false;
		getMyClient().getServer().broadcast(p);
	}
	
	// serverside
	public void reviveCharacter(int netID, int reviverID) {
		if (getMyClient().getServer() == null) return; // not a server
		
		Packet.SCharacterDiedRevive p = new Packet.SCharacterDiedRevive();
		p.netID = netID;
		p.isRevive = true;
		p.reviver = reviverID;
		getMyClient().getServer().broadcast(p);
	}
	
	public void tellCharacterDied(int netID) {
		Character c = getCharacterByNetID(netID);
		c.die();
		
		Server s = myClient.getServer();
		if (s != null) {
			if (!checkPlayersEntrance() && checkAllDied()) {
				// end game
				Packet.SGameEnd p = new Packet.SGameEnd();
				s.broadcast(p);
				return;
			}
			pendingPlayer.add(netID); // pretending the dead players are already standing at the entrance
		}
		
		if (netID == getMyClient().getMyPlayer().getNetID()) {
			// OH NO IM DED
			CoreGame.instance().getConsole().print("Died.");
			spectatingDelay = 1.5f;
			spectatingDelayStarting = true;
		}
		
		feedChat(-5, c.getPlayer().getUsername() + " was killed !", false);
	}
	
	public void tellCharacterRevive(int netID, int reviverNetID) {
		if (gameEnd) return; // TOO LATE LMAO
		Character c = getCharacterByNetID(netID);
		Character r = getCharacterByNetID(reviverNetID);
		c.revive();
		
		Server s = myClient.getServer();
		if (s != null) {
			pendingPlayer.remove(netID); // pretending the revived players aren't already standing at the entrance. need recheck
		}
		
		if (netID == getMyClient().getMyPlayer().getNetID()) {
			// OH MY GOODNESS U ALIVE
			CoreGame.instance().getConsole().print("I'M ALIVE.");
			spectatingCharacter = null; // stop spectating
		}
		
		feedChat(-6, c.getPlayer().getUsername() + " has been revived by " + r.getPlayer().getUsername() + " !", false);
	}
	
	private boolean checkAllDied() {
		// TODO : is this thread safe ?
		int died = 0;
		for (Character c : clientCharacterList) {
			if (c.isDied()) died++;
		}
		return died == clientCharacterList.size();
	}
	
	public void endGame() {
		if (isDemoWriting()) {
			// HOORAY
			demoW.finishWriting();
		}
		gameEnd = true;
		spectatingDelayStarting = false; // stop the spectating timer
		spectatingCharacter = null; // stop spectating
		insideEntranceArea = false;
		pendingPlayer.clear();
		hud.clearChat();
		
		CoreGame.instance().getConsole().print("Game ended");
		
		playMusic("m_game_end", true);
	}
	
	private void spectate(int add) {
		int tries = 0;
		while (true) {
			spectatingIndex += add;
			
			if (spectatingIndex >= clientCharacterList.size()) {
				spectatingIndex = 0;
			} else if (spectatingIndex < 0) {
				spectatingIndex = clientCharacterList.size() - 1;
			}
			spectatingCharacter = clientCharacterList.get(spectatingIndex);
			
			if (spectatingCharacter != null && spectatingCharacter.isDied()) {
				// nah. try the next character instead
				if (tries >= clientCharacterList.size()) {
					// hmmm. no one to spectate
					spectatingCharacter = null;
					break;
				}
				tries += 1;
				if (add == 0) add = 1;
				continue;
			}
			break;
		}
	}
	
	public void toggleSpectate() {
		if (spectatingCharacter == null) {
			spectate(0);
		} else {
			spectatingCharacter = null;
		}
	}
	
	/* DEMO REPLAY RECORDING */
	
	public void markRecord() {
		Console c = CoreGame.instance().getConsole();
		if (isDemoReading()) {
			c.print("Cannot record during playback.");
			return;
		}
		if (!atLobby) {
			c.print("Cannot record during a game session.");
			return;
		}
		
		markRecording = !markRecording;
		if (markRecording) {
			c.print("A recording will start in a game session.");
		} else {
			c.print("A recording was canceled.");
		}
	}
	
	public void tellToggleCheat(int cheatNumber) {
		Packet.CCheatToggle p = new Packet.CCheatToggle();
		p.cheat = cheatNumber;
		getMyClient().send(p);
	}
	
	/////////////////////////////////
	
	public void dispose() {
		if (isDemoWriting()) demoW.finishWriting();
			
		if (worldMap != null)
			worldMap.dispose();
		if (worldRenderer != null)
			worldRenderer.dispose();
		stage.clear();
		stage.dispose();
	}
}
