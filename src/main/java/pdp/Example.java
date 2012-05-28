package pdp;

import rinde.sim.event.pdp.MyType;
import rinde.sim.event.pdp.StandardType;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.TimedEvent;

/**
 * 
 */
public class Example {
	
	public static void main(String[] args) throws Exception{
		ScenarioBuilder builder = new ScenarioBuilder(StandardType.ADD_TRUCK, StandardType.ADD_PACKAGE, MyType.ADD_AGENCY, MyType.ADD_RESOURCE);
		
		builder.add(
				new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
						0, //at time 0
						1, 
						new ScenarioBuilder.EventTypeFunction(
								MyType.ADD_AGENCY
						)
				)
		);
		
//		builder.add(
//				new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
//						0, //at time 0
//						1, 
//						new ScenarioBuilder.EventTypeFunction(
//								MyType.ADD_RESOURCE
//						)
//				)
//		);
		
		builder.add(
				new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
						0, //at time 0
						3, //amount of trucks to be added
						new ScenarioBuilder.EventTypeFunction(
								StandardType.ADD_TRUCK
						)
				)
		);
		
		builder.add(
				new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
						0, 
						10,
						new ScenarioBuilder.EventTypeFunction(
								StandardType.ADD_PACKAGE
						)
				)
		);
		
		int timeStep = 100000000;
		
		builder.add(
				new ScenarioBuilder.TimeSeries<TimedEvent>(
						0, // start time
						20*timeStep, // end time
						timeStep, // step
						new ScenarioBuilder.EventTypeFunction(
								StandardType.ADD_PACKAGE
						)
				)
		);
//		
//		
		Scenario scenario = builder.build();
		
		final String MAP_DIR = "../../RinSim2/core/files/maps/";

		new SimpleController(scenario, -1, MAP_DIR + "leuven-simple.dot");		
	}
}
