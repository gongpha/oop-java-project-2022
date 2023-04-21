package oop.project.grouppe.y2022;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Physics {
	public static boolean raycastRect(
		/* INPUT */
		Vector2 origin, Vector2 dir, Rectangle rect,
		
		/* OUTPUT */
		Vector2 normal //, Vector2 hitpos << lazy
	) {
		Vector2 dirfrac = new Vector2(
			1.0f / dir.x,
			1.0f / dir.y
		);
		
		float t1 = (rect.x - origin.x) * dirfrac.x;
		float t2 = (rect.x + rect.width - origin.x) * dirfrac.x;
		float t3 = (rect.y - origin.y) * dirfrac.y;
		float t4 = (rect.y + rect.height - origin.y) * dirfrac.y;
		
		float tmin = Math.max(Math.min(t1, t2), Math.min(t3, t4));
		float tmax = Math.min(Math.max(t1, t2), Math.max(t3, t4));
		boolean hit = true;
		float t = tmin;
		
		if (tmax < 0 || tmin > tmax) {
			hit = false;
			t = tmax;
		}
		
		// pain
		if (t == t1) {
			normal.x = -1.0f;
			normal.y = 0.0f;
		}
		if (t == t2) {
			normal.x = 1.0f;
			normal.y = 0.0f;
		}
		if (t == t3) {
			normal.x = 0.0f;
			normal.y = -1.0f;
		}
		if (t == t4) {
			normal.x = 0.0f;
			normal.y = 1.0f;
		}
		
		//if (t < 0) return true;
		//System.out.println(">> " + t + " " + hit);
		
		return hit;
	}
}
