package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
	private HashMap<Integer, Entity> entities;
	private HashMap<Integer, Character> clientCharacters;
	
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
		float time;
		public TextItem(String text, float duration) {
			s = text;
			time = duration;
		}
	}
	private LinkedList<TextItem> texts;
	private float textTimer = 4.0f;
	
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
		public final static int UP =		1;
		public final static int DOWN =		1 << 1;
		public final static int LEFT =		1 << 2;
		public final static int RIGHT =		1 << 3;
		public final static int DASH =		1 << 4;
		
		public final static int ATTACK1 =	1 << 5;
		public final static int ATTACK2 =	1 << 6;
		public final static int ATTACK3 =	1 << 7;
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
		
		
		hudFont1 = (BitmapFont) ResourceManager.instance().get("hud_font");
		hudFont2 = (BitmapFont) ResourceManager.instance().get("default_font");
		
		
		
		
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
		return clientCharacters.get(netID);
	}
	
	public void registerNewPlayer(int entID, Player p) {
		Character ent = (Character) createEntityAuthorized(entID,
			Character.class.getName()
		);
		ent.setupPlayer(p);
		
		clientCharacters.put(p.getNetID(), ent);
		
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
		for (Character c : clientCharacters.values()) {
			c.teleport(currentMapspawnPoint[0], currentMapspawnPoint[1]);
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
	
	public boolean isPressed(int mask) {
		return (input & mask) != 0;
	}
	public int isPressedInt(int mask) {
		return (int)Math.signum((float)(input & mask));
	}
	
	public boolean keyDown(int i) {
		int old = input;
		switch (i) {
			case Input.Keys.A :
				input |= InputMap.LEFT;
				feedText("Pressed A WOW !");
				break;
			case Input.Keys.S :
				input |= InputMap.DOWN; break;
			case Input.Keys.D :
				input |= InputMap.RIGHT; break;
			case Input.Keys.W :
				input |= InputMap.UP; break;
		}
		if (old != input) {
			// UPDATE THE INPUT
			myClient.updateInput(input);
			return true;
		}
		return false;
	}
	
	public boolean keyUp(int i) {
		int old = input;
		switch (i) {
			case Input.Keys.A :
				input &= ~InputMap.LEFT; break;
			case Input.Keys.S :
				input &= ~InputMap.DOWN; break;
			case Input.Keys.D :
				input &= ~InputMap.RIGHT; break;
			case Input.Keys.W :
				input &= ~InputMap.UP; break;
		}
		if (old != input) {
			// UPDATE THE INPUT
			myClient.updateInput(input);
			return true;
		}
		return false;
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
			if (!texts.isEmpty())
				texts.remove(0);
			if (!texts.isEmpty())
				textTimer = texts.get(0).time;
		}
		
		SpriteBatch batch = CoreGame.instance().getBatch();
		batch.begin();
		for (int i = 0; i < texts.size(); i++) {
			hudFont2.draw(batch, texts.get(i).s, 20f, 690 + (-i * 40));
		}
		batch.end();
		
		Character m = myClient.getCharacter();
		
		if (myClient.isServer()) {
			for (HashMap.Entry<Integer, Entity> e : entities.entrySet()) {
				e.getValue().process(delta, false);
			}
		} else {
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
			batch.begin();
			batch.draw(m.getIcon(), 96.0f, 12.0f, 96.0f, 96.0f);
			hudFont1.draw(batch, "" + m.getHealth(), 225.0f, 70.0f);
			batch.end();
		}
	}
	
	public void resize(int w, int h) {
		stage.getViewport().update(w, h, true);
	}
	
	// add texts on the top left
	public void feedText(String text) {
		CoreGame.instance().flashScreen();
		if (texts.isEmpty())
			textTimer = 4.0f;
		texts.add(new TextItem(text, 4.0f - textTimer));
	}
	
	public void dispose() {
		stage.dispose();
	}
}
