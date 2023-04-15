package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

// game world

public class World { // implements Screen
	//private Server server;
	
	private boolean markInit = false;
	private boolean ready = false;
	
	private OrthographicCamera camera;
	public OrthographicCamera getCamera() { return camera; }
	private Stage stage;
	
	private int lastID = 0;
	
	private Client myClient;
	private final HashMap<Integer, Entity> entities; // < entID : Entity >
	private final HashMap<Integer, Integer> clientCharacters; // < netID : entID >, included ourselves
	private final LinkedList<Integer> entitiesRemoveLater;
	
	private BSPDungeonGenerator bsp = null;
	private TextureRegion bspBackgroundTexture = null;
	private TiledMap worldMap = null;
	private WorldRenderer worldRenderer = null;
	private QuadTree mapQuadTree = null;
	private String currentLevelName = "The Nameless City";
	private char[][] mapColTiles;
	
	private int paperCount;
	private int collectedPaperCount;
	
	// usually for servers
	private boolean atLobby = false;
	private final float[] currentMapspawnPoint;
	private final LinkedList<Integer> pendingPlayer; // generating levels
	
	private RectangleMapObject serverReadyArea;
	private boolean insideReadyArea = false;
	
	private final Texture overlay;
	
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
	private float CHAT_DURATION = 8.0f;
	private int CHAT_MAX = 10;
	private float textTimer = CHAT_DURATION;
	private float chatCaret = 0.0f;
	private boolean chatMode = false;
	private boolean chatModeReady = false;
	private TextInput chatText;
	private Ghost ghost = null;
	
	private boolean processing = false;
	
	private boolean drawQuadTree = false;
	public void toggleDrawQuadTree() { drawQuadTree = !drawQuadTree; }
	public boolean isDrawQuadTree() { return drawQuadTree; }
	public void setDrawQuadTree(boolean yes) { drawQuadTree = yes; }
	
	private boolean drawPath = false;

	private final int DUNX = 48;
	private final int DUNY = 48;
	private final int DUNS = 5;
	
	private boolean scoreboardVisible = false;
	
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
		entitiesRemoveLater = new LinkedList<>();
		
		chatText = new TextInput();
		chatText.setLimited(96);
		
		overlay = (Texture) ResourceManager.instance().get("darkness1");
		
		hudFont1 = (BitmapFont) ResourceManager.instance().get("hud_font");
		hudFont2 = (BitmapFont) ResourceManager.instance().get("chat_font");
		hudFont2.getData().markupEnabled = true;
		
		pendingPlayer = new LinkedList<>();
		
		
		camera.update();
		
		/////////////////////
		
		//generateMap(System.nanoTime(), 0);
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
		collectedPaperCount += 1;
		
		Server s = getMyClient().getServer();
		Player p = ch.getPlayer();
		
		s.sendChat(-3,
			p.getUsername() + " collects a paper. (" + collectedPaperCount + "/" + paperCount + ")"
		, p.getNetID());
		
		if (collectedPaperCount >= paperCount) {
			s.sendChat(-4, "All papers have been collected ! Come back to the entrance", -1);
		}
	}
	
	// clientside
	public void addCollectedPaperCountPuppet(int netID, int currentPaperCount) {
		collectedPaperCount += 1;
		
		Player p = getCharacterByNetID(netID).getPlayer();
		
		feedChat(-3,
			p.getUsername() + " collects a paper. (" + currentPaperCount + "/" + paperCount + ")"
		, netID == getMyClient().getMyPlayer().getNetID());
	}
	
	public void addEntity(Entity ent) {
		int i = allocateID();
		ent.setID(i);
		addEntityInternal(ent, i);
		
		Packet.SEntCreate p = new Packet.SEntCreate();
		p.ent = ent;
		myClient.getServer().broadcastExceptServer(p);
	}
	
	public void addEntities(Entity[] ents) {
		for (int i = 0; i < ents.length; i++) {
			int id = allocateID();
			ents[i].setID(id);
			addEntityInternal(ents[i], id);
		}
		Packet.SEntCreateMultiple p = new Packet.SEntCreateMultiple();
		p.ents = ents;
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
	
	public Entity createEntityAuthorized(int ID, String className) {
		try {
			Entity ent = (Entity) Class.forName(className).getDeclaredConstructor().newInstance();
			addEntityInternal(ent, ID);
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
	
	public char[][] getColMapTiles() {
		return mapColTiles;
	}
	
	public Character getCharacterByNetID(int netID) {
		return (Character) entities.get(clientCharacters.get(netID));
	}
	
	public HashMap<Integer, Integer> dumpCharactersEntID() {
		return clientCharacters;
	}
	
	public void registerNewPlayer(int entID, Player p, boolean newConnect) {
		Character ent = (Character) createEntityAuthorized(entID,
			Character.class.getName()
		);
		ent.setupPlayer(p);
		
		clientCharacters.put(p.getNetID(), entID);
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
		} else if (newConnect && p.getNetID() != myClient.getMyPlayer().getNetID()) {
			feedChat(-1, p.getUsername() + " joined the game", false);
		}
	}
	
	public void setMyClient(Client myClient) {
		this.myClient = myClient;
	}
	public Client getMyClient() {
		return myClient;
	}
	
	public void loadMap(String mapName) {
		TiledMap map = (TiledMap) ResourceManager.instance().get(mapName);
		MapObjects objs = map.getLayers().get("OBJECT").getObjects();
		MapProperties prop = objs.get("spawnPoint").getProperties();
		currentMapspawnPoint[0] = ((Float)prop.get("x")).floatValue();
		currentMapspawnPoint[1] = ((Float)prop.get("y")).floatValue();
		
		// server ready area
		serverReadyArea = (RectangleMapObject) objs.get("serverReadyArea");
		
		// reteleport players to the spawn point
		for (int id : clientCharacters.values()) {
			entities.get(id).teleport(currentMapspawnPoint[0], currentMapspawnPoint[1]);
		}
		
		setMap(map);
	}
	
	public void markReady() {
		markInit = true;
	}
	
	public void setMap(TiledMap map) {
		worldMap = map;
		worldRenderer = new WorldRenderer(map);
	}
	
	public void generateMap(long seed, int tilesetIndex) {
		Texture texture = (Texture) ResourceManager.instance().get("tileset__" + BSPDungeonGenerator.tilesets[tilesetIndex]);
		bspBackgroundTexture = new TextureRegion();
		bspBackgroundTexture.setTexture(texture);
		bspBackgroundTexture.setRegion(0, 0, 32, 32);
		
		bsp = new BSPDungeonGenerator(seed, DUNX, DUNY, DUNS, texture);
		bsp.startGenerate();
	}
	
	/////////////////////
	
	public boolean keyTyped (char c) {
		if (!chatModeReady) return false;
		return chatText.keyTyped(c);
	}
	
	public boolean keyDown(int i) {
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
			return false;
		}
		
		if (insideReadyArea) {
			if (i == Input.Keys.SPACE) {
				commitReady();
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
		
		Character m = myClient.getCharacter();
		if (m != null) {
			return m.keyUp(i);
		}
		
		if (i == Input.Keys.TAB) {
			showScoreboard(false);
			return true;
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
	public void commitReady() {
		if (!insideReadyArea) return;
		
		Packet.SGenLevel p = new Packet.SGenLevel();
		p.mapName = currentLevelName;
		//p.seed = -977121143;
		p.seed = new Random().nextInt();
		
		
		// add all players as pending
		for (HashMap.Entry<Integer, Integer> e : clientCharacters.entrySet()) {
			pendingPlayer.add(e.getKey());
		}
		
		int tilesetIndex = new Random().nextInt();
		p.tilesetIndex = Math.abs(tilesetIndex) % BSPDungeonGenerator.tilesets.length;
		CoreGame.instance().getConsole().print(">> " + p.seed + " : " + p.tilesetIndex);
		
		
		getMyClient().send(p);
		
		insideReadyArea = false;
		atLobby = false;
	}
	
	public void pendingRemove(int netID) {
		pendingPlayer.remove(Integer.valueOf(netID));
		
		if (pendingPlayer.isEmpty()) {
			// ALRIGHT LETS GO
			initGame();
			
			Packet.SInitGame p = new Packet.SInitGame();
			p.maxPaperCount = paperCount;
			getMyClient().send(p);
		}
	}
	
	// serverside
	private void initGame() {
		mapColTiles = bsp.getColTiles2DArray();
		
		// setup a quadtree
		mapQuadTree = new QuadTree(DUNX * DUNS, DUNY * DUNS);

		if (myClient.getCharacter() != null)
			myClient.getCharacter().teleport(bsp.getSpawnPointX(), bsp.getSpawnPointY());

		// create objects
		LinkedList<Entity> added = new LinkedList<>();

		// SPAWN THE ENEMY (ghost)
		ghost = new Ghost();
		ghost.setPosition(bsp.getEnemySpawnPointX(), bsp.getEnemySpawnPointY());
		added.add(ghost);

		// items
		LinkedList<Item> items = new LinkedList<>();

		// papers
		Vector2[] spawns = bsp.getPaperSpawns();
		for (Vector2 v : spawns) {
			Paper m = new Paper();
			m.setPosition(v.x, v.y);
			added.add(m);
			items.add(m);
		}
		paperCount = spawns.length;
		collectedPaperCount = 0;

		// powers
		spawns = bsp.getPowerSpawns();
		Random rand = new Random();
		rand.setSeed(bsp.getSeed());
		for (Vector2 v : spawns) {
			Item m = null;
			switch (rand.nextInt() % 2) {
			case 0 :
				m = new Protection();
				break;
			case 1 :
				m = new Faster();
				break;
			}
			if (m == null) continue;
			m.setPosition(v.x, v.y);
			added.add(m);
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

		addEntities((Entity[]) added.toArray(new Entity[added.size()]));
		ghost.setupAStar();
	}
	
	// clientside
	public void initGamePuppet(int maxPaperCount) {
		paperCount = maxPaperCount;
		bsp = null;
	}
	
	public void render(float delta) {
		if (markInit) {
			atLobby = true;
			loadMap("demo1");
			markInit = false;
			ready = true;
		}
		
		if (!ready) return;
		SpriteBatch batch = CoreGame.instance().getBatch();
		
		if (bsp != null) {
			// do not render anything when generating
			TiledMap map = bsp.getMap();
			if (map != null) {
				setMap(map);
				
				if (myClient.isServer()) {
					// remove myself
					pendingRemove(1);
				} else {
					// tell the server
					Packet.CGenerateDone p = new Packet.CGenerateDone();
					myClient.send(p);
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
		
		if (worldRenderer != null) {
			worldRenderer.setView(camera);
			worldRenderer.render();
		}
		
		batch.begin();
		
		// texts
		if (textTimer > 0.0) {
			textTimer -= delta;
		} else {
			textsPop();
		}
		
		if (drawQuadTree && mapQuadTree != null) {
			batch.end();
			ShapeRenderer s = new ShapeRenderer();
			s.setProjectionMatrix(camera.combined);
			s.begin(ShapeRenderer.ShapeType.Line);
			drawQuadTree(mapQuadTree.getRoot(), s, 0);
			s.end();
			batch.begin();
		}
		
		
		//batch.begin();
		
			
		
		batch.end();
		
		Character m = myClient.getCharacter();
		
		if (myClient.isServer()) {
			// process entities (server)
			processing = true;
			for (HashMap.Entry<Integer, Entity> e : entities.entrySet()) {
				e.getValue().process(delta);
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
		
		// poll the cam position to all entites
		for (HashMap.Entry<Integer, Entity> e : entities.entrySet()) {
			e.getValue().updateCameraPos(camera.position.x, camera.position.y);
		}
		
		if (m != null) {
			camera.position.set(
				new Vector3(camera.position.x, camera.position.y, 0.0f).lerp(
					new Vector3(m.getX() + 16.0f, m.getY() + 16.0f, 0.0f)
				, delta * 15.0f)
			);
			camera.update();
		}
		
		
		stage.act(delta);
		stage.draw();
		
		batch.begin();
		batch.draw(overlay, 0.0f, 0.0f); // draw the darkness 0_0
		
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
		
		if (insideReadyArea) {
			drawChatText(batch, "[GREEN]Press SPACE to enter the dungeon !", "Press SPACE to enter the dungeon !", 20, 200);
		}
		
		/////////////////////
		// draw hud
		batch.setColor(Color.WHITE);
		
		if (scoreboardVisible) {
			// show everyone's names
			int cursorY = 0;
			drawChatText(batch, "[PINK]PLAYERS", "PLAYERS", 800, 1000 - cursorY);
			cursorY = -32;
			for (HashMap.Entry<Integer, Integer> e : clientCharacters.entrySet()) {
				Character ch = (Character) entities.get(e.getValue());
				Player player = ch.getPlayer();
				
				drawChatText(batch, player.getUsername(), player.getUsername(), 200, 1000 - cursorY);
				cursorY -= 32;
			}
		} else {
			if (m != null) {
				batch.draw(m.getIcon(), 96.0f, 12.0f, 96.0f, 96.0f); // player status
				hudFont1.draw(batch, "" + m.getHealth(), 225.0f, 70.0f); // health point. remove soon
			}
		}
		
		batch.end();
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
		deleteEntityInternal(clientCharacters.get(netID));
		clientCharacters.remove(netID);
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
			i.authorColor = "GREEN"; // cheat notify
			ResourceManager.instance().playSound("s_paper");
		}
		else if (netID == -4) {
			i.authorColor = "MAGENTA"; // cheerful notify
			ResourceManager.instance().playSound("s_completed");
		}
		else {
			i.author = myClient.getPlayer(netID).getUsername();
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
		// is this the server character ?
		if (ent == getMyClient().getCharacter()) {
			// is it in the lobby too ?
			if (atLobby) {
				// is it touching the ready area ?
				Rectangle entRect = ent.getRect();
				insideReadyArea = (serverReadyArea.getRectangle().overlaps(entRect));
			}
		}
		
	}
	
	/////////////////////////////////
	
	public boolean attemptExit(Character ch) {
		if (paperCount < collectedPaperCount) return false; // nah
		
		return false;
	}
	
	/////////////////////////////////
	
	public void dispose() {
		stage.clear();
		stage.dispose();
	}
}
