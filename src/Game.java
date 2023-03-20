// our game
// ( ... SOVIET ANTHEM INTENSIFIES ... )

// how the game runs
// 1. create a window
// 2. add a paper into the window
// 3. hav fun

public class Game {
	public static void main(String[] args) {
		// Create a new window
		var window = new Window(640, 506);
		
		// create and add the paper into the window for drawing
		var hl3 = new BorealisWowPaper();
		window.addPaper(hl3);
	}
}
