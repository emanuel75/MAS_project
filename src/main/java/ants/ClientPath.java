package ants;

import java.util.Queue;

import agents.ClientAgent;

import rinde.sim.core.graph.Point;

public class ClientPath {
	
	private Queue<Point> path;
	private double travelTime;
	private ClientAgent client;
	private long time;
	private boolean disappeared;
	private boolean onPath;

	public ClientPath(Queue<Point> path, double travelTime, ClientAgent client, long time) {
		this.path = path;
		this.travelTime = travelTime;
		this.client = client;
		this.time = time;
		this.disappeared = false;
		this.onPath = true;
	}
	
	public Queue<Point> getPath() {
		return path;
	}
	
	public double getTravelTime() {
		return travelTime;
	}
	
	public void setTravelTime(double travelTime){
		this.travelTime = travelTime;
	}
	
	public ClientAgent getClient() {
		return client;
	}
	
	public void setPath(Queue<Point> path) {
		this.path = path;
	}
	
	public long getTime(){
		return time;
	}
	
	public void setTime(long time){
		this.time = time;
	}
	
	public boolean isDisappeared(){
		return this.disappeared;
	}
	
	public void setDisappeared(){
		this.disappeared = true;
	}
	
	public boolean isOnPath(){
		return this.onPath;
	}
	
	public void setOffPath(){
		this.onPath = false;
	}

}
