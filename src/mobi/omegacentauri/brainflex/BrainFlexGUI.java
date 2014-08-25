package mobi.omegacentauri.brainflex;

import java.util.List;


public abstract class BrainFlexGUI {
	protected List<Mark> marks;

	abstract void updateGraphs();
	abstract void terminate();
	abstract int getMode();
	abstract List<MindFlexReader.PowerData> getPowerDataCopy();
	abstract List<Integer> getRawDataCopy();
	abstract List<Mark> getMarksCopy();
	abstract MindFlexReader getMindFlexReader();
	abstract void log(String s);
	
	
	public void addMark(Mark mark) {
		synchronized(marks) {
			marks.add(mark);
		}		
	}
}
