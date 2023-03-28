package oop.project.grouppe.y2022;

// an object that does like a character 

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Character extends Entity {
	private CoreGame game;
	private World world;
	private final TextureRegion region;
	
	// SERVER & PREDICTABLE
	private Vector2 velocity;
	private Vector2 wishdir;
	private int input = 0;
	
	private Player player = null;
	private float speed = 400.0f;
	
	public final static String[] characters = {
		"character1",
		"character2",
	};
	
	// for servers
	//private Client client;
	
	public Character() {
		game = CoreGame.instance();
		world = game.getWorld();
		
		region = new TextureRegion();
		
		velocity = new Vector2();
		wishdir = new Vector2();
	}
	
	public Player getPlayer() {
		return player;
	}
	public void setupPlayer(Player player) {
		this.player = player;
		region.setTexture(
			(Texture) ResourceManager.instance().get("character__" + characters[player.getIdent(0)])
		);
		region.setRegionX(0);
		region.setRegionY(32);
		region.setRegionWidth(32);
		region.setRegionHeight(32);
	}
	
	public void setInput(int input) {
		this.input = input;
	}
	
	public void process(float delta) {
		World world = getWorld();
		
		wishdir.x = world.isPressedInt(World.InputMap.RIGHT) - world.isPressedInt(World.InputMap.LEFT);
		wishdir.y = world.isPressedInt(World.InputMap.UP) - world.isPressedInt(World.InputMap.DOWN);
		wishdir = wishdir.nor();
		
		// accel
		velocity = velocity.lerp(wishdir.scl(speed * delta), delta * 20.0f);
		
		move(velocity);
	}
	
	@Override
	public void draw(Batch batch, float alpha){
		if (region.getTexture() == null) return; // no
		batch.draw(region, getX(), getY());
	}
	
	public void serializeConstructor(DataOutputStream d) throws IOException {
		d.writeInt(player.getNetID());
	}
	public void deserializeConstructor(DataInputStream d) throws IOException {
		setupPlayer(world.getMyClient().getPlayer(d.readInt()));
	}
}
