
package oop.project.grouppe.y2022;

import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;

// an implementation of pr quadtree for this project
// used for collision detection for dynamic objects like entities
// not included in dsa curriculum. go read by yourself lmao

public class QuadTree {
	public final int MAX_OBJECTS = 8;
	public final int MAX_RECURSION = 4;
	private Node root;
	public Node getRoot() { return root; }
	
	public class Node {
		float X, Y, W, H;
		Rectangle rect;
		int lvl;
		int quadrant; // 0 : leaf, 1|2|3|4 : quadrant
		Node[] nodes;
		Node parent = null;
		ArrayList<Entity> entities;
		
		public Node(int lvl, float X, float Y, float W, float H, int quadrant) {
			this.lvl = lvl;
			this.X = X;
			this.Y = Y;
			this.W = W;
			this.H = H;
			this.rect = new Rectangle(X, Y, W, H);
			nodes = new Node[4];
			entities = new ArrayList<>();
			this.quadrant = quadrant;
		}
		
		public Node(int lvl, float X, float Y, float W, float H, int quadrant, Node parent) {
			this(lvl, X, Y, W, H, quadrant);
			this.parent = parent;
		}
		
		public void split() {
			float hW = W * 0.5f;
			float hH = H * 0.5f;
			int l = lvl + 1;
			nodes[0] = new Node(l, X + hW, Y     , hW, hH, 1, this);
			nodes[1] = new Node(l, X     , Y     , hW, hH, 2, this);
			nodes[2] = new Node(l, X     , Y + hH, hW, hH, 3, this);
			nodes[3] = new Node(l, X + hW, Y + hH, hW, hH, 4, this);
		}
		
		public Node insert(Entity ent) {
			int quad = getQuadrant(ent);
			
			if (nodes[0] != null && quad != 0) {
				return nodes[quad - 1].insert(ent);
			}
			
			entities.add(ent);
			ent.setCurrentNode(this);
			
			if (lvl < MAX_RECURSION && entities.size() > MAX_OBJECTS) {
				// oh no
				return expand(ent);
			}
			return this;
		}
		
		public Node findChildren(Entity ent) {
			int quad = getQuadrant(ent);
			
			if (quad != 0) return nodes[quad - 1];
			return null;
		}
		
		public int getQuadrant(Entity ent) {
			float cW = X + W * 0.5f;
			float cH = Y + H * 0.5f;
			Rectangle entR = ent.getRectInTile();
			
			int quad = 0;
			boolean left = entR.x + entR.width < cW;
			boolean right = entR.x > cW;
			boolean top = entR.y + entR.height < cH;
			boolean bottom = entR.y > cH;
			if (left && top) quad = 2;
			if (right && top) quad = 1;
			if (right && bottom) quad = 4;
			if (left && bottom) quad = 3;
			return quad;
		}
		
		public Node expand(Entity newEnt) {
			split();
			
			Node inNode = null;
			
			// move all entities to children
			int i = 0;
			while (i < entities.size()) {
				Entity ent = entities.get(i);
				int quad = getQuadrant(ent);

				if (quad != 0) {
					Node node = nodes[quad - 1].insert(
						entities.remove(i)
					);
					if (newEnt == ent) inNode = node;
				}
				else {
					i++;
					if (newEnt == ent) inNode = this;
				};
			}
			return inNode;
		}
		
		/*
			( 3 4 ) -> ( 2 1 )
			( 2 1 ) -> ( 3 4 )
		*/
		private int vflipquad(int quad) {
			if (quad == 0) return 0;
			return Math.abs(quad - 4) + 1;
		}
		
		public String toString() {
			String l = "";
			Node p = this;
			while (p != null) {
				l += vflipquad(p.quadrant) + " ";
				p = p.parent;
			}
			return "( " + X + " " + Y + " " + W + " " + H + " ) { " + entities.size() + " } < " + l + ">";
		}
	}
	
	public QuadTree(int sizeX, int sizeY) {
		root = new Node(0, 0, 0, sizeX, sizeY, 0);
	}
	
	public int updatePos(Entity ent) {
		Node node = ent.getCurrentNode();
		boolean findNew = false;
		if (node == null) {
			findNew = true;
		} else {
			if (node.nodes[0] == null) {
				// this node is a leaf (no children)
				if (!node.rect.contains(ent.getRectInTile())) {
					// the entity is not inside the node rectangle !
					findNew = true;
				}
			} else {
				// check if touched any child nodes
				Node c = node.findChildren(ent);
				if (c != null) {
					node = c.insert(ent);
				}
			}
			
		}
		if (findNew) {
			// find a new node
			node = root.insert(ent);
			//System.out.println(ent.getID() + " changes the node to " + node);
		}
		
		// test all entities in the node
		// ite : test count
		return checkOverlaps(ent, node);
	}
	
	private int checkOverlaps(Entity ent, Node node) { return checkOverlaps(ent, node, true); }
	private int checkOverlaps(Entity ent, Node node, boolean recursionAll) {
		int i = 0;
		ArrayList<Entity> clone = new ArrayList<>(node.entities);
		Rectangle r = ent.getRect();
		
		for (Entity e : clone) {
			if (e == ent) continue;
			if (r.overlaps(e.getRect())) ent.collidedWith(e);
			i += 1;
		}
		
		if (recursionAll) {
			if (node.nodes[0] != null) {
				// not a leaf
				i += checkOverlaps(ent, node.nodes[0]);
				i += checkOverlaps(ent, node.nodes[1]);
				i += checkOverlaps(ent, node.nodes[2]);
				i += checkOverlaps(ent, node.nodes[3]);
			}
		}
		if ((r.x == ent.getX() || r.y == ent.getY()) && node.parent != null) {
			// check parent too
			i += checkOverlaps(ent, node.parent, false);
		}
		clone.clear(); // i hate gc
		return i;
	}
}
