package oop.project.grouppe.y2022;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import oop.project.grouppe.y2022.CoreGame;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setForegroundFPS(60);
		config.setTitle("OOP Java Project : Nextbot");
		
		// edited by gongpha
		config.setResizable(false);
		config.setWindowedMode(1280, 720);
		
		new Lwjgl3Application(new CoreGame(), config);
	}
}
