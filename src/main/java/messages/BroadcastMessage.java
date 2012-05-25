package messages;

import java.util.HashSet;

import agents.ClientAgent;

import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;

public class BroadcastMessage extends Message {
	
	private HashSet<ClientAgent> clients;
	
	public BroadcastMessage(CommunicationUser sender, HashSet<ClientAgent> clients) {
		super(sender);
		this.clients = clients;
	}
	
	public HashSet<ClientAgent> getClients() {
		return clients;
	}


}
