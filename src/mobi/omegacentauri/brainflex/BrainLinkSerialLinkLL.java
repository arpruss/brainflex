/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

// serial proxied via BrainLink

package mobi.omegacentauri.brainflex;

import java.io.IOException;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class BrainLinkSerialLinkLL extends DataLink {
  // Use information from: http://www.brainlinksystem.com/brainlink-hardware-description
  // and calculator from: http://www.avrcalc.elektronik-projekt.de/xmega/baud_rate_calculator
  // with 32MHz clock rate.
	private static final byte[] BAUD9600 = { '*', 'C', 829>>8, 829&0xFF, -2 };
	private static final byte[] BAUD57600 = { '*', 'C', 0, (byte)135, -2 };
	private static final byte[] BAUD1200 = { '*', 'C', (byte)(6663>>8), (byte)(6663&0xFF), -2 };
	private static final byte[] BAUD19200 = { '*', 'C', (byte)(825>>8), (byte)(825&0xFF), -3 };
	private static final byte[] BAUD115200 = { '*', 'C', 0, (byte)(131&0xFF), -3 };
	private static final byte[] BAUD38400 = { '*', 'C', 0, (byte)(204&0xFF), -2 };

	private SerialPort p;
	private int baud;

	public BrainLinkSerialLinkLL(String port) {
//		CommPortIdentifier id;
		try {
			System.out.println("Opening "+port);
			p = new SerialPort(port);
			if (! p.openPort())
				throw(new IOException("Cannot open "+p.getPortName()));
			System.out.println("Opened port "+p.getPortName());
			p.writeByte((byte)'*');
		} catch (Exception e) {
			System.err.println("Ooops "+e);
			System.exit(1);
		}
	}

	public void start(int baud) {
		this.baud = baud;
		try {
			if (baud == 9600)
				p.writeBytes(BAUD9600);
			else if (baud == 57600)
				p.writeBytes(BAUD57600);
			else if (baud == 1200)
				p.writeBytes(BAUD1200);
			else if (baud == 19200)
				p.writeBytes(BAUD19200);
			else if (baud == 115200)
				p.writeBytes(BAUD115200);
			else if (baud == 38400)
				p.writeBytes(BAUD38400);
			else {
				System.err.println("Unrecognized baud "+baud);
			}
		} catch (SerialPortException e) {
		}
	}

	public void stop() {
		try {
			p.writeByte((byte)'Q');
		} catch (SerialPortException e) {			
		}
		try {
			p.closePort();
		} catch (SerialPortException e) {
		}
	}

	@Override
	public byte[] receiveBytes() {
		byte[] buff = new byte[0];

		try {
			p.writeBytes(new byte[] { '*','r' } );
			if (!readUntil((byte)'*',100) || !readUntil((byte)'r',600))
				return buff;
			byte[] oneByte = p.readBytes(1, scaleTimeout(50));
			if (oneByte.length != 1)
				return buff;
			int length = (0xFF&(int)(oneByte[0]));
			length = (length-1)&0xFF;
			if (length == 0)
				return buff;
			//System.out.println("data "+length);
			byte[] out = p.readBytes(length, scaleTimeout(5*length)); 
			if (out.length == length) {
				buff = out;
				//BrainFlex.dumpData(out);
			}
		} catch (SerialPortException e) {
		} catch (SerialPortTimeoutException e) {
		}

		return buff;
	}

	// Timeouts designed for 9600 baud
	private boolean readUntil(byte b, int timeout) {
		long t1 = getTimeoutTime(timeout);
		do {
			try {
				byte[] oneByte = p.readBytes(1, (int)(1+t1-System.currentTimeMillis()));
				if (oneByte[0] == b)
					return true;
			} catch (SerialPortException e) {
				return false;
			} catch (SerialPortTimeoutException e) {
			}
		} while(System.currentTimeMillis() <= t1);
		return false;
	}
	
	private int scaleTimeout(int timeout) {
		return timeout * 9600 / baud;
	}
	
	private long getTimeoutTime(int timeout) {
		timeout = timeout * 9600 / baud;
		if (timeout>0)
			timeout=2;
		return System.currentTimeMillis() + timeout;
	}

	@Override
	public void transmit(byte... data) {
		try {
			p.writeBytes(new byte[] { (byte)'*', (byte)'t', (byte)data.length });
			p.writeBytes(data);
		} catch (SerialPortException e) {
		}
	}

	@Override
	public void clearBuffer() {
		receiveBytes();
	}
}
