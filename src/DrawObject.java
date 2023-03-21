
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

// DRAW THINGS

public abstract class DrawObject {
	protected Paper parent = null;
	private int posX = 0;
	private int posY = 0;
	private boolean visible = true;
	private OriginMode origin = OriginMode.TOP_LEFT;
	
	public abstract Point getMySize();
	public abstract void draw(Graphics2D g);
	
	public enum OriginMode {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT,
		CENTER
	};
	
	public DrawObject(int x, int y, OriginMode origin) {
		this(x, y);
		this.origin = origin;
	}
	
	public DrawObject(int x, int y) {
		posX = x;
		posY = y;
	}
	
	public DrawObject() {
		posX = 0;
		posY = 0;
	}
	
	public void setParent(Paper paper) {
		parent = paper;
	}
	
	public void setPosition(int x, int y) {
		posX = x;
		posY = y;
		//parent.forceRedraw();
	}
	
	public int getPositionX() {
		return posX;
	}
	
	public int getPositionY() {
		return posY;
	}
	
	public Point getActualPosition() {
		int x = posX;
		int y = posY;
		Point mySize = getMySize();
		switch (origin) {
			case TOP_LEFT: break;
			case TOP_RIGHT:
				x -= mySize.x;
				break;
			case BOTTOM_LEFT:
				y -= mySize.y;
				break;
			case BOTTOM_RIGHT:
				x -= mySize.x;
				y -= mySize.y;
				break;
			case CENTER:
				x -= mySize.x * 0.5;
				y -= mySize.y * 0.5;
				break;
		}
		return new Point(x, y);
	}
	
	public Rectangle getActualArea() {
		Point point = getActualPosition();
		Point mySize = getMySize();
		return new Rectangle(point.x, point.y, point.x + mySize.x, point.y + mySize.y);
	}
	
	public void move(int x, int y) {
		posX += x;
		posY += y;
		//parent.forceRedraw();
	}
	
	public void setOrigin(OriginMode origin) {
		this.origin = origin;
		//parent.forceRedraw();
	}
	
	public OriginMode getOrigin() {
		return origin;
	}
	
	public void setVisible(boolean yes) {
		visible = yes;
		//parent.forceRedraw();
	}
	
	public boolean getVisible() {
		return visible;
	}
	
	////////////////////////
	
	public void onPressed() {}
	
	public void keyPressed(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}
}
