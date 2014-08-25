package mobi.omegacentauri.brainflex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Mark implements Comparable {
	int t;
	int rawCount;
	String descriptor;
	
	public Mark(int t, int rawCount) {
		this(t,rawCount,"");
	}
	
	public Mark(int t, int rawCount, String descriptor) {
		this.t = t;
		this.rawCount = rawCount;
		this.descriptor = descriptor;
	}

	public void write(DataOutputStream dataOut) throws IOException {
		dataOut.writeInt(t);
		dataOut.writeInt(rawCount);
		dataOut.writeUTF(descriptor);
	}		
	
	public Mark(DataInputStream dataIn) throws IOException {
		t = dataIn.readInt();
		rawCount = dataIn.readInt();
		descriptor = dataIn.readUTF();
	}

	@Override
	public int compareTo(Object arg0) {
		Mark m = (Mark)arg0;
		if (t < m.t)
			return -1;
		else if (m.t < t)
			return 1;
		else
			return 0;
	}
}
