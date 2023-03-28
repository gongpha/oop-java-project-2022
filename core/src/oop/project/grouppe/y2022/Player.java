
package oop.project.grouppe.y2022;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Player {
	private int netID;
	private String username;
	private int[] idents = new int[]{ 0, 0, 0, 0 }; // 4 custom properties. ask gongpha if you want more
	
	public Player() {}

	public Player(int netID) {
		this.netID = netID;
	}

	public String getUsername() {
		return username;
	}
	
	public int getIdent(int index) {
		return idents[index];
	}
	
	public int getNetID() {
		return netID;
	}
	
	public void writeStream(DataOutputStream s) throws IOException {
		s.writeInt(netID);
		s.writeUTF(username);
		s.writeInt(idents[0]);
		s.writeInt(idents[1]);
		s.writeInt(idents[2]);
		s.writeInt(idents[3]);
	}
	
	public void readStream(DataInputStream s) throws IOException {
		netID = s.readInt();
		username = s.readUTF();
		idents[0] = s.readInt();
		idents[1] = s.readInt();
		idents[2] = s.readInt();
		idents[3] = s.readInt();
	}
	
	public void putData(
		int netID,
		String name,
		int i1, int i2, int i3, int i4
	) {
		this.netID = netID;
		username = name;
		idents[0] = i1;
		idents[1] = i2;
		idents[2] = i3;
		idents[3] = i4;
	}
}
