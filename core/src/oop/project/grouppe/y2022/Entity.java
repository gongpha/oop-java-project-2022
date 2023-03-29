package oop.project.grouppe.y2022;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// All entities (except characters) must be controlled by the server

public abstract class Entity extends Actor {
	private World world = null;
	private int ID;
	//private Server server = null;
	
	private int health = 100;
	
	public void setID(int ID) {
		this.ID = ID;
	}
	public int getID() {
		return ID;
	}
	
	public void setHealth(int health) {
		this.health = health;
	}
	public int getHealth() {
		return health;
	}
	
	public void setWorld(World w) {
		this.world = w;
	}
	public World getWorld() {
		return world;
	}
	
	// called by server or client (their charactere)
	public void move(Vector2 rel) {
		if (rel.isZero()) return; // no moving. who cares
		collide(rel);
		if (rel.isZero()) return; // collided ? nah
		setX(getX() + rel.x);
		setY(getY() + rel.y);
		
		reportPos();
	}
	
	public void reportPos() {
		if (world.getMyClient().isServer()) // if server
			world.getMyClient().updateEntPos(this);
	}
	
	public void teleport(float X, float Y) {
		setX(X);
		setY(Y);
		
		if (world.getMyClient().isServer()) // if server
			world.getMyClient().updateEntPos(this);
	}
	
	/*
		(gongpha)
		this performs a fast collision check
		by lookup the tile that was blocking us at our position directly
		from the 2D array.
	
		but also gives us a trash result :/
	
		Should I improve it ? ( )
	*/
	public void collide(Vector2 rel) {
		int XX = (int)(getX() + rel.x);
		int YY = (int)(getY() + rel.y);
		TiledMap worldMap = world.getWorldMap();
		if (worldMap == null) return; // no map ?
		TiledMapTileLayer l = (TiledMapTileLayer) worldMap.getLayers().get("tiles");
		if (l == null) return; // not in the generated tiles
			
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
					//rel = rel.sub(n.scl(rel.dot(n))); <- SLIDE FUNCTION. BUT UNUSED
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
	
	public abstract void process(float delta, boolean prediction);
}
