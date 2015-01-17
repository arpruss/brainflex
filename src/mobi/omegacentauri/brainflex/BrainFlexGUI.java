package mobi.omegacentauri.brainflex;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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

	public BrainFlexGUI() {
		marks = new ArrayList<Mark>();
	}
	
	public void addMark(Mark mark) {
		synchronized(marks) {
			marks.add(mark);
		}		
	}
	public void writeMarks(BufferedWriter out) throws IOException {
		for (Mark m : marks)
			out.write(MindFlexReader.MARKER_MARK+m.toString()+"\n");
	}
	
	public void addMark(String s) {
		addMark(new Mark(s));
	}
}
