package oop.project.grouppe.y2022;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
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
		collide(rel);
		if (rel.isZero()) return; // collided ? nah
		setX(getX() + rel.x);
		setY(getY() + rel.y);
		
		if (world.getMyClient().isPuppet()) // if server
			world.getMyClient().updateEntPos(this, true);
	}
	
	public void teleport(float X, float Y) {
		setX(X);
		setY(Y);
		
		if (world.getMyClient().isPuppet()) // if server
			world.getMyClient().updateEntPos(this, false); // no predicting
	}
	
	public void collide(Vector2 rel) {
		int XX = (int)(getX() + rel.x);
		int YY = (int)(getY() + rel.y);
		TiledMapTileLayer l = (TiledMapTileLayer) world.getWorldMap().getLayers().get("tiles");
			
		int[][] cs = new int[][] {
			{0, 0},
			{1, 0},
			{0, 1},
			{1, 1},
		};
		for (int[] c : cs) {
			int X = (int)XX / l.getTileWidth() + c[0];
			int Y = (int)YY / l.getTileHeight() + c[1];
			Cell cell = l.getCell(X, Y);
			if (cell != null) {
				if (cell.getTile().getProperties().containsKey("col")) {
					//System.out.println(c[0] + " " + c[1]);
					//Vector2 n = new Vector2(0, 0);
					///if (c[0] == 0) {
					///	rel.x = 0.0f;
					//}

					//rel.mul(-1.0f).
					//rel = rel.sub(n.scl(rel.dot(n)));
					rel.x = 0.0f;
					rel.y = 0.0f;
					break;
				}
			}
		}
	}
	
	////////
	
	//public void sendPacket(Packet packet) {
	//	stream.writeInt(getX());
	//}
	
	public abstract void serializeConstructor(DataOutputStream d) throws IOException;
	public abstract void deserializeConstructor(DataInputStream d) throws IOException;
	
	public abstract void process(float delta);
}
