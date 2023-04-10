
package oop.project.grouppe.y2022;

import com.badlogic.gdx.net.SocketHints;

public class GameSocketHint extends SocketHints {
	public GameSocketHint() {
		connectTimeout = 30000; // sure ?
		tcpNoDelay = true; // disable nagle's algorithm (dont wait for a reasonable packet size. just send them anyways)
	}
}
