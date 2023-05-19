package oop.project.grouppe.y2022;

public class Customization {
	public final static String[] CHARACTERS = {
		"character1",
		"character2",
		"character3",
		"character4",
		"character5",
		"character6",
	};
	
	public static final String[][] GHOSTS = {
		{"ghost1.png", "ghost_idle1.mp3"},
		{"ghost2.png", "ghost_idle2.mp3"},
		{"ghost3.png", "ghost_idle3.mp3"},
		{"ghost1_old.png", "ghost_idle1.mp3"},
		{"ghost2_old.png", "ghost_idle2.mp3"},
		{"ghost3_old.png", "ghost_idle3.mp3"},
	};
	
	public static final String[] ROOMS = new String[] {
		"__lobby",
		
		"_entrance",
		"leftroom", "toproom", "rightroom",
		"hallwayhorizontal", "hallwayvertical",
		"corner1", "corner2", "corner3", "corner4",
		"corner5", "hallwayvertical2", "corner1_2",
		"corner2_2", "corner5_2",
		
		"bigroom1", "grid1"
		/* INSERT YOUR ROOM NAMES HERE */
	};
}
