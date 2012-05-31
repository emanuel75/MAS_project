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

import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;

public class ExplorationAnt extends Ant {
	
	private HashSet<ClientAgent> clients;
	private Queue<Point> visitedNodes;
	private List<Point> oldVisitedNodes;
	private double minDistance=-1;
	private boolean mainAnt;
	private HashMap<Point,ClientPath> paths;
	private ResourceAgent res;
		
	public ExplorationAnt(TaxiAgent taxi, Point startLocation, HashSet<ClientAgent> clients, Mode mode, long time){
		super(taxi,startLocation,mode,time);
		this.clients = clients;
		this.visitedNodes = new LinkedList<Point>();
		this.oldVisitedNodes = new LinkedList<Point>();
		this.mainAnt = true;
		this.paths = new HashMap<Point, ClientPath>();
		this.res = taxi.getResource(startLocation);
	}
	
	public ExplorationAnt(TaxiAgent taxi, Point startLocation, double minDistance, HashSet<ClientAgent> clients, List<Point> oldVisitedNodes, HashMap<Point,ClientPath> paths, Mode mode, long time){
		this(taxi,startLocation,clients,mode,time);
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
	
	private boolean onExploredPath(ClientAgent client, Point nextNode){
		return mode.equals(Mode.EXPLORE_PACKAGES) && res.exploredClient(client,time) && res.getClientPath(client).getPath().peek().equals(nextNode) ||
				mode.equals(Mode.EXPLORE_DELIVERY_LOC) && res.exploredDeliveryLoc(client,time) && res.getDeliveryPath(client).getPath().peek().equals(nextNode);
	}
	
	/**
	 * Dynamic programming algorithm
	 * @return The shortest path from the startLocation leading to a client
	 */
	public ClientPath lookForClient(){
//		System.out.println(startLocation + ": ");
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
			Queue<Point> noStep = new LinkedList<Point>();
			noStep.add(startLocation);
			if(mode.equals(Mode.EXPLORE_PACKAGES) && startLocation.equals(client.getClient().getPickupLocation())){
				ClientPath myPath = new ClientPath(noStep, 0, client, time);
				if(!rm.containsObjectAt(client.getClient(), startLocation)){
					myPath.setDisappeared();
				}
				res.setBestClient(myPath);
				res.setClientPath(client, myPath);
				res.setExplored(true);
				return new ClientPath(visitedNodes, 0, client, time);
			}
			else if(mode.equals(Mode.EXPLORE_DELIVERY_LOC) && startLocation.equals(client.getDeliveryLocation())){
				ClientPath myPath = new ClientPath(noStep, 0, client, time); 
				res.setDeliveryPath(client, myPath);
				return new ClientPath(visitedNodes, 0, client, time);
			}
		}
		
		
		double myMinDistance = computeMinDistance();
		//We have to control how far the ants can go, because the computation time would be too much otherwise
		//The minDistance is the minimum distance between the initial startLocation and the closest client returned by Graphs.getShortestPathTo which uses an A* search with Eucledian heuristics
		//myMinDistance is the same distance, but not from the nitial startLocation but from the current position
		if(Graphs.pathLength((LinkedList<Point>) visitedNodes)+myMinDistance<=Math.min(1.3*minDistance,minDistance+5000)){
			it = neighbours.iterator();
			TreeMap<Double,Point> options = new TreeMap<Double, Point>();
			double notExplored = 0;
			double newTime;
			boolean onExploredPath;
			boolean exploredNow = false;
			while(it.hasNext()){
				nextNode = it.next();
				nextRes = ((TaxiAgent) agent).getResource(nextNode);
				onExploredPath = false;
				cl = clients.iterator();
				while(cl.hasNext() && !onExploredPath){
					client = cl.next();
					if(onExploredPath(client,nextNode)){
						onExploredPath = true;
						if(mode.equals(Mode.EXPLORE_PACKAGES) && res.getClientPath(client).getTime()==time ||
							mode.equals(Mode.EXPLORE_DELIVERY_LOC) && res.getDeliveryPath(client).getTime()==time){
							exploredNow = true;
						}
					}
				}
				if(!visitedNodes.contains(nextNode) || onExploredPath){
					notExplored--;
					if(mode.equals(Mode.EXPLORE_PACKAGES) && !nextRes.isExplored(time)){
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
							if(mode.equals(Mode.EXPLORE_PACKAGES) && nextRes.exploredClient(client,time) ||
								mode.equals(Mode.EXPLORE_DELIVERY_LOC) && nextRes.exploredDeliveryLoc(client,time)){
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
				if(exploredNow && nextTime<0){
					if(nodes.hasNext()){
						nextTime = nodes.next();
					}
					else{
						nextTime = null;
					}
					continue;
				}
				nextNode = options.get(nextTime);
				oldVisitedNodes = new LinkedList<Point>();
				oldVisitedNodes.addAll(visitedNodes);
				//We create a new ant which would do the same thing than this one, but from the next neighbor
				//Thus it will return the best path from the next node, here we have to decide
				//which neighbor is the best taking into account the travelTime of the best path from the neighbor
				//and the travelTime leading to the neighbor
				newAnt = new ExplorationAnt((TaxiAgent) agent, nextNode, minDistance, clients, oldVisitedNodes, paths, mode, time);
				newAnt.initRoadUser(rm);
				antPath = newAnt.lookForClient();
				//If the created ant found a path and it is better than the path found so far, we store it in the bestPath
				if(antPath != null){
					double newTravelTime = antPath.getTravelTime()+computeTravelTime(startLocation,nextNode);
//					if(mainAnt)
//						System.out.println(mode + ": " + newTravelTime + ", " + antPath.getPath().size() + " --> " + antPath.getClient().getPosition());
					if(bestPath==null || bestPath.getTravelTime()>newTravelTime){
						bestPath = new ClientPath(antPath.getPath(), newTravelTime, antPath.getClient(), time);
					}
					if(antPath.isOnPath() && onExploredPath(antPath.getClient(), nextNode)){
						if(antPath.isDisappeared()){
							res.setExplored(false);
						}
						else{
							if(mode.equals(Mode.EXPLORE_PACKAGES)){
								res.getClientPath(antPath.getClient()).setTravelTime(newTravelTime);
								res.getClientPath(antPath.getClient()).setTime(time);
							}
							else{
								res.getDeliveryPath(antPath.getClient()).setTravelTime(newTravelTime);
								res.getDeliveryPath(antPath.getClient()).setTime(time);
							}
						}
					}
					else{
						antPath.setOffPath();
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
			Queue<Point> nextStep = new LinkedList<Point>();
			LinkedList<Point> bestPathList = (LinkedList<Point>) bestPath.getPath();
			nextStep.add(bestPathList.get(bestPathList.lastIndexOf(startLocation)+1));
			ClientPath myPath = new ClientPath(nextStep, bestPath.getTravelTime(), bestPath.getClient(), time);
			if(mode.equals(Mode.EXPLORE_PACKAGES) && (!res.isExplored(time) || !res.exploredClient(bestPath.getClient(),time) ||
				res.getClientPath(bestPath.getClient()).getTravelTime() >= bestPath.getTravelTime())){
				res.setClientPath(bestPath.getClient(), myPath);
				if(!res.isExplored(time) || res.getBestClient().getTravelTime() >= bestPath.getTravelTime()){
					res.setBestClient(myPath);
				}
				res.setExplored(true);
			}
			else if(mode.equals(Mode.EXPLORE_DELIVERY_LOC) && (!res.exploredDeliveryLoc(bestPath.getClient(),time) || 
					res.getDeliveryPath(bestPath.getClient()).getTravelTime() >= bestPath.getTravelTime())){
				res.setDeliveryPath(bestPath.getClient(), myPath);
			}
		}
		
		return bestPath;
	}

}
