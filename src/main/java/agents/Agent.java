package agents;

import java.util.Queue;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;

public class Agent implements SimulatorUser, CommunicationUser {
	
	protected SimulatorAPI simulator;
	protected Queue<Point> path;
	protected CommunicationAPI communicationAPI;
	protected double reliability, radius;
	protected Mailbox mailbox;
	protected RoadModel rm;
	
	public Agent(double radius, double reliability){
		this.radius = radius;
		this.reliability = reliability;
		this.mailbox = new Mailbox();
	}
	
	public void initialize(RoadModel rm){
		this.rm = rm;
	}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		this.communicationAPI = api;
	}

	@Override
	public Point getPosition() {
		return new Point(0, 0);
	}

	@Override
	public double getRadius() {
		return this.radius;
	}

	@Override
	public double getReliability() {
		return this.reliability;
	}

	@Override
	public void receive(Message message) {
		this.mailbox.receive(message);
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
	}

}
