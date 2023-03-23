// This class acts like a paper that allows you to draw anything into it

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

public class Paper extends JPanel implements KeyListener, MouseListener {
	ArrayList<DrawObject> objects;
	private boolean showInfo;
	private final Font font;
	
	private DrawObject currentFocus = null;
	
	public Paper() {
		objects = new ArrayList<DrawObject>();
		super.addMouseListener(this);
		super.addKeyListener(this);
		setFocusable(true);
		font = new Font("Courier New", Font.PLAIN, 16);
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		
		int visibleCount = 0;
		for (int i = 0; i < objects.size(); i++) {
			DrawObject o = objects.get(i);
			if (!o.getVisible()) continue;
			o.draw(g2d);
			visibleCount++;
		}
		
		if (showInfo) {
			g2d.drawString((
				"Objects : " + objects.size() + " | Visible Objects : " + visibleCount + " | Focus : " + currentFocus
			), 8, 600 - 16);
		}
		
		customDraw(g2d);
	}
	
	public void forceRedraw() {
		repaint();
	}
	
	public void toggleShowInfo() {
		showInfo = !showInfo;
		forceRedraw();
	}
	
	///////////////////////////////////
	
	public void addObject(DrawObject d) {
		objects.add(d);
		d.setParent(this);
	}
	public void removeObject(DrawObject d) {
		objects.remove(d);
		d.setParent(null);
	}
	public void addObjectAndMoveToBack(DrawObject d) {
		objects.add(0, d);
		d.setParent(this);
	}
	
	public void moveToFront(DrawObject d) {
		int i = objects.indexOf(d);
		if (i == -1) return;
		if (i == objects.size() - 1) return; // do nothing becuz it was already on the front
		objects.remove(i);
		objects.add(d);
	}
	
	///////////////////////////////////
	
	public void drawTexture(Graphics g, Image image, int x, int y) {
		g.drawImage(image, x, y, this);
	}
	public void drawTexture(Graphics g, String path, int x, int y) {
		Image image = new ImageIcon(path).getImage();
		drawTexture(g, image, x, y);
	}
	
	public void drawText(Graphics g, String text, int x, int y) {
		g.drawString(text, x, y);
	}
	
	///////////////////////////////////
	
	public void keyTyped(KeyEvent e) {
		for (int i = objects.size() - 1; i >= 0 ; i--) {
			DrawObject o = objects.get(i);
			if (!o.getVisible() && o.ignoreWhenInvisible()) continue;
			if (o.keyTyped(e))
				break;
		}
	}
    public void keyPressed(KeyEvent e) {
		for (int i = objects.size() - 1; i >= 0 ; i--) {
			DrawObject o = objects.get(i);
			if (!o.getVisible() && o.ignoreWhenInvisible()) continue;
			if (o.keyPressed(e)) break;
		}
	}
    public void keyReleased(KeyEvent e) {}
	
	public DrawObject findClick(MouseEvent e) {
		for (int i = objects.size() - 1; i >= 0 ; i--) {
			DrawObject o = objects.get(i);
			if (!o.getVisible() && o.ignoreWhenInvisible()) continue;
			Rectangle r = o.getActualArea();
			if (isMouseInArea(e, r.x, r.y, r.width, r.height)) {
				if (o.focusOnClick()) {
					if (currentFocus != null) {
						// haha you lost the focus
						currentFocus.focusChanged(false);
						
					}
					currentFocus = o;
					o.focusChanged(true);
				}
				o.onPressed();
				return o;
			}
		}
		return null;
	}
	
	public void mousePressed(MouseEvent e) {
		DrawObject found = findClick(e);
		
		if (currentFocus != null && found != currentFocus) {
			// haha you lost the focus bro
			currentFocus.focusChanged(false);
			currentFocus = null;
		}
	}
	public void mouseReleased(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	
	public void customDraw(Graphics2D g) {}
	
	///////////////////////////////////
	
	public boolean isMouseInArea(MouseEvent e, int x1, int y1, int x2, int y2) {
		Point position = e.getPoint();
		return position.x >= x1 && position.x <= x2 && position.y >= y1 && position.y <= y2;
	}
	public boolean isMouseInAreaRelative(MouseEvent e, int x, int y, int width, int height) {
		return isMouseInArea(e, x, y, x + width, y + height);
	}
	public boolean isMouseInImageArea(MouseEvent e, int x, int y, Image image) {
		return isMouseInAreaRelative(e, x, y, image.getWidth(this), image.getHeight(this));
	}
	public boolean isMouseInImageArea(MouseEvent e, int x, int y, String path) {
		Image image = new ImageIcon(path).getImage();
		return isMouseInImageArea(e, x, y, image);
	}
}
