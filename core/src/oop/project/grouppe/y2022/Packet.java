package oop.project.grouppe.y2022;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

// packets for communication

public abstract class Packet {
	public int header() { return 0; }
	public abstract void write(DataOutputStream s) throws IOException;
	public abstract void read(DataInputStream s) throws IOException;
	private static HashMap<Integer, Class> packetList = new HashMap<>();
	
	static World world;
	
	private Server server;
	private Client client;
	
	public void setCSenderOrSMySelf(Client client) {
		this.client = client;
	}
	public Client getCSenderOrSMySelf() {
		return client;
	}
	public void setCServer(Server server) {
		this.server = server;
	}
	public Server getCServer() {
		return server;
	}
	
	public static void regPackets() {
		regPacket(CMyInfo.class);
		regPacket(CInput.class);
		regPacket(SNewPlayer.class);
		regPacket(SKick.class);
		regPacket(SEntCreate.class);
		regPacket(SEntPos.class);
		regPacket(SNewLevel.class);
		regPacket(CGenerateDone.class);
	}
	
	public static Class getPacketFromHeader(int header) {
		return packetList.get(header);
	}
	
	public static void regPacket(Class c) {
		try {
			Packet packet = (Packet) c.getDeclaredConstructor().newInstance();
			packetList.put(packet.header(), c);
		} catch (Exception e) {
			CoreGame.instance().getConsole().printerr(
				"Cannot register \"" + c.getName() + "\" : " + e.getMessage()
			);
		}
	}
	
	//////////////////////////////////////////////////////
	
	public static class CMyInfo extends Packet {
		public int header() { return 0x01; }
		
		public Client inClient;
		
		public void write(DataOutputStream s) throws IOException {
			Player p = inClient.getMyPlayer();
			s.writeInt(p.getNetID());
			s.writeUTF(p.getUsername());
			s.writeInt(p.getIdent(0));
			s.writeInt(p.getIdent(1));
			s.writeInt(p.getIdent(2));
			s.writeInt(p.getIdent(3));
		}
		public void read(DataInputStream s) throws IOException {
			int netID = s.readInt();
			getCServer().newPlayerReceived(
				getCSenderOrSMySelf(),
				netID,
				s.readUTF(),
				s.readInt(), s.readInt(), s.readInt(), s.readInt()
			);
		}
	}
	
	public static class CInput extends Packet {
		public int header() { return 0x02; }
		
		public int input;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(input);
		}
		public void read(DataInputStream s) throws IOException {
			int input = s.readInt();
			Client sender = getCSenderOrSMySelf();
			if (sender.getMyPlayer().getNetID() != 1)
				getCSenderOrSMySelf().applyInput(input);
		}
	}
	
	public static class SNewPlayer extends Packet {
		public int header() { return 0x03; }
		
		int entID;
		int netID;
		Player p;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(entID);
			s.writeInt(netID);
			s.writeUTF(p.getUsername());
			s.writeInt(p.getIdent(0));
			s.writeInt(p.getIdent(1));
			s.writeInt(p.getIdent(2));
			s.writeInt(p.getIdent(3));
		}
		public void read(DataInputStream s) throws IOException {
			int entID = s.readInt();
			int netID = s.readInt();
			Client myClient = getCSenderOrSMySelf();
			Player p = myClient.newPlayer(netID);
			p.putData(netID, s.readUTF(), s.readInt(), s.readInt(), s.readInt(), s.readInt());
			
			Character ent = (Character) world.createEntityAuthorized(entID,
				Character.class.getName()
			);
			ent.setupPlayer(p);
			if (netID == myClient.getMyPlayer().getNetID())
				myClient.setCharacter(ent);
		}
	}
	
	public static class SKick extends Packet {
		public int header() { return 0x04; }
		
		String reason;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeUTF(reason);
		}
		public void read(DataInputStream s) throws IOException {
			// OOF
			CoreGame.instance().forceDisconnect(s.readUTF());
		}
	}
	
	public static class SEntCreate extends Packet {
		public int header() { return 0x05; }
		
		int ID;
		String name;
		
		byte[] forwardedBytes;
		
		// when using an object
		Entity ent;
		
		public void write(DataOutputStream s) throws IOException {
			if (ent != null) {
				ID = ent.getID();
				name = ent.getClass().getName();
			}
			s.writeInt(ID);
			s.writeUTF(name);
			
			//ByteArrayOutputStream baos = new ByteArrayOutputStream();
			//DataOutputStream dos = new DataOutputStream(baos);
			if (ent != null)
				ent.serializeConstructor(s);
			else
				s.write(forwardedBytes);
			//baos.flush();
			//byte[] bytes = baos.toByteArray();
			
			//s.writeInt(bytes.length);
			//s.write(bytes);
		}
		public void read(DataInputStream s) throws IOException {
			ent = world.createEntityAuthorized(s.readInt(), s.readUTF());
			//int size = s.readInt();
			ent.deserializeConstructor(s);
		}
	}
	
	public static class SEntPos extends Packet {
		public int header() { return 0x07; }
		Entity ent;
		boolean predictable = false;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(ent.getID());
			s.writeBoolean(predictable);
			s.writeFloat(ent.getX());
			s.writeFloat(ent.getY());
		}
		public void read(DataInputStream s) throws IOException {
			int netID = s.readInt();
			predictable = s.readBoolean();
			float X = s.readFloat();
			float Y = s.readFloat();
			
			if (predictable && world.getMyClient().getMyPlayer().getNetID() == netID)
				return; // this packet allows me to predict and also this is also myself. so don't care
			
			ent = world.getCharacterByNetID(s.readInt());
			ent.setX(s.readFloat());
			ent.setY(s.readFloat());
		}
	}
	
	public static class SNewLevel extends Packet {
		public int header() { return 0x08; }
		
		String mapName;
		int seed;
		int tilesetIndex;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeUTF(mapName);
			s.writeInt(seed);
			s.writeInt(tilesetIndex);
		}
		public void read(DataInputStream s) throws IOException {
			// OOF
			world.setLevelName(s.readUTF());
			world.generateMap(s.readInt(), s.readInt());
		}
	}
	
	public static class CGenerateDone extends Packet {
		public int header() { return 0x09; }
		
		public int input;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(input);
		}
		public void read(DataInputStream s) throws IOException {
			int input = s.readInt();
			Client sender = getCSenderOrSMySelf();
			if (sender.getMyPlayer().getNetID() != 1)
				getCSenderOrSMySelf().applyInput(input);
		}
	}
}
