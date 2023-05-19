package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// it protecc

public class Protection extends Item {
	public TextureRegion getTexture() {
		return new TextureRegion(
			(Texture) ResourceManager.instance().get("items")
		, 32, 0, 32, 32);
	}

	public boolean playerObtained(Character ch) {
		// give an effect
		if (ch.hasProtection()) return false;
		ch.givePower((byte)0);

		return true;
		//return false;
	}
}
