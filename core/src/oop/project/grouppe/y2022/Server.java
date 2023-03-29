
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
	
	public Client getClient(int netID) {
		return clients.get(netID);
	}
	
	public HashMap<Integer, Client> dumpClients() {
		return clients;
	}
	
	public void newPlayerReceived(
		Client client, int netID,
		String name,
		int i1, int i2, int i3, int i4
	) {
		// send everyone to the new player
		Packet.SSyncState P = new Packet.SSyncState();
		client.send(P);
		CoreGame.instance().getConsole().print("Sending a sync state " + client.getMyPlayer().getNetID());
		
		clients.put(netID, client);
		client.getMyPlayer().putData(netID, name, i1, i2, i3, i4);
		CoreGame.instance().getConsole().print("Registered Player " + netID);
		
		// and . . . send to all players (this client too. becuz it includes an entID)
		Packet.SNewPlayer p = new Packet.SNewPlayer();
		p.entID = game.getWorld().allocateID();
		p.netID = netID;
		p.p = client.getMyPlayer();
		broadcast(p);
		CoreGame.instance().getConsole().print("Broadcasting Player " + netID + " to everyone");
		
		
	}
	
	public void kickClient(Client client, String reason) {
		Packet.SDisconnectPlayer p = new Packet.SDisconnectPlayer();
		p.reason = reason;
		p.netID = client.getMyPlayer().getNetID();
		client.send(p);
	}
	
	public void removeClient(Client client) {
		clients.remove(client.getMyPlayer().getNetID());
	}
	
	public void broadcast(Packet packet) {
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			e.getValue().send(packet);
		}
	}
	public void broadcastExcept(Packet packet, Client client) {
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			if (e.getValue().getMyPlayer().getNetID() == client.getMyPlayer().getNetID()) continue;
			e.getValue().send(packet);
		}
	}
	public void broadcastExceptAndServer(Packet packet, Client client) {
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			int netID = e.getValue().getMyPlayer().getNetID();
			if (netID == 1) continue;
			if (netID == client.getMyPlayer().getNetID()) continue;
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
		// tell everyone
		Packet.SDisconnectPlayer p = new Packet.SDisconnectPlayer();
		p.netID = 1;
		p.reason = "Server is closed";
		broadcast(p);
		
		running = false;
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			e.getValue().kill("Server closed");
		}
		clients.clear();
		if (server != null) server.dispose();
	}
}
