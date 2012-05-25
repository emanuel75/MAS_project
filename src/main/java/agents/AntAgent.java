package agents;

import ants.Ant;

public class AntAgent extends Agent {
	
	private Ant ant;
	
	public AntAgent(Ant ant, double radius, double reliability){
		super(radius,reliability);
		this.ant = ant;
	}

}
