package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// heal

public class Medkit extends Item {
	public TextureRegion getTexture() {
		return new TextureRegion(
			(Texture) ResourceManager.instance().get("items")
		, 32, 32, 32, 32);
	}

	public boolean playerObtained(Character ch) {
		if (ch.getHealth() >= 100) return false; // already full
		
		// HEAL
		ch.heal(15);
		
		// tell the player
		Packet.SMedkitCollected p = new Packet.SMedkitCollected();
		p.hp = 15;
		getWorld().getMyClient().getServer().getClient(ch.getPlayer().getNetID()).send(p);
		
		// tell new health
		Packet.SEntUpdateHealth pp = new Packet.SEntUpdateHealth();
		pp.netID = ch.getPlayer().getNetID();
		pp.newHealth = ch.getHealth();
		getWorld().getMyClient().getServer().broadcast(pp);

		return true;
	}
}
