
package oop.project.grouppe.y2022;

import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;

// an implementation of pr quadtree for this project
// used for collision detection for dynamic objects like entities
// not included in dsa curriculum. go read by yourself lmao

public class QuadTree {
	public final int MAX_OBJECTS = 16;
	public final int MAX_RECURSION = 3;
	private Node root;
	public Node getRoot() { return root; }
	
	public class Node {
		float X, Y, W, H;
		int lvl;
		int quadrant; // 0 : leaf, 1|2|3|4 : quadrant
		Node[] nodes;
		ArrayList<Entity> entities;
		
		public Node(int lvl, float X, float Y, float W, float H) {
			this.lvl = lvl;
			this.X = X;
			this.Y = Y;
			this.W = W;
			this.H = H;
			nodes = new Node[4];
			entities = new ArrayList<>();
			quadrant = 0;
		}
		
		public void split() {
			float hW = W * 0.5f;
			float hH = H * 0.5f;
			int l = lvl + 1;
			nodes[0] = new Node(l, X + hW, Y, hW, hH);
			nodes[1] = new Node(l, X, Y, hW, hH);
			nodes[2] = new Node(l, X, Y + hH, hW, hH);
			nodes[3] = new Node(l, X + hW, Y + hH, hW, hH);
		}
		
		public Node insert(Entity ent) {
			int quad = getQuadrant(ent);
			
			if (nodes[0] != null && quad != 0) {
				return nodes[quad - 1].insert(ent);
			}
			
			entities.add(ent);
			
			if (lvl < MAX_RECURSION && entities.size() > MAX_OBJECTS) {
				// oh no
				return expand(ent);
			}
			return this;
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
			if (quadrant == 0) split();
			
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
					ent.setCurrentNode(node);
					if (newEnt == ent) inNode = node;
				}
				else {
					i++;
					if (newEnt == ent) inNode = this;
				};
			}
			return inNode;
		}
	}
	
	public QuadTree(int sizeX, int sizeY) {
		root = new Node(0, 0, 0, sizeX, sizeY);
	}
	
	public void updatePos(Entity ent) {
		Node node = ent.getCurrentNode();
		
		if (node == null || !new Rectangle(node.X, node.Y, node.W, node.H).contains(ent.getRectInTile())) {
			// find a new node
			node = root.insert(ent);
			//System.out.println(ent.getID() + " changes the node to " + node);
		}
		
		ent.setCurrentNode(node);
		
		// test all entities in the node
		Rectangle r = ent.getRect();
		//System.out.println(node.entities.size());
		
		ArrayList<Entity> clone = new ArrayList<>(node.entities);
		
		for (Entity e : clone) {
			if (e == ent) continue;
			if (r.overlaps(e.getRect())) ent.collidedWith(e);
		}
		
	}
}
