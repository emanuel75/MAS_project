package messages;

import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;

public class ClientRequestMessage extends Message {

	public ClientRequestMessage(CommunicationUser sender) {
		super(sender);
	}
	
}
