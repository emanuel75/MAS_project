package agents;

import ants.Ant.Mode;
import ants.FeasibilityAnt;
import messages.ClientRequestMessage;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.lab.common.packages.Package;

public class ClientAgent extends Agent implements TickListener{

	private Package myClient;
	private Agency agency;
	private int limit = 7500;
	private long lastSendOut;
	
	public ClientAgent(Package myClient, Agency agency, double radius, double reliability){
		super(radius,reliability);
		this.myClient = myClient;
		this.agency = agency;
		
	}
	
	private void sendAnts(long currentTime){
		FeasibilityAnt ant = new FeasibilityAnt(this, myClient.getPickupLocation(), Mode.EXPLORE_PACKAGES, currentTime);
		ant.initRoadUser(rm);
		ant.notifyNeighbours();
		ant = new FeasibilityAnt(this, myClient.getDeliveryLocation(), Mode.EXPLORE_DELIVERY_LOC, currentTime);
		ant.initRoadUser(rm);
		ant.notifyNeighbours();
		lastSendOut = currentTime;
	}
	
	@Override
	public void initialize(RoadModel rm){
		super.initialize(rm);
		sendAnts(0);
		sendRequest();
	}
	
	private void sendRequest(){
		agency.receive(new ClientRequestMessage(this));
	}
	
	@Override
	public Point getPosition() {
		return this.myClient.getPickupLocation();
	}
	
	public Point getDeliveryLocation(){
		return myClient.getDeliveryLocation();
	}
	
	public Package getClient(){
		return myClient;
	}
	
	public ResourceAgent getResource(Point node){
		return agency.getResource(node);
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if(myClient.delivered())
			simulator.unregister(this);
		if(currentTime-lastSendOut > limit*timeStep){
			sendAnts(currentTime);
		}
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {	}

}
