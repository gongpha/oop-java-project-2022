package oop.project.grouppe.y2022;

// OUR GAME
// ( ... SOVIET ANTHEM INTENSIFIES ... )

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Net.Protocol;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.utils.ScreenUtils;

public class CoreGame extends ApplicationAdapter implements InputProcessor {
	// singleton hoho
	private static CoreGame singleton = null;
	public static synchronized CoreGame instance() { return singleton; }
	
	/////////////////////////////////////
	ResourceManager rman;
	SpriteBatch batch;
	public SpriteBatch getBatch() { return batch; }
	public final static int PORT = 13131;
	
	private ServerSocket server;
	
	Console console;
	Menu menu;
	
	private enum Status {
		PRELOADING,
		
		PLAYING_DEMO, // in Main menu
		PLAYING
	}
	private Status status = Status.PRELOADING;
	
	@Override
	public void create () {
		singleton = this;
		rman = ResourceManager.instance();
		rman.preloads();
		batch = new SpriteBatch();
		
		console = new Console();
		
		Gdx.input.setInputProcessor(this);
		////////////////////////////////
		
		console.print("Initialized !");
		
		console.exec("developers");
		
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
		menu.renderMenu();
	}
	
	////////////////////////////////////////////
	
	public void hostGame() {
		// UDP is unreliable, u kno ?
		server = Gdx.net.newServerSocket(Protocol.TCP, PORT, new ServerSocketHints());
	}
	
	////////////////////////////////////////////

	@Override
	public void render () {
		ScreenUtils.clear(1, 0, 0, 1);
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
				status = Status.PLAYING;
				startGame();
				console.hide();
			}
			
			break;
		case PLAYING_DEMO:
		case PLAYING:
			renderBatch();
			break;
		}
		
		console.renderConsole();
		//
		batch.end();
	}
	
	@Override
	public void dispose () {
		console.dispose();
		rman.dispose();
		batch.dispose();
	}
	
	////////////////////////////////////////////
	
	public boolean keyTyped (char c) {
		if (console.isActivating()) {
			if (console.keyTyped(c)) return true;
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
		if (status == Status.PLAYING_DEMO) {
			if (menu.isShowing()) {
				if (i == Input.Keys.ESCAPE) {
					menu.toggle();
					return true;
				} else {
					return menu.keyDown(i);
				}
			} else {
				menu.toggle();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyUp(int i) { return false; }

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
