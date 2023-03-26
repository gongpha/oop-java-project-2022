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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// for hackers

public class Console {
	private CoreGame game;
	private SpriteBatch batch;
	private Pattern execPattern;
	
	private BitmapFont font;
	private Texture bg;
	
	private double location = 1.0; // [0, 1]. 0 = fully hidden, 1 = fully showed
	private int aniMoving = 0; // 1 = moving up, 2 = moving down. otherwise, do nothing
	private double limit = 1.0; // 1.0 = full, 0.5 = half
	
	private HashMap<String, ConsoleCommand> commands;
	private ArrayList<String> history;
	private int historylook = -3;
	
	private boolean activating = true;
	
	private float drawCaret = 0.0f; // [0, 2] (>= 1 : draw, < 1 : hide)
	
	private String lineEnter = "";
	
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
	ArrayList<Line> lines;
	
	public Console() {
		game = CoreGame.instance();
		lines = new ArrayList<>();
		history = new ArrayList<>();
		
		execPattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'", Pattern.CASE_INSENSITIVE);
		
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(new FileHandle(
			"font/cour.ttf"
		));
		FreeTypeFontParameter param = new FreeTypeFontParameter();
		param.size = 24;
		font = generator.generateFont(param);
		generator.dispose();
		
		//bgRegion.
		bg = new Texture("core/console.jpg");
		
		batch = game.getBatch();
		
		// register commands
		commands = new HashMap<String, ConsoleCommand>();
		ConsoleCommand[] cmds = ConsoleCommandCore.regCommands();
		for (int i = 0; i < cmds.length; i++) {
			ConsoleCommand cmd = cmds[i];
			cmd.setConsole(this);
			commands.put(cmd.getCmd(), cmd);
		}
		print("Registered " + commands.size() + " commands !", Color.GREEN);
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
		
		float relativeY = -(int)(Gdx.graphics.getHeight() * (-1.0 + location));
		
		// draw bg
		batch.draw(
			bg,
			0, relativeY,
			Gdx.graphics.getWidth(), Gdx.graphics.getHeight()
		);
		
		// caret
		drawCaret += delta;
		if (drawCaret >= 2.0f) drawCaret = 0.0f;
		
		for (int i = 0; i < lines.size(); i++) {
			float drawY = ((lines.size() - i - 1) * 24) + relativeY;
			if (drawY > Gdx.graphics.getHeight()) continue; // off screen
			
			Line l = lines.get(i);
			if (l.content.isEmpty()) continue; // TODO : sep
			font.setColor(l.color);
			font.draw(batch, l.content, 12, 64 + drawY);
		}
		
		font.setColor(Color.WHITE);
		font.draw(batch, "] " + lineEnter + (drawCaret > 1.0f ? "_" : ""), 12, 32 + relativeY);
	}
	
	public boolean isActivating() {
		return activating;
	}
	
	// from game
	public boolean keyTyped (char c) {
		if (!activating) return false;
		if (c == '\b' && !lineEnter.isEmpty()) {
			// backspace
			lineEnter = lineEnter.substring(0, lineEnter.length() - 1);
			return true;
		}
		if (c >= 0x20) {
			lineEnter += c;
			return true;
		}
		return false;
	}
	
	public boolean keyDown(int i) {
		if (i == Input.Keys.ENTER) {
			exec(lineEnter, false);
			lineEnter = "";
		}
		return false;
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
	
	public void print(String s) {
		lines.add(new Line(s));
	}
	public void print(String s, Color color) {
		lines.add(new Line(s, color));
	}
	public void printSep() {
		lines.add(new Line());
	}
	public void printerr(String s) {
		print(s, Color.RED);
	}
	
	public void exec(String l, boolean silent) {
		if (!silent) {
			String print_ = "] " + l;
			print(print_);
			history.add(l);
		}
		if (l.isEmpty()) return;
		
		
		
		Matcher m = execPattern.matcher(l);
		ArrayList<String> args = new ArrayList<>();
		while (m.find()) {
			if (m.group().isEmpty()) continue;
			args.add(m.group());
		}
		ConsoleCommand found = commands.get(args.get(0));
		if (found != null) {
			// CALLLL
			String[] argsArray = new String[args.size()];
			argsArray = args.toArray(argsArray);
			found.exec(argsArray);
		}
		
	}
	public void exec(String s) {
		exec(s, false);
	}
	
	////////////////////////////
	
	public void dispose () {
		font.dispose();
		bg.dispose();
	}
}
