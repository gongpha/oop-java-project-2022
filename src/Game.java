// our game
// ( ... SOVIET ANTHEM INTENSIFIES ... )

import java.awt.Dimension;
import javax.swing.*;

public class Game {
	private final JFrame mainWindow;
	private final Paper gameRoom;
	
	private boolean showingItems;
	private ItemToggleButton itemsButton;
	
	private ItemSlot[] items;
	
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
		} else {
			// hide
			itemsButton.setPosition(600, 600);
			itemsButton.setImage("assets/items_up.png");
			for (int i = 0; i < 4; i++) {
				items[i].setVisible(false);
			}
		}
		gameRoom.forceRedraw();
	}
}
