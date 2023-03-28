
package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket;
import java.util.HashMap;

// yo server

public class Server extends Thread {
	private ServerSocket server;
	private int port;
	private CoreGame game;
	private Console console;
	
	private final static int MAX_PLAYER = 8;
	
	private boolean gameStarted = false;
	private boolean running = true;
	
	private HashMap<Integer, Client> clients;
	
	public Server(int port) {
		this.port = port;
		
		game = CoreGame.instance();
		console = game.getConsole();
		
		clients = new HashMap<Integer, Client>();
	}
	
	public void welcomeNewClient(Socket socket) {
		Client client = new Client(this, socket);
		
		if (gameStarted) {
			// NAH
			kickClient(client, "the game is starting");
			return;
		}
		if (clients.size() == MAX_PLAYER) {
			// SERVER IS FULL LMAO
			kickClient(client, "the server is full");
			return;
		}
		
		
		client.start();
		
		// wait for the client to send their info (CMyInfo)
	}
	
	public void newPlayerReceived(
		Client client, int netID,
		String name,
		int i1, int i2, int i3, int i4
	) {
		clients.put(netID, client);
		// and . . . send to all players
		Packet.SNewPlayer p = new Packet.SNewPlayer();
		p.entID = game.getWorld().allocateID();
		p.netID = netID;
		p.p = client.getMyPlayer();
		p.p.putData(netID, name, i1, i2, i3, i4);
		broadcast(p);
		
		
	}
	
	public void kickClient(Client client, String reason) {
		Packet.SKick p = new Packet.SKick();
		p.reason = reason;
		client.send(p);
	}
	
	public void broadcast(Packet packet) {
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			e.getValue().send(packet);
		}
	}
	public void broadcastExcept(Packet packet, Client client) {
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			if (e.getValue() == client) continue;
			e.getValue().send(packet);
		}
	}
	
	public void run() {
		try {
			console.print("Creating a server . . .");
			ServerSocketHints s = new ServerSocketHints();
			s.acceptTimeout = 0;
			server = Gdx.net.newServerSocket(Net.Protocol.TCP, port, s);
		} catch (Exception e) {
			console.printerr("Creating failed : " + e.getMessage());
			running = false;
		}
		
		while (running) {
			// Looking for new clients
			Socket socket = server.accept(new GameSocketHint());
			console.print("+CLIENT " + socket.getRemoteAddress());
			welcomeNewClient(socket);
		}
		
		server.dispose();
		console.print("Closing Server . . .");
		game.tellServerKilled();
	}
	
	public void kill() {
		running = false;
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			e.getValue().kill();
		}
		clients.clear();
		if (server != null) server.dispose();
	}
}
