package scenario;

import rinde.sim.core.Simulator;
import rinde.sim.event.Event;
import rinde.sim.lab.session2.example2.StatisticsCollector;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioController;

public class PdpScenarioController extends ScenarioController {

	public PdpScenarioController(Scenario scen, int numberOfTicks) throws ConfigurationException {
		super(scen, numberOfTicks);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean handleCustomEvent(Event e) {
		MyType eT = (MyType) e.getEventType();
		switch (eT) {
		case ADD_AGENCY:
			return handleAddAgency(e);
		case ADD_BLOCKADES:
			return handleAddBlockades(e);
		case CHANGE_WINTER:
			return handleChangeWinter(e);
		case GET_STAT:
			return handleGetStat(e);
		case HEAVY_TRAFFIC:
			return handleHeavyTraffic(e);
		case MODIFY_SPEED:
			return handleModifySpeed(e);
		case REMOVE_CLIENT:
			return handleRemoveClient(e);
		case ROAD_BLOCKADE:
			return handleRoadBlockade(e);
		case ROAD_SPEED:
			return handleRoadSpeed(e);
		default:
			return false;
		}
	}

	@Override
	protected Simulator createSimulator() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	protected boolean handleAddAgency(Event e) {
		return false;
	}

	protected boolean handleAddBlockades(Event e) {
		return false;
	}
	
	protected boolean handleChangeWinter(Event e) {
		return false;
	}

	protected boolean handleGetStat(Event e) {
		return false;
	}
	
	protected boolean handleHeavyTraffic(Event e) {
		return false;
	}

	protected boolean handleModifySpeed(Event e) {
		return false;
	}

	protected boolean handleRemoveClient(Event e) {
		return false;
	}

	protected boolean handleRoadBlockade(Event e) {
		return false;
	}

	protected boolean handleRoadSpeed(Event e) {
		return false;
	}
}
