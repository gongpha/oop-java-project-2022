public class ActionGoTo extends Action {
	private Room targetRoom;
	public ActionGoTo(int posX, int posY, int sizeX, int sizeY, Room room) {
		super(posX, posY, sizeX, sizeY);
		targetRoom = room;
	}
	public ActionGoTo(int posX, int posY, int sizeX, int sizeY, Room room, Item required) {
		super(posX, posY, sizeX, sizeY, required);
		targetRoom = room;
	}
	public void actionPressed() {
		Item r = getRequiredItem();
		if (r == null)
			getGame().goToRoom(targetRoom);
		else
			getGame().goToRoom(targetRoom, r);
	}
}
