
package oop.project.grouppe.y2022;

// it can hurt you

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.HashMap;
import java.util.Random;


public abstract class Enemy extends Entity {
	
	private float findDelay = 0.0f;
	private Random random;
	
	public void Enemy() {
		random = new Random();
		
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
	
	public void collidedWith(Entity collidee) {
		if (collidee instanceof Character) {
			( (Character) collidee).hurt(getDamage());
		}
	}

	public void process(float delta) {
		if (findDelay > 0.5) {
			// looking for characters (player entities)
			HashMap<Integer, Integer> cls = getWorld().dumpCharactersEntID();
			for (int i = 0; i < cls.size(); i++) {
				Character c = (Character) getWorld().getEntities(cls.get(i));

				// check distance
				Vector2 p = new Vector2(c.getX(), c.getY());
				float distance = p.dst(new Vector2(getX(), getY()));
				
			}
		}
	}
}
