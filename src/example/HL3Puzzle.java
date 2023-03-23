public class HL3Puzzle {
	public static Room ROOM(String n) {
		return new Room("assets/example/" + n + ".jpg");
	}
	public static Room ROOM(String n1, String n2) {
		return new Room(new String[]{
			"assets/example/" + n1 + ".jpg",
			"assets/example/" + n2 + ".jpg"
		});
	}
	public static Item ITEM(String n) {
		return new Item("assets/example/" + n + ".jpg");
	}
	/////////////////////////////////////
	
	public static void preparePuzzle(Game game) {
		Room room2 = ROOM("room2");
		
		// the room that contains the SILENHEAD
		Room room7 = ROOM("room7", "room15");
		
		Item silenhead = ITEM("silenhead");
		
		Room room = ROOM("room1")
			.leftRoom(room2)
			.rightRoom(ROOM("room3")
				.createGoTo(187, 178, 482, 327, (
					ROOM("room4")
						.leftRoom(ROOM("room5")
							.createGoTo(424, 287, 533, 334, ROOM("room10")
									.createGoTo(424, 298, 489, 365, ROOM("room9")
										.leftRoom(ROOM("room12")
											.createGoTo(500, 200, 580, 320, ROOM("room11")
												.leftRoom(ROOM("room14")
													.createGoTo(489, 316, 520, 434, ROOM("room13")
														.createGoTo(569, 291, 598, 406, ROOM("room16")
															.createGotoRequired(152, 9, 456, 295, ROOM("borealis")
																.putMusic("*assets/example/end.mp3")
															, silenhead)
															.putMusic("*assets/example/hazard.mp3")
														)
													)
												)
												.rightRoom(room2)
											)
										)
									)
								)
							.leftRoom(ROOM("room6")
								.createGoTo(12, 477, 81, 559, ROOM("room8")
									.createGoTo(438, 229, 581, 303, room7
										.putItem(316, 213, 498, 365, silenhead, 1)
									)
								)
							)
						)
				))
			)
			;
		game.goToRoom(room);
	}
}
