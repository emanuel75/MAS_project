package pdp;


import java.util.Iterator;
import java.util.Set;

import org.apache.commons.math.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import agents.Agency;
import agents.ClientAgent;
import agents.ResourceAgent;
import agents.TaxiAgent;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.event.Event;
import rinde.sim.lab.common.packages.DeliveryLocation;
import rinde.sim.lab.common.packages.Package;
import rinde.sim.lab.common.trucks.Truck;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.ObjectRenderer;
import rinde.sim.ui.renderers.UiSchema;

public class SimpleController extends ScenarioController{

	String map;
	
	private RoadModel roadModel;
	private CommunicationModel communicationModel;
	
	private int truckID = 0;
	private int packageID = 0;
	private Graph<MultiAttributeEdgeData> graph;
	
	private Agency agency;
	
	public SimpleController(Scenario scen, int numberOfTicks, String map) throws ConfigurationException {
		super(scen, numberOfTicks);
		this.map = map;
		this.agency = new Agency(-1, 1);
		
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
		schema.add(Truck.class, new RGB(0,0,255));
		schema.add(Package.class, new RGB(255,0,0));
		schema.add(DeliveryLocation.class, new RGB(0,255,0));

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
	protected boolean handleAddTruck(Event e) {
		agency.freeUpTaxi();
		Truck truck = new Truck("Truck-"+truckID++, graph.getRandomNode(getSimulator().getRandomGenerator()), 7);
		getSimulator().register(truck);
		TaxiAgent agent = new TaxiAgent(truck, agency, -1, 1);
		getSimulator().register(agent);
		return true;
	}	

	@Override
	protected boolean handleAddPackage(Event e){
		Point pl = graph.getRandomNode(getSimulator().getRandomGenerator());
		DeliveryLocation dl = new DeliveryLocation(graph.getRandomNode(getSimulator().getRandomGenerator()));
		getSimulator().register(pl);
		getSimulator().register(dl);
		
		Package p = new Package("Package-"+packageID++, pl, dl);
		getSimulator().register(p);
		ClientAgent agent = new ClientAgent(p, agency, -1, 1);
		getSimulator().register(agent);
		return true;
	}
	
	@Override
	protected boolean handleAddResource(Event e){
		System.out.println("lefutottam");
		Set<Point> nodes = roadModel.getGraph().getNodes();
		Iterator<Point> it = nodes.iterator();
		ResourceAgent res;
		while(it.hasNext()){
			res = new ResourceAgent(it.next());
			agency.addResource(res);
			getSimulator().register(res);
		}
		return true;
	}

}
