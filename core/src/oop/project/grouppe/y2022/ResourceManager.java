
package oop.project.grouppe.y2022;

// a singleton class that provides you with resources

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import java.util.HashMap;

public class ResourceManager {
	public void preloads() {
		// FONTS
		preloadFont("default_font", "font/JLX_Pixel.ttf", 32);
		preloadFont("title_font", "font/JLX_Pixel.ttf", 64);
		preloadFont("menu_font", "font/JLX_Pixel.ttf", 48);
		
		preloadTexture("defcharacter", "character/_def.png");
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
	
	private AssetManager manager; // O_O
	private HashMap<String, String> map; // < Name : Path >
	private HashMap<String, Object> loaded; // < Name : Object >
	
	private float lastProgress = -1.0f;
	
	public ResourceManager() {
		map = new HashMap<String, String>();
		loaded = new HashMap<String, Object>();
		manager = new AssetManager();
		
		// declare a font loader
		FileHandleResolver resolver = new InternalFileHandleResolver();
		manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
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
		map.put(name, path);
		FreeTypeFontLoaderParameter param = new FreeTypeFontLoaderParameter();
		param.fontFileName = path;
		param.fontParameters.size = size;
		manager.load(path, BitmapFont.class, param);
	}
	
	public Object get(String name) {
		if (!loaded.containsKey(name)) {
			loaded.put(name, manager.get(map.get(name)));
		}
		return loaded.get(name);
	}
	
	/////////////////////////////////
	
	public void dispose() {
		manager.clear();
	}
}
