package ants;

import agents.Agent;
import agents.Truck;
import agents.TaxiAgent;
import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

public class Ant implements RoadUser {
	
	public enum Mode{EXPLORE_PACKAGES,EXPLORE_DELIVERY_LOC}
	
	protected RoadModel rm;
	protected Point startLocation;
	protected Agent agent;
	protected Mode mode;
	protected long time;
	
	public Ant(Agent agent, Point startLocation, Mode mode, long time){
		this.agent = agent;
		this.startLocation = startLocation;
		this.mode = mode;
		this.time = time;
	}
	
	/**
	 * 
	 * @param start
	 * @param stop
	 * @return The travel time between two nodes
	 */
	public double computeTravelTime(Point start, Point stop){
		if(start.equals(stop)){
			return 0;
		}
		
		double maxSpeed;
		EdgeData data = rm.getGraph().connectionData(start, stop);
		double distance = data.getLength();
		double truckSpeed;
		if(agent instanceof TaxiAgent){
			Truck truck = ((TaxiAgent) agent).getTruck();
			truckSpeed = truck.getSpeed();
		}
		else{
			truckSpeed = 15;
		}
		if (data instanceof MultiAttributeEdgeData) {
			MultiAttributeEdgeData maed = (MultiAttributeEdgeData) data;
			double speed = maed.getMaxSpeed();
			maxSpeed = Double.isNaN(speed) ? truckSpeed : Math.min(speed, truckSpeed);
		}
		else{
			maxSpeed = truckSpeed;
		}
		
		return distance/maxSpeed;
	}
	
	public Agent getAgent(){
		return agent;
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		this.rm = model;
	}
	
	public RoadModel getRoadModel(){
		return rm;
	}
	
	public Point getPosition(){
		return rm.getPosition(this);
	}
	
	public Point getLastCrossRoad(){
		return rm.getLastCrossRoad(this);
	}

}
