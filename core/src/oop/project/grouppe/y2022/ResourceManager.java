
package oop.project.grouppe.y2022;

// a singleton class that provides you with resources

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.util.ArrayList;
import java.util.HashMap;

public class ResourceManager {
	
	// for debugging
	public static boolean playMusic = true;
	
	private float volume = 1.0f;
	private float musicVolume = 1.0f;
	
	public void preloads() {
		// FONTS
		preloadFont("plat", "font/plat.ttf", 32);
		
		preloadFont("default_font", "font/JLX_Pixel.ttf", 32);
		preloadFont("title_font", "font/JLX_Pixel.ttf", 80);
		preloadFont("menu_font", "font/JLX_Pixel.ttf", 48);
		preloadFont("hud_font", "font/JLX_Pixel.ttf", 60);
		
		preloadFont("chat_font", "font/JLX_Pixel.ttf", 30);
		preloadFont("character_name_font", "font/JLX_Pixel.ttf", 24);
		
		preloadTexture("oop", "core/oop.png");
		preloadTexture("mainmenu", "core/mainmenu.jpg");
		preloadTexture("connecting", "core/connecting.jpg");
		preloadTexture("mainmenu_logo", "core/mainmenu_logo.png");
		preloadTexture("darkness1", "character/darkness1.png");
		
		for (String s : Customization.CHARACTERS) {
			preloadTexture("character__" + s, "character/" + s + ".png");
		}
		//for (String s : BSPDungeonGenerator.tilesets) {
		//	preloadTexture("tileset__" + s, "tileset/" + s + ".png");
		//}
		for (String s : Customization.ROOMS) {
			preloadMap("prefab__" + s, "room/" + s + ".tmx");
		}
		
		preloadTexture("items", "character/items.png");
		preloadTexture("end", "core/end.jpg");
		
		preloadSound("s_cheat", "sound/cheat.wav");
		preloadSound("s_protect", "sound/protect.wav");
		preloadSound("s_paper", "sound/paper.wav");
		preloadSound("s_chat", "sound/chat.wav");
		preloadSound("s_faster", "sound/faster.wav");
		preloadSound("s_completed", "sound/completed.wav");
		preloadSound("s_hit", "sound/hit.wav");
		preloadSound("s_invisible", "sound/invisible.wav");
		preloadSound("s_revive", "sound/revive.wav");
		preloadSound("s_revived", "sound/revived.wav");
		
		preloadSound("s_menu1", "sound/menu1.wav");
		preloadSound("s_menu2", "sound/menu2.wav");
		preloadSound("s_menu3", "sound/menu3.wav");
		
		preloadTexture("ghost1", "character/ghost1.png");
		preloadTexture("ghost2", "character/ghost2.png");
		preloadTexture("ghost3", "character/ghost3.png");
		
		preloadMusic("m_ghost1", "sound/ghost_idle1.mp3");
		preloadMusic("m_ghost2", "sound/ghost_idle2.mp3");
		preloadMusic("m_ghost3", "sound/ghost_idle3.mp3");
		preloadMusic("m_game_end", "sound/game_end.mp3");
		preloadMusic("m_lobby", "sound/lobby.mp3");
		
		preloadMusic("m_mainmenu1", "sound/mainmenu1.mp3");
	}
	
	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////
	
	private static ResourceManager singleton = null;
	public static synchronized ResourceManager instance() {
		if (singleton == null)
			singleton = new ResourceManager();
		return singleton;
	}
	
	private final AssetManager manager;
	private HashMap<String, String> map; // < Name : Path >
	private final HashMap<String, Object> loaded; // < Name : Object >
	private final ArrayList<String> loadedLater; // via load* methods
	
	private class PlayingSoundMusic {
		Disposable sm;
		long i;
	}
	private final ArrayList<PlayingSoundMusic> psms;
	
	private float lastProgress = -1.0f;
	
	public ResourceManager() {
		map = new HashMap<>();
		loaded = new HashMap<>();
		manager = new AssetManager();
		loadedLater = new ArrayList<>();
		
		psms = new ArrayList<>();
		
		// declare a font loader
		FileHandleResolver resolver = new InternalFileHandleResolver();
		manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
		
		// tiled map
		manager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
	}
	
	public boolean poll() {
		return manager.update();
	}
	public float getProgress() {
		if (lastProgress < manager.getProgress()) {
			lastProgress = manager.getProgress();
			return lastProgress;
		}
		return -1.0f;
	}
	
	/////////////////////////////////
	
	private void preloadTexture(String name, String path) {
		map.put(name, path);
		manager.load(path, Texture.class);
	}
	
	public Texture loadTexture(String path) {
		final String name = path;
		if (loaded.containsKey(name)) return (Texture) get(name);
		loadedLater.add(name);
		try {
			Texture t = new Texture(path);
			loaded.put(name, t);
			return t;
		} catch (GdxRuntimeException e) {
			CoreGame.instance().getConsole().printerr(e.getMessage());
			return null;
		}
	}
	
	private void preloadFont(String name, String path, int size) {
		map.put(name, name + ".ttf");
		FreeTypeFontLoaderParameter param = new FreeTypeFontLoaderParameter();
		param.fontFileName = path;
		param.fontParameters.size = size;
		manager.load(new AssetDescriptor<BitmapFont>(name + ".ttf", BitmapFont.class, param));
		//manager.load(path, BitmapFont.class, param);
	}
	
	private void preloadMap(String name, String path) {
		map.put(name, path);
		manager.load(path, TiledMap.class);
	}
	
	private void preloadSound(String name, String path) {
		map.put(name, path);
		manager.load(path, Sound.class);
	}
	
	private void preloadMusic(String name, String path) {
		map.put(name, path);
		manager.load(path, Music.class);
	}
	
	public Music loadMusic(String path) {
		final String name = path;
		if (loaded.containsKey(name)) return (Music) get(name);
		loadedLater.add(name);
		try {
			Music m = Gdx.audio.newMusic(Gdx.files.internal(path));
			loaded.put(name, m);
			return m;
		} catch (GdxRuntimeException e) {
			CoreGame.instance().getConsole().printerr(e.getMessage());
			return null;
		}
	}
	
	public Object get(String name) {
		if (!loaded.containsKey(name)) {
			try {
				loaded.put(name, manager.get(map.get(name)));
			} catch (GdxRuntimeException e) {
				CoreGame.instance().getConsole().printerr(e.getMessage());
				return null;
			}
			
		}
		return loaded.get(name);
	}
	
	/////////////////////////////////
	
	public synchronized void playSound(String name) {
		Sound s = ((Sound)ResourceManager.instance().get(name));
		long i = s.play(CoreGame.instance().getVolumef());
		
		CoreGame.instance().getConsole().print("Playing sound " + name);
		PlayingSoundMusic psm = new PlayingSoundMusic();
		psm.sm = s;
		psm.i = i;
		psms.add(psm);
	}
	
	public synchronized void playMusic(Music m) {
		if (!playMusic) return;
		
		m.setVolume(CoreGame.instance().getVolumef());
		try {
			m.play();
		} catch (Exception e) {
			CoreGame.instance().getConsole().printerr("Cannot play music " + m.toString());
			m.stop();
		}
		
		CoreGame.instance().getConsole().print("Playing music " + m.toString());
		PlayingSoundMusic psm = new PlayingSoundMusic();
		psm.sm = m;
		psm.i = -1L;
		psms.add(psm);
	}
	
	public synchronized void setVolume(int percent) {
		volume = percent / 100.0f;
		for (PlayingSoundMusic psm : psms) {
			Object o = psm.sm;
			if (o instanceof Music) {
				((Music) o).setVolume(volume * musicVolume);
			}
			if (o instanceof Sound) {
				((Sound) o).setVolume(psm.i, volume);
			}
		}
	}
	
	public synchronized void setMusicVolume(int percent) {
		musicVolume = percent / 100.0f;
		for (PlayingSoundMusic psm : psms) {
			Object o = psm.sm;
			if (o instanceof Music) {
				((Music) o).setVolume(volume * musicVolume);
			}
		}
	}
	
	public synchronized void stopMusic(Music music) {
		PlayingSoundMusic remove = null;
		for (PlayingSoundMusic psm : psms) {
			Object o = psm.sm;
			if (o instanceof Music && music == o) {
				((Music) o).stop();
				remove = psm;
				
				return;
			}
		}
		if (remove != null)
			psms.remove(remove);
	}
	
	public synchronized void stopAllSoundMusic() {
		for (PlayingSoundMusic psm : psms) {
			Object o = psm.sm;
			if (o instanceof Music) {
				((Music) o).stop();
			}
			if (o instanceof Sound) {
				((Sound) o).stop();
			}
		}
		psms.clear();
	}
	
	/////////////////////////////////
	
	public void dispose() {
		for (String s : loadedLater) {
			((Disposable) loaded.get(s)).dispose();
		}
		loadedLater.clear();
		manager.dispose();
		manager.clear();
	}
}
