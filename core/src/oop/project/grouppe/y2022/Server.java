
package oop.project.grouppe.y2022;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Server extends Thread {
	private ServerSocket server;
	private int port;
	private CoreGame game;
	private Console console;
	private World world;
	
	private final static int MAX_PLAYER = 8;
	
	private boolean gameStarted = false;
	private boolean running = true;
	
	private HashMap<Integer, Client> clients;
	
	public Server(int port) {
		this.port = port;
		
		game = CoreGame.instance();
		console = game.getConsole();
		world = game.getWorld();
		
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
	
	public void newPlayerReceived(Client client, int netID) {
		clients.put(netID, client);
		// and . . . send to all players
		Packet.SNewPlayer p = new Packet.SNewPlayer();
		p.netID = netID;
		p.p = client.getMyPlayer();
		broadcastExcept(p, client);
		
		world.newCharacter(client.getMyPlayer());
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
			server = Gdx.net.newServerSocket(Net.Protocol.TCP, port, new ServerSocketHints());
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
		for (HashMap.Entry<Integer, Client> e : clients.entrySet()) {
			e.getValue().kill();
		}
		clients.clear();
		running = false;
		if (server != null) server.dispose();
	}
}
