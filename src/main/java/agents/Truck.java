package agents;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.MovingRoadUser;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadModel.PathProgress;
import rinde.sim.lab.common.packages.Package;

public class Truck implements MovingRoadUser{

	private RoadModel rm;
	private Point startLocation;
	private String truckID;
	private double speed;
	private Package load;
	protected static final Logger LOGGER = LoggerFactory.getLogger(Truck.class);
	
	public Truck(String truckID, Point startLocation, double speed){
		this.truckID = truckID;
		this.startLocation = startLocation;
		this.speed = speed;
	}
	
	public String getTruckID(){
		return truckID;
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		this.rm = model;
		this.rm.addObjectAt(this, startLocation);
	}

	@Override
	public double getSpeed() {
		return speed;
	}
	
	public RoadModel getRoadModel(){
		return rm;
	}
	
	public PathProgress drive(Queue<Point> path, long time){
		return this.rm.followPath(this, path, time);
	}
	
	public Point getPosition(){
		return rm.getPosition(this);
	}
	
	public Point getLastCrossRoad(){
		return rm.getLastCrossRoad(this);
	}
	
	public boolean hasLoad(){
		return load != null;
	}
	
	public Package getLoad(){
		return this.load;
	}
	
	public boolean tryPickup(String PackageId){
		if(load == null){
			Set<Package> packages = rm.getObjectsAt(this, Package.class);
			if(!packages.isEmpty()){
				Iterator<Package> it = packages.iterator();
				boolean foundIt = false;
				Package p = null;
				while(it.hasNext() && foundIt==false){
					p = it.next();
					if(p.packageID.equals(PackageId)){
						foundIt = true;
					}
				}
				if(foundIt){
					load = p;
					p.pickup();
					LOGGER.info(this.truckID + " picked up "+p);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean tryDelivery(){
		if(load!=null){
			if(load.getDeliveryLocation().equals(this.getPosition())){
				LOGGER.info(this.truckID + " delivered "+load);
				load.deliver();
				load = null;
				return true;
			}
		}
		return false;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	
}
