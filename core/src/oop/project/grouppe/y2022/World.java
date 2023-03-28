package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.HashMap;

public class World { // implements Screen
	//private Server server;
	
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
	
	// HUD
	
	private BitmapFont hudFont1;
	
	public class WorldRenderer extends OrthogonalTiledMapRenderer {
		public WorldRenderer(TiledMap map) { super(map); }

		public void renderObject (MapObject object) {
			TextureMapObject t = (TextureMapObject)object;
			batch.draw(t.getTextureRegion(), t.getX(), t.getY());
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
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, w, h);
		
		stage = new Stage(new FitViewport(w, h));
		stage.getViewport().setCamera(camera);
		camera.zoom = 0.5f;
		
		entities = new HashMap<>();
		clientCharacters = new HashMap<>();
		
		
		hudFont1 = (BitmapFont) ResourceManager.instance().get("hud_font");
		
		
		
		
		camera.update();
		
		/////////////////////
		
		generateMap(System.nanoTime(), 0);
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
	
	public void setMyClient(Client myClient) {
		this.myClient = myClient;
	}
	public Client getMyClient() {
		return myClient;
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
				input |= InputMap.LEFT; break;
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
		if (bsp != null) {
			TiledMap map = bsp.getMap();
			if (map != null) {
				worldMap = map;
				
				worldRenderer = new WorldRenderer(map);
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
		
		Character m = myClient.getCharacter();
		if (m != null) {
			if (true || myClient.isPuppet()) // is server OR my character (todo)
				m.process(delta);
			
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
			SpriteBatch batch = CoreGame.instance().getBatch();
			batch.begin();
			batch.draw(m.getIcon(), 96.0f, 12.0f, 96.0f, 96.0f);
			hudFont1.draw(batch, "" + m.getHealth(), 225.0f, 70.0f);
			batch.end();
		}
	}
	
	public void resize(int w, int h) {
		stage.getViewport().update(w, h, true);
	}
	
	public void dispose() {
		stage.dispose();
	}
}
