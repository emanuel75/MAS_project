package ants;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import rinde.sim.core.graph.Point;
import agents.Agent;
import agents.ClientAgent;
import agents.ResourceAgent;

public class FeasibilityAnt extends Ant {
	
	private HashSet<Point> visitedNodes;
	
	public FeasibilityAnt(Agent agent, Point startLocation, Mode mode, long time){
		super(agent,startLocation,mode,time);
		visitedNodes = new HashSet<Point>();
	}
	
	private void setResource(Point node, Point prevoiusNode, double travelTime){
		ClientAgent client = (ClientAgent) agent;
		ResourceAgent res = client.getResource(node);
		Queue<Point> path = new LinkedList<Point>();
		path.add(prevoiusNode);
		ClientPath myPath = new ClientPath(path, travelTime, client, time);
		if(mode.equals(Mode.EXPLORE_PACKAGES) && (!res.isExplored(time) || !res.exploredClient(client,time) ||
			res.getClientPath(client).getTravelTime() > travelTime)){
			res.setClientPath(client, myPath);
			if(!res.isExplored(time) || res.getBestClient().getTravelTime() > travelTime){
				res.setBestClient(myPath);
			}
			res.setExplored(true);
		}
		else if(mode.equals(Mode.EXPLORE_DELIVERY_LOC) && (!res.exploredDeliveryLoc(client,time) || 
			res.getDeliveryPath(client).getTravelTime() > travelTime)){
			res.setDeliveryPath(client, myPath);
		}		
	}
	
	public void notifyNeighbours(){
		this.notifyNeighbours(startLocation,startLocation,0);
	}
	
	private void notifyNeighbours(Point node, Point previousNode, double travelTime){
		setResource(node,previousNode,travelTime);
		visitedNodes.add(node);
		
		Collection<Point> neighbours = rm.getGraph().getIncomingConnections(node);
		Iterator<Point> it = neighbours.iterator();
		double newTravelTime;
		double nextNodeTime;
		ClientAgent client = (ClientAgent) agent;
		while(it.hasNext()){
			Point nextNode = it.next();
			newTravelTime = travelTime + computeTravelTime(nextNode, node);
			if(mode.equals(Mode.EXPLORE_PACKAGES) && !client.getResource(nextNode).exploredClient(client,time) ||
				mode.equals(Mode.EXPLORE_DELIVERY_LOC) && !client.getResource(nextNode).exploredDeliveryLoc(client,time)){
				nextNodeTime = -1;
			}
			else{
				nextNodeTime = mode.equals(Mode.EXPLORE_PACKAGES) ? client.getResource(nextNode).getClientPath(client).getTravelTime() : client.getResource(nextNode).getDeliveryPath(client).getTravelTime();
			}
			if((!visitedNodes.contains(nextNode) ||  nextNodeTime==-1 || nextNodeTime>newTravelTime) && newTravelTime < 10){
				notifyNeighbours(nextNode,node,newTravelTime);
			}
		}
	}

}
