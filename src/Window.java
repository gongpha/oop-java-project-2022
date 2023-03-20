// This class helps you create a window

import javax.swing.*;

public class Window extends JPanel {
	private final JFrame frame;
	
	public Window(int winsize_x, int winsize_y) {
		// create a new window and set its size
		frame = new JFrame();
		frame.setSize(winsize_x, winsize_y);
		frame.setVisible(true);
		
		// YOU AREN'T ALLOWED TO RESIZE THE WINDOW
		frame.setResizable(false);
		
		// when we've closed the window, exits the program too
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	public void addPaper(Paper paper) {
		frame.add(paper);
		frame.addKeyListener(paper);
	}
}
