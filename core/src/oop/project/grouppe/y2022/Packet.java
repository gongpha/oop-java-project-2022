package oop.project.grouppe.y2022;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

// packets for communication
// most of them are RPCs

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
		regPacket(SInitGame.class);
		regPacket(CSendChat.class);
		regPacket(SSendChat.class);
		//regPacket(SEntUpdateHealth.class);
		regPacket(SEntCreateMultiple.class);
		regPacket(SCharacterUpdatePowerup.class);
		regPacket(SPlayerScoreAdd.class);
		regPacket(CUpdateAtTheEntrance.class);
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
	// TODO : may get removed soon
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
				Character ch = e.getValue().getCharacter();
				Player p = e.getValue().getMyPlayer();
				s.writeInt(ch.getID());
				p.writeStream(s);
				ch.serializeConstructor(s);
			}
		}
		public void read(DataInputStream s) throws IOException {
			int clientCount = s.readInt();
			for (int i = 0; i < clientCount; i++) {
				Player p = new Player();
				int entID = s.readInt();
				p.readStream(s);
				getCSenderOrSMySelf().newPlayer(p);
				Character nch = world.registerNewPlayer(entID, p, false);
				nch.deserializeConstructor(s);
			}
			world.markReady();
		}
	}
	public static class SNewPlayer extends Packet {
		public int header() { return 4; }
		
		int entID;
		Player p;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(entID);
			p.writeStream(s);
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
			if (world != null)
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
			
			if (world != null)
				world.feedChat(-1, that.getUsername() + " left the game", false);
		}
	}
	
	// create one entity
	// not suitable for multiple objects. use SEntCreateMultiple instead
	public static class SEntCreate extends Packet {
		public int header() { return 7; }
		
		// when has no object.
		// "i would like to send a pure byte sequence"
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
	
	// used for syncing entity position
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
			//ent.afterPosChange();
		}
	}
	
	// used for syncing player properties i.e. animation state and position
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
			
			int myNetID = world.getMyClient().getMyPlayer().getNetID();
			
			if (myNetID != fromNetID) {
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
		int level;
		int seed;
		int tilesetIndex;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeUTF(mapName);
			s.writeInt(seed);
			s.writeInt(tilesetIndex);
			s.writeInt(level);
		}
		public void read(DataInputStream s) throws IOException {
			world.setLevelName(s.readUTF());
			world.generateMap(s.readInt(), s.readInt(), s.readInt());
		}
	}
	
	// tells the server that our dungeon is done
	public static class CGenerateDone extends Packet {
		public int header() { return 12; }
		
		public void write(DataOutputStream s) throws IOException {}
		public void read(DataInputStream s) throws IOException {
			world.pendingRemove(getCSenderOrSMySelf().getMyPlayer().getNetID());
		}
	}
	
	public static class SInitGame extends Packet {
		public int header() { return 13; }
		
		int maxPaperCount = 666;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(maxPaperCount);
		}
		public void read(DataInputStream s) throws IOException {
			maxPaperCount = s.readInt();
			world.initGamePuppet(maxPaperCount);
		}
	}
	
	public static class CSendChat extends Packet {
		public int header() { return 14; }
		
		public int netID = -1;
		public String message;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(netID);
			s.writeUTF(message);
		}
		public void read(DataInputStream s) throws IOException {
			// forward to clients
			netID = s.readInt();
			getCServer().sendChat(netID, s.readUTF(), -1);
		}
	}
	
	public static class SSendChat extends Packet {
		public int header() { return 15; }
		
		public String message;
		
		// -1 : system messages (yellow)
		// -2 : cheat notify (red and sound fx)
		// -3 : paper (green)
		// -4 : cheerful notify (completed a level, pink)
		public int netID = -1;
		public int flashID = -1; // neg : no flashing, 0 : EVERYONE, otherwise : specific
		// ^^^ TODO : wat about negative-number netIDs ???
		
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
	
	/*
	public static class SEntUpdateHealth extends Packet {
		public int header() { return 16; }
		
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
	}*/
	
	// tell to clients to create a bunch of entities in one packet
	// do not let the server invoke this (by calling "broadcastExceptServer")
	public static class SEntCreateMultiple extends Packet {
		public int header() { return 17; }
		
		Entity[] ents;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(ents.length);
			CoreGame.instance().getConsole().print("Sending " + ents.length + " entities");
			for (int i = 0; i < ents.length; i++) {
				s.writeInt(ents[i].getID());
				s.writeUTF(ents[i].getClass().getName());
				ents[i].serializeConstructor(s);
			}
		}
		public void read(DataInputStream s) throws IOException {
			int count = s.readInt();
			Console cs = CoreGame.instance().getConsole();
			cs.print("Received " + count + " entities");
			for (int i = 0; i < count; i++) {
				int entID = s.readInt();
				String c = s.readUTF();
				//cs.print("    - " + c);
				Entity ent = world.createEntityAuthorized(entID, c);
				ent.deserializeConstructor(s);
			}
		}
	}
	
	public static class SCharacterUpdatePowerup extends Packet {
		public int header() { return 18; }
		
		Character target;
		
		// 0 = protecc
		// 1 = faster
		// . = MORE SOON !
		char powerup;
		
		// 0 = begin
		// 1 = about to end
		// 2 = end
		char tell;
		
		// status texture region
		int x = -1;
		int y = -1;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(target.getPlayer().getNetID());
			s.writeChar(powerup);
			s.writeChar(tell);
			s.writeInt(x);
			s.writeInt(y);
		}
		public void read(DataInputStream s) throws IOException {
			int netID = s.readInt();
			powerup = s.readChar();
			tell = s.readChar();
			x = s.readInt();
			y = s.readInt();
			Character target = world.getCharacterByNetID(netID);
			target.updatePowerup(powerup, tell, x, y);
		}
	}
	
	public static class SPlayerScoreAdd extends Packet {
		public int header() { return 19; }
		
		Character ch;
		int playerCurrentPaperCount = -1;
		int currentPaperCount = -1;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(ch.getPlayer().getNetID());
			s.writeInt(playerCurrentPaperCount);
			s.writeInt(currentPaperCount);
		}
		public void read(DataInputStream s) throws IOException {
			int netID = s.readInt();
			playerCurrentPaperCount = s.readInt();
			currentPaperCount = s.readInt();
			world.addCollectedPaperCountPuppet(netID, playerCurrentPaperCount, currentPaperCount);
		}
	}
	
	public static class CUpdateAtTheEntrance extends Packet {
		public int header() { return 20; }
		
		boolean yes;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeBoolean(yes);
		}
		public void read(DataInputStream s) throws IOException {
			yes = s.readBoolean();
			world.updateAtTheEntrance(getCSenderOrSMySelf().getMyPlayer().getNetID(), yes);
		}
	}
	
	public static class SCharacterDied extends Packet {
		public int header() { return 21; }
		
		int netID = -1;
		
		public void write(DataOutputStream s) throws IOException {
			s.writeInt(netID);
		}
		public void read(DataInputStream s) throws IOException {
			world.tellCharacterDied(s.readInt());
		}
	}
}
