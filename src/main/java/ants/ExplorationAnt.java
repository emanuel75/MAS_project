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
	
	public enum Mode{EXPLORE_PACKAGES,EXPLORE_DELIVERY_LOC}
	
	private HashSet<ClientAgent> clients;
	private Queue<Point> visitedNodes;
	private List<Point> oldVisitedNodes;
	private double minDistance=-1;
	private boolean mainAnt;
	private HashMap<Point,ClientPath> paths;
	private ResourceAgent res;
	private Mode mode;
		
	public ExplorationAnt(TaxiAgent taxi, Point startLocation, HashSet<ClientAgent> clients, Mode mode){
		super(taxi,startLocation);
		this.clients = clients;
		this.visitedNodes = new LinkedList<Point>();
		this.oldVisitedNodes = new LinkedList<Point>();
		this.mainAnt = true;
		this.paths = new HashMap<Point, ClientPath>();
		this.res = taxi.getResource(startLocation);
		this.mode = mode;
	}
	
	public ExplorationAnt(TaxiAgent taxi, Point startLocation, double minDistance, HashSet<ClientAgent> clients, List<Point> oldVisitedNodes, HashMap<Point,ClientPath> paths, Mode mode){
		this(taxi,startLocation,clients,mode);
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
		double clientDistance;
		Iterator<ClientAgent> it = clients.iterator();
		while(it.hasNext()){
			if(mode.equals(Mode.EXPLORE_PACKAGES)){
				clientDistance = Graphs.pathLength(rm.getShortestPathTo(startLocation, it.next().getPosition()));
			}
			else{
				clientDistance = Graphs.pathLength(rm.getShortestPathTo(startLocation, it.next().getDeliveryLocation()));
			}
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
			if(mode.equals(Mode.EXPLORE_PACKAGES) && rm.containsObjectAt(client.getClient(), startLocation)){
				ClientPath myPath = new ClientPath(new LinkedList<Point>(), 0, client); 
				res.setBestClient(myPath);
				res.setClientPath(client, myPath);
				res.setExplored(true);
				return new ClientPath(visitedNodes, 0, client);
			}
			else if(mode.equals(Mode.EXPLORE_DELIVERY_LOC) && startLocation.equals(client.getDeliveryLocation())){
				ClientPath myPath = new ClientPath(new LinkedList<Point>(), 0, client); 
				res.setDeliveryPath(client, myPath);
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
				if(!visitedNodes.contains(nextNode)){
					notExplored--;
					if(mode.equals(Mode.EXPLORE_PACKAGES) && !nextRes.isExplored()){
						options.put(notExplored,nextNode);
					}
					else if(mode.equals(Mode.EXPLORE_PACKAGES) &&  clients.contains(nextRes.getBestClient().getClient())){
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
							if(mode.equals(Mode.EXPLORE_PACKAGES) && nextRes.exploredClient(client) ||
								mode.equals(Mode.EXPLORE_DELIVERY_LOC) && nextRes.exploredDeliveryLoc(client)){
								if(mode.equals(Mode.EXPLORE_PACKAGES)){
									newTime = computeTravelTime(startLocation,nextNode) + nextRes.getClientPath(client).getTravelTime();
								}
								else{
									newTime = computeTravelTime(startLocation,nextNode) + nextRes.getDeliveryPath(client).getTravelTime();
								}
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
				newAnt = new ExplorationAnt((TaxiAgent) agent, nextNode, minDistance, clients, oldVisitedNodes, paths, mode);
				newAnt.initRoadUser(rm);
				antPath = newAnt.lookForClient();
				//If the created ant found a path and it is better than the path found so far, we store it in the bestPath
				if(antPath != null){
					double newTravelTime = antPath.getTravelTime()+computeTravelTime(startLocation,nextNode);
					if(mainAnt)
						System.out.println(mode + ": " + newTravelTime + ", " + antPath.getPath().size() + " --> " + antPath.getClient().getPosition());
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
		
		if(bestPath!=null){
			if(mode.equals(Mode.EXPLORE_PACKAGES) && (!res.isExplored() || !res.exploredClient(bestPath.getClient()) ||
				res.getClientPath(bestPath.getClient()).getTravelTime() > bestPath.getTravelTime())){
				res.setClientPath(bestPath.getClient(), bestPath);
				if(!res.isExplored() || res.getBestClient().getTravelTime() > bestPath.getTravelTime()){
					res.setBestClient(bestPath);
				}
				res.setExplored(true);
			}
			else if(mode.equals(Mode.EXPLORE_DELIVERY_LOC) && (!res.exploredDeliveryLoc(bestPath.getClient()) || 
					res.getDeliveryPath(bestPath.getClient()).getTravelTime() > bestPath.getTravelTime())){
				res.setDeliveryPath(bestPath.getClient(), bestPath);
			}
		}
		
		return bestPath;
	}

}
