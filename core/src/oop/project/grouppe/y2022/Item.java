
package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import java.io.DataInputStream;
import java.io.DataOutputStream;

// collectible entity
// all logics are in the serverside

public abstract class Item extends Entity {
	private TextureRegion texture;
	
	public Item() {
		texture = getTexture();
	}
	
	public void draw(Batch batch, float alpha) {
		if (texture != null)
			batch.draw(texture, getX(), getY(), 32.0f, 32.0f);
		super.draw(batch, alpha);
	}
	
	public Rectangle getRect() {
		return new Rectangle(getX(), getY(), 32, 32);
	}
	public Rectangle getRectInTile() {
		return new Rectangle(getX() / 32.0f, getY() / 32.0f, 1, 1);
	}
	
	public void collidedWith(Entity entity) {}

	public void process(float delta) {}
	
	public abstract TextureRegion getTexture();
	public abstract boolean playerObtained(Character ch);
}
