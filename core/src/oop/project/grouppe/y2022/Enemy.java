
package oop.project.grouppe.y2022;

// it can hurt you

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.HashMap;
import java.util.Random;


public abstract class Enemy extends Entity {
	private AStar pathfinding;
	private float findDelay = 0.033f;
	private Random random;
	private Vector2 velocity;
	
	private TextureRegion region;
	private AStar.Point walkTo = null;
	private Character walkingTo = null;
	private Vector2 lastCheck;
	
	private Music music;
	
	public Enemy() {
		random = new Random();
		region = getTexture();
		velocity = new Vector2();
		
		music = getMusic();
		if (music != null) {
			music.setLooping(true);
			ResourceManager.instance().playMusic(music);
		}
	}
	
	public void updateCameraPos(float camX, float camY) {
		//float X = camX - getX();
		float dst = new Vector2(camX, camY).dst(getX(), getY());
		dst = Utils.clamp(dst / 1200.0f, 0.0f, 1.0f);
		//System.out.println(dst);
		music.setVolume((1.0f - dst) * CoreGame.instance().getVolumef());
	}
	
	public void setupAStar() {
		pathfinding = new AStar(getWorld().getColMapTiles());
	}
	
	public int getDamage() {
		return (int)(20.0f * (random.nextFloat() * 0.5f));
	}
	
	public Rectangle getRect() {
		return new Rectangle(getX(), getY(), 32, 32);
	}
	public Rectangle getRectInTile() {
		return new Rectangle(getX() / 32.0f, getY() / 32.0f, 1, 1);
	}
	
	public void collidedWith(Entity collidee) {}
	
	public void draw(Batch batch, float alpha) {
		if (region.getTexture() == null) return; // no
		
		//batch.setColor(Color.WHITE);
		batch.draw(region, getX(), getY(), 64.0f, 64.0f);
		
		if (getWorld().isDrawPathEnabled()) {
			batch.end();
			ShapeRenderer s = new ShapeRenderer();
			s.setProjectionMatrix(getWorld().getCamera().combined);
			s.begin(ShapeRenderer.ShapeType.Line);
			s.setColor(Color.GREEN);
			drawNode(s, walkTo, 0);
			s.end();
			batch.begin();
		}
		
		super.draw(batch, alpha);
	}
	
	public void drawNode(ShapeRenderer s, AStar.Point node, int i) {
		if (i > 1000) return;
		if (node != null) {
			s.circle(node.x * 32.0f, node.y * 32.0f, 3.0f);
			if (node.prev != null) {
				s.setColor(Color.RED);
				s.line(node.x * 32.0f, node.y * 32.0f, node.prev.x * 32.0f, node.prev.y * 32.0f);
				drawNode(s, node.prev, i + 1);
			}
			
		}
	}
	
	//public void collide(Vector2 rel) {}

	public void process(float delta) {
		if (walkTo != null && walkingTo != null) {
			
			if (!walkingTo.isInvisible()) {
				
				Vector2 myPos = new Vector2(getX(), getY());
				Vector2 walkPos = new Vector2(walkTo.x, walkTo.y).scl(32.0f);
				if (myPos.dst(walkPos) < 48.0f) {
					walkTo = walkTo.prev;
				}
				Vector2 wishdir = walkPos.sub(myPos).nor();

				//if (walkingTo.hasProtection()) wishdir.scl(-1.0f);
				//System.out.println("wish : " + wishdir.x + " " + wishdir.y);

				// accel
				velocity = velocity.lerp(wishdir.scl(500.0f * delta), delta * 20.0f);
				if (Math.abs(velocity.x) < 0.01f) velocity.x = 0.0f;
				if (Math.abs(velocity.y) < 0.01f) velocity.y = 0.0f;

				move(velocity);
				//System.out.println("vel : " + velocity.x + " " + velocity.y);

				// is it nearby the target (check by distance)
				if (new Vector2(getX() + 32, getY() + 32).dst(new Vector2(walkingTo.getX() + 16, walkingTo.getY() + 16)) <= 64.0) {
					// KILL
					if (!walkingTo.hasProtection()) {
						//getWorld().killCharacter(walkingTo.getPlayer().getNetID());
						//walkingTo = null;
						ResourceManager.instance().playSound("s_hit");
					}
				}
			} else {
				walkingTo = null;
			}
		}
		if (findDelay > 0.125f) {
			// looking for characters (player entities)
			
			Character nearest = null;
			float nearestDist = 555555.0f;
			Vector2 myPos = new Vector2(getX(), getY());
			for (HashMap.Entry<Integer, Integer> e : getWorld().dumpCharactersEntID().entrySet()) {
				Character c = (Character) getWorld().getEntities(e.getValue());
				
				if (c.isDied() || c.isInvisible()) continue;

				// check distance
				Vector2 p = new Vector2(c.getX(), c.getY());
				float distance = p.dst(myPos);
				if (distance < nearestDist) {
					nearest = c;
					nearestDist = distance;
				}
			}
			
			if (nearest != null && pathfinding != null) {
				if (walkingTo == nearest) {
					if (lastCheck != null) {
						if (lastCheck.dst(nearest.getX(), nearest.getY()) > 16.0f) {
							walkTo = null; // to far
						}
					}
				} else {
					walkTo = null;
				}
				if (walkTo == null) {
					walkTo = pathfinding.getPath(
						(int)(nearest.getX() / 32.0f),
						(int)(nearest.getY() / 32.0f),
						(int)(getX() / 32.0f),
						(int)(getY() / 32.0f)
					);
					walkingTo = nearest;
				}
				lastCheck = new Vector2(nearest.getX(), nearest.getY());
			}
			
			findDelay = 0.0f;
		} else {
			findDelay += delta;
		}
	}
	
	///////////////////////
	
	public abstract TextureRegion getTexture();
	public abstract Music getMusic();
}
