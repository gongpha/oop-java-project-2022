package oop.project.grouppe.y2022;

// OUR GAME
// ( ... SOVIET ANTHEM INTENSIFIES ... )

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.ArrayList;

public class CoreGame extends ApplicationAdapter implements InputProcessor {
	// singleton hoho
	private static CoreGame singleton = null;
	public static synchronized CoreGame instance() { return singleton; }
	
	/////////////////////////////////////
	private ResourceManager rman;
	private SpriteBatch batch;
	public SpriteBatch getBatch() { return batch; }
	public final static int PORT = 13131;
	public final static int NAMELENGTH = 12;
	
	private Preferences pref;
	public Preferences getPref() { return pref; }
	
	private Client client = null;
	private Server server = null;
	public Server getServer() { return server; }
	private boolean connectOK = false;
	private boolean disconnected = false;
	private String disconnectedReason = "@@@";
	
	private World world;
	public World getWorld() { return world; }
	
	private Console console;
	public Console getConsole() { return console; }
	
	private Menu menu;
	public Menu getMenu() { return menu; }
	
	private float flash = 0.0f;
	private ShapeRenderer shapeRenderer;
	
	private boolean drawShowinfo = false;
	private BitmapFont infoFont;
	private ArrayList<Integer> infoGraph;
	private int graphFrame = 0;
	private ShapeRenderer graphShapeRenderer;
	
	private int volume = 0;
	
	private enum Status {
		PRELOADING,
		
		PLAYING_DEMO, // in Main menu
		PLAYING,
		
		CONNECTING,
	}
	private Status status = Status.PRELOADING;
	private String connectingIP = "?localhost?";
	
	private Texture connectingTexture;
	
	@Override
	public void create() {
		
		singleton = this;
		rman = ResourceManager.instance();
		rman.preloads();
		
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		graphShapeRenderer = new ShapeRenderer();
		
		console = new Console();
		infoFont = console.getFont();
		infoGraph = new ArrayList<>();
		
		pref = Gdx.app.getPreferences("oop.proj.2022.settings");
		volume = pref.getInteger("volume");
		
		Gdx.input.setInputProcessor(this);
		////////////////////////////////
		
		Packet.regPackets();
		console.print("Initialized !");
		
		console.exec("developers");
		
	}
	
	public void killNet() {
		if (client != null) {
			client.kill("Exited the game");
		}
		if (server != null) {
			server.kill();
		}
	}
	
	public void quit() {
		disconnect();
		Gdx.app.exit();
	}
	
	////////////////////////////////////////////
	
	private void startGame() {
		connectingTexture = (Texture) rman.get("connecting");
		
		menu = new Menu();
		//console.exec("map");
		
		// PROTOTYPE
		status = Status.PLAYING_DEMO;
	}
	
	// WONT CALL IF NOT PLAYING (status)
	private void renderBatch() {
		float delta = Gdx.graphics.getDeltaTime();
		if (world != null) {
			batch.end();
			
			world.render(delta);
			batch.begin();
		}
		
		if (flash > 0.0) {
			batch.end();
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
			shapeRenderer.setColor(new Color(1.0f, 1.0f, 0.0f, flash));
			shapeRenderer.rect(0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			shapeRenderer.end();
			
			Gdx.gl.glDisable(GL20.GL_BLEND);
			batch.begin();
			flash -= delta * 2.0f;
		}
		
		menu.renderMenu();
	}
	
	////////////////////////////////////////////
	
	public void hostGame() {
		// UDP is unreliable, u kno ?
		server = new Server(PORT);
		server.start();
	}
	
	public void tellServerCreated() {
		joinGame("localhost");
	}
	
	public void joinGame(String ip) {
		client = new Client(ip,
			menu.getUsername(),
			new int[]{menu.getIdent1(), 0, 0, 0},
			server
		);
		client.start();
		status = Status.CONNECTING;
		connectingIP = ip;
	}
	
	public void enterWorld() {
		world = new World();
		Packet.world = world;
		world.setMyClient(client);
		client.sendMyInfo();
		
		status = Status.PLAYING;
		menu.showAsPauseMenu();
		if (menu.isShowing()) menu.toggle(); // hide the menu
	}
	
	public void exitWorld() {
		if (world != null) {
			world.dispose();
			world = null;
		}
		if (client != null) {
			client.kill("Disconnected by game");
			client = null;
		}
		if (server != null) {
			server.kill();
			server = null;
		}
		ResourceManager.instance().stopAllSoundMusic();
		menu.showAsMainMenu();
		status = Status.PLAYING_DEMO;
		if (!menu.isShowing()) menu.toggle(); // show the menu
		
		console.print("Exited the world (" + disconnectedReason + ")");
	}
	
	public void flashScreen() {
		flash = 0.5f;
	}
	
	public void disconnect() {
		if (client == null) return;
		client.disconnectMe();
	}
	
	public void toggleShowinfo() {
		drawShowinfo = !drawShowinfo;
	}
	public void setShowinfo(boolean yes) {
		drawShowinfo = yes;
	}
	
	////////////////////////////////////////////

	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1);
		batch.begin();
		//batch.draw(img, 0, 0);
		//
		
		
		
		switch (status) {
		case PRELOADING:
			boolean done = rman.poll();
			float p = rman.getProgress();
			if (p >= 0.0)
				console.print("Loading . . . (" + (int)(p * 100.0) + "%)");
			
			if (done) {
				// PRELOADING DONE !!!1!1
				status = Status.PLAYING_DEMO;
				startGame();
				console.hide();
			}
			
			break;
		case PLAYING_DEMO:
		case PLAYING:
			if (disconnected) {
				exitWorld();
				disconnected = false;
			}
			renderBatch();
			break;
		case CONNECTING:
			if (disconnected) {
				exitWorld();
				disconnected = false;
				break;
			}
			if (connectOK) {
				enterWorld();
				connectOK = false;
			}
			batch.draw(connectingTexture, 0, 0);
			console.getFont().setColor(Color.BLACK);
			console.getFont().draw(batch, "Connecting to " + connectingIP, 60, 600);
			break;
		}
		
		if (drawShowinfo) {
			//batch.begin();
			
			if (world != null) {
				Character c = world.getMyClient().getCharacter();
				String s = "<null>";
				if (c != null && c.getCurrentNode() != null) {
					s = c.getCurrentNode().toString();
				}
				infoFont.draw(batch, "Quadtree : " + s, 30, 30);
			}
			
			
			//batch.end();
			
			// draw network graphs
			if (infoGraph.size() == 128)
				infoGraph.remove(0);
			infoGraph.add(graphFrame);
			graphFrame = 0;
			
			final int POS = 128;
			if (!infoGraph.isEmpty()) {
				batch.end();
				graphShapeRenderer.begin(ShapeRenderer.ShapeType.Line);
				graphShapeRenderer.setColor(Color.RED);
				for (int i = 0; i < infoGraph.size(); i++) {
					graphShapeRenderer.line(
						new Vector2(POS + i, POS),
						new Vector2(POS + i, POS + infoGraph.get(i))
					);
				}
				graphShapeRenderer.end();
				batch.begin();
			}
		}

		console.renderConsole();
		//
		batch.end();
	}
	
	@Override
	public void dispose () {
		killNet();
		console.dispose();
		rman.dispose();
		batch.dispose();
		if (world != null) {
			world.dispose();
			world = null;
		}
	}
	
	////////////////////////////////////////////
	// DO NOT CALL THESE
	
	public void tellConnectSuccess() {
		connectOK = true;
	}
	
	public void tellDisconnected(String reason) {
		disconnectedReason = reason;
		disconnected = true;
	}
	
	public void tellReceivedPacket() {
		if (!drawShowinfo) return;
		graphFrame += 1;
	}
	
	////////////////////////////////////////////
	
	public void setVolume(int v) {
		volume = v;
		ResourceManager.instance().setVolume(v);
	}
	public int getVolume() { return volume; }
	public float getVolumef() { return volume / 100.0f; }
	
	////////////////////////////////////////////
	
	public boolean keyTyped (char c) {
		if (console.isActivating()) {
			if (console.keyTyped(c)) return true;
		}
		
		
		if (status == Status.PLAYING_DEMO) {
			if (menu.isShowing()) {
				return menu.keyTyped(c);
			}
		} else if (status == Status.PLAYING) {
			if (world != null) {
				if (world.keyTyped(c)) return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean keyDown(int i) {
		if (console.isActivating()) {
			if (
				i == Input.Keys.ESCAPE || i == Input.Keys.F7
			) {
				// close console
				console.hide();
				return true;
			}
			// whether you pressed any keys, but if the console is activating
			// YOUR KEY WILL GET CONSUMED LMAO
			return console.keyDown(i);
		}
		if (i == Input.Keys.F7) {
			// toggle console
			console.showHalf();
			return true;
		}
		if (status == Status.PLAYING_DEMO || status == Status.PLAYING) {
			
			if (i == Input.Keys.ESCAPE) {
				if (menu.isShowing()) {
					if (menu.isOnRoot()) {
						menu.toggle();
					} else {
						menu.playSoundEnter();
						menu.showMain();
					}
				} else if (world != null && world.handleEscapeKey()) {
					return true;
				} else {
					menu.toggle();
					return true;
				}
			} else {
				if (menu.isShowing()) {
					return menu.keyDown(i);
				} else {
					if (status == Status.PLAYING_DEMO) {
						menu.toggle();
						return true;
					}
					if (world != null) {
						if (world.keyDown(i)) return true;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public void resize(int w, int h) {
		if (world != null) {
			world.resize(w, h);
		}
	}

	@Override
	public boolean keyUp(int i) {
		if (world != null) {
			if (world.keyUp(i)) return true;
		}
		return false;
	}

	@Override
	public boolean touchDown(int i, int i1, int i2, int i3) { return false; }

	@Override
	public boolean touchUp(int i, int i1, int i2, int i3) { return false; }

	@Override
	public boolean touchDragged(int i, int i1, int i2) { return false; }

	@Override
	public boolean mouseMoved(int i, int i1) { return false; }

	@Override
	public boolean scrolled(float f, float f1) { return false; }
}
