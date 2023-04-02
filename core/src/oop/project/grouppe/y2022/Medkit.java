package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Medkit extends Item {
	public final static int healPoint = 30;

	public TextureRegion getTexture() {
		return new TextureRegion(
			(Texture) ResourceManager.instance().get("items")
		, 0, 0, 32, 32);
	}

	public boolean playerObtained(Character ch) {
		if (ch.getHealth() < 100) {
			ch.heal(healPoint);
			
			if (!ch.isMySelf()) {
				// tell this guy (client) that you get healed
				getWorld().getMyClient().getServer().sendChatToClient(ch.getPlayer().getNetID(),
					"You received " + healPoint + " HP !"
				, true);
			}
			return true;
		}
		return false;
	}
}
