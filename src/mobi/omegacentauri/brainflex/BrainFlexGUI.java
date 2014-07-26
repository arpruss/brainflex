package mobi.omegacentauri.brainflex;

import java.util.List;

public interface BrainFlexGUI {
	void updateGraphs();
	void terminate();
	int getMode();
	List<MindFlexReader.Data> getDataCopy();
	List<Mark> getMarksCopy();
	MindFlexReader getMindFlexReader();
	
	public class Mark {
		int t;
		int rawCount;
		
		public Mark(int t, int rawCount) {
			this.t = t;
			this.rawCount = rawCount;
		}
	}
	
}
