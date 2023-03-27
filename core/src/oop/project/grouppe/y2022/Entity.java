package oop.project.grouppe.y2022;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Entity extends Actor {
	private World world = null;
	private int ID;
	private Server server = null;
	
	public void setID(int ID) {
		this.ID = ID;
	}
	public int getID() {
		return ID;
	}
	
	public void setWorld(World w) {
		this.world = w;
	}
	public World getWorld() {
		return world;
	}
	
	// called by server or client (predict)
	public void move(Vector2 rel) {
		if (rel.isZero()) return; // no moving. who cares
		setX(getX() + rel.x);
		setY(getY() + rel.y);
		
		if (world.getMyClient().isPuppet()) // if server
			world.getMyClient().updateEntPos(this, true);
	}
	
	////////
	
	//public void sendPacket(Packet packet) {
	//	stream.writeInt(getX());
	//}
	
	public abstract void serializeConstructor(DataOutputStream d) throws IOException;
	public abstract void deserializeConstructor(DataInputStream d) throws IOException;
	
	public abstract void process(float delta);
}
