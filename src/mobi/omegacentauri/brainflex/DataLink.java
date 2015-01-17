/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

public abstract class DataLink {
	 int baud;
	
	 // returns 0 if baud rate is flexible; otherwise, return the baud rate;
	 public abstract int getFixedBaud();
	 
	// returns null if nothing available
	 public abstract byte[] receiveBytes();

	 public abstract void transmit(byte... data);
	 
	 public void transmit(int... data) {
		 byte[] out = new byte[data.length];
		 for (int i=0; i < data.length; i++) {
			 out[i] = (byte)(int)data[i];
		 }
		 transmit(out);
	 }
	 
	public boolean isRaw() {
		return true;
	}
	
	public String readLine() {
		return null;
	}

	public abstract void clearBuffer();
	
	public abstract void start(int baud);
	
	public abstract void stop();

	// quick pre-initialization, possibly at a different baud rate from the main one
	// The following implementation works for fixed-baud datalinks (assuming baud <= fixed baud).
	public void preStart(int baud, byte[] data) {
		MindFlexReader.dumpData(data);
		transmitFakeBaud(baud, data);
	}

	
	static public byte[] upscaleBaud(int from, int to, byte[] data) {
		double ratio = (double)to / from;
		int inBits = 10 * data.length;
		int outBits = (int) (0.5 + ratio * inBits);
		byte[] out = new byte[( outBits + 9 ) / 10];
		for (int i=0; i<outBits; i++)
			put8N1Bit(out, i, get8N1Bit(data, (int)(i / ratio)));
		MindFlexReader.dumpData(out);
		return out;
	}
	
	// fakes a transmission at a lower baud rate
	// assume 8N1
	public void transmitFakeBaud(int fakeBaud, byte... data) {
		transmit(upscaleBaud(fakeBaud, baud, data));
	}
	
	private static boolean get8N1Bit(byte[] data, int bit) {
		if (bit % 10 == 0) // START
			return false;
		if (bit % 10 == 9) // STOP
			return true;
		if (data.length <= bit/10)
			return false;
		return 0 != ( data[bit/10] & ( 1 << ( (bit % 10) - 1 ) ) );
	}
	
	private static void put8N1Bit(byte[] out, int bit, boolean value) {
		if (bit % 10 == 0 || bit % 10 == 9 || bit/10 >= out.length) // ignore start/stop/overflow
			return;
		if (value)
			out[bit/10] |= 1 << (( bit % 10) - 1);
		else
			out[bit/10] &= ~(1 << (( bit % 10) - 1));
	}

	public boolean eof() {
		return false;
	}
	
	public boolean isRealTime() {
		return true;
	}
}
