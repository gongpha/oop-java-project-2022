package oop.project.grouppe.y2022;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;


public class Ghost1 extends Enemy {
	public TextureRegion getTexture() {
		return new TextureRegion(
			(Texture) ResourceManager.instance().get("ghost1"),
				0, 0, 64, 64
		);
	}
	
	public Music getMusic() {
		return (Music) ResourceManager.instance().get("m_ghost1");
	}
}
