package mobi.omegacentauri.brainflex;

import java.util.List;

public interface BrainFlexGUI {
	void update();
	void terminate();
	int getMode();
	Pause getPause();
	List<MindFlexReader.Data> getDataCopy();
	List<Mark> getMarksCopy();
	double getScale();
	MindFlexReader getMindFlexReader();
	void setTime(int t, int packetCount, int goodPacketCount);
	
	public class Mark {
		int t;
		int rawCount;
		
		public Mark(int t, int rawCount) {
			this.t = t;
			this.rawCount = rawCount;
		}
	}
	
	public class Pause {
	    int point = -1;
		int pausedBadPacketCount;
		int pausedPacketCount;
	}
}
