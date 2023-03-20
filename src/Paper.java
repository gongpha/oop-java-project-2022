// This class acts like a paper that allows you to draw anything into it
// See the example to learn how to draw

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class Paper extends JPanel implements KeyListener {	
	public void paintComponent(Graphics g) {
		draw(g);
	}
	
	public void redraw() {
		repaint();
	}
	
	public void drawTexture(Graphics g, String path, int x, int y) {
		Image image = new ImageIcon(path).getImage();
		drawTexture(g, image, x, y);
	}
	public void drawTexture(Graphics g, Image image, int x, int y) {
		g.drawImage(image, x, y, null);
	}
	
	public void drawText(Graphics g, String text, int x, int y) {
		g.drawString(text, x, y);
	}
	
	///////////////////////////////////
	
	public void keyTyped(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
	
	///////////////////////////////////
	// inherit me plz
	
	public abstract void draw(Graphics g);
}
