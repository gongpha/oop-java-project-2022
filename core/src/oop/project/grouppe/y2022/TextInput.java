package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Clipboard;

public class TextInput {
	private String lineEnter = "";
	private int limit = 0;
	
	public String getString() {
		return lineEnter;
	}
	public void setString(String s) {
		this.lineEnter = s;
	}
	public void setLimited(int limit) {
		this.limit = limit;
	}
	
	private void limitString() {
		if (limit > 0) {
			lineEnter = lineEnter.substring(0, Math.min(limit, lineEnter.length()));
		}
	}
	
	public boolean keyTyped (char c) {
		if (c == '\b' && !lineEnter.isEmpty()) {
			// backspace
			lineEnter = lineEnter.substring(0, lineEnter.length() - 1);
			limitString();
			return true;
		}
		if (c >= 0x20) {
			lineEnter += c;
			limitString();
			return true;
		}
		return false;
	}
	
	public boolean keyDown (int i) {
		if (CoreGame.instance().isCtrlPressed() && i == Input.Keys.V) {
			Clipboard c = Gdx.app.getClipboard();
			if (c.hasContents()) {
				lineEnter += c.getContents();
				limitString();
			}
			return true;
		}
		return false;
	}
}
