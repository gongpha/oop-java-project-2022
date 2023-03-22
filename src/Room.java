
import java.util.ArrayList;
import java.util.Arrays;

public class Room extends Sprite {
	private Room[] rooms;
	private ArrayList<Action> actions;
	private ArrayList<String> paths;
	
	public Room(String path) {
		setImage(path);
		paths = new ArrayList<String>();
		paths.add(path);
		
		// left, right, back
		rooms = new Room[3];
		actions = new ArrayList<Action>();
	}
	public Room(ArrayList<String> paths) {
		this(paths.get(0));
		
		this.paths = paths;
	}
	public Room(String[] pathArray) {
		this(new ArrayList<String>(Arrays.asList(pathArray)));
	}
	
	////////////////////////////
	// ez builder (tm)
	
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
	public Room createGoTo(int x1, int y1, int x2, int y2, Room targetRoom) {
		actions.add(new ActionGoTo(x1, y1, x2 - x1, y2 - y1, targetRoom));
		targetRoom.setRoom(2, this);
		return this;
	}
	public Room createGotoRequired(int x1, int y1, int x2, int y2, Room targetRoom, Item item) {
		actions.add(new ActionGoTo(x1, y1, x2 - x1, y2 - y1, targetRoom, item));
		targetRoom.setRoom(2, this);
		return this;
	}
	public Room putItem(int x1, int y1, int x2, int y2, Item item, int afterObtained) {
		actions.add(new ActionItem(x1, y1, x2 - x1, y2 - y1, item, this, afterObtained));
		return this;
	}
	
	////////////////////////////////////////////////////////
	
	// 0 : left
	// 1 : right
	// 2 : back
	public Room getRoom(int what) {
		return rooms[what];
	}
	
	public ArrayList<Action> getActions() {
		return actions;
	}
	
	public void changeToIndex(int idx) {
		setImage(paths.get(idx));
	}
}
