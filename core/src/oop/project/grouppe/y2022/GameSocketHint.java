
package oop.project.grouppe.y2022;

import com.badlogic.gdx.net.SocketHints;

// no wat

public class GameSocketHint extends SocketHints {
	public GameSocketHint() {
		connectTimeout = 30000;
		//tcpNoDelay = true;
	}
}
