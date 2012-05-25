package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;


import messages.BidMessage;
import messages.BroadcastMessage;
import messages.ClientRequestMessage;
import messages.ConfirmationMessage;

import rinde.sim.core.TickListener;
import rinde.sim.core.model.communication.Message;

public class Agency extends Agent implements TickListener {
	
	private HashMap<ClientAgent,TaxiAgent> clients;

	public Agency(double radius, double reliability){
		super(radius, reliability);
		this.clients = new HashMap<ClientAgent, TaxiAgent>();
	}

	@Override
	public void tick(long currentTime, long timeStep) {
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
			bestBids.get(client).getSender().receive(new ConfirmationMessage(this,bestBids.get(client).getClosestClient()));
		}
		
		HashSet<ClientAgent> needTaxi = new HashSet<ClientAgent>();
		it = clients.keySet().iterator();
		while(it.hasNext()){
			client = it.next();
			if(clients.get(client)==null){
				needTaxi.add(client);
			}
		}
		if(needTaxi.size()>0){
			communicationAPI.broadcast(new BroadcastMessage(this,needTaxi));
		}
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		// TODO Auto-generated method stub
		
	}
	
}
