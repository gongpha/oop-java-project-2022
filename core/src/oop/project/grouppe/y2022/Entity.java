package oop.project.grouppe.y2022;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import java.io.DataOutputStream;

public abstract class Entity extends Actor {
	private World world = null;
	private Server server = null;
	private Player player = null;
	
	public void setWorld(World w) {
		this.world = w;
	}
	public World getWorld() {
		return world;
	}
	public Player getPlayer() {
		return player;
	}
	
	public void setupPlayer(Player player) {
		this.player = player;
	}
	
	public void move(Vector2 rel) {
		setX(getX() + rel.x);
		setY(getY() + rel.y);
		
		
	}
	
	////////
	
	//public void sendPacket(Packet packet) {
	//	stream.writeInt(getX());
	//}
	
	public abstract void process(float delta);
}
