package oop.project.grouppe.y2022;

// a player entity

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
	private World world;
	private final TextureRegion region;
	private final TextureRegion regionIcon;
	
	// CLIENTSIDE
	private Vector2 velocity;
	private final Vector2 wishdir; // for calculating velocity
	private int input = 0;
	
	private Player player = null;
	private float speed = 500.0f;
	
	private final BitmapFont labelFont;
	
	private boolean god;
	private boolean buddha;
	private boolean notarget;
	
	// clientside cheat
	public boolean toggleNoclip() {
		boolean b = super.toggleNoclip();
		if (b)
			world.submitChat(-2, getPlayer().getUsername() + " has used a cheat ! : Noclip");
		else
			world.submitChat(-1, getPlayer().getUsername() + " has disabled their cheat : Noclip");
		return b;
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
	
	// server side
	private final float POWER_DURATION = 15.0f;
	private final float POWER_DURATION_END = 3.0f;
	private boolean protection = false;
	public boolean hasProtection() { return protection; }
	
	private boolean invisible = false;
	public boolean isInvisible() { return invisible || notarget; }
	
	private boolean reviving = false;
	public boolean canRevive() { return reviving; }
	
	private int health = 100;
	
	private class Power {
		byte powerup;
		float timer = POWER_DURATION;
		int regionX;
		int regionY;
		boolean aboutToEnd = false;
		public Power(byte powerup) {
			this.powerup = powerup;
		}
	}
	private ArrayList<Power> powers;
	
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
	
	private boolean faster = false;
	public boolean hasFaster() { return faster; }
	
	/* end */
	
	public Character() {
		world = CoreGame.instance().getWorld();
		labelFont = (BitmapFont) ResourceManager.instance().get("character_name_font");
		
		region = new TextureRegion();
		regionIcon = new TextureRegion();
		
		velocity = new Vector2();
		wishdir = new Vector2();
		
		powers = new ArrayList<>();
		
		god = false;
		buddha = false;
		notarget = false;
	}
	
	public Player getPlayer() {
		return player;
	}
	public void setupPlayer(Player player) {
		this.player = player;
		region.setTexture(
			(Texture) ResourceManager.instance().get("character__" + Customization.CHARACTERS[player.getIdent(0)])
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
		//delta *= 0.2f;
		if (isDied()) return;
		wishdir.x = isPressedInt(MoveDirection.RIGHT) - isPressedInt(MoveDirection.LEFT);
		wishdir.y = isPressedInt(MoveDirection.UP) - isPressedInt(MoveDirection.DOWN);
		
		setAnimating(!wishdir.isZero());
		if (animating)
			direction = directionBywishdir[(int)(wishdir.y * -1.0f) + 1][(int)wishdir.x + 1];
		
		// wishdir = wishdir.nor();
		
		// accel
		
		float fast = 1.0f;
		if (faster) {
			fast = 1.5f;
		}
		
		//velocity = wishdir;
		//velocity.scl(8.0f);
		velocity = velocity.lerp(wishdir.scl(speed * delta * fast), delta * 15.0f);
		if (Math.abs(velocity.x) < 0.1f) velocity.x = 0.0f;
		if (Math.abs(velocity.y) < 0.1f) velocity.y = 0.0f;
		
		move(velocity);
	}
	
	public void reportPos() {
		if (world.getMyClient().isServer() && player.getNetID() != world.getMyClient().getMyPlayer().getNetID()) {
			super.reportPos();
		} else {
			world.getMyClient().updateMyPlayerState();
		}
	}
	
	public void setPosition(float x, float y) {
		super.setPosition(x, y);
		if (!world.getMyClient().isServer()) return;
		
		// check collision (dynamic)
		QuadTree q = world.getMapQuadTree();
		if (q != null)
			q.updatePos(this);
			//System.out.println(q.updatePos(this));
	}
	
	public boolean isMySelf() {
		return player.getNetID() == world.getMyClient().getMyPlayer().getNetID();
	}
	
	public boolean isSpectatee() {
		// returns true if i'm spectating this guy
		return player.getNetID() == world.getSpectatingCharacter().getPlayer().getNetID();
	}
	
	public void removePower(Power p) {
		Packet.SCharacterUpdatePowerup packet = new Packet.SCharacterUpdatePowerup();
		packet.target = this;
		packet.powerup = p.powerup;
		packet.tell = 2;

		// reset to default face
		packet.x = 128;
		packet.y = 0;

		powers.remove(p);
		switch (p.powerup) {
		case 0:
			protection = false;
			break;
		case 2:
			invisible = false;
			break;
		case 3:
			reviving = false;
			break;
		}

		// set the status to the latest power
		if (!powers.isEmpty()) {
			Power po = powers.get(powers.size() - 1);
			packet.x = po.regionX;
			packet.y = po.regionY;
		}

		world.getMyClient().getServer().broadcast(packet);
	}
	
	public void removeRevivePower() {
		for (Power p : new ArrayList<>(powers)) {
			if (p.powerup == (byte)3) {
				removePower(p);
				return;
			}
		}
	}
	
	@Override
	public void draw(Batch batch, float alpha) {
		if (region.getTexture() == null) return; // no
		
		boolean flipH = false;
				
		if (!isDied()) {
			float delta = Gdx.graphics.getDeltaTime(); // 3x faster

			if (world.getMyClient().isServer()) {
				for (Power p : new ArrayList<>(powers)) {
					p.timer -= delta;
					if (!p.aboutToEnd && p.timer < POWER_DURATION_END) {
						p.aboutToEnd = true;
						Packet.SCharacterUpdatePowerup packet = new Packet.SCharacterUpdatePowerup();
						packet.target = this;
						packet.powerup = p.powerup;
						packet.tell = 1;
						world.getMyClient().getServer().broadcast(packet);
					}
					if (p.timer <= 0.0f) {
						removePower(p);
					}
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

			int[] aniCoord = aniSeq[(int)animationIndex]; // 0 1 2
			flipH = aniDir[1] == 1;
			region.setRegion(
				aniCoord[0],
				aniCoord[1],
				32, 32
			);

			if (isMySelf()) {
				animationIndex += delta;
				if (animationIndex >= 3) animationIndex = 0.0f; // reset
			}
		}
		
		
		batch.setColor(Color.WHITE);
		batch.draw(region, getX() + (flipH ? 32.0f : 0.0f), getY(), flipH ? -32.0f : 32.0f, 32.0f);
		
		super.draw(batch, alpha);
		
		if (!isMySelf()) {
			// draw the username if they're others
			final String username = player.getUsername();
			
			// shaping to the center
			GlyphLayout layout = new GlyphLayout(labelFont, username);
			float fontX = getX() + (32.0f - layout.width) / 2;
			float fontY = getY() + 52.0f;
			labelFont.setColor(Color.BLACK);
			labelFont.draw(batch, username, fontX + 1, fontY - 1);
			labelFont.setColor(isDied() ? Color.RED : Color.GREEN);
			labelFont.draw(batch, username, fontX, fontY);
		}
		
		/*
		debugRenderer = new ShapeRenderer();
		if (debugRenderer != null) {
			batch.end();
			Vector2 p = new Vector2(getX(), getY());
			debugRenderer.setProjectionMatrix(getWorld().getCamera().combined);
			debugRenderer.begin(ShapeRenderer.ShapeType.Line);
			debugRenderer.setColor(Color.BLUE);
			debugRenderer.line(p, p.cpy().add(velocity.cpy().scl(16.0f)));
			debugRenderer.end();
			debugRenderer = null;
			batch.begin();
		}*/
	}
	
	public void die() {
		super.die();
		
		// clear all powers
		if (world.getMyClient().isServer()) {
			powers.clear();
			protection = false;
			invisible = false;
			reviving = false;
		}
		faster = false;
		
		region.setRegion(
			64, 128,
			32, 32
		);
		regionIcon.setRegion(224, 0, 32, 32);
	}
	
	public void revive() {
		super.revive();
		health = 100;
		regionIcon.setRegion(128, 0, 32, 32);
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
		
		d.writeFloat(getAnimationIndex());
		d.writeByte(getDirection());
		d.writeBoolean(getAnimating());
		
		super.serializeConstructor(d);
	}
	public void deserializeConstructor(DataInputStream d) throws IOException {
		if (world.isDemoReading()) {
			// ignore putting a player info.
			// because we already did it
			d.readInt();
		}
		else {
			setupPlayer(world.getMyClient().getPlayer(d.readInt()));
		}
		
		setAnimationIndex(d.readFloat());
		setDirection(d.readByte());
		setAnimating(d.readBoolean());
		
		super.deserializeConstructor(d);
	}
	
	public boolean serializeRecord(DataOutputStream d) throws IOException {
		serializeConstructor(d);
		return true;
	}
	
	public void deserializeRecord(DataInputStream d) throws IOException {
		deserializeConstructor(d);
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
		} else if (collidee instanceof Character) {
			Character collideeCharacter = (Character) collidee;
			if (reviving && collideeCharacter.isDied()) {
				// REVIVES THAT DUDE
				int dudeNetID = collideeCharacter.getPlayer().getNetID();
				world.reviveCharacter(dudeNetID, getPlayer().getNetID());
				world.addRevivingBonus(this);
				
				// remove the revive power NOW
				removeRevivePower();
				getWorld().getMyClient().getServer().sendChatToClient(dudeNetID, "You have used the power !", true);
			}
		}
	}
	
	//////////////////////
	
	// server only
	public void givePower(byte powerup) {
		Power po = new Power(powerup);
		powers.add(po);
		
		Packet.SCharacterUpdatePowerup p = new Packet.SCharacterUpdatePowerup();

		switch (powerup) {
		case 0 :
			p.x = 0;
			p.y = 224;
			protection = true;
			break;
		case 1 :
			p.x = 32;
			p.y = 224;
			break;
		case 2 :
			p.x = 64;
			p.y = 224;
			invisible = true;
			break;
		case 3 :
			p.x = 96;
			p.y = 224;
			reviving = true;
			po.timer = 60.0f; // 60 secs
			break;
		}
		
		po.regionX = p.x;
		po.regionY = p.y;
		
		p.target = this;
		p.powerup = powerup;
		p.tell = 0;
		
		world.getMyClient().getServer().broadcast(p);
	}
	
	// called from the server
	public void updatePowerup(byte powerup, char tell, int x, int y) {
		if (!isSpectatee()) return; // not a guy i follow
		
		//CoreGame.instance().getConsole().print("!P " + (int)powerup + " " + (int)tell + " " + x + " " + y);
		
		switch (tell) {
			case 0 : powerBegin(powerup); break;
			case 1 : powerAboutToEnd(powerup); break;
			case 2 : powerEnd(powerup); break;
		}
		if (x != -1)
			regionIcon.setRegion(x, y, 32, 32);
	}
	
	private void powerBegin(byte powerup) {
		String who = "You";
		if (!isMySelf())
			who = "They";
		
		// set icon
		String c = "??? begin ???";
		switch (powerup) {
			case 0 : // protecc
				c = who + " got the protection !";
				getWorld().playSound("s_protect");
				break;
			case 1 : // faster
				c = who + " move faster !";
				getWorld().playSound("s_faster");
				faster = true;
				
				break;
			case 2 : // invisible
				c = who + " make yourself invisible to them !";
				getWorld().playSound("s_invisible");
				
				break;
			case 3 : // angel
				c = who + " can revive players !";
				getWorld().playSound("s_revive");
				
				break;
		}
		world.feedChat(-1, c, true);
	}
	
	private void powerAboutToEnd(byte powerup) {
		//if (!isSpectatee()) return; // not a guy i follow
		
		String c = "??? expire ???";
		switch (powerup) {
			case 0 : // protecc
				c = "Protection is about to expire !";
				break;
			case 1 : // faster
				c = "Faster is about to expire !";
				break;
			case 2 : // invisible
				c = "Invisibility is about to expire !";
				break;
			case 3 : // angel
				c = "Reviving is about to expire !";
				break;
		}
		world.feedChat(-1, c, true);
	}
	
	private void powerEnd(byte powerup) {
		if (!isMySelf()) return; // not my self. dont care
		
		if (powerup == 1) {
			faster = false;
		}
		//world.feedChat(-1, c, true);
	}
	
	public void setHealth(int health) {
		this.health = health;
	}
	public int getHealth() {
		return health;
	}
	
	// serverside
	public void heal(int add) {
		if (add < 0 && god) return;
		
		health += add;
		if (health <= 0 && !buddha) {
			health = 0;
			world.killCharacter(getPlayer().getNetID());
			return;
		}
		
		if (health > 100) health = 100;
	}
	
	/* serverside cheats */
	public boolean toggleGod() {
		god = !god;
		return god;
	}
	public boolean toggleBuddha() {
		buddha = !buddha;
		return buddha;
	}
	public boolean toggleNoTarget() {
		notarget = !notarget;
		return notarget;
	}
}
