// this is a template class.
// copy this file and do what ever you want

import java.awt.*;
import java.awt.event.KeyEvent;

public class BorealisWowPaper extends Paper {
	private char pressed = 0;
	
	public void draw(Graphics g) {
		// draws an image from specified path
		drawTexture(g, "assets/borealis.jpg", 0, 0);
		
		// set font and color for next drawing
		g.setFont(new Font("Cooper Black", Font.PLAIN, 32));
		g.setColor(Color.red);
		drawText(g, "THE Borealis WOW !!!1!1", 60, 60);
		
		// show the key which you pressed recently
		if (pressed == 0) {
			drawText(g, "press any button", 180, 450);
		} else {
			drawText(g, "wow you pressed " + pressed, 180, 450);
		}
		
		// !!! DONT FORGET TO PUT "g" VARIABLE IN ALL DRAWING CALLS !!!
	}
	
	public void keyPressed(KeyEvent e) {
		// observes the key you have pressed
		
		// aha you pressed the key
		pressed = e.getKeyChar();
		
		// draw again
		redraw();
	}
}
