/**
*
* Copyright (c) 2013 Alexander Pruss
* Distributed under the GNU GPL v2 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

public abstract class DataLink {
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
}
