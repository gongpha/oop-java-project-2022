package oop.project.grouppe.y2022;

// included a Main menu and a pause menu
// hard coded ><

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Menu {
	private BitmapFont font;
	private BitmapFont fontBig;
	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;
	
	private enum Mode {
		MAINMENU,
		CUSTOMIZE,
		SETTINGS,
		CREDITS
	}
	private Mode mode = Mode.MAINMENU;
	
	// { label, command }
	private final static String[][] mainMenuStructure = {
		{"Host Game",	"mmnu_customize_host"},
		{"Join Game",	"mmnu_customize_join"},
		{"Settings",	"menu_settings"},
		{"Credits",		"mmnu_credits"},
		{"Quit",		"quit"},
	};
	private final static String[][] pauseMenuStructure = {
		{"Close Menu",	"menu_toggle"},
		{"Leave Game",	"disconnect"},
		{"Settings",	"menu_settings"},
		{"Quit",		"quit"},
	};
	private boolean isPauseMenu = false;
	private int cursor = 0;
	private int prevCursor = 0;
	private boolean forHost = false;
	private String[][] getCurrentStructure() {
		return isPauseMenu ? pauseMenuStructure : mainMenuStructure;
	}
	
	private TextInput nameInput;
	public String getUsername() { return nameInput.getString(); }
	private TextInput ipInput;
	public String getIP() { return ipInput.getString(); }
	private float caret = 0.0f;
	
	private TextureRegion selectingCharacterTexture;
	private int selectingCharacterIndex = 0;
	public int getIdent1() { return selectingCharacterIndex; }
	
	private Preferences pref;
	
	// funni box
	private Vector2 oopBox;
	private Vector2 oopBoxVec;
	private Texture oopBoxTexture;
	
	boolean showing = false;
	float alpha = 0.0f; // [0, 1]
	
	public Menu() {
		font = (BitmapFont) ResourceManager.instance().get("menu_font");
		fontBig = (BitmapFont) ResourceManager.instance().get("title_font");
		batch = CoreGame.instance().getBatch();
		
		pref = CoreGame.instance().getPref();
		oopBox = new Vector2();
		oopBoxVec = new Vector2(16, 16);
		oopBoxTexture = (Texture) ResourceManager.instance().get("oop");
		
		shapeRenderer = new ShapeRenderer();
		
		nameInput = new TextInput();
		nameInput.setLimited(CoreGame.NAMELENGTH);
		ipInput = new TextInput();
		ipInput.setLimited(15); // XXX.XXX.XXX.XXX
		
		selectingCharacterTexture = new TextureRegion();
		resetSelectingCharacter();
		selectingCharacterTexture.setRegionWidth(128);
		selectingCharacterTexture.setRegionHeight(32);
	}
	
	public void resetSelectingCharacter() {
		Texture t = (Texture) ResourceManager.instance().get(
			"character__" + Character.characters[selectingCharacterIndex]
		);
		selectingCharacterTexture.setTexture(t);
	}
	
	public void renderMenu() {
		// animate
		alpha += Gdx.graphics.getDeltaTime() * 3.0f * (showing ? 1.0f : -1.0f);
		alpha = Math.max(0.0f, Math.min(0.5f, alpha)); // clamp [0, 1]
		
		if (alpha > 0.0) {
			// enabling alpha (wtf libgdx why ???)
			batch.end();
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			
			shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
			shapeRenderer.setColor(new Color(0.0f, 0.0f, 0.0f, alpha));
			shapeRenderer.rect(0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			shapeRenderer.end();
			
			Gdx.gl.glDisable(GL20.GL_BLEND);
			batch.begin();
			
			if (alpha >= 0.25f) {
				switch (mode) {
				case MAINMENU:
					renderMainmenu();
					break;
				case CUSTOMIZE:
					renderCustomize();
					break;
				case SETTINGS:
					renderSettings();
					break;
				case CREDITS:
					renderCredits();
					break;
				}
			}
		}
	}
	
	public void showAsPauseMenu() {
		isPauseMenu = true;
		showMain();
	}
	
	public void showAsMainMenu() {
		isPauseMenu = false;
		showMain();
	}
	
	public void renderMainmenu() {
		String[][] structure = getCurrentStructure();
		for (int i = 0; i < structure.length; i++) {
			font.draw(batch, (
				(i == cursor ? ">> " : "") + structure[i][0]
			), 200, 400 + (-48 * i));
		}
	}
	
	public void renderCustomize() {
		float delta = Gdx.graphics.getDeltaTime();
		if (cursor == 0 || (cursor == 2 && !forHost)) {
			caret += delta * 1.5f;
			if (caret >= 2.0f) caret = 0.0f;
		}
		font.draw(batch, "Enter your name :", 200, 690);
		fontBig.draw(batch, (
			((cursor == 0) ? ">> " : " ") + nameInput.getString() + ((caret > 1.0f && cursor == 0) ? "_" : "")
		), 200, 600);
		
		font.draw(batch, "Your character :" + ((cursor == 1) ? " <<" : ""), 200, 500);
		batch.draw(selectingCharacterTexture, 200, 300, 512, 128);
		
		int Yrela = 0;
		if (!forHost) {
			// ENTER YOUR IP BRO
			font.draw(batch, "IP Address :", 200, 250);
			fontBig.draw(batch, (
				((cursor == 2) ? ">> " : " ") + ipInput.getString() + ((caret > 1.0f && cursor == 2) ? "_" : "")
			), 200, 200);
			Yrela -= 120;
		}
		
		boolean selecting = (
			(forHost) ? (cursor == 2) : (cursor == 3)
		);
		font.draw(batch, (selecting ? ">> " : "") + (
			forHost ? "START HOST" : "START JOIN"
		) + (selecting ? " <<" : ""), 200, 200 + Yrela);
	}
	
	public void renderSettings() {		
		font.draw(batch, (
			"Volume : " + CoreGame.instance().getVolume() + "%" + (cursor == 0 ? " <<" : "")
		), 200, 600);
	}
	
	public void renderCredits() {
		String[][] m = Developers.names;
		for (int i = 0; i < m.length; i++) {
			String[] name = m[i];
			int y = -i * 48;
			font.draw(batch, name[0], 200, 500 + y);
			font.draw(batch, name[1], 600, 500 + y);
		}
		
		batch.draw(oopBoxTexture, oopBox.x, oopBox.y);
		
		oopBox = oopBox.add(oopBoxVec);
		int w = Gdx.graphics.getWidth() - 128;
		int h = Gdx.graphics.getHeight() - 128;
		if (oopBox.x > w) {
			oopBox.x = w;
			oopBoxVec.x *= -1.0f;
		}
		if (oopBox.x < 0) {
			oopBox.x = 0;
			oopBoxVec.x *= -1.0f;
		}
		if (oopBox.y < 0) {
			oopBox.y = 0;
			oopBoxVec.y *= -1.0f;
		}
		if (oopBox.y > h) {
			oopBox.y = h;
			oopBoxVec.y *= -1.0f;
		}
	}
	
	public void toggle() {
		showing = !showing;
	}
	public boolean isShowing() {
		return showing;
	}
	
	public void saveCustomize() {
		pref.putString("username", nameInput.getString());
		pref.putString("ip", ipInput.getString());
		pref.putInteger("custom1", selectingCharacterIndex);
		pref.flush();
	}
	
	public void showMain() {
		if (mode == Mode.CUSTOMIZE) {
			saveCustomize();
		}
		mode = Mode.MAINMENU;
		cursor = prevCursor;
	}
	public void showCustomize(boolean host) {
		mode = Mode.CUSTOMIZE;
		prevCursor = cursor;
		cursor = 0;
		forHost = host;
		
		nameInput.setString(pref.getString("username"));
		selectingCharacterIndex = pref.getInteger("custom1");
		resetSelectingCharacter();
	}
	public void showSettings() {
		mode = Mode.SETTINGS;
		prevCursor = cursor;
		cursor = 0;
	}
	public void showCredits() {
		mode = Mode.CREDITS;
		prevCursor = cursor;
		cursor = 0;
	}
	
	public boolean keyTyped (char c) {
		if (mode == Mode.CUSTOMIZE) {
			if (cursor == 0) {
				return nameInput.keyTyped(c);
			} else if (cursor == 2 && !forHost) {
				return ipInput.keyTyped(c);
			}
		}
		return false;
	}
	
	public boolean keyDown(int i) {
		String[][] structure;
		
		switch (mode) {
		case MAINMENU:
			structure = getCurrentStructure();
			if (i == Input.Keys.ENTER) {
				String cmd = structure[cursor][1];
				CoreGame.instance().getConsole().exec(cmd);
			}
			break;
		case CUSTOMIZE:
			if (forHost)
				structure = new String[][]{{}, {}, {}};
			else
				structure = new String[][]{{}, {}, {}, {}};
			if (cursor == 1) {
				String[] sss = Character.characters;
				if (i == Input.Keys.LEFT) {
					selectingCharacterIndex -= 1;
				} else if (i == Input.Keys.RIGHT) {
					selectingCharacterIndex += 1;
				}
				selectingCharacterIndex = Math.max(0, Math.min(sss.length - 1, selectingCharacterIndex));
				resetSelectingCharacter();
			} else {
				if (forHost) {
					if (cursor == 2) {
						if (i == Input.Keys.ENTER) {
							saveCustomize();
							CoreGame.instance().getConsole().exec("host");
						}
					}
				} else {
					if (cursor == 3) {
						if (i == Input.Keys.ENTER) {
							saveCustomize();
							CoreGame.instance().getConsole().exec("join");
						}
					}
				}
			}
			break;
		case SETTINGS:
			structure = new String[][]{{}, {}};
			
			if (cursor == 0) {
				int volume = pref.getInteger("volume", 100);
				if (i == Input.Keys.LEFT) {
					volume -= 10;
					volume = Math.max(0, Math.min(100, volume));
					pref.putInteger("volume", volume);
					CoreGame.instance().setVolume(volume);
					pref.flush();
				} else if (i == Input.Keys.RIGHT) {
					volume += 10;
					volume = Math.max(0, Math.min(100, volume));
					pref.putInteger("volume", volume);
					CoreGame.instance().setVolume(volume);
					pref.flush();
				}
			}
			
			break;
		default:
			return false;
		}
		
		
		if (i == Input.Keys.DOWN) {
			caret = 0.0f;
			if (cursor == structure.length - 1)
				cursor = 0;
			else
				cursor += 1;
			return true;
		} else if (i == Input.Keys.UP) {
			caret = 0.0f;
			if (cursor == 0)
				cursor = structure.length - 1;
			else
				cursor -= 1;
			return true;
		}
		return false;
	}
	
	public boolean isOnRoot() {
		return mode == Mode.MAINMENU;
	}
}
