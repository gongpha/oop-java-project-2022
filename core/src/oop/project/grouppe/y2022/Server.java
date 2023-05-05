
package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket; // it's just a regular Socket class.
import java.util.ArrayList;
import java.util.HashMap;

// yo server

public class Server extends Thread {
	private ServerSocket server;
	private final int port;
	private final CoreGame game;
	private final Console console;
	
	private final static int MAX_PLAYER = 8;
	
	private boolean running = false;
	
	private final ArrayList<Client> createdClients; // created PUPPET clients (include pending and connected)
	private final HashMap<Integer, Client> clients; // connected clients
	
	public Server(int port) {
		this.port = port;
		
		game = CoreGame.instance();
		console = game.getConsole();
		
		createdClients = new ArrayList<Client>();
		clients = new HashMap<>();
	}
	
	private void welcomeNewClient(Socket socket) {
		Client client = new Client(this, socket);
		
		World world = Packet.world;
		
		if (world != null && !world.isInLobby()) {
			// NAH
			kickClient(client, "the game is starting");
			return;
		}
		if (clients.size() == MAX_PLAYER) {
			// SERVER IS FULL LMAO
			kickClient(client, "the server is full");
			return;
		}
		
		createdClients.add(client);
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
		console.print("Sending a sync state " + client.getMyPlayer().getNetID());
		
		clients.put(netID, client);
		
		client.getMyPlayer().putData(netID, name, i1, i2, i3, i4);
		console.print("Registered Player " + netID);
		
		// and . . . send to all players (this client too. becuz it includes an entID)
		Packet.SNewPlayer p = new Packet.SNewPlayer();
		p.entID = game.getWorld().allocateID();
		p.p = client.getMyPlayer();
		broadcast(p);
		console.print("Broadcasting Player " + netID + " to everyone");
		
		
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
	public void broadcastExceptServer(Packet packet) {
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			if (e.getValue().getMyPlayer().getNetID() == 1) continue;
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
	
	public void sendChat(int netID, String text, int flashID) {
		Packet.SSendChat p = new Packet.SSendChat();
		p.message = text;
		p.flashID = flashID;
		p.netID = netID;
		broadcast(p);
	}
	
	public void sendChatToClient(int netID, String text, boolean flash) {
		Packet.SSendChat p = new Packet.SSendChat();
		p.message = text;
		p.flashID = flash ? netID : -1;
		
		Client c = getClient(netID);
		c.send(p);
	}
	
	public void sendChatAndFlashesClient(int netID, String text, int flashID) {
		// like sendChat but also flashes the target netID
		Packet.SSendChat p = new Packet.SSendChat();
		p.message = text;
		p.netID = netID;
		p.flashID = flashID;
		broadcast(p);
	}
	
	public void run() {
		try {
			console.print("Creating a server . . .");
			ServerSocketHints s = new ServerSocketHints();
			s.acceptTimeout = 0;
			server = Gdx.net.newServerSocket(Net.Protocol.TCP, port, s);
			running = true;
			game.tellServerCreated();
		} catch (Exception e) {
			console.printerr("Creating failed : " + e.getMessage());
			running = false;
		}
		
		while (running) {
			// Looking for new clients
			try {
				Socket socket = server.accept(new GameSocketHint());
				console.print("+CLIENT " + socket.getRemoteAddress());
				welcomeNewClient(socket);
			} catch (com.badlogic.gdx.utils.GdxRuntimeException e) {
				// closed
				break;
			}
			
		}
		
		if (server != null) {
			server.dispose();
		}
		game.tellDisconnected("Server closed by user");
	}
	
	public void pumpPackets() {
		for (int i = 0; i < createdClients.size(); i++) {
			createdClients.get(i).pumpPackets();
		}
	}
	
	public void kill() {
		if (server == null) return;
		
		// tell everyone
		Packet.SDisconnectPlayer p = new Packet.SDisconnectPlayer();
		p.netID = 1;
		p.reason = "Server is closed";
		broadcast(p);
		
		running = false;
		for (Client c : createdClients) {
			c.kill("Server closed");
		}
		clients.clear();
		createdClients.clear();
		if (server != null) {
			server.dispose();
			server = null;
		}
	}
}
