package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Paper extends Item {
	public TextureRegion getTexture() {
		return new TextureRegion(
			(Texture) ResourceManager.instance().get("items")
		, 0, 0, 32, 32);
	}

	public boolean playerObtained(Character ch) {
		// tell this guy (client) that you collect the paper
		// then tell everyone
		World w = getWorld();
		w.addCollectedPaperCount(ch);

		return true;
		//return false;
	}
}
