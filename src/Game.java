// our game
// ( ... SOVIET ANTHEM INTENSIFIES ... )

import java.awt.Dimension;
import javax.swing.*;

public class Game {
	private final JFrame mainWindow;
	private final Paper gameRoom;
	
	private final Console console;
	
	private boolean showingItems;
	private ItemToggleButton itemsButton;
	
	private Room currentRoom;
	private RoomNavigateButton bLeft;
	private RoomNavigateButton bRight;
	private RoomNavigateButton bBack;
	
	private ItemSlot[] items;
	
	public enum NavigateDirection {
		GO_LEFT,
		GO_RIGHT,
		GO_BACK
	}
	
	public Game() {
		// create a new window
		mainWindow = new JFrame();
		mainWindow.setTitle("OOP PROJECT GROUPPE");
		mainWindow.getContentPane().setPreferredSize(new Dimension(800, 600));
		mainWindow.setResizable(false);
		mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		gameRoom = new Paper();
		mainWindow.add(gameRoom);
		mainWindow.pack();
		
		mainWindow.setLocationRelativeTo(null);
		mainWindow.setVisible(true);
		
		bLeft = new RoomNavigateButton(this, NavigateDirection.GO_LEFT);
		bRight = new RoomNavigateButton(this, NavigateDirection.GO_RIGHT);
		bBack = new RoomNavigateButton(this, NavigateDirection.GO_BACK);
		gameRoom.addObject(bLeft);
		gameRoom.addObject(bRight);
		gameRoom.addObject(bBack);
		bLeft.setImage("assets/room_go_left.png");
		bRight.setImage("assets/room_go_right.png");
		bBack.setImage("assets/room_go_back.png");
		bRight.setOrigin(DrawObject.OriginMode.TOP_RIGHT);
		bBack.setOrigin(DrawObject.OriginMode.BOTTOM_LEFT);
		bLeft.setPosition(0, 128);
		bRight.setPosition(800, 128);
		bBack.setPosition(200, 600);
		
		itemsButton = new ItemToggleButton(this);
		gameRoom.addObject(itemsButton);
		itemsButton.setOrigin(DrawObject.OriginMode.BOTTOM_RIGHT);
		
		items = new ItemSlot[]{
			new ItemSlot(this), new ItemSlot(this),
			new ItemSlot(this), new ItemSlot(this)
		};
		for (int i = 0; i < 4; i++) {
			ItemSlot s = items[i];
			s.setPosition(128 + (i * 128), 600 - 128);
			gameRoom.addObject(s);
			s.setImage("assets/items_slot.png");
			s.setVisible(false);
		}
		
		console = new Console();
		gameRoom.addObject(console);
		gameRoom.grabFocus();
		
		showingItems = true;
		toggleItems();
	}
	
	public void toggleItems() {
		showingItems = !showingItems;
		if (showingItems) {
			// show
			itemsButton.setPosition(600, 600 - 128);
			itemsButton.setImage("assets/items_down.png");
			for (int i = 0; i < 4; i++) {
				items[i].setVisible(true);
			}
			bBack.setPosition(200, 600 - 128);
		} else {
			// hide
			itemsButton.setPosition(600, 600);
			itemsButton.setImage("assets/items_up.png");
			for (int i = 0; i < 4; i++) {
				items[i].setVisible(false);
			}
			bBack.setPosition(200, 600);
		}
		gameRoom.forceRedraw();
	}
	
	public void goToRoom(Room room) {
		if (currentRoom != null) {
			gameRoom.removeObject(currentRoom);
		}
		currentRoom = room;
		if (currentRoom != null) {
			gameRoom.addObjectAndMoveToBack(room);
			
			bLeft.setVisible(currentRoom.getRoom(0) != null);
			bRight.setVisible(currentRoom.getRoom(1) != null);
			bBack.setVisible(currentRoom.getRoom(2) != null);
			
		}
		gameRoom.forceRedraw();
	}
	
	public void navigate(NavigateDirection direction) {
		Room going = null;
		switch (direction) {
		case GO_LEFT:
			going = currentRoom.getRoom(0);
			break;
		case GO_RIGHT:
			going = currentRoom.getRoom(1);
			break;
		case GO_BACK:
			going = currentRoom.getRoom(2);
			break;
		}
		if (going == null) return;
		goToRoom(going);
	}
	
	public void toggleConsole() {
		console.setVisible(!console.getVisible());
		gameRoom.forceRedraw();
	}
}
