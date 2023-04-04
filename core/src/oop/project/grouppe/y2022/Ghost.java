package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;


public class Ghost extends Enemy {
	public TextureRegion getTexture() {
		return new TextureRegion(
			(Texture) ResourceManager.instance().get("ghost1"),
				0, 0, 64, 64
		);
	}
}
