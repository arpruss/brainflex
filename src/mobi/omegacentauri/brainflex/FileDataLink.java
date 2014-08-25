/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class FileDataLink extends DataLink {
	private DataInputStream in;
	public final int BUFFER_SIZE = 512;
	public boolean eof;

	public FileDataLink(DataInputStream in) throws Exception {
		this.in = in;
		eof = false;
	}
	
	@Override 
	public boolean isRealTime() {
		return false;
	}
	
	@Override
	public boolean eof() {
		return eof;
	}

	public byte[] receiveBytes() {
		byte[] buff = new byte[BUFFER_SIZE];
		int size;
		try {
			size = in.read(buff);
		} catch (IOException e) {
			eof = true;
			return null;
		}
		if (size <= 0) {
			eof = true;
			return null;
		}
		if (size < BUFFER_SIZE) {
			byte[] b2 = new byte[size];
			System.arraycopy(buff, 0, b2, 0, size);
			return b2;
		}
		return buff;
	}

	 public void transmit(byte... data) {
	 }
	 
	 public void transmit(int... data) {
	 }

	public void clearBuffer() {
	}
	
	public void start(int baud) {
		this.baud = baud;
	}
	
	public void stop() {
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// quick pre-initialization, possibly at a different baud rate from the main one
	@Override
	public void preStart(int baud, byte[] data) {
	}

	public boolean isValid() {
		return true;
	}

	@Override
	public int getFixedBaud() {
		return 0;
	}
}
