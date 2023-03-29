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
		regPacket(SSyncState.class);
		regPacket(SNewPlayer.class);
		regPacket(SDisconnectPlayer.class);
		regPacket(SEntCreate.class);
		regPacket(SEntPos.class);
		regPacket(FPlayerState.class);
		regPacket(SGenLevel.class);
		regPacket(CGenerateDone.class);
		regPacket(CSendChat.class);
		regPacket(SSendChat.class);
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
	
	// this packet can be sent by clients or the server.
	// it also can be sent to other clients without making a new packet.
	public static abstract class ForwardablePacket extends Packet {
		public int fromNetID = -1;
		
		public void write(DataOutputStream s) throws IOException {
			if (getCSenderOrSMySelf().isPuppet())
				s.writeInt(fromNetID);
		}
		public void read(DataInputStream s) throws IOException {
			if (getCSenderOrSMySelf().isPuppet())
				fromNetID = getCSenderOrSMySelf().getMyPlayer().getNetID();
			else {
				fromNetID = s.readInt();
			}
		}
		public void forward() {
			if (getCSenderOrSMySelf().isPuppet()) {
				// forward to all clients except the sender
				getCServer().broadcastExceptAndServer(this, getCSenderOrSMySelf());
			}
		}
	}
	
	//////////////////////////////////////////////////////
	
	public static class CMyInfo extends Packet {
		public int header() { return 1; }
		
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
	
	// not for movement keys !
	public static class CInput extends Packet {
		public int header() { return 2; }
		
		public int input;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(input);
		}
		public void read(DataInputStream s) throws IOException {
			int input = s.readInt();
			Client sender = getCSenderOrSMySelf();
			if (sender.getMyPlayer().getNetID() != 1) {
				sender.applyInput(input);
				CoreGame.instance().getConsole().print("INPUT (" + sender.getMyPlayer().getNetID() + ") " + input);
			}
		}
	}
	
	public static class SSyncState extends Packet {
		public int header() { return 3; }
		
		public void write(DataOutputStream s) throws IOException {
			HashMap<Integer, Client> clients = getCServer().dumpClients();
			s.writeInt(clients.size());
			for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
				Player p = e.getValue().getMyPlayer();
				s.writeInt(e.getValue().getCharacter().getID());
				p.writeStream(s);
			}
		}
		public void read(DataInputStream s) throws IOException {
			int clientCount = s.readInt();
			for (int i = 0; i < clientCount; i++) {
				Player p = new Player();
				int entID = s.readInt();
				p.readStream(s);
				getCSenderOrSMySelf().newPlayer(p);
				world.registerNewPlayer(entID, p);
			}
			world.markReady();
		}
	}
	public static class SNewPlayer extends Packet {
		public int header() { return 4; }
		
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
			Client myClient = getCSenderOrSMySelf();
			Player p = new Player();
			p.readStream(s);
			myClient.newPlayer(p);
			world.registerNewPlayer(entID, p);
		}
	}
	
	// client requests the server to exit
	public static class CRequestDisconnect extends Packet {
		public int header() { return 5; }
		
		String reason;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeUTF(reason);
		}
		public void read(DataInputStream s) throws IOException {
			// ah yes byebye
			getCSenderOrSMySelf().kill("Disconnected by user");
		}
	}
	
	// player is disconnected
	public static class SDisconnectPlayer extends Packet {
		public int header() { return 6; }
		
		String reason;
		int netID;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(netID);
			s.writeUTF(reason);
		}
		public void read(DataInputStream s) throws IOException {
			int netID = s.readInt();
			String reason = s.readUTF();
			
			getCSenderOrSMySelf().removeClient(netID);
			world.removeDisconnectedClient(netID);
			
			if (netID == 1) {
				// server is closed !
				getCSenderOrSMySelf().kill(reason);
			}
			
			if (getCSenderOrSMySelf().getMyPlayer().getNetID() == netID) {
				// holy crap that's me !
				getCSenderOrSMySelf().kill(reason);
			}
		}
	}
	
	public static class SEntCreate extends Packet {
		public int header() { return 7; }
		
		int ID;
		String name;
		
		byte[] forwardedBytes;
		
		// when using an object
		Entity ent;
		
		public void write(DataOutputStream s) throws IOException {
			ID = ent.getID();
			name = ent.getClass().getName();
			//}
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
		public int header() { return 8; }
		Entity ent;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(ent.getID());
			s.writeFloat(ent.getX());
			s.writeFloat(ent.getY());
		}
		public void read(DataInputStream s) throws IOException {
			int entID = s.readInt();
			float X = s.readFloat();
			float Y = s.readFloat();
			
			ent = world.getEntities(entID);
			ent.setX(X);
			ent.setY(Y);
		}
	}
	
	public static class FPlayerState extends ForwardablePacket {
		public int header() { return 9; }
		Character ent;
		
		public void write(DataOutputStream s) throws IOException {
			super.write(s);
			s.writeFloat(ent.getX());
			s.writeFloat(ent.getY());
			
			s.writeFloat(ent.getAnimationIndex());
			s.writeByte(ent.getDirection());
			s.writeBoolean(ent.getAnimating());
		}
		public void read(DataInputStream s) throws IOException {
			super.read(s);
			ent = world.getCharacterByNetID(fromNetID);
			ent.setPosition(s.readFloat(), s.readFloat());
			ent.setAnimationIndex(s.readFloat());
			ent.setDirection(s.readByte());
			ent.setAnimating(s.readBoolean());
			
			forward();
		}
	}
	
	public static class SGenLevel extends Packet {
		public int header() { return 10; }
		
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
		public int header() { return 11; }
		
		public int input;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(input);
		}
		public void read(DataInputStream s) throws IOException {
			int input = s.readInt();
			Client sender = getCSenderOrSMySelf();
			if (sender.getMyPlayer().getNetID() != 1) {
				getCSenderOrSMySelf().applyInput(input);
			}
		}
	}
	
	public static class CSendChat extends Packet {
		public int header() { return 12; }
		
		public String message;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeUTF(message);
		}
		public void read(DataInputStream s) throws IOException {
			// forward to clients
			Packet.SSendChat p = new Packet.SSendChat();
			p.message = s.readUTF();
			p.netID = getCSenderOrSMySelf().getMyPlayer().getNetID();
			getCServer().broadcast(p);
		}
	}
	
	public static class SSendChat extends Packet {
		public int header() { return 13; }
		
		public String message;
		public int netID;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(netID);
			s.writeUTF(message);
		}
		public void read(DataInputStream s) throws IOException {
			int netID = s.readInt();
			String t = s.readUTF();
			world.feedChat(netID, t);
		}
	}
}
