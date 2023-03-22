
import java.awt.Point;

public class ItemSlot extends Sprite {
	private Game game = null;
	private Item item;
	public ItemSlot(Game game) {
		this.game = game;
	}
	
	public boolean setItem(Item item) {
		if (this.item != null) return false;
		this.item = item;
		parent.addObject(item);
		Point p = getMySize();
		Point a = getActualPosition();
		item.setPosition((int)(p.x * 0.5) + a.x, (int)(p.y * 0.5) + a.y);
		return true;
	}
	
	public boolean removeItem() {
		if (item == null) return false;
		parent.removeObject(item);
		item = null;
		return true;
	}
	
	public Item getItem() {
		return item;
	}
	
	public void onPressed() {
		// TODO
	}
}
