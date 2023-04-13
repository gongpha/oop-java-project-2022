package oop.project.grouppe.y2022;

public class Utils {
	public static float lerp(float a, float b, float w)
	{
		return a * (1.0f - w) + (b * w);
	}
	
	public static float clamp(float v, float min, float max) {
		return (v < min) ? min : ((v > max) ? max : v);
	}
}
