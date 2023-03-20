
import java.awt.Image;
import javax.swing.ImageIcon;

public class Room {
	private Image scenePicture;
	
	public Room(String path) {
		setScenePictureByPath(path);
	}
	
	public void setScenePictureByPath(String path) {
		scenePicture = new ImageIcon(path).getImage();
	}
	public Image getScenePicture() {
		return scenePicture;
	}
}
