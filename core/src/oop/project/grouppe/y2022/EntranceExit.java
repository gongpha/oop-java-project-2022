package oop.project.grouppe.y2022;

// used for exit the room when completed

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;


public class EntranceExit extends Item {

	@Override
	public TextureRegion getTexture() {
		return null;
	}

	@Override
	public boolean playerObtained(Character ch) {
		return getWorld().attemptExit(ch);
	}
	
	public Rectangle getRect() {
		return new Rectangle(getX(), getY(), 32 * 4, 32);
	}
}
