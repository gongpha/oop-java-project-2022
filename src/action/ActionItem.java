public class ActionItem extends Action {
	private Item item;
	private Room on;
	private int changeTo;
	public ActionItem(int posX, int posY, int sizeX, int sizeY, Item item, Room onRoom, int changeTo) {
		super(posX, posY, sizeX, sizeY);
		this.item = item;
		on = onRoom;
		this.changeTo = changeTo;
	}
	public void actionPressed() {
		if (getGame().addItem(item)) {
			setVisible(false);
			on.changeToIndex(changeTo);
			parent.forceRedraw();
		}
	}
}
