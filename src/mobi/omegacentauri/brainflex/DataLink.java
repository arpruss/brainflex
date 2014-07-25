/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

public abstract class DataLink {
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

	public abstract void clearBuffer();
	
	public abstract void start(int baud);
	
	public abstract void stop();

	// quick pre-initialization, possibly at a different baud rate from the main one
	public abstract void preStart(int baud, byte[] data);
}
