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
		regPacket(CRequestDisconnect.class);
		regPacket(SDisconnectPlayer.class);
		regPacket(SEntCreate.class);
		regPacket(SEntDelete.class);
		regPacket(SEntPos.class);
		regPacket(FPlayerState.class);
		regPacket(SGenLevel.class);
		regPacket(CGenerateDone.class);
		regPacket(CSendChat.class);
		regPacket(SSendChat.class);
		regPacket(SEntUpdateHealth.class);
		regPacket(SEntCreateMultiple.class);
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
				//CoreGame.instance().getConsole().print("INPUT (" + sender.getMyPlayer().getNetID() + ") " + input);
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
				world.registerNewPlayer(entID, p, false);
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
			world.registerNewPlayer(entID, p, true);
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
			getCSenderOrSMySelf().kill(s.readUTF());
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
			
			Client c = getCSenderOrSMySelf();
			
			Player that = c.getPlayer(netID);
			
			c.removeClient(netID);
			world.removeDisconnectedClient(netID);
			
			if (netID == 1) {
				// server is closed !
				c.kill(reason);
				return;
			}
			
			if (c.getMyPlayer().getNetID() == netID) {
				// holy crap that's me !
				c.kill(reason);
				return;
			}
			
			world.feedChat(-1, that.getUsername() + " left the game", false);
		}
	}
	
	public static class SEntCreate extends Packet {
		public int header() { return 7; }
		
		byte[] forwardedBytes;
		
		// when using an object
		Entity ent;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(ent.getID());
			s.writeUTF(ent.getClass().getName());
			
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
	
	public static class SEntDelete extends Packet {
		public int header() { return 8; }
		
		Entity ent;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(ent.getID());
		}
		public void read(DataInputStream s) throws IOException {
			world.deleteEntityInternal(s.readInt());
		}
	}
	
	public static class SEntPos extends Packet {
		public int header() { return 9; }
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
			ent.afterPosChange();
		}
	}
	
	public static class FPlayerState extends ForwardablePacket {
		public int header() { return 10; }
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
			
			float X = s.readFloat();
			float Y = s.readFloat();
			float aniIndex = s.readFloat();
			byte direction = s.readByte();
			boolean animating = s.readBoolean();
			
			if (getCSenderOrSMySelf().getMyPlayer().getNetID() != fromNetID) {
				ent.setPosition(X, Y);
				ent.setAnimationIndex(aniIndex);
				ent.setDirection(direction);
				ent.setAnimating(animating);
			}
			
			forward();
		}
	}
	
	// tells clients to start generating the dungeon
	public static class SGenLevel extends Packet {
		public int header() { return 11; }
		
		String mapName;
		int seed;
		int tilesetIndex;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeUTF(mapName);
			s.writeInt(seed);
			s.writeInt(tilesetIndex);
		}
		public void read(DataInputStream s) throws IOException {
			world.setLevelName(s.readUTF());
			world.generateMap(s.readInt(), s.readInt());
		}
	}
	
	// tells the server that our dungeon is done
	public static class CGenerateDone extends Packet {
		public int header() { return 12; }
		
		public void write(DataOutputStream s) throws IOException {}
		public void read(DataInputStream s) throws IOException {
			
		}
	}
	
	public static class CSendChat extends Packet {
		public int header() { return 13; }
		
		public String message;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeUTF(message);
		}
		public void read(DataInputStream s) throws IOException {
			// forward to clients
			getCServer().sendChat(getCSenderOrSMySelf().getMyPlayer().getNetID(), s.readUTF(), false);
		}
	}
	
	public static class SSendChat extends Packet {
		public int header() { return 14; }
		
		public String message;
		public int netID = -1;
		public int flashID = -1; // neg : no flashing, 0 : EVERYONE, otherwise : specific
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(netID);
			s.writeUTF(message);
			s.writeInt(flashID);
		}
		public void read(DataInputStream s) throws IOException {
			int netID = s.readInt();
			String t = s.readUTF();
			int flashID = s.readInt();
			world.feedChat(netID, t, flashID == 0 || flashID == getCSenderOrSMySelf().getMyPlayer().getNetID());
		}
	}
	
	public static class SEntUpdateHealth extends Packet {
		public int header() { return 15; }
		
		public int entID;
		public int newHealth;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(entID);
			s.writeInt(newHealth);
		}
		public void read(DataInputStream s) throws IOException {
			Entity e = world.getEntities(s.readInt());
			e.setHealth(s.readInt());
		}
	}
	
	// tell to clients to create a bunch of entities in one packet
	// do not let the server invoke this (by calling "broadcastExceptServer")
	public static class SEntCreateMultiple extends Packet {
		public int header() { return 16; }
		
		Entity[] ents;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(ents.length);
			for (int i = 0; i < ents.length; i++) {
				s.writeInt(ents[i].getID());
				s.writeUTF(ents[i].getClass().getName());
				ents[i].serializeConstructor(s);
			}
		}
		public void read(DataInputStream s) throws IOException {
			int count = s.readInt();
			for (int i = 0; i < count; i++) {
				Entity ent = world.createEntityAuthorized(s.readInt(), s.readUTF());
				ent.deserializeConstructor(s);
			}
		}
	}
}
