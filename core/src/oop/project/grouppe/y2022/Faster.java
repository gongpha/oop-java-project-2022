package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// move fast

public class Faster extends Item {
	public TextureRegion getTexture() {
		return new TextureRegion(
			(Texture) ResourceManager.instance().get("items")
		, 64, 0, 32, 32);
	}

	public boolean playerObtained(Character ch) {
		if (ch.hasFaster()) return false;
		ch.givePower((char)1);
		return true;
	}
}
