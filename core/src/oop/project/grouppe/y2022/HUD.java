package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.LinkedList;

// draw hud things

public class HUD {
	private static final float CHAT_DURATION = 8.0f;
	
	private World world;
	private Character spectatingCharacter;
	
	private boolean scoreboardVisible;
	private boolean chatMode;
	private boolean chatModeReady;
	
	private BitmapFont hudFont1;
	private BitmapFont hudFont2;
	private BitmapFont hudFont3;
	
	private LinkedList<ChatText> texts;
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
		
		ipList = new ArrayList<String[]>();
	}
	
	public void draw(SpriteBatch batch) {
		if (world.isGenerating()) {
			// GENERATING !!! DONT DRAW ANYTHING
			batch.draw(bspBackgroundTexture, 0.0f, 0.0f, 1280.0f, 720.0f);
			hudFont1.draw(batch, "Generating Dungeon . . .", 10.0f, 60.0f);
		}
		
		
		if (world.isDemoReading() || !scoreboardVisible || world.isGameEnded()) {
			drawChats(batch);
			drawCornerText(batch);
		}
		/////////////////////
		// draw hud
		batch.setColor(Color.WHITE);
		
		drawPlayerInfo(batch);
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
		} else if (spectatingCharacter != null) {
			s = "Spectating other players . . .\nPress SPACE to cycle targets";
			drawChatText(batch, s, s, 20, 300);
		} else if (world.isInsideEntranceArea()) {
			if (world.getCollectedPaperCount() >= world.getPaperCount()) {
				s = "Waiting for players";
			} else {
				s = "Collect all golds to exit";
			}
			drawChatText(batch, s, s, 20, 200);
		}
	}
	
	private void drawPlayerInfo(SpriteBatch batch) {
		Character m = world.getMyClient().getCharacter();
		
		if (!world.isDemoReading() && (scoreboardVisible || world.isGameEnded())) {
			// show everyone's names
			int cursorY = 0;
			
			String header = "PLAYERS";
			int headerX = 600;
			if (world.isGameEnded()) {
				header = (world.getPreviousCollectedPaperCount() + world.getCollectedPaperCount()) + " golds were collected.";
				headerX -= 200;
			}
			
			drawChatText(batch, "[PINK]" + header, header, headerX, 600 - cursorY);
			if (!world.isGameEnded()) {
				header = world.getCollectedPaperCount() + " golds were collected from " + world.getPaperCount() + " golds";
				drawChatText(batch, header, header, headerX - 230, 550 - cursorY);
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
		} else {
			if (spectatingCharacter != null) m = spectatingCharacter; // show the spectatee's info instead
			if (!world.isDemoReading() && !world.isGameEnded() && m != null && !world.isInLobby()) {
				batch.draw(m.getIcon(), 96.0f, 12.0f, 96.0f, 96.0f); // player status
				hudFont1.draw(batch, "Level " + world.getCurrentLevelNumber(), 550.0f, 70.0f); // level number
			}
		}
		
		if (world.isGameEnded() && world.getMyClient().isServer()) {
			hudFont1.draw(batch, "Press SPACE to restart", 300.0f, 70.0f); // restart notice
		}
		
		if (world.isDemoWriting()) {
			drawChatText(batch, "[RED]RECORDING . . .", "RECORDING . . .", 1000, 70);
		}
		
		if (world.isInLobby() && !ipList.isEmpty()) {
			int i = 0;
			hudFont3.setColor(Color.YELLOW);
			hudFont3.draw(batch, "My Local Addresses", 1000.0f, 700.0f); // restart notice
			for (String[] s : ipList) {
				hudFont3.setColor(Color.WHITE);
				hudFont3.draw(batch, s[1], 1000.0f, 670.0f + (20 * -i)); // restart notice
				i++;
			}
		}
	}
	
	public void drawChatText(SpriteBatch batch, String t, String nmt, int X, int Y) {
		hudFont2.draw(batch, "[BLACK]" + nmt, X + 2, Y - 2);
		hudFont2.draw(batch, t, X, Y);
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
