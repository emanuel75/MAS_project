package pdp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;


import agents.Agency;
import agents.ClientAgent;
import agents.Truck;
import agents.TaxiAgent;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.event.Event;
import rinde.sim.lab.common.packages.DeliveryLocation;
import rinde.sim.lab.common.packages.Package;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.Scenario;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.ObjectRenderer;
import rinde.sim.ui.renderers.UiSchema;
import scenario.PdpScenarioController;
import scenario.StatisticsCollector;
import agents.Agency;
import agents.ClientAgent;
import agents.TaxiAgent;

public class SimpleController extends PdpScenarioController {

	private final StatisticsCollector statistics = new StatisticsCollector();

	String map;

	private RoadModel roadModel;
	private CommunicationModel communicationModel;

	private int truckID = 0;
	private int packageID = 0;
	private Graph<MultiAttributeEdgeData> graph;

	private Vector<TaxiAgent> taxiagents;
	private Vector<ClientAgent> clientagents;

	private Agency agency;

	public SimpleController(Scenario scen, int numberOfTicks, String map) throws ConfigurationException {
		super(scen, numberOfTicks);
		this.map = map;
		this.agency = new Agency(-1, 1);
		this.taxiagents = new Vector<TaxiAgent>();
		this.clientagents = new Vector<ClientAgent>();

		initialize();
	}

	@Override
	protected Simulator createSimulator() throws Exception {
		try {
			graph = DotGraphSerializer.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(map);
		} catch (Exception e) {
			throw new ConfigurationException("e:", e);
		}
		roadModel = new RoadModel(graph);
		MersenneTwister rand = new MersenneTwister(123);
		communicationModel = new CommunicationModel(rand, true);
		Simulator s = new Simulator(rand, 10000);
		s.register(roadModel);
		s.register(communicationModel);

		return s;
	}

	@Override
	protected boolean createUserInterface() {
		UiSchema schema = new UiSchema();
		schema.add(Truck.class, new RGB(0, 0, 255));
		schema.add(Package.class, new RGB(255, 0, 0));
		schema.add(DeliveryLocation.class, new RGB(0, 255, 0));

		View.startGui(getSimulator(), 3, new ObjectRenderer(roadModel, schema, false));

		return true;
	}

	@Override
	protected boolean handleAddAgency(Event e) {
		agency.initialize(roadModel);
		getSimulator().register(agency);
		return true;
	}

	@Override
	/**
	 * The speed of the taxis become slower (70% of their original speed) as it would be winter
	 */
	protected boolean handleChangeWinter(Event e) {
		System.out.println("Winter conditions");
		for (TaxiAgent ta : taxiagents) {
			ta.getTruck().setSpeed(ta.getTruck().getSpeed() * 0.7);

		}

		return true;
	}

	@Override
	protected boolean handleHeavyTraffic(Event e) {
		System.out.println("Heavy traffic");

		Random random = new Random();

		int numOfSlow = generateRandomInteger((int) (taxiagents.size() * 0.3), (int) (taxiagents.size() * 0.7), random);
		int[] index = new int[numOfSlow];

		for (int i = 0; i < numOfSlow; i++) {

		}

		for (TaxiAgent ta : taxiagents) {
			ta.getTruck().setSpeed(ta.getTruck().getSpeed() * 0.7);

		}

		return true;
	}

	@Override
	/**
	 * Speed of the taxis change in the range of 5 to 10
	 */
	protected boolean handleModifySpeed(Event e) {
		int START = 5;
		int END = 10;
		Random random = new Random();

		for (TaxiAgent ta : taxiagents) {
			ta.getTruck().setSpeed(generateRandomInteger(START, END, random));
			System.out.println(ta.getTruck().getTruckID() + " - new speed: " + ta.getTruck().getSpeed());
		}

		return true;
	}

	/**
	 * Some of the roads become blockaded
	 */
	/*
	 * @Override protected boolean handleRoadBlockade(Event e) { int START = 5;
	 * int END = 10; Random random = new Random(); for (TaxiAgent ta :
	 * taxiagents) { ta.getTruck().setSpeed(generateRandomInteger(START, END,
	 * random)); System.out.println(ta.getTruck().getTruckID() +
	 * " - new speed: " + ta.getTruck().getSpeed()); } return true; }
	 */
	@Override
	/**
	 * Speed of the taxis change in the range of 5 to 10
	 */
	protected boolean handleRoadSpeed(Event e) {
		List<?> edges = roadModel.getGraph().getConnections();
		Random rand = new Random();

		for (Object c : edges) {
			if (c instanceof Connection<?>) {
				Connection<?> conn = (Connection<?>) c;
				EdgeData data = conn.edgeData;
				if (data instanceof MultiAttributeEdgeData) {

					MultiAttributeEdgeData maed = (MultiAttributeEdgeData) data;
					if (!Double.isNaN(maed.getMaxSpeed())) {
						double speed = (maed.getMaxSpeed() / 10000) * (7 / 3) * generateRandomInteger(5, 7, rand) * 0.1;
						maed.setMaxSpeed(speed);
						// System.out.println("Road new speed: " + speed);
					}
				}
			}
		}

		return true;

	}

	public static int generateRandomInteger(int startR, int endR, Random rand) {
		if (startR > endR) {
			throw new IllegalArgumentException("Start cannot exceed End.");
		}
		long range = (long) endR - (long) startR + 1;
		long fraction = (long) (range * rand.nextDouble());
		int randomNumber = (int) (fraction + startR);
		return randomNumber;
	}

	@Override
	protected boolean handleAddTruck(Event e) {
		Truck truck = new Truck("Truck-" + truckID++, graph.getRandomNode(getSimulator().getRandomGenerator()), 7);
		getSimulator().register(truck);
		TaxiAgent agent = new TaxiAgent(truck, agency, -1, 1);
		getSimulator().register(agent);
		taxiagents.add(agent);

		agent.addListener(statistics, TaxiAgent.Type.values());
		agency.freeUpTaxi(agent);
		return true;
	}

	@Override
	protected boolean handleAddPackage(Event e) {
		Point pl = graph.getRandomNode(getSimulator().getRandomGenerator());
		DeliveryLocation dl = new DeliveryLocation(graph.getRandomNode(getSimulator().getRandomGenerator()));
		getSimulator().register(pl);
		getSimulator().register(dl);

		Package p = new Package("Package-" + packageID++, pl, dl);
		getSimulator().register(p);
		ClientAgent agent = new ClientAgent(p, agency, -1, 1);
		agent.initialize(roadModel);
		getSimulator().register(agent);
		clientagents.add(agent);
		agent.addListener(statistics, ClientAgent.Type.values());
		agent.startWaiting(getSimulator().getCurrentTime());
		return true;
	}

	@Override
	/**
	 * Cancels 10%-20% of the clients in random way
	 */
	protected boolean handleRemovePackage(Event e) {
		Random rand = new Random();
		int pos = generateRandomInteger((int) (clientagents.size() * 0.1), (int) (clientagents.size() * 0.2), rand);

		ArrayList<Integer> cancelledClients = generateRandomPositions(clientagents.size(), pos);
		Collections.sort(cancelledClients);

		for (int i = cancelledClients.size(); i > 0; i--) {
			ClientAgent agent = clientagents.get(cancelledClients.get(i - 1));

			System.out.println(agent.getClient().getPackageID() + " - client has been cancelled.");

			getSimulator().unregister(agent.getClient());
			getSimulator().unregister(agent);
			clientagents.remove(i);
		}

		return true;
	}

	@Override
	/**
	 * Cancels 10%-20% of the clients in random way
	 */
	protected boolean handleRemoveClient(Event e) {

		Random rand = new Random();
		int pos = generateRandomInteger(0, clientagents.size() - 1, rand);
//		int pos = 0;

		System.out.println(clientagents.get(pos).getClient().getPackageID() + " - client has been cancelled.");
		
		clientagents.get(pos).getClient().unregister();
		getSimulator().unregister(clientagents.get(pos));
		agency.removeClient(clientagents.get(pos));
		clientagents.remove(pos);

		return true;
	}

	public ArrayList<Integer> generateRandomPositions(int size, int numOfPos) {
		Random random = new Random();
		ArrayList<Integer> index = new ArrayList<Integer>();
		Integer tmp;

		for (int i = 0; i < numOfPos; i++) {
			tmp = generateRandomInteger(0, size, random);
			while (index.contains(tmp)) {
				tmp = generateRandomInteger(0, size, random);
			}
			index.add(tmp);
		}

		return index;

	}

}
