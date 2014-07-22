/**
*
* Copyright (c) 2013 Alexander Pruss
* Distributed under the GNU GPL v2 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

public class BrainLinkSerialLinkLL extends DataLink {
	private static final byte[] BAUD9600 = { '*', 'C', 829>>8, 829&0xFF, -2 };
	private static final byte[] BAUD57600 = { '*', 'C', 0, (byte)135, -2 };
	private static final byte[] BAUD1200 = { '*', 'C', (byte)(6632>>8), (byte)(6632&0xFF), -2 };
	SerialPort p;
	InputStream iStream;
	OutputStream oStream;
	private int baud;

	public BrainLinkSerialLinkLL(String port) {
		CommPortIdentifier id;
		try {
			id = null;
			CommPortIdentifier ignoreCaseID = null;
			System.out.println("Searching for "+port);
			Enumeration<CommPortIdentifier> ids = CommPortIdentifier.getPortIdentifiers();
			while(ids.hasMoreElements()) {
				id = ids.nextElement();
				if (id.getName().equals(port))
					break;
				if (id.getName().equalsIgnoreCase(port))
					ignoreCaseID = id;
			}
			if (id == null)
				id = ignoreCaseID;
			if (id == null)
				throw new IOException("Cannot find serial port");

			p = (SerialPort) id.open("BrainLinkSerialLinkLL", 5000);
			p.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			iStream = p.getInputStream();
			oStream = p.getOutputStream();
			oStream.write(new byte[] { '*' });
		} catch (Exception e) {
			System.err.println("Ooops "+e);
			System.exit(1);
		}
	}

	public void start(int baud) {
		this.baud = baud;
		try {
			oStream.write(new byte[] { '*' });
			if (baud == 9600)
				oStream.write(BAUD9600);
			else if (baud == 57600)
				oStream.write(BAUD57600);
			else if (baud == 1200) 
				oStream.write(BAUD1200);
			else {
				System.err.println("Unrecognized baud "+baud);
			}
		} catch (IOException e) {
			System.err.println("Ooops "+e);
		}

	}

	public void stop() {
		try {
			iStream.close();
		} catch (IOException e) {
		}
		try {
			oStream.close();
		} catch (IOException e) {
		}
		p.close();
	}

	@Override
	public byte[] receiveBytes() {
		byte[] buff = new byte[0];
		byte[] oneByte = new byte[1];

		try {
			oStream.write(new byte[] { '*', 'r' } );
			if (!readUntil(iStream,(byte)'*',50) || !readUntil(iStream,(byte)'r',50))
				return buff;
			if (!readBytes(iStream,oneByte,50)) 
				return buff;
			int length = (0xFF&(int)(oneByte[0]));
			length = (length-1)&0xFF;
			if (length == 0)
				return buff;
			//System.out.println("data "+length);
			buff = new byte[length]; 
			if(!readBytes(iStream,buff,5*length))
				return buff;
			//BrainFlex.dumpData(buff);
		} catch (IOException e) {
		}

		return buff;
	}

	// Timeouts designed for 9600 baud
	private boolean readUntil(InputStream stream, byte b, long timeout) {
		byte[] oneByte = new byte[1];
		long t1 = getTimeoutTime(timeout);
		do {
			try {
				if (0 < stream.available() && 
						1==stream.read(oneByte) && 
						oneByte[0] == b)
					return true;
			} catch (IOException e) {
				return false;
			}
		} while(System.currentTimeMillis() <= t1);
		return false;
	}
	
	private long getTimeoutTime(long timeout) {
		timeout = timeout * 9600 / baud;
		if (timeout>0)
			timeout=2;
		return System.currentTimeMillis() + timeout;
	}

	private boolean readBytes(InputStream stream, byte[] data, long timeout) {
		long t1 = getTimeoutTime(timeout);

		int i = 0;
		while (i < data.length && System.currentTimeMillis() <= t1) {
			int avail;
			try {
				avail = stream.available();
				if (0<avail) {
					if (avail > data.length - i)
						avail = data.length - i;
					i += stream.read(data, i, avail);
				}
			} catch (IOException e) {
				return false;
			}
		}

		return i == data.length;
	}

	@Override
	public void transmit(byte... data) {
		try {
			oStream.write('*');
			oStream.write('t');
			oStream.write(new byte[] { (byte)data.length });
			oStream.write(data);
		} catch (IOException e) {
		}
	}

	@Override
	public void clearBuffer() {
		receiveBytes();
	}
}
