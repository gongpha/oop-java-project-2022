package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

// game world

public class World {
	private boolean markInit = false;
	private boolean ready = false;
	
	private OrthographicCamera camera;
	public OrthographicCamera getCamera() { return camera; }
	private Stage stage;
	
	private int lastID = 0;
	
	private Client myClient;
	private final HashMap<Integer, Entity> entities; // < entID : Entity >
	private final HashMap<Integer, Integer> clientCharacters; // < netID : entID >, included ourselves
	private final ArrayList<Character> clientCharacterList;
	private final LinkedList<Integer> entitiesRemoveLater;
	
	private DungeonGenerator generator = null;
	private Texture bspBackgroundTexture = null;
	private TiledMap worldMap = null;
	private WorldRenderer worldRenderer = null;
	private boolean waiting = false;
	private QuadTree mapQuadTree = null;
	private String currentLevelName = "The Nameless City"; // TODO : remove soon
	private byte[][] mapColTiles;
	private final LinkedList<Entity> currentLevelEntities;
	
	private int paperCount;
	private int collectedPaperCount;
	private int collectedPaperCountPrevious;
	private int currentLevel = 0;
	
	// usually for servers
	private boolean atLobby = false;
	public boolean isInLobby() { return atLobby; }
	private final float[] currentMapspawnPoint;
	
	// generating levels AND using for counting players at the entrance
	private final HashSet<Integer> pendingPlayer;
	
	private RectangleMapObject serverReadyArea;
	private boolean insideReadyArea = false;
	
	// common
	private RectangleMapObject entranceArea;
	private boolean insideEntranceArea = false;
	
	private final Texture overlay;
	private final Texture endBG;
	
	// HUD
	
	private BitmapFont hudFont1;
	private BitmapFont hudFont2;
	private class TextItem {
		String s;
		String author = "";
		String authorColor = "GREEN";
		float time;
		public TextItem(String text, float duration) {
			s = text.replace("[", "[[");
			time = duration;
		}
	}
	private LinkedList<TextItem> texts;
	private final float CHAT_DURATION = 8.0f;
	private int CHAT_MAX = 6;
	private float textTimer = CHAT_DURATION;
	private float chatCaret = 0.0f;
	private boolean chatMode = false;
	private boolean chatModeReady = false;
	private TextInput chatText;
	private Enemy ghost = null;
	
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
	
	private boolean scoreboardVisible = false;
	
	private float spectatingDelay = 0.0f; // after died
	private boolean spectatingDelayStarting = false;
	private int spectatingIndex = 0;
	private Character spectatingCharacter = null;
	
	private boolean gameEnd = false;
	
	private Music worldMusic;
	
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
	
	public class InputMap {
		public final static int DASH =		1 << 1;
		public final static int ATTACK1 =	1 << 2;
		public final static int ATTACK2 =	1 << 3;
		public final static int ATTACK3 =	1 << 4;
	}
	
	public World() {
		//this.server = server;
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		texts = new LinkedList<>();
		currentMapspawnPoint = new float[]{0.0f, 0.0f};
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, w, h);
		
		stage = new Stage(new FitViewport(w, h));
		stage.getViewport().setCamera(camera);
		camera.zoom = 1.0f;
		
		entities = new HashMap<>();
		clientCharacters = new HashMap<>();
		clientCharacterList = new ArrayList<>();
		entitiesRemoveLater = new LinkedList<>();
		
		chatText = new TextInput();
		chatText.setLimited(96);
		
		overlay = (Texture) ResourceManager.instance().get("darkness1");
		endBG = (Texture) ResourceManager.instance().get("end");
		
		hudFont1 = (BitmapFont) ResourceManager.instance().get("hud_font");
		hudFont2 = (BitmapFont) ResourceManager.instance().get("chat_font");
		hudFont2.getData().markupEnabled = true;
		
		pendingPlayer = new HashSet<>();
		
		currentLevelEntities = new LinkedList();
		
		
		camera.update();
		
		/////////////////////
		
		//generateMap(System.nanoTime(), 0);
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
			p.getUsername() + " collects a paper. (" + collectedPaperCount + "/" + paperCount + ")"
		, netID == getMyClient().getMyPlayer().getNetID());
		
		checkPaperCount();
	}
	
	public void checkPaperCount() {
		if (getMyClient().isServer()) {
			if (collectedPaperCount >= paperCount) {
				getMyClient().getServer().sendChat(-4, "All papers have been collected ! Come back to the entrance", -1);
			}
		}
	}
	
	public void addEntity(Entity ent) {
		int i = allocateID();
		ent.setID(i);
		addEntityInternal(ent, i);
		
		Packet.SEntCreate p = new Packet.SEntCreate();
		p.ent = ent;
		myClient.getServer().broadcastExceptServer(p);
	}
	
	public void addEntities(Entity[] ents, boolean isLevelEntities) {
		for (int i = 0; i < ents.length; i++) {
			int id = allocateID();
			ents[i].setID(id);
			addEntityInternal(ents[i], id);
		}
		Packet.SEntCreateMultiple p = new Packet.SEntCreateMultiple();
		p.ents = ents;
		p.isLevelEntities = isLevelEntities;
		myClient.getServer().broadcastExceptServer(p);
	}
	
	public void deleteEntity(Entity ent) {
		Packet.SEntDelete p = new Packet.SEntDelete();
		p.ent = ent;
		myClient.getServer().broadcastExceptServer(p);
		
		deleteEntityInternal(ent.getID());
	}
	
	public void addEntityInternal(Entity ent, int ID) {
		ent.setWorld(this);
		stage.addActor(ent);
		entities.put(ID, ent);
		ent.setID(ID);
	}
	
	public void deleteEntityInternal(int ID) {
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
	}
	
	public Entity getEntities(int ID) {
		return entities.get(ID);
	}
	
	// FOR SERVER
	//public void addEntity(Entity ent) {
	//	addEntity(ent, entities.size() - 1);
	//}
	
	public int allocateID() {
		lastID += 1;
		return lastID - 1;
	}
	
	public Entity createEntityAuthorized(int ID, String className, boolean isLevelEntities) {
		try {
			Entity ent = (Entity) Class.forName(className).getDeclaredConstructor().newInstance();
			addEntityInternal(ent, ID);
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
		removeOldLevelEnts();
		
		currentLevel = level;
		
		//Texture texture = (Texture) ResourceManager.instance().get("tileset__" + BSPDungeonGenerator.tilesets[tilesetIndex]);
		Texture texture = (Texture) ResourceManager.instance().get("connecting");
		bspBackgroundTexture = texture;
		
		//generator = new BSPDungeonGenerator(seed, DUNX, DUNY, DUNS, texture);
		generator = new PrefabDungeonGenerator(seed, 500);
		generator.startGenerateInstant();
	}
	
	/////////////////////
	
	public boolean keyTyped (char c) {
		if (!chatModeReady) return false;
		return chatText.keyTyped(c);
	}
	
	public boolean keyDown(int i) {
		if (gameEnd && getMyClient().isServer() && i == Input.Keys.SPACE) {
			returnToLobbyRemote();
			return true;
		}
		
		if (!chatMode && i == Input.Keys.T) {
			chatMode = !chatMode;
			chatCaret = 0.0f;
			Character m = myClient.getCharacter();
			if (m != null) {
				m.resetMoveInput();
			}
			return true;
		}
		if (chatMode) {
			if (i == Input.Keys.ENTER) {
				submitChat();
				return true;
			}
			return chatText.keyDown(i);
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
		
		if (i == Input.Keys.TAB) {
			showScoreboard(true);
			return true;
		}
		
		Character m = myClient.getCharacter();
		if (m != null) {
			return m.keyDown(i);
		}
		return false;
	}
	
	public boolean keyUp(int i) {
		if (chatMode) return false;
		
		if (i == Input.Keys.TAB) {
			showScoreboard(false);
			return true;
		}
		
		Character m = myClient.getCharacter();
		if (m != null) {
			return m.keyUp(i);
		}
		
		return false;
	}
	
	public void showScoreboard(boolean visible) {
		scoreboardVisible = visible;
	}
	
	public boolean handleEscapeKey() {
		if (chatMode) {
			chatMode = false;
			chatModeReady = false;
			return true; // editing the chat text. then close
		}
		return false;
	}
	
	public void drawChatText(SpriteBatch batch, String t, String nmt, int X, int Y) {
		hudFont2.draw(batch, "[BLACK]" + nmt, X + 2, Y - 2);
		hudFont2.draw(batch, t, X, Y);
	}
	
	public void textsPop() {
		if (!texts.isEmpty())
			texts.remove(0);
		if (!texts.isEmpty())
			textTimer = texts.get(0).time;
	}
	
	// serverside
	public void newLevel() {
		atLobby = false;
		insideReadyArea = false;
		
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
	
	// serverside
	private void initGame() {
		// setup a quadtree
		if (mapQuadTree != null) {
			// delete the old tree
			mapQuadTree.release();
			mapQuadTree = null;
		}
		
		mapQuadTree = new QuadTree(mapColTiles.length, mapColTiles[0].length);

		// create objects

		// SPAWN THE ENEMY (ghosts)
		switch ((int)(Math.abs(generator.getSeed()) % 3)) {
			case 0:
				ghost = new Ghost1();
				break;
			case 1:
				ghost = new Ghost2();
				break;
			case 2:
				ghost = new Ghost3();
				break;
		}
		
		Random rand = new Random();
		rand.setSeed(generator.getSeed());
		
		Vector2[] enemySpawnpoints = generator.getEnemySpawnPoints();
		// choose the furthest enemy spawn point
		Vector2 p = new Vector2(entranceArea.getRectangle().x, entranceArea.getRectangle().y);
		float furthest = 0.0f;
		Vector2 enemySpawnpoint = new Vector2(0, 0);
		for (int i = 0; i < enemySpawnpoints.length; i++) {
			Vector2 v = enemySpawnpoints[i];
			float dst = v.dst(p);
			if (dst > furthest) {
				furthest = dst;
				enemySpawnpoint = v;
			}
		}
		ghost.setPosition(enemySpawnpoint.x, enemySpawnpoint.y);
		currentLevelEntities.add(ghost);

		// items
		LinkedList<Item> items = new LinkedList<>();

		// papers
		Vector2[] spawns = generator.getPaperSpawns();
		for (Vector2 v : spawns) {
			Paper m = new Paper();
			m.setPosition(v.x, v.y);
			currentLevelEntities.add(m);
			items.add(m);
		}
		paperCount = spawns.length;
		collectedPaperCount = 0;

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
		ghost.setupAStar();
	}
	
	// locally delete all entities that are associated with this level
	public void removeOldLevelEnts() {
		for (Entity e : currentLevelEntities) {
			//if (e == null) continue;
			deleteEntityInternal(e.getID());
		}
		currentLevelEntities.clear();
		
		ResourceManager.instance().stopAllSoundMusic();
		worldMusic = null;
	}
	
	// clientside
	public void initGamePuppet(int maxPaperCount) {
		paperCount = maxPaperCount;
		generator = null;
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
		
		if (worldMusic != null) {
			ResourceManager.instance().stopMusic(worldMusic);
		}
		worldMusic = (Music) ResourceManager.instance().get("m_lobby");
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
		SpriteBatch batch = CoreGame.instance().getBatch();
		
		if (!gameEnd) {
			pollDungeonGenerator(batch);
			tickTimers(delta);
			renderLevel(batch, delta);
			processEntities(delta);

			stage.act(delta);
			stage.draw();
			
			cameraUpdatePos(delta);

			batch.begin();
			batch.draw(overlay, 0.0f, 0.0f); // draw the darkness 0_0
		} else {
			batch.begin();
			batch.draw(endBG, 0, 0);
		}
		
		if (!scoreboardVisible || gameEnd) {
			drawChats(batch, delta);
			drawCornerText(batch);
		}
		/////////////////////
		// draw hud
		batch.setColor(Color.WHITE);
		
		drawHUD(batch);
		
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
					
					if (myClient.isServer()) {
						// remove myself
						pendingRemove(1);
					} else {
						// tell the server
						Packet.CGenerateDone p = new Packet.CGenerateDone();
						myClient.send(p);
					}
					
					waiting = true;
				} else {
					// await (for server)
					waiting = false;
					if (myClient.isServer() && pendingPlayer.isEmpty()) {
						// ALRIGHT LETS GO
						initGame();

						Packet.SInitGame p = new Packet.SInitGame();
						p.maxPaperCount = paperCount;
						getMyClient().getServer().broadcastExceptServer(p);
						generator = null;
					}
				}
			}
			
			if (bspBackgroundTexture != null) {
				batch.begin();
				batch.draw(bspBackgroundTexture, 0.0f, 0.0f, 1280.0f, 720.0f);
				hudFont1.draw(batch, "Generating Dungeon . . .", 10.0f, 60.0f);
				batch.end();
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
	
	private void drawChats(SpriteBatch batch, float delta) {
		if (textTimer > 0.0) {
			textTimer -= delta;
		} else {
			textsPop();
		}
		
		int i = 0;
		for (; i < texts.size(); i++) {
			TextItem j = texts.get(i);
			String b = texts.get(i).s;
			String s = b;
			String st = b;
			if (j.author.isEmpty()) {
				s = "[" + j.authorColor + "]" + b;
			} else {
				s = "[" + j.authorColor + "]< " + j.author + " >[WHITE] " + b;
				st = "< " + j.author + " > " + b;
			}

			drawChatText(batch, s, st, 20, 690 + (-i * 40));
		}
		if (chatMode) {
			chatModeReady = true;
			String c = chatText.getString() + (
				(chatCaret > 1.0f) ? "_" : ""
			);
			drawChatText(batch,
				"[CYAN][[Chat][WHITE] : " + c,	
				"[Chat] : " + c,	
			20, 690 + (-i * 40));
			chatCaret += delta * 1.5f;
			if (chatCaret >= 2.0f) chatCaret = 0.0f;
		}
	}
	
	private void drawCornerText(SpriteBatch batch) {
		String s;
		if (insideReadyArea) {
			String col;
			if (getMyClient().isServer()) {
				col = "[GREEN]";
				s = "Press SPACE to enter the dungeon !";
			} else {
				col = "";
				s = "Wait for the host to start the game !";
			}
			drawChatText(batch, col + s, s, 20, 200);
		} else if (spectatingCharacter != null) {
			s = "Spectating other players . . .\nPress SPACE to cycle targets";
			drawChatText(batch, s, s, 20, 300);
		} else if (insideEntranceArea) {
			if (collectedPaperCount >= paperCount) {
				s = "Waiting for players";
			} else {
				s = "Collect all papers to exit";
			}
			drawChatText(batch, s, s, 20, 200);
		}
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
	
	private void drawHUD(SpriteBatch batch) {
		Character m = myClient.getCharacter();
		
		if (scoreboardVisible || gameEnd) {
			// show everyone's names
			int cursorY = 0;
			
			String header = "PLAYERS";
			int headerX = 600;
			if (gameEnd) {
				header = (collectedPaperCountPrevious + collectedPaperCount) + " papers have been collected.";
				headerX -= 200;
			}
			
			drawChatText(batch, "[PINK]" + header, header, headerX, 600 - cursorY);
			cursorY = -32;
			
			for (Character ch : clientCharacterList) {
				Player player = ch.getPlayer();
				
				// Name
				String c = "";
				if (player.getNetID() == myClient.getMyPlayer().getNetID()) {
					// highlight myself in green
					c = "[YELLOW]";
				} else if (ch.isDied() && !gameEnd) {
					c = "[RED]"; // ded (while playing)
				}
				drawChatText(batch, c + player.getUsername(), player.getUsername(), 200, 500 + cursorY);
				
				// Score
				String s = "" + player.getScore();
				drawChatText(batch, c + s, s, 1000, 500 + cursorY);
				cursorY -= 32;
			}
		} else {
			if (spectatingCharacter != null) m = spectatingCharacter; // show the spectatee's info instead
			if (!gameEnd && m != null && !atLobby) {
				batch.draw(m.getIcon(), 96.0f, 12.0f, 96.0f, 96.0f); // player status
				hudFont1.draw(batch, "Level " + currentLevel, 550.0f, 70.0f); // level number
			}
		}
		
		if (gameEnd && getMyClient().isServer()) {
			hudFont1.draw(batch, "Press SPACE to restart", 300.0f, 70.0f); // restart notice
		}
	}
	
	private Color[] quadTreeColors = {
		Color.RED, Color.ORANGE, Color.YELLOW,
		Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA
	};
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
		stage.getViewport().update(w, h, true);
	}
	
	public void feedTextItem(TextItem i) {
		if (texts.isEmpty())
			textTimer = CHAT_DURATION;
		texts.add(i);
		while (texts.size() > CHAT_MAX) textsPop(); // limit the text count
	}
	
	public void submitChat() {
		submitChat(getMyClient().getMyPlayer().getNetID(), chatText.getString());
		chatText.setString("");
		chatMode = false;
		chatModeReady = false;
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
		feedTextItem(new TextItem(text, CHAT_DURATION - textTimer));
	}
	
	public void removeDisconnectedClient(int netID) {
		if (!clientCharacters.containsKey(netID)) return;
		deleteEntityInternal(clientCharacters.get(netID));
		clientCharacters.remove(netID);
		for (int i = 0; i < clientCharacterList.size(); i++) {
			if (clientCharacterList.get(i).getPlayer().getNetID() == netID) {
				clientCharacterList.remove(i);
				break;
			}
		}
	}
	
	public void feedChat(int netID, String text, boolean flash) {
		TextItem i = new TextItem(text, CHAT_DURATION - textTimer);
		
		if (netID == -1) {
			i.authorColor = "YELLOW"; // system
		}
		else if (netID == -2) {
			i.authorColor = "RED"; // cheat notify
			ResourceManager.instance().playSound("s_cheat");
		}
		else if (netID == -3) {
			i.authorColor = "GREEN"; // paper collected notify
			ResourceManager.instance().playSound("s_paper");
		}
		else if (netID == -4) {
			i.authorColor = "MAGENTA"; // cheerful notify
			ResourceManager.instance().playSound("s_completed");
		}
		else if (netID == -5) {
			i.authorColor = "RED"; // death notify
			//ResourceManager.instance().playSound("s_completed");
		}
		else if (netID == -6) {
			i.authorColor = "CYAN"; // revive notify
			ResourceManager.instance().playSound("s_revived");
		}
		else {
			Character c = getCharacterByNetID(netID);
			if (c.isDied()) i.authorColor = "RED"; // set to red (others)
			
			i.author = c.getPlayer().getUsername();
			ResourceManager.instance().playSound("s_chat");
		}
		
		if (netID == myClient.getMyPlayer().getNetID())
			i.authorColor = "YELLOW"; // your message
		feedTextItem(i);
		
		if (flash)
			CoreGame.instance().flashScreen();
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
		setCollectedPaperCount(getMyClient().getCharacter(), 666);
	}
	
	/////////////////////////////////
	
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
		gameEnd = true;
		spectatingDelayStarting = false; // stop the spectating timer
		spectatingCharacter = null; // stop spectating
		pendingPlayer.clear();
		
		CoreGame.instance().getConsole().print("Game ended");
		
		ResourceManager.instance().stopAllSoundMusic();
		worldMusic = (Music) ResourceManager.instance().get("m_game_end");
		ResourceManager.instance().playMusic(worldMusic);
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
	
	/////////////////////////////////
	
	public void dispose() {
		if (worldMap != null)
			worldMap.dispose();
		if (worldRenderer != null)
			worldRenderer.dispose();
		stage.clear();
		stage.dispose();
	}
}
