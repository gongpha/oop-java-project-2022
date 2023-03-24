package oop.project.grouppe.y2022;

// included a Main menu and a pause menu

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Menu {
	private BitmapFont font;
	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;
	
	private final static String[] mainMenuStructure = {
		"Host Game",
		"Join Game",
		"Settings",
		"Credits",
		"Quit",
	};
	private final static String[] pauseMenuStructure = {
		"Resume",
		"Leave Game",
		"Settings",
		"Quit",
	};
	private boolean isPauseMenu = false;
	private int cursor = 0;
	private String[] getCurrentStructure() {
		return isPauseMenu ? pauseMenuStructure : mainMenuStructure;
	}
	
	boolean showing = false;
	float alpha = 0.0f; // [0, 1]
	
	public Menu() {
		font = (BitmapFont) ResourceManager.instance().get("menu_font");
		batch = CoreGame.instance().getBatch();
		
		shapeRenderer = new ShapeRenderer();
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
				for (int i = 0; i < mainMenuStructure.length; i++) {
					font.draw(batch, (
						(i == cursor ? ">> " : "") + mainMenuStructure[i]
					), 200, 400 + (-48 * i));
				}
			}
		}
	}
	
	public void toggle() {
		showing = !showing;
	}
	public boolean isShowing() {
		return showing;
	}
	
	public boolean keyDown(int i) {
		if (i == Input.Keys.DOWN) {
			String[] structure = getCurrentStructure();
			if (cursor == structure.length - 1)
				cursor = 0;
			else
				cursor += 1;
			return true;
		} else if (i == Input.Keys.UP) {
			String[] structure = getCurrentStructure();
			if (cursor == 0)
				cursor = structure.length - 1;
			else
				cursor -= 1;
			return true;
		}
		return false;
	}
}
