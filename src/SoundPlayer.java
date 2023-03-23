
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import javazoom.jl.player.Player;

// plays sounds
// to loop sounds, add an asterisk at begin
// example : "*common/hl2/hl2_2001/sound/tv_loop2.wav"

public class SoundPlayer extends Thread {
	private String path;
	private Player player;
	private boolean loop;
	
	public SoundPlayer(String path) {
		this.path = path;
		loop = path.startsWith("*");
		if (loop) {
			this.path = path.substring(1);
		}
	}
		
	public void run() {
		try {
			do {
				player = new Player(new BufferedInputStream(new FileInputStream(new File(path))));
				player.play();
			} while (loop);
		} catch(Exception e) {
			throw new IllegalStateException("{!} your sound file (" + path + ") is too powerful. we can't play it TwT");
		}
	}
	
	public void stopPlaying() {
		loop = false;
		player.close();
		interrupt();
	}
	
	public String getPath() {
		return path;
	}
	
	public boolean isLooping() {
		return loop;
	}
}
