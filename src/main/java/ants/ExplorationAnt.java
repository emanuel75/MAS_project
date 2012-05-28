package ants;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;

import agents.ClientAgent;
import agents.ResourceAgent;
import agents.TaxiAgent;

import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.lab.common.trucks.Truck;

public class ExplorationAnt extends Ant {
	
	private HashSet<ClientAgent> clients;
	private Queue<Point> visitedNodes;
	private List<Point> oldVisitedNodes;
	private double minDistance=-1;
	private boolean mainAnt;
	private HashMap<Point,ClientPath> paths;
	private ResourceAgent res;
		
	public ExplorationAnt(TaxiAgent taxi, Point startLocation, HashSet<ClientAgent> clients){
		super(taxi,startLocation);
		this.clients = clients;
		this.visitedNodes = new LinkedList<Point>();
		this.oldVisitedNodes = new LinkedList<Point>();
		this.mainAnt = true;
		this.paths = new HashMap<Point, ClientPath>();
		this.res = taxi.getResource(startLocation);
	}
	
	public ExplorationAnt(TaxiAgent taxi, Point startLocation, double minDistance, HashSet<ClientAgent> clients, List<Point> oldVisitedNodes, HashMap<Point,ClientPath> paths){
		this(taxi,startLocation,clients);
		this.oldVisitedNodes = oldVisitedNodes;
		this.minDistance = minDistance;
		this.mainAnt = false;
		this.paths = paths;
	}
	
	/**
	 * 
	 * @return The minimum distance from the closest client accoring to the A* search (rm.getShortestPathTo)
	 */
	private double computeMinDistance(){
		double myMinDistance = -1;
		Iterator<ClientAgent> it = clients.iterator();
		while(it.hasNext()){
			double clientDistance = Graphs.pathLength(rm.getShortestPathTo(startLocation, it.next().getPosition()));
			if(myMinDistance==-1 || myMinDistance>clientDistance){
				myMinDistance = clientDistance;
			}
		}
		return myMinDistance;
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		super.initRoadUser(model);
		if(mainAnt){
			minDistance = computeMinDistance(); 
		}
	}
	
	/**
	 * 
	 * @param start
	 * @param stop
	 * @return The travel time between two nodes
	 */
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
	
	/**
	 * Dynamic programming algorithm
	 * @return The shortest path from the startLocation leading to a client
	 */
	public ClientPath lookForClient(){
//		System.out.println(startLocation);
		// if the shortest path from this node is already known, it will be returned
//		if(paths.containsKey(startLocation)){
//			Queue<Point> completePath = new LinkedList<Point>();
//			completePath.addAll(oldVisitedNodes);
//			completePath.addAll(paths.get(startLocation).getPath());
//			return new ClientPath(completePath, paths.get(startLocation).getTravelTime(), paths.get(startLocation).getClient());
//		}
		
		Collection<Point> neighbours = rm.getGraph().getOutgoingConnections(startLocation);
		Iterator<Point> it;
		Iterator<Double> nodes;
		Iterator<ClientAgent> cl;
		Point nextNode;
		ExplorationAnt newAnt;
		ClientAgent client;
		ClientPath antPath;
		ClientPath bestPath = null;
		ResourceAgent nextRes;
		
		//The oldVisitNodes containes the nodes were visited earlier on this path
		visitedNodes.addAll(oldVisitedNodes);
		visitedNodes.add(startLocation);

		//If there is a client here, we return visitedNodes which containes the nodes were visited during the path leading here
		cl = clients.iterator();
		while(cl.hasNext()){
			client = cl.next();
			if(rm.containsObjectAt(client.getClient(), startLocation)){
				ClientPath myPath = new ClientPath(new LinkedList<Point>(), 0, client); 
				res.setBestClient(myPath);
				res.setClientPath(client, myPath);
				res.setExplored(true);
				return new ClientPath(visitedNodes, 0, client);
			}
		}
		
		
		double myMinDistance = computeMinDistance();
		//We have to control how far the ants can go, because the computation time would be too much otherwise
		//The minDistance is the minimum distance between the initial startLocation and the closest client returned by Graphs.getShortestPathTo which uses an A* search with Eucledian heuristics
		//myMinDistance is the same distance, but not from the initial startLocation but from the current position
		if(Graphs.pathLength((LinkedList<Point>) visitedNodes)+myMinDistance<=1.5*minDistance){
			it = neighbours.iterator();
			TreeMap<Double,Point> options = new TreeMap<Double, Point>();
			double notExplored = 0;
			double newTime;
			while(it.hasNext()){
				nextNode = it.next();
				nextRes = ((TaxiAgent) agent).getResource(nextNode);
				if(!visitedNodes.contains(nextNode) /*&& !nextNode.equals(new Point(3304702.8030133694,2.5709013414073147E7))*/){
					notExplored--;
					if(!nextRes.isExplored()){
						options.put(notExplored,nextNode);
					}
					else if(clients.contains(nextRes.getBestClient().getClient())){
						newTime = computeTravelTime(startLocation,nextNode) + nextRes.getBestClient().getTravelTime();
						if(options.containsKey(newTime)){
							options.put(newTime+0.001, nextNode);
						}
						else{
							options.put(newTime, nextNode);
						}
					}
					else{
						double bestTime = notExplored;
						cl = clients.iterator();
						while(cl.hasNext()){
							client = cl.next();
							if(nextRes.exploredClient(client)){
								newTime = computeTravelTime(startLocation,nextNode) + nextRes.getClientPath(client).getTravelTime();
								if(bestTime<0 || newTime<bestTime){
									bestTime = newTime;
								}
							}
						}
						options.put(bestTime, nextNode);
					}
				}
			}
			
			nodes = options.keySet().iterator();
			Double nextTime = null;
			if(nodes.hasNext()){
				nextTime = nodes.next();
			}
			while(nextTime!=null && (bestPath==null || nextTime<bestPath.getTravelTime())){
				nextNode = options.get(nextTime);
				oldVisitedNodes = new LinkedList<Point>();
				oldVisitedNodes.addAll(visitedNodes);
				//We create a new ant which would do the same thing than this one, but from the next neighbor
				//Thus it will return the best path from the next node, here we have to decide
				//which neighbor is the best taking into account the travelTime of the best path from the neighbor
				//and the travelTime leading to the neighbor
				newAnt = new ExplorationAnt((TaxiAgent) agent, nextNode, minDistance, clients, oldVisitedNodes, paths);
				newAnt.initRoadUser(rm);
				antPath = newAnt.lookForClient();
				//If the created ant found a path and it is better than the path found so far, we store it in the bestPath
				if(antPath != null){
//					if(startLocation.equals(new Point(3304221.306136328,2.5708335364835046E7)))
//						System.out.println(nextNode + ": "  + Graphs.pathLength((LinkedList<Point>) antPath.getPath()));
					double newTravelTime = antPath.getTravelTime()+computeTravelTime(startLocation,nextNode);
					if(mainAnt)
						System.out.println("TravelTime: " + newTravelTime + ", " + antPath.getPath().size() + " --> " + antPath.getClient().getPosition());
					if(bestPath==null || bestPath.getTravelTime()>newTravelTime){
						bestPath = new ClientPath(antPath.getPath(), newTravelTime, antPath.getClient());
					}
				}
				
				if(nodes.hasNext()){
					nextTime = nodes.next();
				}
				else{
					nextTime = null;
				}
			}
		}
		
		//If we found a path from this node, we store is so that we can use it later
//		if(bestPath != null && Graphs.pathLength((LinkedList<Point>) bestPath.getPath())<=1.2*minDistance){
//			LinkedList<Point> myPath = (LinkedList<Point>) bestPath.getPath();
//			Queue<Point> myPathQ = new LinkedList<Point>();
//			myPathQ.addAll(myPath.subList(myPath.lastIndexOf(startLocation), myPath.size()));
//			paths.put(startLocation, new ClientPath(myPathQ, bestPath.getTravelTime(), bestPath.getClient()));
//		}
		
		if(bestPath!=null){
			if(!res.isExplored() || !res.exploredClient(bestPath.getClient()) ||
				res.getClientPath(bestPath.getClient()).getTravelTime() > bestPath.getTravelTime()){
				res.setClientPath(bestPath.getClient(), bestPath);
				if(!res.isExplored() || res.getBestClient().getTravelTime() > bestPath.getTravelTime()){
					res.setBestClient(bestPath);
				}
				res.setExplored(true);
			}
		}
		
		return bestPath;
	}

}
