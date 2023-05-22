package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// for hackers

public class Console {
	private CoreGame game;
	private SpriteBatch batch;
	private Pattern execPattern;
	
	private BitmapFont font;
	private Texture bg;
	private Texture sep;
	
	private double location = 1.0; // [0, 1]. 0 = fully hidden, 1 = fully showed
	private int aniMoving = 0; // 1 = moving up, 2 = moving down. otherwise, do nothing
	private double limit = 1.0; // 1.0 = full, 0.5 = half
	
	private HashMap<String, ConsoleCommand> commands;
	private ArrayList<String> history;
	private int historylook = -1;
	private String cursorStore = "";
	
	private boolean activating = true;
	
	private TextInput textInput;
	private float drawCaret = 0.0f; // [0, 2] (>= 1 : draw, < 1 : hide)
	
	class Line {
		String content = "";
		Color color = Color.WHITE;
		public Line() {}
		public Line(String c) {
			content = c;
		}
		public Line(String c, Color color) {
			content = c;
			this.color = color;
		}
	}
	private LinkedList<Line> lines;
	
	public Console() {
		game = CoreGame.instance();
		lines = new LinkedList<>();
		history = new ArrayList<>();
		textInput = new TextInput();
		sep = new Texture("core/sep.png");
		
		execPattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'", Pattern.CASE_INSENSITIVE);
		
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(new FileHandle(
			"font/cour.ttf"
		));
		FreeTypeFontParameter param = new FreeTypeFontParameter();
		param.size = 24;
		font = generator.generateFont(param);
		generator.dispose();
		
		bg = new Texture("core/console.jpg");
		
		batch = game.getBatch();
		
		// register commands
		commands = new HashMap<>();
		ConsoleCommand[] cmds = ConsoleCommandCore.regCommands();
		for (int i = 0; i < cmds.length; i++) {
			ConsoleCommand cmd = cmds[i];
			cmd.setConsole(this);
			commands.put(cmd.getCmd(), cmd);
		}
		print("Registered " + commands.size() + " commands !", Color.GREEN);
	}
	
	public BitmapFont getFont() {
		return font;
	}
	
	public void renderConsole() {
		// DO ANIMATION
		float delta = Gdx.graphics.getDeltaTime();
		switch (aniMoving) {
			// move the console until reached the top/bottom
			case 1 : location -= delta * 1.75; break;
			case 2 : location += delta * 1.75; break;
		}
		location = Math.max(0.0, Math.min(limit, location)); // clamp [0, 1]
		
		if (location == 0.0) {
			// console is hiding. don't draw anything
			return;
		}
		
		/////////////////////////////
		
		float relativeY = -(int)(720 * (-1.0 + location));
		
		// draw bg
		batch.draw(
			bg,
			0, relativeY,
			1280, 720
		);
		
		// caret
		drawCaret += delta;
		if (drawCaret >= 2.0f) drawCaret = 0.0f;

		while (lines.size() > 30) {
			lines.pollFirst();
		}

		for (int i = 0; i < lines.size(); i++) {
			float drawY = ((lines.size() - i - 1) * 24) + relativeY;
			if (drawY > 720) continue; // off screen
			
			Line l = lines.get(i);
			if (l.content.isEmpty()) {
				batch.draw(sep, 25, 48 + drawY);
				continue;
			}
			font.setColor(l.color);
			font.draw(batch, l.content, 12, 64 + drawY);
		}
		
		font.setColor(Color.WHITE);
		font.draw(batch, "] " + textInput.getString() + (drawCaret > 1.0f ? "_" : ""), 12, 32 + relativeY);
	}
	
	public boolean isActivating() {
		return activating;
	}
	
	// from game
	public boolean keyTyped (char c) {
		if (!activating) return false;
		return textInput.keyTyped(c);
	}
	
	public boolean keyDown(int i) {
		if (!activating) return false;
		if (i == Input.Keys.ENTER) {
			exec(textInput.getString(), false);
			textInput.setString("");
			return true;
		}
		if (i == Input.Keys.DOWN) {
			if (historylook == history.size()) {
				cursorStore = textInput.getString();
			}
			historylook += 1;
			if (historylook >= history.size()) historylook = history.size();
			if (historylook == history.size()) {
				textInput.setString(cursorStore);
			} else {
				textInput.setString(history.get(historylook));
			}
			return true;
		} else if (i == Input.Keys.UP) {
			if (historylook == history.size()) {
				cursorStore = textInput.getString();
			}
			historylook -= 1;
			if (historylook < 0) historylook = 0;
			textInput.setString(history.get(historylook));
			return true;
		}
		return textInput.keyDown(i);
	}
	
	public void hide() {
		aniMoving = 1;
		activating = false;
	}
	public void show() {
		aniMoving = 2;
		activating = true;
	}
	public void showHalf() {
		limit = 0.33;
		show();
	}
	public void showFull() {
		limit = 1.0;
		show();
	}
	public void clear() {
		lines.clear();
	}
	
	////////////////////////////
	
	public synchronized void print(String s) {
		print(s, Color.WHITE);
	}
	public synchronized void print(String s, Color color) {
		while (s.length() > 80) {
			String l = s.substring(0, 80);
			s = s.substring(80);
			lines.add(new Line(l, color));
		}
		lines.add(new Line(s, color));
	}
	public synchronized void printSep() {
		lines.add(new Line());
	}
	public synchronized void printerr(String s) {
		print(s, Color.RED);
	}
	
	public synchronized void exec(String l, boolean silent) {
		if (!silent) {
			String print_ = "] " + l;
			print(print_);
			history.add(l);
			historylook = history.size();
		}
		if (l.isEmpty()) return;
		
		
		Matcher m = execPattern.matcher(l);
		ArrayList<String> args = new ArrayList<>();
		while (m.find()) {
			int i = m.groupCount() - 1;
			
			for (; i >= 0; i--) {
				String s = m.group(i);
				if (s == null) continue;
 				if (!s.isEmpty()) break;
			}
				
			
			args.add(m.group(i));
		}
		
		ConsoleCommand found = commands.get(args.get(0));
		if (found != null) {
			// CALLLL
			String[] argsArray = new String[args.size()];
			argsArray = args.toArray(argsArray);
			found.exec(argsArray);
		}
		
	}
	public synchronized void exec(String s) {
		exec(s, true);
	}
	
	////////////////////////////
	
	public void dispose () {
		font.dispose();
		bg.dispose();
	}
}
