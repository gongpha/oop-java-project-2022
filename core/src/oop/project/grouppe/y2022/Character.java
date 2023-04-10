package oop.project.grouppe.y2022;

// a player entity

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/*
	clients own :
		- position
		- direction index
		- animation (playing)
	servers own :
		- otherwise
*/

public class Character extends Entity {
	private CoreGame game;
	private World world;
	private final TextureRegion region;
	private final TextureRegion regionIcon;
	
	// CLIENTSIDE
	private Vector2 velocity;
	private Vector2 wishdir; // for calculating velocity
	private int input = 0;
	
	private Player player = null;
	private float speed = 400.0f;
	
	private final BitmapFont labelFont;
	
	public void toggleNoclip() {
		super.toggleNoclip();
		world.submitChat(-2, getPlayer().getUsername() + " has used a cheat ! : Noclip");
	}
	
	public class MoveDirection {
		public final static int UP =		1;
		public final static int DOWN =		1 << 1;
		public final static int LEFT =		1 << 2;
		public final static int RIGHT =		1 << 3;
	}
	private int moveInput = 0;
	
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
	private final static byte[][] directionBywishdir = {
		{2, 1, 5},
		{3, 0, 6},
		{4, 0, 7}
	};
	
	public final static String[] characters = {
		"character1",
		"character2",
	};
	
	// server side
	private final float POWER_DURATION = 15.0f;
	private final float POWER_DURATION_END = 3.0f;
	private class Power {
		char powerup;
		float timer = POWER_DURATION;
		boolean aboutToEnd = false;
		public Power(char powerup) {
			this.powerup = powerup;
		}
	}
	private ArrayList<Power> powers;
	private boolean protection = false;
	public boolean hasProtection() { return protection; }
	
	/* control by clients */
	private float animationIndex = 0.0f; // [0, 3] index [0, 1, 2] by Math.floor ((int) cast)
	public void setAnimationIndex(float a) { animationIndex = a; }
	public float getAnimationIndex() { return animationIndex; }

	private byte direction = 0; // I HATE (java) ENUMS
	public void setDirection(byte a) { direction = a; }
	public byte getDirection() { return direction; }
	
	private boolean animating = false;
	public void setAnimating(boolean a) { animating = a; }
	public boolean getAnimating() { return animating; }
	
	/* end */
	
	public Character() {
		game = CoreGame.instance();
		world = game.getWorld();
		labelFont = (BitmapFont) ResourceManager.instance().get("character_name_font");
		
		region = new TextureRegion();
		regionIcon = new TextureRegion();
		
		velocity = new Vector2();
		wishdir = new Vector2();
		
		powers = new ArrayList<>();
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
	
	public void resetMoveInput() {
		moveInput = 0;
	}
	
	public int getInput() { // TODO : USE THIS INSIDE THE SERVER
		return input;
	}
	
	public boolean isPressed(int mask) {
		return (moveInput & mask) != 0;
	}
	public int isPressedInt(int mask) {
		return (int)Math.signum((float)(moveInput & mask));
	}
	
	// only process from their machines
	public void process(float delta) {
		wishdir.x = isPressedInt(MoveDirection.RIGHT) - isPressedInt(MoveDirection.LEFT);
		wishdir.y = isPressedInt(MoveDirection.UP) - isPressedInt(MoveDirection.DOWN);
		
		setAnimating(!wishdir.isZero());
		if (animating)
			direction = directionBywishdir[(int)(wishdir.y * -1.0f) + 1][(int)wishdir.x + 1];
		
		// wishdir = wishdir.nor();
		
		// accel
		velocity = velocity.lerp(wishdir.scl(speed * delta), delta * 20.0f);
		if (Math.abs(velocity.x) < 0.01f) velocity.x = 0.0f;
		if (Math.abs(velocity.y) < 0.01f) velocity.y = 0.0f;
		
		move(velocity);
	}
	
	public void reportPos() {
		world.getMyClient().updateMyPlayerPos();
	}
	
	public void setPosition(float x, float y) {
		super.setPosition(x, y);
		if (!world.getMyClient().isServer()) return;
		
		// check collision (dynamic)
		QuadTree q = world.getMapQuadTree();
		if (q != null)
			q.updatePos(this);
	}
	
	public boolean isMySelf() {
		return player.getNetID() == world.getMyClient().getMyPlayer().getNetID();
	}
	
	@Override
	public void draw(Batch batch, float alpha) {
		if (region.getTexture() == null) return; // no
		
		float delta = Gdx.graphics.getDeltaTime(); // 3x faster
		
		if (world.getMyClient().isServer()) {
			ArrayList<Power> toRemove = new ArrayList<>();
			for (Power p : powers) {
				p.timer -= delta;
				if (!p.aboutToEnd && p.timer < POWER_DURATION_END) {
					p.aboutToEnd = true;
					Packet.SCharacterUpdatePowerup packet = new Packet.SCharacterUpdatePowerup();
					packet.target = this;
					packet.powerup = p.powerup;
					packet.tell = 1;
					world.getMyClient().send(packet);
				}
				if (p.timer <= 0.0f) {
					Packet.SCharacterUpdatePowerup packet = new Packet.SCharacterUpdatePowerup();
					packet.target = this;
					packet.powerup = p.powerup;
					packet.tell = 2;
					world.getMyClient().send(packet);
				}
				toRemove.clear();
			}
			
			for (Power p : toRemove) {
				if (p.powerup == (char)0) {
					protection = false;
				}
				powers.remove(p);
			}
		}

		int[] aniDir = animationsDir[direction];
		int[][] aniSeq = animations[aniDir[0]];
		if (isMySelf()) {
			if (animating) {
				animationIndex += delta * 3.0f;
				if (animationIndex >= 3) animationIndex = 0.0f; // reset
			} else {
				animationIndex = 0.0f;
			}
			
		}
		
		int[] aniCoord = aniSeq[(int)animationIndex];
		boolean flipH = aniDir[1] == 1;
		region.setRegion(
			aniCoord[0],
			aniCoord[1],
			32, 32
		);
		
		if (isMySelf()) {
			animationIndex += delta;
		}
		
		
		
		batch.setColor(Color.WHITE);
		batch.draw(region, getX() + (flipH ? 32.0f : 0.0f), getY(), flipH ? -32.0f : 32.0f, 32.0f);
		
		if (!isMySelf()) {
			// draw the username if they're others
			final String username = player.getUsername();
			GlyphLayout layout = new GlyphLayout(labelFont, username);
			float fontX = getX() + (32.0f - layout.width) / 2;
			float fontY = getY() + 48.0f;
			labelFont.setColor(Color.BLACK);
			labelFont.draw(batch, username, fontX + 1, fontY - 1);
			labelFont.setColor(Color.GREEN);
			labelFont.draw(batch, username, fontX, fontY);
		}
	}
	
	public boolean keyDown(int i) {
		switch (i) {
			case Input.Keys.A :
				moveInput |= MoveDirection.LEFT; return true;
			case Input.Keys.S :
				moveInput |= MoveDirection.DOWN; return true;
			case Input.Keys.D :
				moveInput |= MoveDirection.RIGHT; return true;
			case Input.Keys.W :
				moveInput |= MoveDirection.UP; return true;
		}
		return false;
	}
	
	public boolean keyUp(int i) {
		switch (i) {
			case Input.Keys.A :
				moveInput &= ~MoveDirection.LEFT; return true;
			case Input.Keys.S :
				moveInput &= ~MoveDirection.DOWN; return true;
			case Input.Keys.D :
				moveInput &= ~MoveDirection.RIGHT; return true;
			case Input.Keys.W :
				moveInput &= ~MoveDirection.UP; return true;
		}
		return false;
	}
	
	public void serializeConstructor(DataOutputStream d) throws IOException {
		d.writeInt(player.getNetID());
	}
	public void deserializeConstructor(DataInputStream d) throws IOException {
		setupPlayer(world.getMyClient().getPlayer(d.readInt()));
	}
	
	public Rectangle getRect() {
		return new Rectangle(getX(), getY(), 32, 32);
	}
	public Rectangle getRectInTile() {
		return new Rectangle(getX() / 32.0f, getY() / 32.0f, 1, 1);
	}
	
	public void collidedWith(Entity collidee) {
		if (collidee instanceof Item) {
			if (( (Item) collidee).playerObtained(this)) collidee.deleteMe();
		}
	}
	
	//////////////////////
	
	// server only
	public void givePower(char powerup) {
		powers.add(new Power(powerup));
		
		if (powerup == (char)0) {
			protection = true;
		}
		
		Packet.SCharacterUpdatePowerup p = new Packet.SCharacterUpdatePowerup();
		p.target = this;
		p.powerup = powerup;
		p.tell = 0;
		
		world.getMyClient().send(p);
	}
	
	// called from the server
	public void updatePowerup(char powerup, char tell) {
		switch (tell) {
			case 0 : powerBegin(powerup); break;
			case 1 : powerAboutToEnd(powerup); break;
			case 2 : powerEnd(powerup); break;
		}
	}
	
	private void powerBegin(char powerup) {
		if (!isMySelf()) return; // not my self. dont care
		
		
		
		// set icon
		String c = "??? begin ???";
		switch (powerup) {
			case 0 : // protecc
				regionIcon.setRegion(0, 224, 32, 32);
				c = "You got the protection !";
				((Sound)ResourceManager.instance().get("s_protect")).play();
				break;
		}
		world.feedChat(-1, c, true);
	}
	
	private void powerAboutToEnd(char powerup) {
		if (!isMySelf()) return; // not my self. dont care
		String c = "??? expire ???";
		switch (powerup) {
			case 0 : // protecc
				c = "Protection is about to expire !";
				break;
		}
		world.feedChat(-1, c, true);
	}
	
	private void powerEnd(char powerup) {
		if (!isMySelf()) return; // not my self. dont care
		
		// reset
		regionIcon.setRegion(128, 0, 32, 32);
		//world.feedChat(-1, c, true);
	}
	
	
	
}
