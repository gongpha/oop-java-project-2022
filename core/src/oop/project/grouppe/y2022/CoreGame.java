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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

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
	
	private World world;
	public World getWorld() { return world; }
	
	private Console console;
	public Console getConsole() { return console; }
	
	private Menu menu;
	public Menu getMenu() { return menu; }
	
	private float flash = 0.0f;
	private ShapeRenderer shapeRenderer;
	
	private enum Status {
		PRELOADING,
		
		PLAYING_DEMO, // in Main menu
		PLAYING,
		
		CONNECTING,
	}
	private Status status = Status.PRELOADING;
	
	@Override
	public void create() {
		
		singleton = this;
		rman = ResourceManager.instance();
		rman.preloads();
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		
		console = new Console();
		
		pref = Gdx.app.getPreferences("oop.proj.2022.settings");
		
		Gdx.input.setInputProcessor(this);
		////////////////////////////////
		
		Packet.regPackets();
		console.print("Initialized !");
		
		console.exec("developers");
		
	}
	
	public void killNet() {
		if (client != null) {
			client.kill();
		}
		if (server != null) {
			server.kill();
		}
	}
	
	public void quit() {
		Gdx.app.exit();
	}
	
	////////////////////////////////////////////
	
	private void startGame() {
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
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
			shapeRenderer.setColor(new Color(1.0f, 1.0f, 0.0f, flash));
			shapeRenderer.rect(0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			shapeRenderer.end();
			
			Gdx.gl.glDisable(GL20.GL_BLEND);
			flash -= delta * 2.0f;
		}
		
		menu.renderMenu();
	}
	
	////////////////////////////////////////////
	
	public void hostGame() {
		// UDP is unreliable, u kno ?
		server = new Server(PORT);
		server.start();
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
	}
	
	public void enterWorld() {
		world = new World();
		world.setMyClient(client);
		client.sendMyInfo();
		
		status = Status.PLAYING;
		menu.showAsPauseMenu();
		if (menu.isShowing()) menu.toggle();
	}
	
	public void forceDisconnect(String reason) {
		// OOF
		
		if (client != null) {
			client.kill();
			client = null;
		}
		
		console.print("disconnected : " + reason);
		console.showFull();
	}
	
	public void flashScreen() {
		flash = 0.5f;
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
			renderBatch();
			break;
		case CONNECTING:
			if (connectOK) {
				enterWorld();
				connectOK = false;
			}
			break;
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
	
	public void tellServerKilled() {
		server = null;
	}
	
	public void tellConnectSuccess() {
		connectOK = true;
	}
	
	////////////////////////////////////////////
	
	public boolean keyTyped (char c) {
		if (console.isActivating()) {
			if (console.keyTyped(c)) return true;
		}
		
		
		if (status == Status.PLAYING_DEMO) {
			if (menu.isShowing()) {
				return menu.keyTyped(c);
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
						menu.showMain();
					}
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