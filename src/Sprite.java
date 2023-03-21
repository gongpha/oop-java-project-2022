import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import javax.swing.ImageIcon;

// draw images

public class Sprite extends DrawObject {
	private Image image = null;
	
	public Point getMySize() {
		if (image == null) {
			return new Point();
		}
		return new Point(image.getWidth(parent), image.getHeight(parent));
	}
	
	public void draw(Graphics2D g) {
		if (image == null) {
			return;
		}
		Point p = getActualPosition();
		g.drawImage(image, p.x, p.y, parent);
	}
	
	public void setImage(String path) {
		image = new ImageIcon(path).getImage();
	}
	
	public Image getImage() {
		return image;
	}
}
