public class Room extends Sprite {
	private Room[] rooms;
	public Room(String path) {
		setImage(path);
		
		// left, right, back
		rooms = new Room[3];
	}
	
	public Room leftRoom(Room room) {
		setRoom(0, room);
		room.setRoom(1, this);
		return this;
	}
	public Room rightRoom(Room room) {
		setRoom(1, room);
		room.setRoom(0, this);
		return this;
	}
	public void setRoom(int index, Room room) {
		rooms[index] = room;
	}
	public Room backRoom(Room room) {
		rooms[2] = room;
		return this;
	}
	
	////////////////////////////////////////////////////////
	
	// 0 : left
	// 1 : right
	// 2 : back
	public Room getRoom(int what) {
		return rooms[what];
	}
}
