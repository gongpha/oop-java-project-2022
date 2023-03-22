
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

// a clickable rectangle for doing actions

public abstract class Action extends DrawObject {
	private Point size;
	private Game game = null;
	private Item required = null;
	public Action(int posX, int posY, int sizeX, int sizeY) {
		setPosition(posX, posY);
		size = new Point(sizeX, sizeY);
	}
	public Action(int posX, int posY, int sizeX, int sizeY, Item required) {
		setPosition(posX, posY);
		size = new Point(sizeX, sizeY);
		this.required = required;
	}
	
	public Point getMySize() {
		return size;
	}
	
	public void draw(Graphics2D g) {
		Point p = getActualPosition();
		g.setColor(Color.red);
		g.setStroke(new BasicStroke(2));
		g.drawRect(p.x, p.y, size.x, size.y);
	}
	
	public void onPressed() {
		actionPressed();
	}
	
	public void setGame(Game game) {
		this.game = game;
	}
	
	protected Game getGame() {
		return game;
	}
	
	public Item getRequiredItem() {
		return required;
	}
	
	//////////////////////////
	
	public abstract void actionPressed();
}
