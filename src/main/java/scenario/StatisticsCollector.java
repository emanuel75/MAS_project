package scenario;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import agents.Truck;
import agents.ClientAgent;
import agents.TaxiAgent;
import rinde.sim.lab.common.packages.Package;

public class StatisticsCollector implements Listener {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsCollector.class);
	private double fuelPrice = 1.61;
	private double startFare = 2.4;
	private double minFee = 2;
	private double routeLength = 0;
	private double deliveryTime = 0;

	private HashMap<Truck, ArrayList<Delivery>> deliveries;
	private HashMap<String, Delivery> waitings;

	public StatisticsCollector() {
		super();
		this.deliveries = new HashMap<Truck, ArrayList<Delivery>>();
		this.waitings = new HashMap<String, Delivery>();
	}

	@Override
	public void handleEvent(Event e) {
		if (e instanceof MyEvent) {
			MyEvent me = (MyEvent) e;
			if (me.getEventType() == TaxiAgent.Type.PICKUP) {
				// LOGGER.info(me.getIssuer().toString() +
				// "------------------ I picked up." + me.getCurrentTime());
				if (me.getIssuer() instanceof TaxiAgent) {
					TaxiAgent ta = (TaxiAgent) me.getIssuer();
					// deliveries.add(new
					// Delivery(me.getCurrentTime(),-1,ta.getTruck()));
					if (!deliveries.containsKey(ta.getTruck())) {
						ArrayList<Delivery> al = new ArrayList<Delivery>();
						al.add(new Delivery(me.getCurrentTime(), -1));
						deliveries.put(ta.getTruck(), al);
					} else {

						deliveries.get(ta.getTruck()).add(new Delivery(me.getCurrentTime(), -1));
					}
					waitings.get(ta.getPackageId()).setEndTime(me.getCurrentTime());
					System.out.println("Average waiting time: " + computeAverageWaiting());
					System.out.println("Maximum waiting time: " + computeMaximumWaiting());
				}
			} else if (me.getEventType() == TaxiAgent.Type.DELIVERY) {
				if (me.getIssuer() instanceof TaxiAgent) {
					TaxiAgent ta = (TaxiAgent) me.getIssuer();
					// deliveries.get(ta.getTruck()).setEndTime(me.getCurrentTime());

					ArrayList<Delivery> al = deliveries.get(ta.getTruck());
					al.get(al.size() - 1).setEndTime(me.getCurrentTime());
					System.out.println("---------Delivery happened: "
							+ String.valueOf(me.getCurrentTime() - al.get(al.size() - 1).getStartTime()));
					deliveryTime = deliveryTime + me.getCurrentTime() - al.get(al.size() - 1).getStartTime();
					System.out.println("Average delivery time: " + computeAverageDelivery());
					System.out.println("Maximum delivery time: " + computeMaximumDelivery());
				}
			} else if (me.getEventType() == ClientAgent.Type.START_AGENT) {
				if (me.getIssuer() instanceof ClientAgent) {

					ClientAgent ca = (ClientAgent) me.getIssuer();
					// deliveries.add(new
					// Delivery(me.getCurrentTime(),-1,ta.getTruck()));

					waitings.put(ca.getClient().getPackageID(), new Delivery(me.getCurrentTime(), -1));
					System.out.println(ca.getClient().getPackageID() + " started waiting.");

				}
			}

		}
	}

	public double computeAverageDelivery() {

		double sum = 0;
		int cnt = 0;

		for (Truck t : deliveries.keySet()) {
			for (Delivery d : deliveries.get(t)) {
				if (d.getEndTime() != -1) {
					sum = sum + (d.getEndTime() - d.getStartTime());
					cnt++;
				}
			}
		}

		return sum / cnt;
	}

	public long computeMaximumDelivery() {
		long max = 0;
		for (Truck t : deliveries.keySet()) {
			for (Delivery d : deliveries.get(t)) {
				if (d.getEndTime() != -1) {
					if (max < d.getEndTime() - d.getStartTime())
						max = d.getEndTime() - d.getStartTime();
				}

			}
		}
		return max;
	}

	public double computeAverageWaiting() {

		double sum = 0;
		int cnt = 0;

		for (String s : waitings.keySet()) {
			if (waitings.get(s).getEndTime() != -1) {
				sum = sum + (waitings.get(s).getEndTime() - waitings.get(s).getStartTime());
				cnt++;
			}

		}

		return sum / cnt;
	}

	public long computeMaximumWaiting() {

		long max = 0;
		for (String s : waitings.keySet()) {
			if (waitings.get(s).getEndTime() != -1) {
				if (max < waitings.get(s).getEndTime() - waitings.get(s).getStartTime())
					max = waitings.get(s).getEndTime() - waitings.get(s).getStartTime();
			}

		}

		return max;
	}

	public double computeProfit() {
		return deliveryTime * minFee - routeLength * fuelPrice;
	}

	public double computeAverageRouteLength() {
		return 0;
	}

	public double computeClientsPerHour() {
		return 0;
	}
}
