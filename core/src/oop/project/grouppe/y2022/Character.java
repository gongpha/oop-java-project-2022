package oop.project.grouppe.y2022;

// an object that does like a character 

import com.badlogic.gdx.Gdx;
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
	private final TextureRegion regionIcon;
	
	// SERVER & PREDICTABLE
	private Vector2 velocity;
	private Vector2 wishdir; // for calculating velocity
	private int input = 0;
	
	private Player player = null;
	private float speed = 400.0f;
	
	/*  DIRECTIONS :
	
		DOWN(0),
		UP(1),
		LEFT_UP(2),
		LEFT(3),
		LEFT_DOWN(4),
		RIGHT_UP(5),
		RIGHT(6),
		RIGHT_DOWN(7);
	*/
	
	// according to the sprite sheets
	// @_@ bro
	private final static int[][][] animations = {
		{{0, 32}, {0, 64}, {0, 96}},
		{{32, 32}, {32, 64}, {32, 96}},
		{{64, 32}, {96, 32}, {128, 32}},
		{{64, 64}, {96, 64}, {128, 64}},
		{{64, 96}, {96, 96}, {128, 96}}
	};
	private final static int[][] animationsDir = {
		// animation index, horizontal flip
		{0, 0},
		{1, 0},
		{2, 1},
		{3, 1},
		{4, 1},
		{2, 0},
		{3, 0},
		{4, 0},
	};
	
	// convert wishdir (input) to a direction number
	// [-1, 0, 1] + 1 = [0, 1, 2] ( [y][x] ) (@^@)> what the hell
	private final static int[][] directionBywishdir = {
		{2, 1, 5},
		{3, 0, 6},
		{4, 0, 7}
	};
	
	public final static String[] characters = {
		"character1",
		"character2",
	};
	
	private float animationIndex = 0.0f; // [0, 3] index [0, 1, 2] by Math.floor ((int) cast)
	
	// control by the server & prediction (?)
	private int direction = 0; // I HATE (java) ENUMS
	private boolean animating = false;
	
	// for servers
	//private Client client;
	
	public Character() {
		game = CoreGame.instance();
		world = game.getWorld();
		
		region = new TextureRegion();
		regionIcon = new TextureRegion();
		
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
		region.setRegion(0, 32, 32, 32);
		
		regionIcon.setTexture(region.getTexture());
		regionIcon.setRegion(128, 0, 32, 32);
	}
	public TextureRegion getIcon() {
		return regionIcon;
	}
	
	public void setInput(int input) {
		this.input = input;
	}
	
	public int getInput() { // TODO : USE THIS INSIDE THE SERVER
		return input;
	}
	
	public void process(float delta, boolean prediction) {
		World world = getWorld();
		
		wishdir.x = world.isPressedInt(World.InputMap.RIGHT) - world.isPressedInt(World.InputMap.LEFT);
		wishdir.y = world.isPressedInt(World.InputMap.UP) - world.isPressedInt(World.InputMap.DOWN);
		direction = directionBywishdir[(int)(wishdir.y * -1.0f) + 1][(int)wishdir.x + 1];
		//System.out.println(direction);
		
		wishdir = wishdir.nor();
		
		setAnimating(!wishdir.isZero());
			
		
		// accel
		velocity = velocity.lerp(wishdir.scl(speed * delta), delta * 20.0f);
		
		move(velocity);
	}
	
	public void setAnimating(boolean yes) {
		animating = yes;
		// TODO : Send a packet
	}
	
	@Override
	public void draw(Batch batch, float alpha){
		if (region.getTexture() == null) return; // no
		
		float delta = Gdx.graphics.getDeltaTime() * 3.0f; // 3x faster
		
		int[] aniDir = animationsDir[direction];
		int[][] aniSeq = animations[aniDir[0]];
		if (animating) {
			animationIndex += delta;
			if (animationIndex >= 3) animationIndex = 0.0f; // reset
		} else {
			animationIndex = 0.0f;
		}
		int[] aniCoord = aniSeq[(int)animationIndex];
		boolean flipH = aniDir[1] == 1;
		region.setRegion(
			aniCoord[0],
			aniCoord[1],
			32, 32
		);
		
		animationIndex += delta;
		
		batch.draw(region, getX() + (flipH ? 32.0f : 0.0f), getY(), flipH ? -32.0f : 32.0f, 32.0f);
	}
	
	public void serializeConstructor(DataOutputStream d) throws IOException {
		d.writeInt(player.getNetID());
	}
	public void deserializeConstructor(DataInputStream d) throws IOException {
		setupPlayer(world.getMyClient().getPlayer(d.readInt()));
	}
}
