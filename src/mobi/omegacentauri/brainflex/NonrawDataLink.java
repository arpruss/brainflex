/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NonrawDataLink extends DataLink {
	private BufferedReader in;
	public final int BUFFER_SIZE = 512;
	public boolean eof;

	public NonrawDataLink(File file) throws Exception {
		this.in = new BufferedReader(new FileReader(file));
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
		return null;
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
	
	@Override
	public boolean isRaw() {
		return false;
	}
	
	@Override
	public String readLine() {
		try {
			return in.readLine();
		}
		catch(Exception e) {
			return null;
		}
	}
}
