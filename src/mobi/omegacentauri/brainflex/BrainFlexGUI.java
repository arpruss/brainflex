package mobi.omegacentauri.brainflex;

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
	public void writeMarks(FileOutputStream out) throws IOException {
		DataOutputStream dataOut = new DataOutputStream(out);
		dataOut.writeInt(marks.size());
		for (Mark m : marks)
			m.write(dataOut);
		dataOut.flush();
	}
	
	public void readMarks(DataInputStream in) throws IOException {
		int count = in.readInt();
		for (int i = 0 ; i < count ; i++)
			marks.add(new Mark(in));
	}
}
