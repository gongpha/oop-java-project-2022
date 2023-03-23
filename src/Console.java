import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

// a useless hacking tool

public class Console extends Sprite {
	private String type = "";
	private final Font font;
	private ArrayList<String> history;
	
	private class Caret extends Thread {
		public void run() {
			while (true) {
				caretShowing = !caretShowing;
				parent.forceRedraw();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// amen
				}

			}
		}
	}
	private Caret caret = null;
	
	// THREAD THINGS
	private boolean caretShowing = false;
	
	public Console() {
		setVisible(false);
		setImage("assets/console.jpg");
		font = new Font("Courier New", Font.PLAIN, 16);
		
		history = new ArrayList<String>();
	}
	
	public void draw(Graphics2D g) {
		super.draw(g);
		g.setColor(Color.white);
		g.setFont(font);
		g.drawString("] " + type + (
			(caretShowing) ? "_" : ""
		), 0, 200 - 5);
		
		g.drawString("OOP Project Grouppe Ladkrabang Console 2566", 350, 16);
		
		for (int i = 0; i < history.size(); i++) {
			g.drawString(history.get(history.size() - i - 1), 0, 200 - 5 - (16 * (i + 1)));
		}
	}
	
	public void echo(String s) {
		if (history.size() == 10) {
			history.remove(0);
		}
		history.add(s);
	}
	
	public void submit(String s) {
		if (s.equals("clear")) {
			history.clear();
		} else if (s.equals("help")) {
			echo("ASK THE DEVELOPER LMAO. (type \"developers\")");
		} else if (s.equals("developers")) {
			echo("65070013, 65070024, 65070110, 65070231, 65070245");
			echo("I was lazied in writing all the names :P");
		} else if (s.equals("exit") || s.equals("quit")) {
			System.exit(0);
		} else if (s.equals("showinfo")) {
			parent.toggleShowInfo();
		}
	}
	
	public boolean ignoreWhenInvisible() { return false; }
	
	public boolean keyPressed(KeyEvent k) {
		if (k.getKeyCode() == KeyEvent.VK_ESCAPE) {
			if (getVisible() && caret != null) {
				caret.interrupt();
			} else {
				caret = new Caret();
				caret.start();
			}
			setVisible(!getVisible());
			parent.forceRedraw();
		}
		return false;
	}
	
	public boolean keyTyped(KeyEvent k) {
		if (!getVisible()) return false;
		char c = k.getKeyChar();
		if (c == '\n') {
			echo("] " + type);
			submit(type);
			type = "";
		} else if (c == '\b' && type.length() >= 1) {
			type = type.substring(0, type.length() - 1);
		} else {
			if (!((c >= 'A' && c <= 'z') || (c >= '0' && c <= '9') || c == ' ' || c == '_')) return false;
			type += c;
		}
		parent.forceRedraw();
		return true;
	}
}
