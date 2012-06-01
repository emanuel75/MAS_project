package scenario;

import rinde.sim.lab.common.trucks.Truck;

public class Delivery {

	long startTime;
	long endTime;
	Truck truck;
	
	public Delivery(long startTime, long endTime) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	public Truck getTruck() {
		return truck;
	}
	public void setTruck(Truck truck) {
		this.truck = truck;
	}
	
	
	
}
