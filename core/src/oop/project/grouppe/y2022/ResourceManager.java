
package oop.project.grouppe.y2022;

// a singleton class that provides you with resources

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
import java.util.ArrayList;
import java.util.HashMap;

public class ResourceManager {
	public void preloads() {
		// FONTS
		preloadFont("plat", "font/plat.ttf", 32);
		
		preloadFont("default_font", "font/JLX_Pixel.ttf", 32);
		preloadFont("title_font", "font/JLX_Pixel.ttf", 80);
		preloadFont("menu_font", "font/JLX_Pixel.ttf", 48);
		preloadFont("hud_font", "font/JLX_Pixel.ttf", 60);
		
		preloadFont("chat_font", "font/JLX_Pixel.ttf", 32);
		preloadFont("character_name_font", "font/JLX_Pixel.ttf", 16);
		
		preloadTexture("oop", "core/oop.png");
		preloadTexture("darkness1", "character/darkness1.png");
		
		preloadTexture("ghost1", "character/ghost1.png");
		
		preloadMap("demo1", "map/demo1.tmx");
		
		for (String s : Character.characters) {
			preloadTexture("character__" + s, "character/" + s + ".png");
		}
		for (String s : BSPDungeonGenerator.tilesets) {
			preloadTexture("tileset__" + s, "tileset/" + s + ".png");
		}
		
		preloadTexture("items", "character/items.png");
		
		preloadSound("s_cheat", "sound/cheat.wav");
		preloadSound("s_protect", "sound/protect.wav");
		preloadSound("s_paper", "sound/paper.wav");
		preloadSound("s_chat", "sound/chat.wav");
		preloadSound("s_faster", "sound/faster.wav");
		
		preloadSound("s_menu1", "sound/menu1.wav");
		preloadSound("s_menu2", "sound/menu2.wav");
		preloadSound("s_menu3", "sound/menu3.wav");
		
		preloadMusic("m_ghost", "sound/ghost_idle1.mp3");
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
	
	private AssetManager manager;
	private HashMap<String, String> map; // < Name : Path >
	private HashMap<String, Object> loaded; // < Name : Object >
	
	private class PlayingSoundMusic {
		Object sm;
		long i;
	}
	private ArrayList<PlayingSoundMusic> psms;
	
	private float lastProgress = -1.0f;
	
	public ResourceManager() {
		map = new HashMap<String, String>();
		loaded = new HashMap<String, Object>();
		manager = new AssetManager();
		
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
	
	public void preloadTexture(String name, String path) {
		map.put(name, path);
		manager.load(path, Texture.class);
	}
	
	public void preloadFont(String name, String path, int size) {
		map.put(name, name + ".ttf");
		FreeTypeFontLoaderParameter param = new FreeTypeFontLoaderParameter();
		param.fontFileName = path;
		param.fontParameters.size = size;
		manager.load(new AssetDescriptor<BitmapFont>(name + ".ttf", BitmapFont.class, param));
		//manager.load(path, BitmapFont.class, param);
	}
	
	public void preloadMap(String name, String path) {
		map.put(name, path);
		manager.load(path, TiledMap.class);
	}
	
	public void preloadSound(String name, String path) {
		map.put(name, path);
		manager.load(path, Sound.class);
	}
	
	public void preloadMusic(String name, String path) {
		map.put(name, path);
		manager.load(path, Music.class);
	}
	
	public Object get(String name) {
		if (!loaded.containsKey(name)) {
			loaded.put(name, manager.get(map.get(name)));
		}
		return loaded.get(name);
	}
	
	/////////////////////////////////
	
	public void playSound(String name) {
		Sound s = ((Sound)ResourceManager.instance().get(name));
		long i = s.play(CoreGame.instance().getVolumef());
		PlayingSoundMusic psm = new PlayingSoundMusic();
		psm.sm = s;
		psm.i = i;
		psms.add(psm);
	}
	
	public void playMusic(Music m) {
		m.setVolume(CoreGame.instance().getVolumef());
		m.play();
		PlayingSoundMusic psm = new PlayingSoundMusic();
		psm.sm = m;
		psm.i = -1L;
		psms.add(psm);
	}
	
	public void setVolume(int percent) {
		float v = percent /= 100.0f;
		for (PlayingSoundMusic psm : psms) {
			Object o = psm.sm;
			if (o instanceof Music) {
				((Music) o).setVolume(v);
			}
			if (o instanceof Sound) {
				((Sound) o).setVolume(psm.i, v);
			}
		}
	}
	
	public void stopAllSoundMusic() {
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
		manager.clear();
	}
}
