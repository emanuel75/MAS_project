package agents;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import ants.Ant;

import ants.ClientPath;
import ants.ExplorationAnt;
import ants.Ant.Mode;


import messages.BidMessage;
import messages.BroadcastMessage;
import messages.ConfirmationMessage;


import rinde.sim.core.TickListener;
import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.Message;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Events;
import rinde.sim.event.Listener;
import scenario.MyEvent;
import ants.ClientPath;
import ants.ExplorationAnt;

public class TaxiAgent extends Agent implements TickListener, Events {

	public enum Type {
		START_AGENT, PICKUP, DELIVERY;
	}
	
	private Truck truck;
	private boolean foundAgent;
    private boolean hasAgent;
    private boolean shouldPickup;
    private boolean shouldDeliver;
    private String packageId;
    private Point destination;
    private Agency agency;
    private int capacity;
    
    private final EventDispatcher disp;
    private Queue<Point> nextStep;
    private ExplorationAnt eAnt;
    private ClientAgent client;
    private ClientPath closestClient = null;
	
	public TaxiAgent(Truck truck, Agency agency, double radius, double reliability){
		super(radius,reliability);
		this.truck = truck;
		this.hasAgent = false;
		this.foundAgent = false;
		this.shouldDeliver = false;
		this.shouldPickup = false;
		this.agency = agency;
		
		this.disp = new EventDispatcher(Type.values());
		this.nextStep = new LinkedList<Point>();
	}
	
	private void setClient(ClientPath clientPath){
		this.client = clientPath.getClient();
		this.packageId = client.getClient().getPackageID();
		this.destination = client.getClient().getDeliveryLocation();
		this.path = clientPath.getPath();
		path.add(clientPath.getClient().getPosition());
	}
	
	@Override
	public void tick(long currentTime, long timeStep) {
		if(!hasAgent){
			foundAgent = false;
			Queue<Message> messages = mailbox.getMessages();
			int m = 0;
			for(Message message : messages){
				m++;
				if(!hasAgent && message instanceof ConfirmationMessage){
					ConfirmationMessage cm = (ConfirmationMessage) message;
					setClient(cm.getClosestClient());
					try {
						if(client.getClient().packageID.equals("Package-17")){
							PrintWriter pw = new PrintWriter(new FileWriter("path.txt"));
							for(Point p : path){
								pw.println(p);
							}
						pw.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					this.hasAgent = true;
					System.out.println("[" + truck.getTruckID() + "] I made a contract with: " + this.packageId + " (" + getPosition() + ")");
					this.shouldDeliver = true;
					this.shouldPickup = true;
				}
				else if(!hasAgent && (!foundAgent || messages.size()==m) && message instanceof BroadcastMessage){
					BroadcastMessage bm = (BroadcastMessage) message;
//					if(truck.getTruckID().equals("Truck-1")){
//						Iterator<ClientAgent> cliter = bm.getClients().iterator();
//						while(cliter.hasNext()){
//							ClientAgent nexcl = cliter.next();
//							System.out.println(nexcl.getClient().packageID + ": " + Graphs.pathLength(truck.getRoadModel().getShortestPathTo(getPosition(), nexcl.getClient().getPickupLocation())));
//							System.out.println(truck.getRoadModel().getShortestPathTo(getPosition(), nexcl.getClient().getPickupLocation()));
//						}
//					}
					eAnt= new ExplorationAnt(this, getPosition(), bm.getClients(), Ant.Mode.EXPLORE_PACKAGES,currentTime);
					eAnt.initRoadUser(truck.getRoadModel());
					closestClient = eAnt.lookForClient();
					if(closestClient != null){
						System.out.println("MYCHOICE: " + closestClient.getClient().getClient().packageID);
						System.out.print(Graphs.pathLength((LinkedList<Point>)closestClient.getPath()));
						System.out.println(" <-> " + Graphs.pathLength(truck.getRoadModel().getShortestPathTo(getPosition(), closestClient.getClient().getPosition())));
						foundAgent = true;
					} 
				}
			}
			if(!hasAgent && foundAgent){
				System.out.println("[" + truck.getTruckID() + "] I found a client.");
				agency.receive(new BidMessage(this,closestClient));
			}
		}
		if(path == null || path.isEmpty()){
			if(hasAgent && truck.tryPickup(client.getClient().getPackageID())){
				this.shouldPickup = false;
				System.out.println("[" + truck.getTruckID() + "] I picked up " + this.packageId);
				disp.dispatchEvent(new MyEvent(Type.PICKUP, this, currentTime));
				
				HashSet<ClientAgent> toDeliver = new HashSet<ClientAgent>();
				toDeliver.add(client);
				eAnt = new ExplorationAnt(this, getPosition(), toDeliver, Mode.EXPLORE_DELIVERY_LOC, currentTime);
				eAnt.initRoadUser(truck.getRoadModel());
				this.path = eAnt.lookForClient().getPath();
				this.path.add(client.getDeliveryLocation());
			}
			if(hasAgent && truck.tryDelivery()){
				this.shouldDeliver = false;
				System.out.println("[" + truck.getTruckID() + "] I delivered " + this.packageId);
				hasAgent = false;
				foundAgent = false;
				disp.dispatchEvent(new MyEvent(Type.DELIVERY, this, currentTime));
				agency.removeClient(client);
				agency.freeUpTaxi(this);
			}
			if(!shouldPickup && !shouldDeliver){
				destination = truck.getRoadModel().getGraph().getRandomNode(simulator.getRandomGenerator());
				this.path = new LinkedList<Point>(truck.getRoadModel().getShortestPathTo(truck, destination));
			}
		}else if(hasAgent){
			if(shouldPickup || shouldDeliver){
				if(nextStep.isEmpty() && !path.isEmpty()){
					HashSet<ClientAgent> toExplore = new HashSet<ClientAgent>();
					toExplore.add(client);
					if(shouldPickup){
						eAnt = new ExplorationAnt(this, getPosition(), toExplore, Mode.EXPLORE_PACKAGES, currentTime);
						eAnt.initRoadUser(truck.getRoadModel());
						closestClient = eAnt.lookForClient();
						if(closestClient.isDisappeared()){
							hasAgent = false;
							agency.freeUpTaxi(this);
							path.clear();
						}
						else{
							setClient(closestClient);
						}
					}
					else if(shouldDeliver){
						eAnt = new ExplorationAnt(this, getPosition(), toExplore, Mode.EXPLORE_DELIVERY_LOC, currentTime);
						eAnt.initRoadUser(truck.getRoadModel());
						this.path = eAnt.lookForClient().getPath();
						this.path.add(client.getDeliveryLocation());
					}
					if(!path.isEmpty()){
						if(path.peek().equals(getPosition())){
							path.poll();
						}
						nextStep.add(path.poll());
					}
				}
				if(!nextStep.isEmpty()){
					truck.drive(nextStep, timeStep);
				}
			}
			else{
				truck.drive(path, timeStep);
			}
		}
	}
	
	public ResourceAgent getResource(Point node){
		return agency.getResource(node);
	}
	
	@Override
	public Point getPosition() {
		return this.truck.getPosition();
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void receive(Message message) {
		if(!hasAgent){
			super.receive(message);
		}
	}
	
	public Truck getTruck() {
		return truck;
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

	@Override
	public boolean containsListener(Listener l, Enum<?> eventType) {
		//delegate to the event dispatcher
		return disp.containsListener(l, eventType);
	}

	public String getPackageId() {
		return packageId;
	}

	public void setPackageId(String packageId) {
		this.packageId = packageId;
	}

	
}

