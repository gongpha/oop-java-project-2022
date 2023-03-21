public class HL3Puzzle {
	public static Room ROOM(String n) {
		return new Room("assets/example/" + n + ".jpg");
	}
	/////////////////////////////////////
	
	public static void preparePuzzle(Game game) {
		Room room = ROOM("room1")
			.leftRoom(ROOM("room2"))
			.rightRoom(ROOM("room3"))
			;
		game.goToRoom(room);
	}
}
