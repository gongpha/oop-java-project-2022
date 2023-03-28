
package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;

// yo client

public class Client extends Thread {
	private Socket socket;
	private String address;
	private final boolean puppet;
	private ConnectionStatus status;
	private boolean running = true;
	
	private HashMap<Integer, Player> players;
	
	private Player player;
	private Character character;
	private Server server;
	
	// Client representation in the server
	public Client(Server server, Socket socket) {
		puppet = true;
		status = ConnectionStatus.CONNECTED;
		player = new Player(1);
		this.socket = socket;
		this.server = server;
		players = new HashMap<>();
	}
	
	// REAL CLIENT FOR CLIENTS
	public Client(String address, String username, int[] idents, Server server) {
		this.address = address;
		if (address.isEmpty())
			this.address = "localhost";
		puppet = false;
		status = ConnectionStatus.OFFLINE;
		player = new Player();
		this.server = server;
		player.putData(
			server != null ? 1 : new Random().nextInt(),
			username,
			idents[0],
			idents[1],
			idents[2],
			idents[3]
		);
		this.server = null;
		players = new HashMap<>();
	}
	
	public boolean isPuppet() {
		return puppet;
	}
	
	public boolean isServer() {
		return server != null;
	}
	
	public Player getMyPlayer() {
		return player;
	}
	public Player getPlayer(int netID) {
		return players.get(netID);
	}
	
	public void newPlayer(Player player) {
		players.put(player.getNetID(), player);
	}
	
	public void setCharacter(Character character) {
		this.character = character;
	}
	public Character getCharacter() { return character; }
	
	public void send(Packet packet) {
		try {
			// write the packet for sending
			
			OutputStream o = socket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(o);
			dos.writeByte(packet.header());
			packet.setCServer(server);
			packet.write(dos);
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
		}
	}
	
	///////////////
	
	public void run() {
		int retries = 0;
		CoreGame game = CoreGame.instance();
		Console console = game.getConsole();
		if (!puppet) {
			status = ConnectionStatus.CONNECTING;
			while (true) {
				try {
					console.print("Connecting to " + address + " . . .");
					socket = Gdx.net.newClientSocket(Net.Protocol.TCP, address, CoreGame.PORT, new GameSocketHint());
					status = ConnectionStatus.CONNECTED;
					console.print("Connected to the server " + address + " ! Sending my info . . .");
					game.tellConnectSuccess();
					// the world will invoke the sendMyInfo method
					break;
				} catch (Exception e) {
					retries += 1;
					console.print("Connecting failed (" + retries + ") : " + e.getMessage());
				}
			}
		}
		
		if (socket != null) {
			if (socket.isConnected()) {
				feedPacket();
			}
		}
		
	}
	
	public void sendMyInfo() {
		Packet.CMyInfo p = new Packet.CMyInfo();
		p.inClient = this;
		send(p);
	}
	
	public void feedPacket() {
		Console console = CoreGame.instance().getConsole();
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		
		while (running) {
			int res = -1;
			try {
				res = dis.read();
				if (res == -1)  { running = false; continue; } // Disconnected
				
				Class packetClass = Packet.getPacketFromHeader(res);
				Packet packet = (Packet) packetClass.getDeclaredConstructor().newInstance();
				packet.setCSenderOrSMySelf(this);
				if (puppet) packet.setCServer(server);
				packet.read(dis);
				
			} catch (Exception e) {
				// OOF
				StackTraceElement[] s = e.getStackTrace();
				console.printerr("buffer reading failed (" + res + ") : " + e.getMessage());
				console.showFull();
				running = false;
			}
		}
		
		
	}
	
	public void kill() {
		running = false;
		if (socket != null) socket.dispose();
	}
	
	////////////////////////////////////
	
	// used by clients
	public void updateInput(int newInput) {
		Packet.CInput p = new Packet.CInput();
		p.input = newInput;
		send(p);
		
		// also predict my pos
		applyInput(newInput);
	}
	
	// used by servers (or predicting)
	public void applyInput(int newInput) {
		character.setInput(newInput);
	}
	
	// used by servers ONLY
	public void updateEntPos(Entity ent, boolean predictable) {
		Packet.SEntPos p = new Packet.SEntPos();
		p.ent = ent;
		p.predictable = predictable;
		server.broadcast(p);
	}
}