package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.HashMap;

public class World { // implements Screen
	//private Server server;
	
	private OrthographicCamera camera;
	private Stage stage;
	
	private int input = 0;
	
	private Character myCharacter;
	private HashMap<Integer, Character> clientCharacters;
	
	public class InputMap {
		public final static int UP =		1;
		public final static int DOWN =		1 << 1;
		public final static int LEFT =		1 << 2;
		public final static int RIGHT =		1 << 3;
		public final static int DASH =		1 << 4;
		
		public final static int ATTACK1 =	1 << 4;
		public final static int ATTACK2 =	1 << 5;
		public final static int ATTACK3 =	1 << 6;
	}
	
	public World() {
		//this.server = server;
		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, w, h);
		camera.update();
		stage = new Stage(new FitViewport(w, h));
		
		clientCharacters = new HashMap<>();
		
		/////////////////////
		
	}
	
	public Character newCharacter(Player player) {
		Character character = new Character();
		character.setWorld(this);
		character.setupPlayer(player);
		clientCharacters.put(player.getNetID(), character);
		stage.addActor(character);
		
		return character;
	}
	
	public Character getCharacterByNetID(int netID) {
		return clientCharacters.get(netID);
	}
	
	public void changeMap() {
		
	}
	
	/////////////////////
	
	public boolean isPressed(int mask) {
		return (input & mask) != 0;
	}
	public int isPressedInt(int mask) {
		return (int)Math.signum((float)(input & mask));
	}
	
	public boolean keyDown(int i) {
		switch (i) {
			case Input.Keys.A :
				input |= InputMap.LEFT;
				return true;
			case Input.Keys.S :
				input |= InputMap.DOWN;
				return true;
			case Input.Keys.D :
				input |= InputMap.RIGHT;
				return true;
			case Input.Keys.W :
				input |= InputMap.UP;
				return true;
		}
		return false;
	}
	
	public boolean keyUp(int i) {
		switch (i) {
			case Input.Keys.A :
				input &= ~InputMap.LEFT;
				return true;
			case Input.Keys.S :
				input &= ~InputMap.DOWN;
				return true;
			case Input.Keys.D :
				input &= ~InputMap.RIGHT;
				return true;
			case Input.Keys.W :
				input &= ~InputMap.UP;
				return true;
		}
		return false;
	}
	
	public void render(float delta) {
		//myCharacter.process(delta);
		
		stage.act(delta);
		stage.draw();
	}
	
	public void resize(int w, int h) {
		stage.getViewport().update(w, h, true);
	}
	
	public void dispose() {
		stage.dispose();
	}
}
