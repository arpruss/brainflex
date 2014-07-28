package mobi.omegacentauri.brainflex;

import java.util.List;

public interface BrainFlexGUI {
	void updateGraphs();
	void terminate();
	int getMode();
	List<MindFlexReader.PowerData> getPowerDataCopy();
	List<Integer> getRawDataCopy();
	List<Mark> getMarksCopy();
	MindFlexReader getMindFlexReader();
	void log(String s);
	
	public class Mark {
		int t;
		int rawCount;
		
		public Mark(int t, int rawCount) {
			this.t = t;
			this.rawCount = rawCount;
		}
	}
	
}
