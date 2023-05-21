
package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.Socket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

// yo client

public class Client extends Thread {
	private Socket socket;
	private String address;
	private final boolean puppet;
	private String disconnectReason = "Unknown reason";
	private boolean running = true;
	
	private HashMap<Integer, Player> players;
	
	private Player player;
	private Character character;
	private final Server server;
	
	// incoming packets
	// pump and invoke by the main thread's frames. NOT THE SOCKET THREAD
	private LinkedList<Packet> pump;
	
	// Client representation in the server
	public Client(Server server, Socket socket) {
		this.pump = new LinkedList<>();
		puppet = true;
		player = new Player(1);
		this.socket = socket;
		this.server = server;
		players = new HashMap<>();
	}
	
	// REAL CLIENT FOR CLIENTS
	public Client(String address, String username, int[] idents, Server server) {
		this.pump = new LinkedList<>();
		this.address = address;
		if (address.isEmpty())
			this.address = "localhost";
		puppet = false;
		player = new Player();
		this.server = server;
		
		int netID = server != null ? 1 : new Random().nextInt();
		
		if (username.isEmpty()) {
			// use placeholder username
			// (server1, client89343229, client902871, client908414, ...)
			if (server == null) {
				username = "client" + netID; // client . . .
			} else {
				username = "server" + netID; // server1
			}
		}
		
		player.putData(
			netID,
			username,
			idents[0],
			idents[1],
			idents[2],
			idents[3]
		);
		players = new HashMap<>();
	}
	
	// fake client for demo playing
	public Client() {
		socket = null;
		player = null;
		server = null;
		puppet = false;
		players = new HashMap<>();
	}
	
	public boolean isPuppet() {
		return puppet;
	}
	
	public boolean isServer() {
		return server != null;
	}
	
	public Server getServer() {
		return server;
	}
	
	public Player getMyPlayer() {
		return player;
	}
	public Player getPlayer(int netID) {
		return players.get(netID);
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void newPlayer(Player player) {
		players.put(player.getNetID(), player);
	}
	
	public void setCharacter(Character character) {
		this.character = character;
	}
	public Character getCharacter() { return character; }
	
	public synchronized void send(Packet packet) {
		if (socket == null) return;
		try {
			// write the packet for sending
			// HEADER(byte) + SIZEOFDATA(int) + DATA(sizeof(SIZEOFDATA))
			
			OutputStream o = socket.getOutputStream();
			
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(ba);
			
			
			packet.setCServer(server);
			packet.setCSenderOrSMySelf(this);
			packet.write(dos);
			dos.flush();
			
			DataOutputStream doso = new DataOutputStream(o);
			doso.writeByte(packet.header());
			doso.writeInt(ba.size());
			doso.write(ba.toByteArray());
			
			
			o.flush();
		} catch (Exception e) {
			// nooooooooo
			Console console = CoreGame.instance().getConsole();
			console.printerr(
				(puppet) ?
					("Sending packet to client failed (" + packet.header() + "): " + e.getMessage())
					:
					("Sending packet to client server (" + packet.header() + "): " + e.getMessage())
			);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			console.printerr(sw.toString());
		}
	}
	
	///////////////
	
	public void run() {
		int retries = 0;
		CoreGame game = CoreGame.instance();
		Console console = game.getConsole();
		if (!puppet) { // their own client
			while (true) {
				try {
					console.print("Connecting to " + address + " . . .");
					socket = Gdx.net.newClientSocket(Net.Protocol.TCP, address, CoreGame.PORT, new GameSocketHint());
					console.print("Connected to the server " + address + " ! Sending my info . . .");
					game.tellConnectSuccess();
					// the world will invoke the sendMyInfo method
					break;
				} catch (Exception e) {
					retries += 1;
					//console.print("Connecting failed (" + retries + ") : " + e.getMessage());
					console.print("Connecting failed : " + e.getMessage());
					disconnectReason = e.getMessage();
					break;
					/*
					if (retries >= 4) {
						console.print("Can't connect after 4 retries");
						kill("Can't connect after 4 retries");
						break;
					}*/
				}
			}
		}
		
		if (socket != null) {
			if (socket.isConnected()) {
				feedPacket();
			}
		}
		
		// no more packets ?
		// byebye
		if (puppet) {
			server.removeClient(this);
			
			// Tell everyone
			Packet.SDisconnectPlayer p = new Packet.SDisconnectPlayer();
			int netID = getMyPlayer().getNetID();
			p.netID = netID;
			p.reason = disconnectReason;
			server.broadcast(p);
			console.print("Client " + netID + " is disconnected (" + disconnectReason + ")");
		} else {
			game.tellDisconnected(disconnectReason);
		}
	}
	
	public void sendMyInfo() {
		Packet.CMyInfo p = new Packet.CMyInfo();
		p.inClient = this;
		send(p);
	}
	
	public void feedPacket() {
		CoreGame game = CoreGame.instance();
		Console console = game.getConsole();
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		
		while (running) {	
			int res = -1;
			try {
				res = dis.read();
				
				// read the header first
				if (res == -1)  {
					if (puppet)
						kill("Failed to reach the client");
					else
						kill("Failed to reach the server");
					break;
				}
				// read the size of the packet
				int size = dis.readInt();
				// read the body
				byte[] bytes = dis.readNBytes(size);
				
				// instantiate and add to pump
				Class packetClass = Packet.getPacketFromHeader(res);
				Packet packet = (Packet) packetClass.getDeclaredConstructor().newInstance();
				packet.putDeferredData(bytes);
				pump.add(packet);
				
				/* INVOKE LATER BY EVENT PUMP */
				
				game.tellReceivedPacket();
				
			} catch (java.net.SocketException e) {
				// closed BYEBYE
				running = false;
				break;
			} catch (Exception e) {
				// OOF
				console.printerr("buffer reading failed (" + res + ") : " + e.getMessage());
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				console.printerr(sw.toString());
				System.err.println(sw.toString());
				console.showFull();
				kill("Failed to read a packet");
				CoreGame.instance().tellDisconnected(disconnectReason);
				interrupt();
			}
		}
	}
	
	// called by the main thread's frame
	public void pumpPackets() {
		while (!pump.isEmpty()) {
			Packet p = pump.pollFirst();
			try {
				p.setCSenderOrSMySelf(this);
				if (puppet) p.setCServer(server);
				p.invoke();
			} catch (Exception e) {
				CoreGame.instance().getConsole().printerr("Cannot invoke packet (" + p.header() + ")" + e.getMessage());
			}
		}
	}
	
	public void kill(String reason) {
		//pumpPackets(); // pump all packets before it closes (must be in the main thread)
		disconnectReason = reason;
		running = false;
		if (socket != null) socket.dispose();
	}
	
	public void disconnectMe() {
		if (server != null) {
			server.kill();
			return;
		}
		Packet.CRequestDisconnect p = new Packet.CRequestDisconnect();
		p.reason = "Disconnected by user";
		send(p);
	}
	
	////////////////////////////////////
	
	// used by servers (or predicting)
	public void applyInput(int newInput) {
		character.setInput(newInput);
	}
	
	// used by servers ONLY
	public void updateEntPos(Entity ent) {
		Packet.SEntPos p = new Packet.SEntPos();
		p.ent = ent;
		server.broadcastExceptServer(p);
	}
	
	// used by clients (and the server's client)
	public void updateMyPlayerState() {
		Packet.FPlayerState p = new Packet.FPlayerState();
		p.ent = getCharacter();
		send(p);
	}
	
	public void removeClient(int netID) {
		players.remove(netID);
	}
}
