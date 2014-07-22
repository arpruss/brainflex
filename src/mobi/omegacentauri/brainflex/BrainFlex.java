/**
*
* Copyright (c) 2013 Alexander Pruss
* Distributed under the GNU GPL v2 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class BrainFlex extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static final int PACKET_NO = -1;
	static final int PACKET_MAYBE = 0;
	public static final String[] POWER_NAMES = { "delta", "theta", "low-alpha", "high-alpha", "low-beta", "high-beta",
		"low-gamma", "mid-gamma"
	};
	private List<Data> data;
	public long t0;
	private int lastSignal;
	private Data curData;

	public BrainFlex() {
		t0 = System.currentTimeMillis();
		data = new ArrayList<Data>();
		lastSignal = 100;

		setSize(640,480);
		add(new MyPanel());
		setVisible(true);
	}

	private class MyPanel extends JPanel {
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		int n = data.size();

		if (n<2)
			return;

		Graphics2D g2 = (Graphics2D) g;
		Dimension s = getContentPane().getSize();
		Point topLeft = getContentPane().getLocation();
		System.out.println(""+topLeft.x+" "+topLeft.y+" "+topLeft.getX()+" "+topLeft.getY());
		double tSize = Math.pow(2, Math.ceil(log2(data.get(n-1).t + 1000 )));
		double tScale = s.getWidth() / tSize;
		double ySize = 0;
		for (Data d: data) 
			for (double y: d.power)
				if (y > ySize)
					ySize = y;
		
		double subgraphHeight = s.getHeight() / (2+POWER_NAMES.length);
		
		double yScale = subgraphHeight / ySize;

		for (int j = 0 ; j < POWER_NAMES.length ; j++) {
			g2.drawChars(POWER_NAMES[j].toCharArray(), 0, POWER_NAMES[j].length(), 
					-topLeft.x, (int)(-topLeft.y + j * yScale * ySize + ySize * .5 * yScale));
		}
		g2.drawChars("Attention".toCharArray(), 0, "Attention".length(), 
				topLeft.x, (int)(topLeft.y + POWER_NAMES.length * yScale * ySize + ySize * .5 * yScale));
		g2.drawChars("Meditation".toCharArray(), 0, "Meditation".length(), 
				topLeft.x, (int)(topLeft.y + (1+POWER_NAMES.length) * yScale * ySize + ySize * .5 * yScale));

		Data d0 = null;
		


		for (int i=0; i<n; i++) {
			Data d1 = data.get(i);
			if (0<i) { 
				if (d0.havePower && d1.havePower) { 
					for (int j=0; j<POWER_NAMES.length; j++) {
						Line2D lin = new Line2D.Double(topLeft.x + d0.t * tScale, 
								topLeft.y + (ySize - d0.power[j]) * yScale + j * yScale * ySize,
								topLeft.x + d1.t * tScale, 
								topLeft.y + (ySize - d1.power[j]) * yScale + j * yScale * ySize);
						g2.draw(lin);
					}
				}
				if (d0.haveAttention && d1.haveAttention) {
					Line2D lin = new Line2D.Double(topLeft.x + d0.t * tScale, 
							topLeft.y + (1 - d0.attention) * subgraphHeight + POWER_NAMES.length * yScale * ySize,
							topLeft.x + d1.t * tScale, 
							topLeft.y + (1 - d1.attention) * subgraphHeight + POWER_NAMES.length * yScale * ySize);
					g2.draw(lin);
				}
				if (d0.haveMeditation && d1.haveMeditation) {
					Line2D lin = new Line2D.Double(topLeft.x + d0.t * tScale, 
							topLeft.y + (1 - d0.meditation) * subgraphHeight + (1+POWER_NAMES.length) * yScale * ySize,
							topLeft.x + d1.t * tScale, 
							topLeft.y + (1 - d1.meditation) * subgraphHeight + (1+POWER_NAMES.length) * yScale * ySize);
					g2.draw(lin);
				}
			}
			d0 = d1;
		}
	}
	}


	public static void main(final String[] args) throws Exception
	{
		BrainFlex bf = new BrainFlex();
		bf.readData();
	}

	public double log2(double d) {
		return Math.log(d)/Math.log(2);
	}

	void readData() throws IOException {
		String comPort;

		comPort = JOptionPane.showInputDialog(null, "Brainlink serial port?");

		byte[] buffer = new byte[0];

		final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		BrainLinkSerialLinkLL dataLink;

		System.out.println("CONNECTING");
		dataLink = new BrainLinkSerialLinkLL(comPort); //brainLink);
		dataLink.start(9600);
		dataLink.transmit(0x03); // 0x00 = 9600 baud, 0x0F = 57600 baud,
		// 0x00 : 9600 : normal
		// 0x01 : ??
		// 0x02 : 57600 : RAW
		// 0x03 : 57600 : lots of 0x82 signals, 4 bytes
		//		sleep(100);
		dataLink.start(57600);
		//		sleep(100);
		System.out.println("CONNECTED");

		do {
			sleep(50);
			byte[] data = dataLink.receiveBytes();
			if (data.length > 0) {
				//brainLink.setFullColorLED(Color.BLUE);
				buffer = concat(buffer, data);
				int skipTo = 0;
				for (int i = 0; i < buffer.length; i++) {
					int length = detectPacket(buffer, i);
					if (length == PACKET_MAYBE) {
						byte[] newBuffer = new byte[buffer.length - i];
						System.arraycopy(buffer, i, newBuffer, 0, buffer.length - i);
						buffer = newBuffer;
						skipTo = 0;
						break;
					}
					else if (length > 0) {
						parsePacket(buffer, i, length);
						i += length - 1;
					}
					skipTo = i + 1;
				}

				if (skipTo > 0) {
					byte[] newBuffer = new byte[buffer.length - skipTo];
					System.arraycopy(buffer, skipTo, newBuffer, 0, buffer.length - skipTo);
					buffer = newBuffer;
				}
			}
			//		}
		} while(! in.ready());

		dataLink.stop();

		//		System.out.println("TERMINATED");

		//		dataLink.stop();
		//		brainLink.disconnect();
	}

	private void parsePacket(byte[] buffer, int pos, int packetLength) {
		curData = new Data(System.currentTimeMillis()-t0);

		//System.out.println("Asked to parse "+pos+" "+packetLength+" of "+buffer.length);
		int end = pos + packetLength - 1;
		pos += 3;

		System.out.println("TIME "+System.currentTimeMillis());

		while((pos = parseRow(buffer, pos, end)) < end);

		if (lastSignal == 0 && ( curData.havePower || curData.haveAttention || curData.haveMeditation ) ) {
			data.add(curData);
			repaint();
		}
	}

	public static void dumpData(byte[] buffer) {
		String out = "";
		for (byte b: buffer) {
			out += String.format("%02X ", 0xFF&(int)b);
		}
		System.out.println(out);
	}	

	private int parseRow(byte[] buffer, int pos, int end) {
		int excodeLevel = 0;
		while (pos < end && buffer[pos] == (byte)0x55) {
			excodeLevel++;
			pos++;
		}
		if (pos >= end)
			return end;
		byte code = buffer[pos];
		pos++;
		if (pos >= end)
			return end;
		int dataLength = 1;
		if ((code&(byte)0x80) == (byte)0x00) {
			dataLength = 1;
		}
		else {
			dataLength = 0xFF & (int)buffer[pos];
			pos++;
		}
		if (pos + dataLength > end)
			return end;
		parseData(excodeLevel, code, buffer, pos, dataLength);
		return pos + dataLength;
	}

	private void parseData(int excodeLevel, byte code, byte[] buffer, int pos, int dataLength) {
		int v;

		if (excodeLevel > 0) {
			System.out.println("UNPARSED "+excodeLevel+" "+code);
			return;
		}

		switch(code) {
		case (byte)0x02:
			System.out.println("POOR_SIGNAL "+(0xFF&(int)buffer[pos]));
		lastSignal = (0xFF)&(int)buffer[pos];
		break;
		case (byte)0x03:
			System.out.println("HEART_RATE "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x04:
			v = 0xFF&(int)buffer[pos];
		System.out.println("ATTENTION "+v);
		curData.attention = v / 100.;
		curData.haveAttention = true;
		break;
		case (byte)0x05:
			v = 0xFF&(int)buffer[pos];
		System.out.println("MEDITATION "+v);
		curData.meditation = v / 100.;
		curData.haveMeditation = true;
		break;
		case (byte)0x06:
			System.out.println("8BIT_RAW "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x07:
			System.out.println("RAW_MARKER "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x80:
			System.out.println("RAW "+(short)(((0xFF&(int)buffer[pos])<<8) | ((0xFF&(int)buffer[pos+1]))) );
		break;
		case (byte)0x81:
			System.out.println("EEG_POWER unsupported");
		break;
		case (byte)0x82:
			System.out.println("0x82 "+getUnsigned32(buffer,pos));
		break;
		case (byte)0x83:
			parseASIC_EEG_POWER(buffer, pos);
		break;
		case (byte)0x86:
			System.out.println("RRINTERVAL "+(((0xFF&(int)buffer[pos])<<8) | ((0xFF&(int)buffer[pos+1]))) );
		break;
		default:
			System.out.println("UNPARSED "+excodeLevel+" "+code);
			break;
		}
	}

	private long getUnsigned32(byte[] buffer, int pos) {
		return ((0xFF&(long)buffer[pos]) << 24) |
				((0xFF&(long)buffer[pos+1]) << 16) |
				((0xFF&(long)buffer[pos+2]) << 8) |
				((0xFF&(long)buffer[pos+3]));
	}

	private void parseASIC_EEG_POWER(byte[] buffer, int pos) {
		double sum = 0;
		for (int i=0; i<POWER_NAMES.length; i++) {
			int v = getUnsigned24(buffer, pos + 3 * i);
			System.out.println(POWER_NAMES[i]+" "+v);
			curData.power[i] = v;
			sum += v;
		}
		for (int i=0; i<POWER_NAMES.length; i++)
			curData.power[i] /= sum;
		curData.havePower = true;
	}

	private static int getUnsigned24(byte[] buffer, int pos) {
		return ((0xFF & (int)buffer[pos+0])<<16) |
				((0xFF & (int)buffer[pos+1])<<8) |
				((0xFF & (int)buffer[pos+2]));
	}

	private static int detectPacket(byte[] buffer, int i) {
		if (buffer.length <= i)
			return PACKET_MAYBE;
		if (buffer[i] != (byte)0xAA)
			return PACKET_NO;
		if (buffer.length <= i+1)
			return PACKET_MAYBE;
		if (buffer[i+1] != (byte)0xAA)
			return PACKET_NO;
		if (buffer.length <= i+2)
			return PACKET_MAYBE;
		int pLength = 0xFF & (int)buffer[i+2];
		if (pLength > 169)
			return PACKET_NO;
		if (buffer.length < i+4+pLength)
			return PACKET_MAYBE;
		byte sum = 0;
		for (int j=0; j<pLength; j++)
			sum += buffer[i+3+j];
		sum ^= (byte)0xFF;
		if (sum != buffer[i+3+pLength]) {
			System.out.println("CSUMERROR "+sum+" vs "+buffer[i+3+pLength]);
			return PACKET_NO;
		}
		return 4+pLength;
	}

	private static byte[] concat(byte[] a, byte[] b) {
		int total = a.length + b.length;
		byte[] out = Arrays.copyOf(a, total);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	private static void sleep(final int millis)
	{
		try
		{
			Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			System.out.println("Error while sleeping: " + e);
		}
	}

	public class Data {
		long t;
		double[] power = new double[BrainFlex.POWER_NAMES.length];
		boolean havePower;
		boolean haveMeditation;
		boolean haveAttention;
		double meditation;
		double attention;

		public Data(long t) {
			this.t = t;
		}
	}
}
