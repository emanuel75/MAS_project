package messages;


import ants.ClientPath;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;

public class BidMessage extends Message {
	
	private ClientPath closestClient;
	
	public BidMessage(CommunicationUser sender, ClientPath closestClient) {
		super(sender);
		this.closestClient = closestClient;
	}
	
	public ClientPath getClosestClient() {
		return closestClient;
	}
	

}
