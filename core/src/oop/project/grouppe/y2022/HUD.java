package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.LinkedList;

// draw hud things

public class HUD {
	private static final float CHAT_DURATION = 8.0f;
	
	private final World world;
	
	private boolean scoreboardVisible;
	private boolean chatMode;
	private boolean chatModeReady;
	
	private final BitmapFont hudFont1;
	private final BitmapFont hudFont2;
	private final BitmapFont hudFont3;
	
	private final LinkedList<ChatText> texts;
	private final int CHAT_MAX = 6;
	private float textTimer = CHAT_DURATION;
	private float chatCaret = 0.0f;
	private final TextInput chatText;
	
	private Texture bspBackgroundTexture = null;
	
	private final ArrayList<String[]> ipList;
	
	public HUD(World world) {
		this.world = world;
		
		chatText = new TextInput();
		chatText.setLimited(64);
		
		texts = new LinkedList<>();
		
		hudFont1 = (BitmapFont) ResourceManager.instance().get("hud_font");
		hudFont2 = (BitmapFont) ResourceManager.instance().get("chat_font");
		hudFont3 = (BitmapFont) ResourceManager.instance().get("character_name_font");
		hudFont2.getData().markupEnabled = true;
		
		bspBackgroundTexture = (Texture) ResourceManager.instance().get("connecting");
		
		ipList = new ArrayList<>();
	}
	
	public void draw(SpriteBatch batch) {
		if (world.isGenerating()) {
			// GENERATING !!! DONT DRAW ANYTHING
			batch.draw(bspBackgroundTexture, 0.0f, 0.0f, 1280, 720);
			hudFont1.draw(batch, "Generating Dungeon . . .", 10.0f, 60.0f);
		}
		
		
		if (world.isDemoReading() || !scoreboardVisible || world.isGameEnded()) {
			drawChats(batch);
			drawCornerText(batch);
		}
		/////////////////////
		// draw hud
		batch.setColor(Color.WHITE);
		
		drawInterface(batch);
	}
	
	private void drawChats(SpriteBatch batch) {
		float delta = Gdx.graphics.getDeltaTime();
		if (textTimer > 0.0) {
			textTimer -= delta;
		} else {
			textsPop();
		}
		
		int i = 0;
		for (; i < texts.size(); i++) {
			ChatText j = texts.get(i);
			String b = texts.get(i).getString();
			String s = b;
			String st = b;
			if (j.getAuthor().isEmpty()) {
				s = "[" + j.getAuthorColor() + "]" + b;
			} else {
				s = "[" + j.getAuthorColor() + "]< " + j.getAuthor() + " >[WHITE] " + b;
				st = "< " + j.getAuthor() + " > " + b;
			}

			drawChatText(batch, s, st, 20, 690 + (-i * 32));
		}
		if (chatMode) {
			chatModeReady = true;
			String c = chatText.getString() + (
				(chatCaret > 1.0f) ? "_" : ""
			);
			drawChatText(batch,
				"[CYAN][[Chat][WHITE] : " + c,	
				"[Chat] : " + c,	
			20, 690 + (-i * 32));
			chatCaret += delta * 1.5f;
			if (chatCaret >= 2.0f) chatCaret = 0.0f;
		}
	}
	
	private void drawCornerText(SpriteBatch batch) {
		String s;
		if (world.isDemoReading()) {
			// WE ARE PLAYING THE DEMO
			// dont show anything that unrelated
			return;
		}
		
		if (world.isInsideReadyArea()) {
			String col;
			if (world.getMyClient().isServer()) {
				col = "[GREEN]";
				s = "Press SPACE to enter the dungeon !";
			} else {
				col = "";
				s = "Wait for the host to start the game !";
			}
			drawChatText(batch, col + s, s, 20, 200);
		} else if (world.isSpectating()) {
			s = "Spectating other players . . .\nPress SPACE to cycle targets";
			drawChatText(batch, s, 20, 300);
		} else if (world.isInsideEntranceArea()) {
			if (world.getCollectedPaperCount() >= world.getPaperCount()) {
				s = "Waiting for players";
			} else {
				s = "Collect all golds to exit";
			}
			drawChatText(batch, s, 20, 200);
		}
	}
	
	private void drawScoreboard(SpriteBatch batch) {
		// show everyone's names
		int cursorY = 0;

		String header = "PLAYERS";
		if (world.isGameEnded()) {
			drawChatTextCenterX(batch, "[RED]GAME OVER", "GAME OVER", 660 - cursorY);
			header = "Level " + world.getCurrentLevelNumber();
			drawChatTextCenterX(batch, header, header, 630 - cursorY);
			header = (world.getPreviousCollectedPaperCount() + world.getCollectedPaperCount()) + " golds were collected.";
		}

		drawChatTextCenterX(batch, "[PINK]" + header, header, 600 - cursorY);
		if (!world.isGameEnded()) {
			header = world.getCollectedPaperCount() + " golds were collected from " + world.getPaperCount() + " golds";
			drawChatTextCenterX(batch, header, header, 550 - cursorY);
		}
		cursorY = -32;

		for (int i = 0; i < world.getCharacterCount(); i++) {
			Character ch = world.getCharacterByIndex(i);
			Player player = ch.getPlayer();

			// Name
			String c = "";
			if (player.getNetID() == world.getMyClient().getMyPlayer().getNetID()) {
				// highlight myself in green
				c = "[YELLOW]";
			} else if (ch.isDied() && !world.isGameEnded()) {
				c = "[RED]"; // ded (while playing)
			}
			drawChatText(batch, c + player.getUsername(), player.getUsername(), 200, 500 + cursorY);

			// Score
			String s = "" + player.getScore();
			drawChatText(batch, c + s, s, 1000, 500 + cursorY);
			cursorY -= 32;
		}
	}
	
	private void drawPlayerInfo(SpriteBatch batch) {
		Character m = world.getSpectatingCharacter();
		
		if (!world.isDemoReading() && !world.isGameEnded() && m != null && !world.isInLobby()) {
			batch.draw(m.getIcon(), 96.0f, 12.0f, 96.0f, 96.0f); // player status
			
			String s = "Level " + world.getCurrentLevelNumber();
			GlyphLayout g = new GlyphLayout(hudFont1, s);
			hudFont1.draw(batch, s,
				1280 - g.width - 32,
				720 - g.height
			); // level number
			
			// health point
			hudFont1.draw(batch, "" + m.getHealth(), 200, 64);
		}
	}
	
	private void drawLocalAddresses(SpriteBatch batch) {
		if (world.isInLobby() && !ipList.isEmpty()) {
			int i = 0;
			hudFont3.setColor(Color.YELLOW);
			hudFont3.draw(batch, "My Local Addresses", 1000.0f, 700.0f);
			for (String[] s : ipList) {
				hudFont3.setColor(Color.WHITE);
				hudFont3.draw(batch, s[1], 1000.0f, 670.0f + (20 * -i));
				i++;
			}
		}
	}
	
	private void drawInterface(SpriteBatch batch) {
		if (!world.isDemoReading() && (scoreboardVisible || world.isGameEnded())) {
			drawScoreboard(batch);
		} else {
			drawPlayerInfo(batch);
		}
		
		if (world.isGameEnded() && world.getMyClient().isServer()) {
			hudFont1.draw(batch, "Press SPACE to restart", 300.0f, 70.0f); // restart notice
		}
		
		if (world.isDemoWriting()) {
			drawChatText(batch, "[RED]RECORDING . . .", "RECORDING . . .", 1000, 70);
		}
		
		drawLocalAddresses(batch);
	}
	
	// t = text without highlighter
	// nmt = texte with highlighter ( [RED], [GREEN], [BLUE], . . . )
	public void drawChatText(SpriteBatch batch, String t, String nmt, int X, int Y) {
		hudFont2.draw(batch, "[BLACK]" + nmt, X + 2, Y - 2);
		hudFont2.draw(batch, t, X, Y);
	}
	
	// plain white text
	public void drawChatText(SpriteBatch batch, String t, int X, int Y) {
		drawChatText(batch, t, t, X, Y);
	}
	
	public void drawChatTextCenterX(SpriteBatch batch, String t, String nmt, int Y) {
		GlyphLayout layout = new GlyphLayout(hudFont2, t);
		int X = 1280 / 2 - (int)(layout.width / 2);
		drawChatText(batch, t, nmt, X, Y);
	}
	
	private void textsPop() {
		if (!texts.isEmpty())
			texts.remove(0);
		if (!texts.isEmpty())
			textTimer = texts.get(0).getTime();
	}
	
	public void refreshIPList() {
		ipList.clear();
		Utils.getLocalIP(ipList);
	}
	
	public void feedTextItem(ChatText i) {
		i.setTime(CHAT_DURATION - textTimer);
		if (texts.isEmpty())
			textTimer = CHAT_DURATION;
		texts.add(i);
		while (texts.size() > CHAT_MAX) textsPop(); // limit the text count
	}
	
	public void clearChat() {
		texts.clear();
	}
	
	public boolean keyTyped (char c) {
		if (!chatModeReady) return false;
		return chatText.keyTyped(c);
	}
	
	public boolean keyDown(int i) {
		if (!chatMode && i == Input.Keys.T) {
			chatMode = !chatMode;
			chatCaret = 0.0f;
			Character m = world.getMyClient().getCharacter();
			if (m != null) {
				m.resetMoveInput();
			}
			return true;
		}
		if (chatMode) {
			if (i == Input.Keys.ENTER) {
				submitChat();
				return true;
			}
			chatText.keyDown(i);
			return true;
		}
		
		if (i == Input.Keys.TAB) {
			showScoreboard(true);
			return true;
		}
		return false;
	}
	
	public boolean keyUp(int i) {
		if (chatMode) return false;
		
		if (i == Input.Keys.TAB) {
			showScoreboard(false);
			return true;
		}
		
		return false;
	}
	
	public boolean handleEscapeKey() {
		if (chatMode) {
			chatMode = false;
			chatModeReady = false;
			chatText.setString("");
			return true; // editing the chat text. then close
		}
		return false;
	}
	
	public void showScoreboard(boolean visible) {
		scoreboardVisible = visible;
	}
	
	public void submitChat() {
		world.submitChat(world.getMyClient().getMyPlayer().getNetID(), chatText.getString());
		chatText.setString("");
		chatMode = false;
		chatModeReady = false;
	}
}
