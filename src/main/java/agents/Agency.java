package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;


import messages.BidMessage;
import messages.BroadcastMessage;
import messages.ClientRequestMessage;
import messages.ConfirmationMessage;

import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;
import rinde.sim.event.Events;
import rinde.sim.event.Listener;
import scenario.StatisticsCollector;

public class Agency extends Agent implements TickListener {
	
	private HashMap<ClientAgent,TaxiAgent> clients;
	private HashMap<Point, ResourceAgent> resources;
	private HashSet<CommunicationUser> taxis;
	private int allTaxi;
	private double avgWaitingTime = 0;
	private double avgDeliveryTime = 0;
	private double avgPickUpTime = 0;
	private double avgIdleTime = 0;
	private long maxWaitingTime = 30000;
	private long maxIdle = 30000;
	private long lastAdded = 0;
	private long lastRemoved = 0;
	private long addlimit = 10000;
	private long removeLimit = 10000;
	private StatisticsCollector statistics;
	private int truckId;
	
	public Agency(double radius, double reliability){
		super(radius, reliability);
		this.clients = new HashMap<ClientAgent, TaxiAgent>();
		this.resources = new HashMap<Point, ResourceAgent>();
		this.taxis = new HashSet<CommunicationUser>();
		this.allTaxi = 0;
		this.truckId = 0;
	}
	
	@Override
	public void initialize(RoadModel rm){
		super.initialize(rm);
		Set<Point> nodes = rm.getGraph().getNodes();
		Iterator<Point> it = nodes.iterator();
		ResourceAgent res;
		while(it.hasNext()){
			res = new ResourceAgent(it.next());
			addResource(res);
		}
	}
	
	public void addResource(ResourceAgent res){
		resources.put(res.getNode(), res);
	}
	
	public ResourceAgent getResource(Point point){
		return resources.get(point);
	}
	
	public void freeUpTaxi(TaxiAgent taxi, boolean newTaxi){
		taxis.add(taxi);
		if(newTaxi){
			allTaxi++;
			truckId++;
		}
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if(currentTime/10000>lastAdded+addlimit && avgWaitingTime>maxWaitingTime-avgPickUpTime*(clients.size()/allTaxi)){
			System.out.println("Too many clients, taxi will be added.");
			addTruck(currentTime);
			lastAdded = currentTime;
		}
		if(currentTime/10000>lastRemoved+removeLimit && taxis.size()>0 && avgIdleTime>maxIdle){
			System.out.println("Too few clients, a taxi will be removed.");
			taxis.remove(taxis.iterator().next());
			lastRemoved = currentTime;
		}
		
		HashMap<ClientAgent,BidMessage> bestBids = new HashMap<ClientAgent, BidMessage>();
		Queue<Message> messages = mailbox.getMessages();
		for(Message message : messages){
			if(message instanceof ClientRequestMessage){
				clients.put((ClientAgent) message.getSender(), null);
				System.out.println("New client: " + ((ClientAgent) message.getSender()).getClient().getPackageID());
			}
			if(message instanceof BidMessage){
				BidMessage bm = (BidMessage) message;
				if(!bestBids.containsKey(bm.getClosestClient().getClient()) || 
					bestBids.get(bm.getClosestClient().getClient()).getClosestClient().getTravelTime() > bm.getClosestClient().getTravelTime()){
					bestBids.put(bm.getClosestClient().getClient(), bm);
				}
			}
		}
		
		Iterator<ClientAgent> it = bestBids.keySet().iterator();
		ClientAgent client;
		while(it.hasNext()){
			client = it.next();
			clients.put(client, (TaxiAgent) bestBids.get(client).getSender());
			bestBids.get(client).getSender().receive(new ConfirmationMessage(this,bestBids.get(client).getClosestClient()));
			taxis.remove(bestBids.get(client).getSender());
		}
		
		if(taxis.size()>0){
			HashSet<ClientAgent> needTaxi = new HashSet<ClientAgent>();
			HashSet<ClientAgent> needTaxiQuickly = new HashSet<ClientAgent>();
			it = clients.keySet().iterator();
			while(it.hasNext()){
				client = it.next();
				if(clients.get(client)==null){
					needTaxi.add(client);
					if(client.getWaitingTime() > client.getMaxWaitingTime()-avgDeliveryTime*(clients.size()/allTaxi)){
						needTaxiQuickly.add(client);
					}
				}
			}
			if(needTaxi.size()>0){
				Iterator<CommunicationUser> taxiIt = taxis.iterator();
				while(taxiIt.hasNext()){
					if(needTaxiQuickly.size()>0){
						taxiIt.next().receive(new BroadcastMessage(this,needTaxiQuickly));
					}
					else{
						taxiIt.next().receive(new BroadcastMessage(this,needTaxi));
					}
				}
			}
		}
	}
	
	public void removeClient(ClientAgent client){
		clients.remove(client);
	}
	
	public void addClient(ClientAgent client){
		clients.put(client, null);
	}
	
	public ClientAgent[] getClients(){
		return clients.keySet().toArray(new ClientAgent[clients.keySet().size()]);
	}
	
	public void setAvgWaitingTime(double avgWaitingTime) {
		this.avgWaitingTime = avgWaitingTime;
	}

	public void setAvgDeliveryTime(double avgDeliveryTime) {
		this.avgDeliveryTime = avgDeliveryTime;
	}
	
	public void setAvgPickUpTime(double avgPickUpTime) {
		this.avgPickUpTime = avgPickUpTime;
	}
	
	public void setAvgIdleTime(double avgIdleTime) {
		this.avgIdleTime = avgIdleTime;
	}
	
	private void addTruck(long currentTime){
		Truck truck = new Truck("Truck-" + truckId, rm.getGraph().getRandomNode(simulator.getRandomGenerator()), 7);
		truckId++;
		allTaxi++;
		simulator.register(truck);
		TaxiAgent agent = new TaxiAgent(truck, this, -1, 1, statistics);
		simulator.register(agent);
		
		agent.addListener(statistics, TaxiAgent.Type.values());
		freeUpTaxi(agent,true);
        agent.startIdle(currentTime);
	}
	
	private void removeTaxi(TaxiAgent taxi){
		simulator.unregister(taxi.getTruck());
		simulator.unregister(taxi);
		allTaxi--;
		taxis.remove(taxi);
	}
	
	public void setStatistics(StatisticsCollector stat){
		this.statistics = stat;
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		// TODO Auto-generated method stub
		
	}
	
}

