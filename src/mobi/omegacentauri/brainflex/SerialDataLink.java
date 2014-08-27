/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

// serial proxied via BrainLink

package mobi.omegacentauri.brainflex;

import jssc.SerialPort;
import jssc.SerialPortException;

abstract public class SerialDataLink extends DataLink {
	protected SerialPort p;

	public SerialDataLink(String port) throws Exception {
		p = new SerialPort(port);
		
		int busyTries = 4;

		while (busyTries-- > 0 && ! p.isOpened()) {
			try {
				p.openPort();
			}
			catch (SerialPortException e) {
				if (busyTries <= 0 || ! e.getExceptionType().equals(SerialPortException.TYPE_PORT_BUSY)) 
					throw e;
			}
		}
		
		p.setParams(115200, 8, 1, 0);
	}
	
	abstract protected void setBaud(int baud);

	public void start(int baud) {
		setBaud(baud);
	}

	public void stop() {
		try {
			p.closePort();
		} catch (SerialPortException e) {
		}
	}

	@Override
	public byte[] receiveBytes() {
		try {
			byte[] data = p.readBytes();
			if (data != null)
				return data;
		} catch (SerialPortException e) {
		}

		return null;
	}

//	private int scaleTimeout(int timeout) {
//		return timeout * 9600 / baud;
//	}
	
	@Override
	public void transmit(byte... data) {
		try {
			p.writeBytes(data);
		} catch (SerialPortException e) {
		}
	}

	@Override
	public void clearBuffer() {
		try {
			p.readBytes();
		} catch (SerialPortException e) {
		}
	}

	@Override
	public int getFixedBaud() {
		return 0;
	}
}
