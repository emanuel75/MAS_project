package ants;

import agents.Agent;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.lab.common.trucks.Truck;

public class Ant implements RoadUser {
	
	protected RoadModel rm;
	protected Point startLocation;
	protected Agent agent;
	
	public Ant(Agent agent, Point startLocation){
		this.agent = agent;
		this.startLocation = startLocation;
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
