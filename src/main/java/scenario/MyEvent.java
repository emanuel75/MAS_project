package scenario;

import rinde.sim.event.Event;

public class MyEvent extends Event {

	private long currentTime;
	
	public MyEvent(Enum<?> type, Object issuer) {
		super(type, issuer);
		// TODO Auto-generated constructor stub
	}

	public MyEvent(Enum<?> type) {
		super(type);
		// TODO Auto-generated constructor stub
	}

	public MyEvent(Enum<?> type, Object issuer, long currentTime) {
		super(type, issuer);
		this.currentTime = currentTime;
	}

	public long getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
	}
	
	

}
