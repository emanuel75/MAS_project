package agents;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import ants.ClientPath;
import ants.ExplorationAnt;
import ants.ExplorationAnt.Mode;


import messages.BidMessage;
import messages.BroadcastMessage;
import messages.ConfirmationMessage;

import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.Message;
import rinde.sim.lab.common.trucks.Truck;

public class TaxiAgent extends Agent implements TickListener {

	private Truck truck;
	private boolean foundAgent;
    private boolean hasAgent;
    private boolean shouldPickup;
    private boolean shouldDeliver;
    private String packageId;
    private Point destination;
    private Agency agency;
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
			Queue<Message> messages = mailbox.getMessages();
//			ClientAgent closestClient = null;
//			Double closestDistance = null;
			int m = 0;
			for(Message message : messages){
				m++;
				if(!hasAgent && message instanceof ConfirmationMessage){
					ConfirmationMessage cm = (ConfirmationMessage) message;
					setClient(cm.getClosestClient());
					try {
						PrintWriter pw = new PrintWriter(new FileWriter("path.txt"));
						for(Point p : path){
							pw.println(p);
						}
						pw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					this.hasAgent = true;
					System.out.println("[" + truck.getTruckID() + "] I made a contract with: " + this.packageId);
					this.shouldDeliver = true;
					this.shouldPickup = true;
				}
				else if(!hasAgent && (!foundAgent || messages.size()==m) && message instanceof BroadcastMessage){
//					System.out.println("Try to find new package");
					foundAgent = false;
					BroadcastMessage bm = (BroadcastMessage) message;
					eAnt= new ExplorationAnt(this, getPosition(), bm.getClients(), Mode.EXPLORE_PACKAGES);
					eAnt.initRoadUser(truck.getRoadModel());
					closestClient = eAnt.lookForClient();
					System.out.println(closestClient==null);
					if(closestClient != null){
						foundAgent = true;
					}
//					ClientAgent client;
//					Iterator<ClientAgent> it = bm.getClients().iterator(); 
//					while(it.hasNext()){
//						client = it.next();
//						double distance = Graphs.pathLength(truck.getRoadModel().getShortestPathTo(truck, client.getPosition()));
//						if (!foundAgent || distance < closestDistance){
//							foundAgent = true;
//							closestClient = client;
//							closestDistance = distance;
//						}
//					} 
				}
			}
			if(!hasAgent && foundAgent){
				System.out.println("[" + truck.getTruckID() + "] I found a client.");
//				System.out.println(closestClient.getPath().size());
//				System.out.println(closestClient.getTravelTime());
				agency.receive(new BidMessage(this,closestClient));
//				Queue q = new LinkedList<Point>();
//				q.addAll(truck.getRoadModel().getShortestPathTo(getPosition(),closestClient.getPosition()));
//				agency.receive(new BidMessage(this, new ClientPath(q,closestDistance, closestClient)));
			}
		}
		if(path == null || path.isEmpty()){
			if(foundAgent && truck.tryPickup()){
				this.shouldPickup = false;
				System.out.println("[" + truck.getTruckID() + "] I picked up " + this.packageId);
				
				HashSet<ClientAgent> toDeliver = new HashSet<ClientAgent>();
				toDeliver.add(client);
				eAnt = new ExplorationAnt(this, getPosition(), toDeliver, Mode.EXPLORE_DELIVERY_LOC);
				eAnt.initRoadUser(truck.getRoadModel());
				this.path = eAnt.lookForClient().getPath();
				this.path.add(client.getDeliveryLocation());
//				this.path = new LinkedList<Point>(truck.getRoadModel().getShortestPathTo(truck, destination));
			}
			if(hasAgent && truck.tryDelivery()){
				this.shouldDeliver = false;
				System.out.println("[" + truck.getTruckID() + "] I delivered " + this.packageId);
				hasAgent = false;
				foundAgent = false;
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
						eAnt = new ExplorationAnt(this, getPosition(), toExplore, Mode.EXPLORE_PACKAGES);
						eAnt.initRoadUser(truck.getRoadModel());
						closestClient = eAnt.lookForClient();
						setClient(closestClient);
					}
					else if(shouldDeliver){
						eAnt = new ExplorationAnt(this, getPosition(), toExplore, Mode.EXPLORE_DELIVERY_LOC);
						eAnt.initRoadUser(truck.getRoadModel());
						this.path = eAnt.lookForClient().getPath();
						this.path.add(client.getDeliveryLocation());
					}
					if(path.peek().equals(getPosition())){
						path.poll();
					}
					nextStep.add(path.poll());
				}
				truck.drive(nextStep, timeStep);
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

}
