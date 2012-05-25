package ants;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import agents.ClientAgent;
import agents.TaxiAgent;

import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.lab.common.trucks.Truck;

public class ExplorationAnt extends Ant {
	
	private HashSet<ClientAgent> clients;
	private Queue<Point> visitedNodes;
	private double travelTime;
		
	public ExplorationAnt(TaxiAgent taxi, Point startLocation, HashSet<ClientAgent> clients){
		super(taxi,startLocation);
		this.clients = clients;
		this.visitedNodes = new LinkedList<Point>();
		this.travelTime = 0;
	}
	
	public ExplorationAnt(TaxiAgent taxi, Point startLocation, HashSet<ClientAgent> clients, Queue<Point> visitedNodes, double travelTime){
		this(taxi,startLocation,clients);
		this.travelTime = travelTime;
		this.visitedNodes = visitedNodes;
	}
	
	private double computeTravelTime(Point start, Point stop){
		if(start.equals(stop)){
			return 0;
		}
		double maxSpeed;
		EdgeData data = rm.getGraph().connectionData(start, stop);
		double distance = data.getLength();
		Truck truck = ((TaxiAgent) agent).getTruck();
		if (data instanceof MultiAttributeEdgeData) {
			MultiAttributeEdgeData maed = (MultiAttributeEdgeData) data;
			double speed = maed.getMaxSpeed();
			maxSpeed = Double.isNaN(speed) ? truck.getSpeed() : Math.min(speed, truck.getSpeed());
		}
		else{
			maxSpeed = truck.getSpeed();
		}
		return distance/maxSpeed;
	}
	
	public ClientPath lookForClient(Point next){
		Collection<Point> neighbours = rm.getGraph().getOutgoingConnections(next);
		Iterator<Point> it;
		Iterator<ClientAgent> cl;
		Point myWay = next;
		Point previousNode = startLocation;
		Point nextNode;
		boolean deadend = false;
		ExplorationAnt newAnt;
		ClientAgent client;
		ClientPath antPath;
		ClientPath bestPath = null;
//		System.out.println(visitedNodes.size());
		while(!deadend && !visitedNodes.contains(myWay)){	
//			System.out.println(myWay);
			if(!previousNode.equals(myWay)){
				visitedNodes.addAll(rm.getShortestPathTo(previousNode, myWay));
				previousNode = myWay;
			}
			else{
				visitedNodes.add(myWay);
			}
			cl = clients.iterator();
			while(cl.hasNext()){
				client = cl.next();
				if(rm.containsObjectAt(client.getClient(), myWay)){
					if(bestPath==null || travelTime<bestPath.getTravelTime()){
						return new ClientPath(visitedNodes, travelTime, client);
					}
					else{
						return bestPath;
					}
				}
			}
			it = neighbours.iterator();
			if(it.hasNext()){
				myWay = it.next();
				travelTime += computeTravelTime(previousNode,myWay);
			}
			else{
				deadend = true;
			}
			while(it.hasNext()){
				nextNode = it.next();
				newAnt = new ExplorationAnt((TaxiAgent) agent, previousNode, clients, visitedNodes, travelTime+computeTravelTime(previousNode,nextNode));
				newAnt.initRoadUser(rm);
				antPath = newAnt.lookForClient(nextNode);
				if(antPath != null && (bestPath==null || bestPath.getTravelTime()>antPath.getTravelTime())){
					if (bestPath!=null)
						System.out.println(bestPath.getClient().getClient().packageID + ": " +bestPath.getTravelTime() + ", " + bestPath.getPath().size());
					bestPath = antPath;
				}
			}
			neighbours = rm.getGraph().getOutgoingConnections(myWay);
			previousNode = myWay;
		}
		return bestPath;
	}

}
