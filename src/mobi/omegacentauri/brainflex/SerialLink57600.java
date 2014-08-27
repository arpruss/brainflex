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

public class SerialLink57600 extends SerialDataLink {
	// connect to 57600 baud

	public SerialLink57600(String port) throws Exception {
		super(port);
	}

	public int getFixedBaud() {
		return 57600;
	}

	@Override
	protected void setBaud(int baud) {
	}
}
