package agents;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import messages.BidMessage;
import messages.BroadcastMessage;
import messages.ConfirmationMessage;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.Message;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Events;
import rinde.sim.event.Listener;
import scenario.MyEvent;
import scenario.StatisticsCollector;
import ants.Ant;
import ants.Ant.Mode;
import ants.ClientPath;
import ants.ExplorationAnt;

public class TaxiAgent extends Agent implements TickListener, Events {

	public enum Type {
		START_AGENT, START_PICKUP, PICKUP, DELIVERY, IDLE;
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
    private double travelTime;
    private final EventDispatcher disp;
    private Queue<Point> nextStep;
    private ExplorationAnt eAnt;
    private ClientAgent client;
    private ClientPath closestClient = null;
    private StatisticsCollector sc;
    private HashMap<ClientAgent,Long> cannotFulfill;
    private int limit = 20000;
    private long blockade = 0;
	
	public TaxiAgent(Truck truck, Agency agency, double radius, double reliability, StatisticsCollector sc){
		super(radius,reliability);
		this.truck = truck;
		this.hasAgent = false;
		this.foundAgent = false;
		this.shouldDeliver = false;
		this.shouldPickup = false;
		this.agency = agency;
		this.travelTime = -1;
		this.sc = sc;
		this.disp = new EventDispatcher(Type.values());
		this.nextStep = new LinkedList<Point>();
		this.cannotFulfill = new HashMap<ClientAgent, Long>();
	}
	
	private void setClient(ClientPath clientPath){
		this.client = clientPath.getClient();
		this.packageId = client.getClient().getPackageID();
		this.destination = client.getClient().getDeliveryLocation();
		this.path = clientPath.getPath();
                this.travelTime = clientPath.getTravelTime();
		path.add(clientPath.getClient().getPosition());
	}
	
	private ClientPath lookForClient(ExplorationAnt ant){
		double multiplier = 1.3;
		ClientPath clp = null;
		while(multiplier<2.8){
			clp = ant.lookForClient(multiplier);
			if(clp != null){
				if(multiplier>1.3){
					System.out.print("Has to go on longer path because road blockade: ");
					System.out.println(Graphs.pathLength(truck.getRoadModel().getShortestPathTo(getPosition(), ((LinkedList<Point>) clp.getPath()).getLast())) + " <-> "  + Graphs.pathLength((LinkedList<Point>)clp.getPath()));
				}
				return clp;
			}
			multiplier += 0.4;
		}
		return clp;
	}
	
	@Override
	public void tick(long currentTime, long timeStep) {
		if(!hasAgent){
			foundAgent = false;
                        travelTime = -1;
			Queue<Message> messages = mailbox.getMessages();
			int m = 0;
			for(Message message : messages){
				m++;
				if(!hasAgent && message instanceof ConfirmationMessage){
					ConfirmationMessage cm = (ConfirmationMessage) message;
					setClient(cm.getClosestClient());
					try {
						if(client.getClient().packageID.equals("Package-4")){
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
					disp.dispatchEvent(new MyEvent(Type.START_PICKUP, this, currentTime));
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
					HashSet<ClientAgent> needTaxi = bm.getClients();
					for(ClientAgent cla : cannotFulfill.keySet()){
						if(cannotFulfill.get(cla)+limit > currentTime/timeStep){
							needTaxi.remove(cla);
						}
					}
					if(needTaxi.size()>0){
						eAnt= new ExplorationAnt(this, getPosition(), needTaxi, Ant.Mode.EXPLORE_PACKAGES,currentTime);
						eAnt.initRoadUser(truck.getRoadModel());
						closestClient = lookForClient(eAnt);
						if(closestClient != null){
							System.out.println("MYCHOICE: " + closestClient.getClient().getClient().packageID);
							System.out.print(Graphs.pathLength((LinkedList<Point>)closestClient.getPath()));
							System.out.println(" <-> " + Graphs.pathLength(truck.getRoadModel().getShortestPathTo(getPosition(), closestClient.getClient().getPosition())));
							foundAgent = true;
						} 
						else{
							System.out.println(truck.getTruckID() + " cannot fulfill the order!");
							for(ClientAgent cla : bm.getClients()){
								cannotFulfill.put(cla, currentTime/timeStep);
							}
						}
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
				ClientPath shortestPath = lookForClient(eAnt);
				if(shortestPath != null){
					this.path = shortestPath.getPath();
				}
				else{
					blockade = currentTime/timeStep;
					path.addAll(truck.getRoadModel().getShortestPathTo(getPosition(), client.getDeliveryLocation()));
					System.out.println(truck.getTruckID() + " try to deliver " + client.getClient().packageID + ", but will probably run into road blockade");
				}
				this.destination = client.getDeliveryLocation();
				this.path.add(client.getDeliveryLocation());
				agency.removeClient(client);
			}
			if(hasAgent && truck.tryDelivery()){
				this.shouldDeliver = false;
				System.out.println("[" + truck.getTruckID() + "] I delivered " + this.packageId);
				hasAgent = false;
				foundAgent = false;
				blockade = 0;
				disp.dispatchEvent(new MyEvent(Type.DELIVERY, this, currentTime));
				disp.dispatchEvent(new MyEvent(Type.IDLE, this, currentTime));
				agency.freeUpTaxi(this,false);
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
						closestClient = lookForClient(eAnt);
						if(closestClient != null){
							if(closestClient.isDisappeared()){
								hasAgent = false;
								agency.freeUpTaxi(this,false);
								path.clear();
							}
							else{
								setClient(closestClient);
							}
						}
						else{
							System.out.println(truck.getTruckID() + " cannot fulfill the order " + client.getClient().getPackageID() + " any more!");
							cannotFulfill.put(client, currentTime/timeStep);
							agency.addClient(client);
							agency.freeUpTaxi(this,false);
						}
					}
					else if(shouldDeliver){
						eAnt = new ExplorationAnt(this, getPosition(), toExplore, Mode.EXPLORE_DELIVERY_LOC, currentTime);
						eAnt.initRoadUser(truck.getRoadModel());
						if(blockade==0 || blockade+limit<currentTime/timeStep){
							ClientPath shortestPath = lookForClient(eAnt);
							if(shortestPath != null){
								this.path = shortestPath.getPath();
							}
							else{
								path = new LinkedList<Point>();
								path.addAll(truck.getRoadModel().getShortestPathTo(getPosition(), client.getDeliveryLocation()));
								blockade = currentTime/timeStep;
								System.out.println(truck.getTruckID() + " try to deliver " + client.getClient().packageID + ", but will probably run into road blockade");
							}
							this.path.add(client.getDeliveryLocation());
						}
					}
					if(!path.isEmpty()){
						while(path.size()>1 && path.peek().equals(getPosition())){
							path.poll();
						}
						nextStep.add(path.poll());
						LinkedList<Point> q = new LinkedList<Point>();
						
						q.add(nextStep.peek());
						q.add(truck.getPosition());
						sc.increaseRouteLength(Graphs.pathLength(q));
					}
				}
				if(!nextStep.isEmpty()){
					truck.drive(nextStep, timeStep);
					
				}
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

        public double getTravelTime(){
		if(shouldDeliver){
			return travelTime;
		}
		return -1;
	}
	
	public Point getDestination(){
		return this.destination;
	}
	public void setPackageId(String packageId) {
		this.packageId = packageId;
	}

	public void startIdle(long currentTime) {
		disp.dispatchEvent(new MyEvent(Type.IDLE, this, currentTime));
	}
	
}

