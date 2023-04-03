package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Paper extends Item {
	public final static int healPoint = 10;

	public TextureRegion getTexture() {
		return new TextureRegion(
			(Texture) ResourceManager.instance().get("items")
		, 0, 0, 32, 32);
	}

	public boolean playerObtained(Character ch) {
		// tell this guy (client) that you collect the paper
		// then tell everyone
		getWorld().getMyClient().getServer().sendChatToClient(ch.getPlayer().getNetID(),
			"You got the paper!"
		, true);
		getWorld().getMyClient().getServer().sendChat(-1,
			ch.getPlayer().getUsername() + " collects a paper. (0/0)"
		, true);

		return true;
		//return false;
	}
}
