package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.HashMap;
import java.util.LinkedList;

public class World { // implements Screen
	//private Server server;
	
	private boolean markInit = false;
	private boolean ready = false;
	
	private OrthographicCamera camera;
	private Stage stage;
	
	private int input = 0;
	private int lastID = 0;
	
	private Client myClient;
	private HashMap<Integer, Entity> entities; // < entID : Entity >
	private HashMap<Integer, Integer> clientCharacters; // < netID : entID >
	
	private BSPDungeonGenerator bsp = null;
	private TiledMap worldMap = null;
	private WorldRenderer worldRenderer = null;
	private String currentLevelName = "The Nameless City";
	
	// usually for servers
	private float[] currentMapspawnPoint;
	
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
		Packet.world = this;
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		texts = new LinkedList<>();
		currentMapspawnPoint = new float[]{0.0f, 0.0f};
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, w, h);
		
		stage = new Stage(new FitViewport(w, h));
		stage.getViewport().setCamera(camera);
		camera.zoom = 0.5f;
		
		entities = new HashMap<>();
		clientCharacters = new HashMap<>();
		
		chatText = new TextInput();
		chatText.setLimited(96);
		
		hudFont1 = (BitmapFont) ResourceManager.instance().get("hud_font");
		hudFont2 = (BitmapFont) ResourceManager.instance().get("chat_font");
		hudFont2.getData().markupEnabled = true;
		
		
		
		
		camera.update();
		
		/////////////////////
		
		//generateMap(System.nanoTime(), 0);
	}
	
	public void setLevelName(String name) {
		currentLevelName = name;
	}
	
	public void addEntity(Entity ent, int ID) {
		ent.setWorld(this);
		stage.addActor(ent);
		entities.put(ID, ent);
		ent.setID(ID);
	}
	
	public void deleteEntity(int ID) {
		Entity ent = entities.get(ID);
		ent.remove();
		entities.remove(ID);
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
	
	public void reportNewEntity(Entity ent) {
		Packet.SEntCreate p = new Packet.SEntCreate();
		p.ent = ent;
		myClient.send(p);
	}
	
	public Entity createEntityAuthorized(int ID, String className) {
		try {
			Entity ent = (Entity) Class.forName(className).getDeclaredConstructor().newInstance();
			addEntity(ent, ID);
			return ent;
		} catch (Exception e) {
			CoreGame.instance().getConsole().printerr("cannot create an entity : " + e.getMessage());
		}
		return null;
	}
	
	public TiledMap getWorldMap() {
		return worldMap;
	}
	
	// FOR SERVERS
	//public Character newCharacter(Player player) {
	//	Character character = new Character();
	//	
	//	addEntity(character);
	//	character.setupPlayer(player);
	//	reportNewEntity(character);
	//	
	//	clientCharacters.put(player.getNetID(), character);
	//	
	//	return character;
	//}
	
	public Character getCharacterByNetID(int netID) {
		return (Character) entities.get(clientCharacters.get(netID));
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
			
			// IM SERVERERE
			// teleport to the spawn point
			if (worldMap != null) // map must be ready
				ent.teleport(currentMapspawnPoint[0], currentMapspawnPoint[1]);
		} else if (newConnect && p.getNetID() != myClient.getMyPlayer().getNetID()) {
			feedChat(-1, p.getUsername() + "joined the game");
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
		MapProperties prop = map.getLayers().get("OBJECT").getObjects().get("spawnPoint").getProperties();
		currentMapspawnPoint[0] = ((Float)prop.get("x")).floatValue();
		currentMapspawnPoint[1] = ((Float)prop.get("y")).floatValue();
		
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
		bsp = new BSPDungeonGenerator(seed, 64, 64,
			(Texture)
			ResourceManager.instance().get("tileset__" + BSPDungeonGenerator.tilesets[tilesetIndex])
		);
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
		return false;
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
	
	public void render(float delta) {
		if (markInit) {
			loadMap("demo1");
			markInit = false;
			ready = true;
		}
		
		if (!ready) return;
		
		if (bsp != null) {
			TiledMap map = bsp.getMap();
			if (map != null) {
				setMap(map);
				if (myClient.getCharacter() != null)
					myClient.getCharacter().teleport(bsp.getSpawnPointX(), bsp.getSpawnPointY());
				
				bsp = null;
			}
			return;
		}
		
		if (worldRenderer != null) {
			worldRenderer.setView(camera);
			worldRenderer.render();
		}
		
		// texts
		if (textTimer > 0.0) {
			textTimer -= delta;
		} else {
			textsPop();
		}
		
		SpriteBatch batch = CoreGame.instance().getBatch();
		batch.begin();
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
			
		
		batch.end();
		
		Character m = myClient.getCharacter();
		
		if (myClient.isServer()) {
			// process entities (server)
			for (HashMap.Entry<Integer, Entity> e : entities.entrySet()) {
				e.getValue().process(delta, false);
			}
		} else {
			// process your character (client) 
			if (m != null) {
				m.process(delta, true);
			}
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
		
		/////////////////////
		if (m != null) {
			batch.setColor(Color.WHITE);
			batch.begin();
			batch.draw(m.getIcon(), 96.0f, 12.0f, 96.0f, 96.0f);
			hudFont1.draw(batch, "" + m.getHealth(), 225.0f, 70.0f);
			batch.end();
		}
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
		if (!chatText.getString().isEmpty()) {
			Packet.CSendChat p = new Packet.CSendChat();
			p.message = chatText.getString();
			myClient.send(p);
		}
		chatText.setString("");
		chatMode = false;
		chatModeReady = false;
	}
	
	// add texts on the top left
	public void feedText(String text) {
		CoreGame.instance().flashScreen();
		feedTextItem(new TextItem(text, CHAT_DURATION - textTimer));
	}
	
	public void removeDisconnectedClient(int netID) {
		deleteEntity(clientCharacters.get(netID));
		clientCharacters.remove(netID);
	}
	
	// -1 is a system chat
	public void feedChat(int netID, String text) {
		TextItem i = new TextItem(text, CHAT_DURATION - textTimer);
		if (netID == -1)
			i.authorColor = "YELLOW"; // system
		else
			i.author = myClient.getPlayer(netID).getUsername();
		
		if (netID == myClient.getMyPlayer().getNetID())
			i.authorColor = "YELLOW"; // your message
		feedTextItem(i);
	}
	
	public void dispose() {
		stage.dispose();
	}
}
