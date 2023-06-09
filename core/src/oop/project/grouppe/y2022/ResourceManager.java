
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
	public float getVolume() { return volume; }
	private float musicVolume = 1.0f;
	public float getMusicVolume() { return musicVolume; }
	
	private String errorMsg = "";
	
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
		preloadTexture("darkness1", "core/darkness1.png");
		
		preloadTexture("toastbg", "core/toastbg.png");
		preloadTexture("toastbgblack", "core/toastbgblack.png");
		
		for (String s : Customization.CHARACTERS) {
			preloadTexture("character__" + s, "character/" + s + ".png");
		}
		//for (String s : BSPDungeonGenerator.tilesets) {
		//	preloadTexture("tileset__" + s, "tileset/" + s + ".png");
		//}
		for (String s : Customization.ROOMS) {
			preloadMap("prefab__" + s, "room/" + s + ".tmx");
		}
		
		preloadTexture("items", "core/items.png");
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
		preloadSound("s_heal", "sound/heal.wav");
		
		preloadSound("s_menu1", "sound/menu1.wav");
		preloadSound("s_menu2", "sound/menu2.wav");
		preloadSound("s_menu3", "sound/menu3.wav");
		
		preloadMusic("m_game_end", "sound/game_end.mp3");
		preloadMusic("m_lobby", "sound/lobby.mp3");
		
		preloadMusic("m_mainmenu1", "sound/mainmenu1.mp3");
		preloadMusic("nokoctave100", "sound/nokoctave100.mp3");
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
	
	/*
		-1 = ERROR
		0 = ok
		1 = done
	*/
	public int poll() {
		try {
			return manager.update() ? 1 : 0;
		} catch (GdxRuntimeException e) {
			errorMsg = e.getMessage();
			return -1;
		}
	}
	
	public String getError() {
		return errorMsg;
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
		long i = s.play(volume);
		
		//CoreGame.instance().getConsole().print("Playing sound " + name);
		PlayingSoundMusic psm = new PlayingSoundMusic(s, i);
		psms.add(psm);
	}
	
	public synchronized PlayingSoundMusic playMusic(Music m, boolean loop) {
		if (!playMusic) return new PlayingSoundMusic(null, -1L);
		
		m.setVolume(musicVolume);
		m.setLooping(loop);
		try {
			m.play();
		} catch (Exception e) {
			CoreGame.instance().getConsole().printerr("Cannot play music " + m.toString());
			m.stop();
		}
		
		//CoreGame.instance().getConsole().print("Playing music " + m.toString());
		m.setVolume(volume * musicVolume);
		PlayingSoundMusic psm = new PlayingSoundMusic(m, -1L);
		psms.add(psm);
		return psm;
	}
	
	public synchronized PlayingSoundMusic playMusicLoop(Music m) {
		return playMusic(m, true);
	}
	
	private synchronized void resetAllPSMsVolume() {
		for (PlayingSoundMusic psm : psms) {
			psm.resetGlobalVolume();
		}
	}
	
	public synchronized void setVolume(int percent) {
		volume = percent / 100.0f;
		resetAllPSMsVolume();
	}
	
	public synchronized void setMusicVolume(int percent) {
		musicVolume = percent / 100.0f;
		resetAllPSMsVolume();
	}
	
	public synchronized void setMusicVolumeObject(int percent) {
		musicVolume = percent / 100.0f;
		resetAllPSMsVolume();
	}
	
	public synchronized void stopMusic(Music music) {
		PlayingSoundMusic remove = null;
		for (PlayingSoundMusic psm : psms) {
			Disposable o = psm.getSM();
			if (o instanceof Music && music == o) {
				((Music) o).stop();
				remove = psm;
				break;
			}
		}
		if (remove != null)
			psms.remove(remove);
	}
	
	public synchronized void stopAllMusics() {
		ArrayList<Integer> removeList = new ArrayList<>();
		for (int i = 0; i < psms.size(); i++) {
			PlayingSoundMusic psm = psms.get(i);
			if (psm.isMusic()) {
				psm.stop();
				removeList.add(i);
			}
		}
		for (int i = removeList.size() - 1; i >= 0; i--) {
			psms.remove(removeList.get(i).intValue());
		}
		removeList.clear(); // gc hater
	}
	
	public synchronized void stopAllSoundMusic() {
		for (PlayingSoundMusic psm : psms) {
			psm.stop();
		}
		psms.clear();
	}
	
	/////////////////////////////////
	
	public void dispose() {
		for (String s : loadedLater) {
			Object o = loaded.get(s);
			if (o == null) continue;
			((Disposable) o).dispose();
		}
		loadedLater.clear();
		manager.dispose();
		manager.clear();
	}
}
