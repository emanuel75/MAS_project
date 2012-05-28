package agents;

import messages.ClientRequestMessage;
import rinde.sim.core.graph.Point;
import rinde.sim.lab.common.packages.Package;

public class ClientAgent extends Agent {

	private Package myClient;
	private Agency agency;
	
	public ClientAgent(Package myClient, Agency agency, double radius, double reliability){
		super(radius,reliability);
		this.myClient = myClient;
		this.agency = agency;
		
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

}
