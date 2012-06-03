package scenario;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import agents.Agency;
import agents.ClientAgent;
import agents.TaxiAgent;
import agents.Truck;

public class StatisticsCollector implements Listener {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsCollector.class);
	private double fuelPrice = 1.61;
	private double startFare = 2.4;
	private double minFee = 2;
	private double routeLength = 0;
	private double deliveryTime = 0;
	private Agency agency;
	private int delivered_clients = 0;

	private HashMap<Truck, ArrayList<Delivery>> deliveries;
	private HashMap<String, Delivery> waitings;
	private HashMap<Truck, ArrayList<Delivery>> pickups;
	private HashMap<Truck, ArrayList<Delivery>> idles;
	private String newline = System.getProperty("line.separator");
	private boolean isAppend = false;

	public StatisticsCollector(Agency agency) {
		super();
		this.deliveries = new HashMap<Truck, ArrayList<Delivery>>();
		this.waitings = new HashMap<String, Delivery>();
		this.pickups = new HashMap<Truck, ArrayList<Delivery>>();
		this.idles = new HashMap<Truck, ArrayList<Delivery>>();
                this.agency = agency;
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
                    agency.setAvgWaitingTime(computeAverageWaiting()/10000);
					System.out.println("Maximum waiting time: " + computeMaximumWaiting());
					writeIntoFile(newline + "\nAverage waiting time: " + computeAverageWaiting() + newline +
							"Maximum waiting time: " + computeMaximumWaiting());
					
					ArrayList<Delivery> al = pickups.get(ta.getTruck());
					al.get(al.size() - 1).setEndTime(me.getCurrentTime());
					System.out.println("Average pickup time: " + computeAveragePickup());
					System.out.println("Maximum pickup time: " + computeMaximumPickup());
					agency.setAvgPickUpTime(computeAveragePickup());
					writeIntoFile(newline + "\nAverage pickup time: " + computeAveragePickup() + newline +
							"Maximum pickup time: " + computeMaximumPickup());
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
                                        agency.setAvgDeliveryTime(computeAverageDelivery()/10000);
					System.out.println("Maximum delivery time: " + computeMaximumDelivery());
					System.out.println("Profit: " + computeProfit());
					writeIntoFile(newline + "Average delivery time: " + computeAverageDelivery() + newline +
							"Maximum delivery time: " + computeMaximumDelivery() + newline +
							"Profit: " + computeProfit());
					delivered_clients++;
					
			}
			
			} else if (me.getEventType() == ClientAgent.Type.START_AGENT) {
				if (me.getIssuer() instanceof ClientAgent) {

					ClientAgent ca = (ClientAgent) me.getIssuer();
					// deliveries.add(new
					// Delivery(me.getCurrentTime(),-1,ta.getTruck()));

					waitings.put(ca.getClient().getPackageID(), new Delivery(me.getCurrentTime(), -1));
					System.out.println(ca.getClient().getPackageID() + " started waiting.");

				}
			} else if (me.getEventType() == TaxiAgent.Type.START_PICKUP) {
				if (me.getIssuer() instanceof TaxiAgent) {
					TaxiAgent ta = (TaxiAgent) me.getIssuer();
					// deliveries.add(new
					// Delivery(me.getCurrentTime(),-1,ta.getTruck()));
					if (!pickups.containsKey(ta.getTruck())) {
						ArrayList<Delivery> al = new ArrayList<Delivery>();
						al.add(new Delivery(me.getCurrentTime(), -1));
						pickups.put(ta.getTruck(), al);
					} else {

						pickups.get(ta.getTruck()).add(new Delivery(me.getCurrentTime(), -1));
					}
					
					ArrayList<Delivery> al = idles.get(ta.getTruck());
					al.get(al.size() - 1).setEndTime(me.getCurrentTime());
					System.out.println("Average idle time: " + computeAverageIdle());
					System.out.println("Maximum idle time: " + computeMaximumIdle());
					agency.setAvgIdleTime(computeAverageIdle());
					writeIntoFile(newline + "\nAverage idle time: " + computeAverageIdle() + newline +
							"Maximum idle time: " + computeMaximumIdle());
					}
			} else if (me.getEventType() == TaxiAgent.Type.IDLE) {
				if (me.getIssuer() instanceof TaxiAgent) {
					TaxiAgent ta = (TaxiAgent) me.getIssuer();
					
					if (!idles.containsKey(ta.getTruck())) {
						ArrayList<Delivery> al = new ArrayList<Delivery>();
						al.add(new Delivery(me.getCurrentTime(), -1));
						idles.put(ta.getTruck(), al);
					} else {

						idles.get(ta.getTruck()).add(new Delivery(me.getCurrentTime(), -1));
					}
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
	
	public double computeAveragePickup() {

		double sum = 0;
		int cnt = 0;

		for (Truck t : pickups.keySet()) {
			for (Delivery d : pickups.get(t)) {
				if (d.getEndTime() != -1) {
					sum = sum + (d.getEndTime() - d.getStartTime());
					cnt++;
				}
			}
		}

		return sum / cnt;
	}
	
	public long computeMaximumPickup() {
		long max = 0;
		for (Truck t : pickups.keySet()) {
			for (Delivery d : pickups.get(t)) {
				if (d.getEndTime() != -1) {
					if (max < d.getEndTime() - d.getStartTime())
						max = d.getEndTime() - d.getStartTime();
				}

			}
		}
		return max;
	}
	
	public double computeAverageIdle() {

		double sum = 0;
		int cnt = 0;

		for (Truck t : idles.keySet()) {
			for (Delivery d : idles.get(t)) {
				if (d.getEndTime() != -1) {
					sum = sum + (d.getEndTime() - d.getStartTime());
					cnt++;
				}
			}
		}

		return sum / cnt;
	}
	
	public long computeMaximumIdle() {
		long max = 0;
		for (Truck t : idles.keySet()) {
			for (Delivery d : idles.get(t)) {
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
	
	public void computeAverageClientsPerTime(long currentTime, long time) {
		double x = new Double(time).longValue();
		double y = new Double(currentTime).longValue();
		double stat = delivered_clients*(x/y);
		System.out.println(delivered_clients);
				System.out.println(time);
						System.out.println(currentTime);
		//int time = 1000000;
		System.out.println("Number of clients delivered per " + time + ": " + stat);
		writeIntoFile(newline + "Number of clients delivered per " + time + ": " + stat);
		
	}
	
	public void increaseRouteLength(double d) {
		routeLength = routeLength + d;
	}
	
	public void writeIntoFile(String text) {		
		 try {
			 FileWriter fstream = new FileWriter("files/statistics_ant.txt",isAppend);
			  BufferedWriter out = new BufferedWriter(fstream);
			  out.write(text);
			  out.close();
			  isAppend = true;
			 /* output = new BufferedWriter(new FileWriter(file));
		  output.write(text);
		  output.close();  */
		  } catch (IOException ioe) {
			  System.out.println("Can't reach file.");
		  }
	}
	
	public void removeClient(ClientAgent ca) {
		
	}
}
