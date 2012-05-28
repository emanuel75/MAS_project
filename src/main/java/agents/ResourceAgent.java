package agents;

import java.util.HashMap;

import ants.ClientPath;

import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;

public class ResourceAgent{ //extends Agent implements TickListener {
	
	private Point node;
	private ClientPath bestClient;
	private HashMap<ClientAgent,ClientPath> clients;
	private boolean explored;

	public ResourceAgent(Point node){
		this.node = node;
		this.explored = false;
		this.clients = new HashMap<ClientAgent, ClientPath>();
	}
	
	public Point getNode() {
		return node;
	}

	public ClientPath getBestClient() {
		return bestClient;
	}
	
	public void setBestClient(ClientPath clientPath){
		this.bestClient = clientPath;
	}

	public ClientPath getClientPath(ClientAgent client) {
		return clients.get(client);
	}
	
	public void setClientPath(ClientAgent client, ClientPath path){
		clients.put(client, path);
	}
	
	public boolean exploredClient(ClientAgent client){
		return clients.containsKey(client);
	}
	
	public boolean isExplored() {
		return explored;
	}

	public void setExplored(boolean explored) {
		this.explored = explored;
	}

//	@Override
//	public void tick(long currentTime, long timeStep) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void afterTick(long currentTime, long timeStep) {
//		// TODO Auto-generated method stub
//		
//	}

}
