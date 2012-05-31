package agents;

import java.util.HashMap;

import ants.ClientPath;

import rinde.sim.core.graph.Point;

public class ResourceAgent {
	
	private Point node;
	private ClientPath bestClient;
	private HashMap<ClientAgent,ClientPath> clients;
	private HashMap<ClientAgent,ClientPath> deliveryLocs;
	private boolean explored;
	private int timeStep = 10000;
	private int limit = 7500;

	public ResourceAgent(Point node){
		this.node = node;
		this.explored = false;
		this.clients = new HashMap<ClientAgent, ClientPath>();
		this.deliveryLocs = new HashMap<ClientAgent, ClientPath>();
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
	
	public boolean exploredClient(ClientAgent client, long time){
		return clients.containsKey(client) && clients.get(client).getTime()+limit*timeStep>time;
	}
	
	public ClientPath getDeliveryPath(ClientAgent client) {
		return deliveryLocs.get(client);
	}
	
	public void setDeliveryPath(ClientAgent client, ClientPath path){
		deliveryLocs.put(client, path);
	}
	
	public boolean exploredDeliveryLoc(ClientAgent client, long time){
		return deliveryLocs.containsKey(client) && deliveryLocs.get(client).getTime()+limit*timeStep>time;
	}
	
	public boolean isExplored(long time) {
		return explored && bestClient.getTime()+limit*timeStep>time;
	}

	public void setExplored(boolean explored) {
		this.explored = explored;
	}

}
