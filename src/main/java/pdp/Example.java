package pdp;

import java.util.Random;

import rinde.sim.event.pdp.StandardType;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.TimedEvent;
import scenario.MyType;
import scenario.StatisticsCollector;

/**
 * 
 */
public class Example {

	public static void main(String[] args) throws Exception {
		ScenarioBuilder builder = new ScenarioBuilder(StandardType.ADD_TRUCK, 
														StandardType.ADD_PACKAGE,
														StandardType.REMOVE_PACKAGE,
														MyType.ADD_AGENCY,
														MyType.MODIFY_SPEED,
														MyType.CHANGE_WINTER,
														MyType.REMOVE_CLIENT,
														MyType.ROAD_SPEED);

		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, // at
																				// time
																				// 0
				1, new ScenarioBuilder.EventTypeFunction(MyType.ADD_AGENCY)));

		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, // at
																				// time
																				// 0
				3, // amount of trucks to be added
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));

		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, 10,
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_PACKAGE)));

		int timeStep = 100000000;

		builder.add(new ScenarioBuilder.TimeSeries<TimedEvent>(0, // start time
				20 * timeStep, // end time
				timeStep, // step
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_PACKAGE)));

		Scenario scenario = builder.build();

		builder = new ScenarioBuilder(StandardType.ADD_TRUCK, 
										StandardType.ADD_PACKAGE,
										StandardType.REMOVE_PACKAGE,
										MyType.ADD_AGENCY,
										MyType.MODIFY_SPEED,
										MyType.CHANGE_WINTER,
										MyType.REMOVE_CLIENT,
										MyType.ROAD_SPEED);

		timeStep = 10000000;

		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, // at
																				// time
																				// 0
				1, new ScenarioBuilder.EventTypeFunction(MyType.ADD_AGENCY)));

		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, // at
																				// time
																				// 0
				3, // amount of trucks to be added
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));

		/*
		 * builder.add( new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
		 * 0, 10, new ScenarioBuilder.EventTypeFunction(
		 * StandardType.ADD_PACKAGE ) ) );
		 */

		for (int i = 0; i < 10; i++) {
			Random rand = new Random();
			int time = SimpleController.generateRandomInteger(0, 20 * timeStep, rand);

			builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(time, 1,
					new ScenarioBuilder.EventTypeFunction(StandardType.ADD_PACKAGE)));
			int time2 = SimpleController.generateRandomInteger(time, 20 * timeStep, rand);
			if (SimpleController.generateRandomInteger(1, 10, rand) < 6) {
//			int time2 = 40000000;
//			if(i==1){
				builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(time2, 1,
						new ScenarioBuilder.EventTypeFunction(MyType.REMOVE_CLIENT)));
			}

		}

		/*for (int i = 0; i < 10; i++) {
			Random rand = new Random();
			int time = SimpleController.generateRandomInteger(0, 20 * timeStep, rand);

			builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(time, 1,
					new ScenarioBuilder.EventTypeFunction(StandardType.REMOVE_PACKAGE)));
		}*/
		/*
		 * builder.add( new ScenarioBuilder.TimeSeries<TimedEvent>( 0, // start
		 * time 20*timeStep, // end time timeStep, // step new
		 * ScenarioBuilder.EventTypeFunction( StandardType.ADD_PACKAGE ) ) );
		 */

		/*
		 * builder.add( new ScenarioBuilder.TimeSeries<TimedEvent>( 0, // start
		 * time 20*timeStep, // end time timeStep, // step new
		 * ScenarioBuilder.EventTypeFunction( MyType.ROAD_SPEED ) ) );
		 */

		Scenario scenario2 = builder.build();

		/*
		 * builder.add( new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
		 * 50000000, // end time 1, // step new
		 * ScenarioBuilder.EventTypeFunction( StandardType.REMOVE_PACKAGE ) ) );
		 */

		/*
		 * builder.add( new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
		 * 0, 1, new ScenarioBuilder.EventTypeFunction( MyType.CHANGE_WINTER ) )
		 * );
		 */
		//
		//

		final String MAP_DIR = "../../RinSim2/core/files/maps/";

		new SimpleController(scenario2, -1, MAP_DIR + "leuven-simple.dot");
	}
}
