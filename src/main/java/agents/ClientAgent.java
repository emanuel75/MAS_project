package agents;

import ants.Ant.Mode;
import ants.FeasibilityAnt;
import messages.ClientRequestMessage;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Events;
import rinde.sim.event.Listener;
import rinde.sim.core.model.RoadModel;
import rinde.sim.lab.common.packages.Package;
import scenario.MyEvent;

public class ClientAgent extends Agent implements Events, TickListener{


	public enum Type {
		START_AGENT, PICKUP, ARRIVE;
	}
	
	private final EventDispatcher disp;
	
	private Package myClient;
	private Agency agency;
	private int limit = 7500;
	private long lastSendOut;
	private long waitingTime;
	private long maxWaitingTime = 30000;
	
	public ClientAgent(Package myClient, Agency agency, double radius, double reliability){
		super(radius,reliability);
		this.myClient = myClient;
		this.agency = agency;
		
		this.disp = new EventDispatcher(Type.values());
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
//		disp.dispatchEvent(new MyEvent(Type.START_AGENT, this, currentTime));
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
	
	public void startWaiting(long currentTime) {
		disp.dispatchEvent(new MyEvent(ClientAgent.Type.START_AGENT, this, currentTime));
	}
	
	@Override
	public void addListener(Listener l, Enum<?>... eventTypes) {
		//delegate to the event dispatcher
		disp.addListener(l, eventTypes);
	}

	@Override
	public void removeListener(Listener l, Enum<?>... eventTypes) {
		//delegate to the event dispatcher
		disp.removeListener(l, eventTypes);
	}
	
	public ResourceAgent getResource(Point node){
		return agency.getResource(node);
	}
	
	public long getWaitingTime() {
		return waitingTime;
	}

	public long getMaxWaitingTime() {
		return maxWaitingTime;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if(myClient.delivered())
			simulator.unregister(this);
		if(currentTime-lastSendOut > limit*timeStep){
			sendAnts(currentTime);
		}
		waitingTime++;
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {	}

	@Override
	public boolean containsListener(Listener l, Enum<?> eventType) {
		//delegate to the event dispatcher
		return disp.containsListener(l, eventType);
	}
	
	
}
