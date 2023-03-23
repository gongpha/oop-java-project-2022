
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class MainMenu extends Sprite {
	private Game game;
	private final Font fontTitle;
	private final Font fontItem;
	
	private String[] menuTexts = {
		"New Game",
		"Load Game",
		"Credits",
		"Exit"
	};
	
	private TextBox nameInput;
	private TextBox addressInput;
	
	public MainMenu(Game game, Paper paper) {
		super();
		this.game = game;
		setImage("assets/console.jpg");
		fontTitle = new Font("Times New Roman", Font.PLAIN, 64);
		fontItem = new Font("Times New Roman", Font.PLAIN, 32);
		
		paper.addObject(this);
		
		nameInput = new TextBox(400, 400, 300, 32, "Dr. Ladkrabang");
		nameInput.setLabel("Player Name");
		paper.addObject(nameInput);
		addressInput = new TextBox(400, 400 + 64, 300, 32, "localhost");
		addressInput.setLabel("Their Address");
		paper.addObject(addressInput);
	}
	
	public void draw(Graphics2D g) {
		super.draw(g);
		g.setColor(Color.black);
		g.fillRect(0, 0, 800, 600);
		g.setFont(fontTitle);
		g.setColor(Color.red);
		g.drawString("OOP Project Grouppe", 30, 120);
		
		g.setColor(Color.yellow);
		g.setFont(fontItem);
		for (int i = 0; i < menuTexts.length; i++) {
			g.drawString(menuTexts[i], 50, 450 + (32 * i));
		}
	}
	
}
