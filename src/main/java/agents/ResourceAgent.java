package agents;

import java.util.HashMap;

import ants.ClientPath;

import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;

public class ResourceAgent{ //extends Agent implements TickListener {
	
	private Point node;
	private ClientPath bestClient;
	private HashMap<ClientAgent,ClientPath> clients;

	public ResourceAgent(Point node){
		this.node = node;
		this.clients = new HashMap<ClientAgent, ClientPath>();
	}
	
	public Point getNode() {
		return node;
	}

	public ClientPath getBestClient() {
		return bestClient;
	}

	public HashMap<ClientAgent, ClientPath> getClients() {
		return clients;
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
