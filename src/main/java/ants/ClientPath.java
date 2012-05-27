package ants;

import java.util.Queue;

import agents.ClientAgent;

import rinde.sim.core.graph.Point;

public class ClientPath {
	
	private Queue<Point> path;
	private double travelTime;
	private ClientAgent client;

	public ClientPath(Queue<Point> path, double travelTime, ClientAgent client) {
		this.path = path;
		this.travelTime = travelTime;
		this.client = client;
	}
	
	public Queue<Point> getPath() {
		return path;
	}
	
	public double getTravelTime() {
		return travelTime;
	}
	
	public ClientAgent getClient() {
		return client;
	}
	
	public void setPath(Queue<Point> path) {
		this.path = path;
	}

}
