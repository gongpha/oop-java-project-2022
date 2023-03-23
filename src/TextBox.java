
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;

// a simple textbox

public class TextBox extends DrawObject {
	private Point size;
	private String text = "";
	private String label = "";
	private final Font font;
	
	private boolean caretShowing = false;
	
	public TextBox(int x, int y, int w, int h) {
		super(x, y);
		size = new Point(w, h);
		font = new Font("Courier New", Font.PLAIN, 16);
	}
	
	public TextBox(int x, int y, int w, int h, String initialText) {
		this(x, y, w, h);
		setText(initialText);
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public Point getMySize() {
		return size;
	}
	
	public void draw(Graphics2D g) {
		g.setColor(Color.black);
		Point p = getActualPosition();
		g.fillRect(p.x, p.y, size.x, size.y);
		g.setColor(Color.white);
		g.drawRect(p.x, p.y, size.x, size.y);
		g.setFont(font);
		g.drawString(text + (
			(caretShowing) ? "|" : ""
		), p.x + 8, p.y + 24);
		g.drawString(label, p.x + 4, p.y - 8);
	}
	
	public void setText(String t) {
		text = t;
	}
	
	public String getText() {
		return text;
	}
	
	public boolean focusOnClick() { return true; }
	public void focusChanged(boolean yes) {
		caretShowing = yes;
		parent.forceRedraw();
	}
	
	public boolean keyTyped(KeyEvent k) {
		char c = k.getKeyChar();
		if (!caretShowing || (c < 0x20 && c != '\b') ) {
			return false;
		}
		if (c == '\b') {
			if (text.length() >= 1) {
				text = text.substring(0, text.length() - 1);
			}
		} else {
			if (text.length() < 24)
				text += c;
		}
		parent.forceRedraw();
		return true;
	}
}
