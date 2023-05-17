package oop.project.grouppe.y2022;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// An object that replicates its properties over the network
// All entities (except characters) must be controlled by the server

public abstract class Entity extends Actor {
	private World world = null;
	private int ID;
	
	private boolean deleted = false; // deleted. no need to process it further
	public boolean isDeleted() {
		return deleted;
	}
	
	private boolean died = false;
	
	private final Vector2 lastNor = new Vector2();
	
	protected ShapeRenderer debugRenderer = null;
	
	private QuadTree.Node node;
	public QuadTree.Node getCurrentNode() { return node; }
	public void setCurrentNode(QuadTree.Node node) {
		if (this.node != null) {
			this.node.entities.remove(this);
		}
		this.node = node;
	}
	
	private boolean noclip = false;
	public boolean toggleNoclip() {
		noclip = !noclip;
		return noclip;
	}
	
	public void setID(int ID) {
		this.ID = ID;
	}
	public int getID() {
		return ID;
	}
	
	// CALL world.killCharacter for killing the character remotely
	public void die() {
		if (died) return;
		died = true;
	}
	
	public boolean isDied() {
		return died;
	}
	
	// CALL world.reviveCharacter for reviving the character remotely
	public void revive() {
		if (!died) return;
		died = false;
	}
	
	public void setWorld(World w) {
		this.world = w;
	}
	public World getWorld() {
		return world;
	}
	
	// called by server or client (their characters)
	public void move(Vector2 rel) {
		if (rel.isZero()) return; // no moving. who cares
		collide(rel);
		if (rel.isZero()) return; // collided ? nah
		
		//System.out.println(rel);
		setPosition(
			getX() + rel.x, getY() + rel.y
		);
		
		reportPos();
		afterPosChange();
	}
	
	protected void reportPos() {
		if (world.getMyClient().isServer()) { // if server
			world.getMyClient().updateEntPos(this);
		}
	}
	
	private void afterPosChange() {
		// report to the world
		world.tellPosChange(this);
	}
	
	public void teleport(float X, float Y) {
		setX(X);
		setY(Y);
		
		reportPos();
	}
	
	/*
		(gongpha)
		this performs a fast collision check
		by lookup the tile that was blocking us at our position directly
		from the 2D array.
	
		the sliding function is a huge pain in my ass. don't give a shit
	
		i almost die because of this. help
	*/
	
	private void collide(Vector2 rel) {
		if (noclip) return;
		
		float XX = getX() + rel.x;
		float YY = getY() + rel.y;
		byte[][] colTiles = world.getColMapTiles();
		if (colTiles == null) return; // no map ?
			
		int[][] cs = new int[][] {
			{0, 0},
			{1, 0},
			{0, 1},
			{1, 1},
		};
		
		int MX = (int)XX / 32;
		int MY = (int)YY / 32;
		//System.out.println(MX + " " + MY);
		
		boolean hit = false;
		Vector2 anor = new Vector2();
		for (int[] c : cs) {
			int X = MX + c[0];
			int Y = MY + c[1];
			
			float BX = X * 32;
			float BY = Y * 32;
			if (X < 0 || Y < 0 || X >= colTiles.length || Y >= colTiles[0].length) continue;
			
			if (colTiles[X][Y] == 1) {
				// raycaast
				Rectangle r = new Rectangle(BX, BY, 32.0f, 32.0f);
				Vector2 nor = new Vector2();
				Vector2 pos = new Vector2(getX() + 16.0f, getY() + 16.0f);
				Vector2 bpos = new Vector2(BX + 16.0f, BY + 16.0f);

				boolean hitl = Physics.raycastRect(
					pos, bpos.sub(pos).nor().add(rel.cpy().nor()).nor(), r,
					nor
				);
				if (hitl) {
					hit = true;
					anor.add(nor);
					//System.out.println(hit + " " + lastNor);
				}


				/*
				Vector2 oldNor = lastNor.cpy();
				lastNor.x = 0.0f;
				lastNor.y = 0.0f;
				if (Math.abs(Math.signum(rel.x)) == Math.abs(Math.signum(rel.y))) {
					// move obliquely
					boolean MXM = X != OX;
					boolean MYM = Y != OY;
					if (MXM && !MYM) {
						lastNor.x = Math.signum(getX() - X * l.getTileWidth());
					}
					if (!MXM && MYM) {
						lastNor.y = Math.signum(getY() - Y * l.getTileHeight());
					}
					if (X == OX && OX == MX) {
						//System.out.println("@@@@@@@@@@@@@@@@@@@@");
					}
					//System.out.println(MXM + " " + MYM);
				}
				else if (rel.x != 0.0f) {
					// move h
					// nor : (-1, 0) (1, 0)
					lastNor.x = Math.signum(XX - X * l.getTileWidth());
				}
				else if (rel.y != 0.0f) {
					// move v
					// nor : (0, -1) (0, 1)
					lastNor.y = Math.signum(YY - Y * l.getTileHeight());
				}
				*/
				//System.out.println(lastNor);
				//System.out.println(rel);
				//System.out.println(X + " " + OX + " " + MX + " : " + Y + " " + OY + " " + MY);


				//System.out.println(c[0] + " " + c[1]);
				//Vector2 n = new Vector2(0, 0);
				///if (c[0] == 0) {
				///	rel.x = 0.0f;
				//}

				//lastNor.x = -1.0f * lastNor.x;
				//lastNor.y = -1.0f * lastNor.y;
				//System.out.println("@@@@@@@@@");
				//System.out.println(lastNor);
				//System.out.println(rel);

				//System.out.println(X * l.getTileWidth() - XX);
				//System.out.println(Y * l.getTileHeight() - YY);

				//rel.x = 0.0f;
				//rel.y = 0.0f;
				//System.out.println(getX() + " " + BX);
				//System.out.println((getX() - BX));
				//System.out.println(getY() + " " + BY);
				//System.out.println((getY() - BY));
				//rel.x += (getX() - XX);
				//rel.y += (getY() - YY);

				/*
				if (lastNor.isZero()) {
					//System.out.println("OOO");
					//System.out.println(oldNor);
					//lastNor.x = oldNor.x;
					//lastNor.y = oldNor.y;
					System.out.println(X + " " + OX + " " + MX + " : " + Y + " " + OY + " " + MY);
				}
				*/
				//if (lastNor.x != 0.0) rel.x = 0.0f;
				//if (lastNor.y != 0.0) rel.y = 0.0f;
				//System.out.println(lastNor);
				//rel.x = 0.0f;
				//rel.y = 0.0f;

				//System.out.println(rel);
				//rel.x = 0.0f;
				//rel.y = 0.0f;
				//break;
			}
		}
		
		//System.out.println(hitc);
		
		if (hit) {
			anor.nor();
			if (anor.x != 0.0f) {
				rel.x = 0.0f;
			}
			if (anor.y != 0.0f) {
				rel.y = 0.0f;
			}
			//System.out.println(rel);
			//rel = rel.sub(anor.cpy().scl(rel.dot(anor)));// <- SLIDE FUNCTION
			lastNor.x = anor.x;
			lastNor.y = anor.y;
		}
		//*/
	}
	
	// server only
	public void deleteMe() {
		if (node != null) {
			// delete this out of the node
			node.entities.remove(this);
			node = null;
		}
		deleted = true;
		world.destroyEntity(this);
	}
	
	public void draw(Batch batch, float alpha) {
		if (node != null && world.isDrawQuadTree()) {
			CoreGame.instance().getConsole().getFont().draw(batch, node.toString(), getX(), getY() + 64.0f);
		}
		/*
		debugRenderer = new ShapeRenderer();
		if (debugRenderer != null) {
			batch.end();
			Vector2 p = new Vector2(getX(), getY());
			debugRenderer.setProjectionMatrix(getWorld().getCamera().combined);
			debugRenderer.begin(ShapeRenderer.ShapeType.Line);
			debugRenderer.setColor(Color.RED);
			debugRenderer.line(p, p.cpy().add(lastNor.cpy().scl(128.0f)));
			
			int[][] cs = new int[][] {
				{0, 0},
				{1, 0},
				{0, 1},
				{1, 1},
			};

			float XX = getX();
			float YY = getY();
			int MX = (int)XX / 32;
			int MY = (int)YY / 32;
			//System.out.println(MX + " " + MY);

			Vector2 anor = new Vector2();
			for (int[] c : cs) {
				int X = MX + c[0];
				int Y = MY + c[1];
				debugRenderer.line(X * 32.0f, Y * 32.0f, X * 32.0f + 32, Y * 32.0f + 32);
			}
			
			debugRenderer.end();
			debugRenderer = null;
			batch.begin();
		}
		*/
	}
	
	public void updateCameraPos(float camX, float camY) {}
	
	////////
	/*
		serializeConstructor & deserializeConstructor
	
		Used for marshaling data for new objects
		e.g. when a new player has entered the server
		and needs to sync their CURRENT state (players' position at the lobby)
	*/
	
	public void serializeConstructor(DataOutputStream d) throws IOException {
		// sync pos on create
		d.writeFloat(getX());
		d.writeFloat(getY());
	}
	public void deserializeConstructor(DataInputStream d) throws IOException {
		float x = d.readFloat();
		float y = d.readFloat();
		setX(x);
		setY(y);
	}
	
	/* for recording */
	
	public boolean serializeRecord(DataOutputStream d) throws IOException {
		return false;
	}
	
	public void deserializeRecord(DataInputStream d) throws IOException {
	}
	
	public abstract Rectangle getRect();
	public abstract Rectangle getRectInTile();
	public abstract void collidedWith(Entity collidee);
	
	public abstract void process(float delta);
}
