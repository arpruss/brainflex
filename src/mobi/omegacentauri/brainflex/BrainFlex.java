/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Double;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class BrainFlex extends JFrame {
	private static final long serialVersionUID = 1L;
	static final int PACKET_NO = -1;
	static final int PACKET_MAYBE = 0;
	public static final String[] POWER_NAMES = { "delta", "theta", "low-alpha", "high-alpha", "low-beta", "high-beta",
		"low-gamma", "mid-gamma"
	};
	private List<Data> data;
	private List<Mark> marks;
	public long t0;
	private long badSignals;
	private int lastSignal;
	private Data curData;
	private long packetCount;
	private long lastPaintTime;
	public static final int MODE_NORMAL = 0;
	public static final int MODE_RAW = 0x02;
    public boolean done;
    private int pause = -1;
	private JTextField timeText;
	private long pausedBadSignalCount;


    private boolean customBrainlinkFW = true; // use only with the custom firmware from https://github.com/arpruss/brainflex
    private int mode = MODE_RAW;
    static final private boolean rawDump = false;

	public BrainFlex() {
		done = false;
		t0 = System.currentTimeMillis();
		packetCount = 0;
		data = new ArrayList<Data>();
		marks = new ArrayList<Mark>();
		lastSignal = 100;
		badSignals = 0;

//		setLayout(new BorderLayout());
//		MyPanel graph = new MyPanel();
//		graph.setLayout(new FlowLayout(FlowLayout.RIGHT));
//		JButton markButton = new JButton("!");
//		markButton.addActionListener(new ActionListener() {		
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				Mark mark = new Mark(System.currentTimeMillis()-t0, signalCount);
//				System.out.println("Mark "+mark.t+ " "+mark.count);
//				marks.add(mark);
//			}
//		}); 	
//		graph.add(markButton);
//		add(graph,BorderLayout.SOUTH);
//		add(new MyPanel());

		setSize(640,480);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		MyPanel graph = new MyPanel();
		getContentPane().add(graph);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		JButton markButton = new JButton("Mark");
		markButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				Mark mark = new Mark();
				System.out.println("Mark "+mark.t+ " "+mark.count);
				marks.add(mark);
			}
		}); 	
		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				done = true;
			}
		}); 	
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pause < 0) {
					pause = data.size();
					pausedBadSignalCount = badSignals;
				}
				else {
					pause = -1;
				}
			}
		}); 	
		timeText = new JTextField();
		setTime(0, 0, 0);
		timeText.setEditable(false);
		Dimension m = timeText.getMaximumSize();
		Dimension p = timeText.getPreferredSize();
		m.height = p.height;
		timeText.setMaximumSize(m);

		buttonPanel.add(pauseButton);
		buttonPanel.add(markButton);
		buttonPanel.add(exitButton);
		buttonPanel.add(timeText);
		
		getContentPane().add(buttonPanel);
		
		setVisible(true);
	}
	
	private void setTime(long t, long c, long bad) {
		timeText.setText(new DecimalFormat("0.000").format(t/1000.)+"s ("+c+" good packets, "+bad+" bad packets)");
	}

	private class MyPanel extends JPanel {
		private static final long serialVersionUID = -1055183524854368685L;
		private static final int GRAPH_SPACING = 3;
		
		public MyPanel() {
			super();
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D) g;
			Dimension s = getSize();
			
			ArrayList<Data> dataCopy;
			synchronized (data) {
				dataCopy = new ArrayList<Data>(data);
			}
			
			int n = pause < 0 ? dataCopy.size() : pause;
			
			if (mode == MODE_RAW) {
				drawRaw(g2, s, dataCopy, n);
			}
			else {
				drawPower(g2, s, dataCopy, n);
			}
			
			long t = n > 0 ? dataCopy.get(n-1).t : 0;
			long c = n > 0 ? dataCopy.get(n-1).count : 0;
			setTime(t,c, pause < 0 ? badSignals : pausedBadSignalCount);
		}
		
		private void drawRaw(Graphics2D g2, Dimension s, List<Data> data, int n) {
			if (n<2)
				return;
			double tSize = Math.pow(2, Math.ceil(log2(data.get(n-1).count + 16)));
			double tScale = s.getWidth() / tSize;

			double ySize = 0;
			for (Data d: data) {
				if (d.raw > ySize)
					ySize = d.raw;
				else if (-d.raw > ySize)
					ySize = d.raw;
			}
			
			ySize *= 2;
			
			double yScale = s.getHeight() / ySize;
			
			g2.setColor(Color.BLUE);
			for (Mark m: marks) {
				Line2D lin = new Line2D.Double(m.count * tScale, 0,
						m.count * tScale, s.getHeight());
				g2.draw(lin);
			}

			g2.setColor(Color.BLACK);
			Data d0 = null;

			for (int i=0; i<n; i++) {
				Data d1 = data.get(i);
				if (0<i && d0.haveRaw && d1.haveRaw) { 
					Line2D lin = new Line2D.Double(d0.count * tScale, 
							(ySize / 2 - d0.raw) * yScale,
							d1.count * tScale, 
							(ySize / 2 - d1.raw) * yScale);
					g2.draw(lin);					
				}
				d0 = d1;
			}
		}

		private void drawPower(Graphics2D g2, Dimension s, List<Data> data, int n) {
			if (n<2)
				return;
			double tSize = Math.pow(2, Math.ceil(log2(data.get(n-1).t + 1000 )));
			double tScale = s.getWidth() / tSize;
			double ySize = 0;
			for (Data d: data) 
				for (double y: d.power)
					if (y > ySize)
						ySize = y;

			double subgraphContentHeight = (s.getHeight() - GRAPH_SPACING * (1+POWER_NAMES.length) ) / (2+POWER_NAMES.length);
			double subgraphHeight = subgraphContentHeight + GRAPH_SPACING;
			double yScale = subgraphContentHeight / ySize;

			g2.setColor(Color.BLUE);
			for (Mark m: marks) {
				Line2D lin = new Line2D.Double(m.t * tScale, 0,
						m.t * tScale, s.getHeight());
				g2.draw(lin);
			}

			g2.setColor(Color.GREEN);
			for (int j = 0 ; j < POWER_NAMES.length + 1 ; j++) {
				Line2D lin = new Line2D.Double(0, subgraphHeight * ( j + 1 ) + GRAPH_SPACING / 2,
						s.getWidth(), subgraphHeight * ( j + 1 ) + GRAPH_SPACING / 2);
				g2.draw(lin);
			}
			
			for (int j = 0 ; j < POWER_NAMES.length ; j++) {
				g2.drawChars(POWER_NAMES[j].toCharArray(), 0, POWER_NAMES[j].length(), 
						0, (int)(j * subgraphHeight + ySize * .5 * yScale));
			}
			g2.drawChars("Attention".toCharArray(), 0, "Attention".length(), 
					0, (int)(POWER_NAMES.length * subgraphHeight + ySize * .5 * yScale));
			g2.drawChars("Meditation".toCharArray(), 0, "Meditation".length(), 
					0, (int)((1+POWER_NAMES.length) * subgraphHeight + ySize * .5 * yScale));

			g2.setColor(Color.BLACK);
			Data d0 = null;

			for (int i=0; i<n; i++) {
				Data d1 = data.get(i);
				if (0<i) { 
					if (d0.havePower && d1.havePower) { 
						for (int j=0; j<POWER_NAMES.length; j++) {
							Line2D lin = new Line2D.Double(d0.t * tScale, 
									(ySize - d0.power[j]) * yScale + j * subgraphHeight,
									d1.t * tScale, 
									(ySize - d1.power[j]) * yScale + j * subgraphHeight);
							g2.draw(lin);
						}
					}
					if (d0.haveAttention && d1.haveAttention) {
						Line2D lin = new Line2D.Double(d0.t * tScale, 
								(1 - d0.attention) * subgraphContentHeight + POWER_NAMES.length * subgraphHeight,
								d1.t * tScale, 
								(1 - d1.attention) * subgraphContentHeight + POWER_NAMES.length * subgraphHeight);
						g2.draw(lin);
					}
					if (d0.haveMeditation && d1.haveMeditation) {
						Line2D lin = new Line2D.Double(d0.t * tScale, 
								(1 - d0.meditation) * subgraphContentHeight + (1+POWER_NAMES.length) * subgraphHeight,
								d1.t * tScale, 
								(1 - d1.meditation) * subgraphContentHeight + (1+POWER_NAMES.length) * subgraphHeight);
						g2.draw(lin);
					}
				}
				d0 = d1;
			}
		}
	}


	public static void main(final String[] args) throws Exception
	{
		final String comPort = JOptionPane.showInputDialog(null, "Brainlink serial port?");
		if (comPort == null)
			return;

		if (rawDump) {
			DataLink dataLink;
			FileOutputStream o = new FileOutputStream("data.raw");

			dataLink = new BrainLinkBridgeSerialLink(comPort); 
			dataLink.preStart(9600, new byte[] { MODE_RAW });
			dataLink.start(57600);

			long t0 = System.currentTimeMillis();
			while(System.currentTimeMillis() < t0 + 20000) {
				byte[] data = dataLink.receiveBytes();
				o.write(data);
			}
			dataLink.stop();
			o.close();
			System.out.println("Done");
		}
		else {
			final BrainFlex bf = new BrainFlex();
			
			Thread reader = new Thread() {
				@Override 
				public void run() {
					try {
						bf.readData(comPort);
					} catch (IOException e) {
					}
				}			
			};
			
			try {
				reader.setPriority(Thread.MAX_PRIORITY);
			}
			catch(Exception e) {
				System.err.println("Cannot set max priority for reader thread.");
			}
			reader.start();
		}
	}

	public double log2(double d) {
		return Math.log(d)/Math.log(2);
	}

	void readData(String comPort) throws IOException {
		byte[] buffer = new byte[0];

		DataLink dataLink;

		System.out.println("CONNECTING");
		dataLink = customBrainlinkFW ? new BrainLinkBridgeSerialLink(comPort) : new BrainLinkSerialLinkLL(comPort); 
		int baud = 9600;
		if (mode != MODE_NORMAL) {
			dataLink.preStart(9600, new byte[] { (byte)mode });
			if (mode >= 0x02)
				baud = 57600;
			else if (mode == 0x01)
				baud = 1200;
		}
		dataLink.start(baud);
		// 0x00 : 9600 : normal
		// 0x01 : 1200
		// 0x02 : 57600 : RAW
		// 0x03 : 57600 : lots of 0x82 signals, 4 bytes
		//		sleep(100);
		//dataLink.start(1200);
		//		sleep(100);
		System.out.println("CONNECTED");
		
		while (!done) {
			byte[] data = dataLink.receiveBytes();
			if (data != null) {
				buffer = concat(buffer, data);

				for (int i = 0; i < buffer.length; i++) {
					if (buffer[i] == (byte)0xAA) {
						int length = detectPacket0(buffer, i);
						if (length == PACKET_MAYBE) {
							// possible start of unfinished packet
							byte[] newBuffer = new byte[buffer.length - i];
							System.arraycopy(buffer, i, newBuffer, 0, buffer.length - i);
							buffer = newBuffer;
						}
						else if (length > 0) {
							parsePacket(buffer, i, length);
							i += length - 1;
						}
					}
				}
			}
		} 

		System.out.println("Terminated");
		dataLink.stop();
		dispose();
	}

	private void parsePacket(byte[] buffer, int pos, int packetLength) {
		curData = new Data(System.currentTimeMillis()-t0);

		//System.out.println("Asked to parse "+pos+" "+packetLength+" of "+buffer.length);
		int end = pos + packetLength - 1;
		pos += 3;

		rtLog("TIME "+System.currentTimeMillis());

		while((pos = parseRow(buffer, pos, end)) < end);

		if (curData.haveRaw || ( lastSignal == 0 && ( curData.havePower || curData.haveAttention || curData.haveMeditation ) ) ) {
			synchronized(data) {
				data.add(curData);
			}
			if (System.currentTimeMillis() - lastPaintTime > 250) {
				lastPaintTime = System.currentTimeMillis();
				if (pause < 0) {
					repaint();
				}
			}
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
//		int excodeLevel = 0;
//		while (pos < end && buffer[pos] == (byte)0x55) {
//			excodeLevel++;
//			pos++;
//		}
		if (pos >= end)
			return end;
		byte code = buffer[pos];
		pos++;
		if (pos >= end)
			return end;
		int dataLength;
		if ((code&(byte)0x80) == (byte)0x00) {
			dataLength = 1;
		}
		else {
			dataLength = 0xFF & (int)buffer[pos];
			pos++;
		}
		if (pos + dataLength > end)
			return end;

//		if (excodeLevel > 0) {
//			rtLog("UNPARSED "+excodeLevel+" "+code);
//			return pos + dataLength;
//		}

		int v;

		switch(code) {
		case (byte)0x02:
			rtLog("POOR_SIGNAL "+(0xFF&(int)buffer[pos]));
		lastSignal = (0xFF)&(int)buffer[pos];
		break;
		case (byte)0x03:
			rtLog("HEART_RATE "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x04:
			v = 0xFF&(int)buffer[pos];
		rtLog("ATTENTION "+v);
		curData.attention = v / 100.;
		curData.haveAttention = true;
		break;
		case (byte)0x05:
			v = 0xFF&(int)buffer[pos];
		rtLog("MEDITATION "+v);
		curData.meditation = v / 100.;
		curData.haveMeditation = true;
		break;
		case (byte)0x06:
			rtLog("8BIT_RAW "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x07:
			rtLog("RAW_MARKER "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x80:
			curData.raw = (short)(((0xFF&(int)buffer[pos])<<8) | ((0xFF&(int)buffer[pos+1])));
			curData.haveRaw = true;
			rtLog("RAW " + curData.raw);
		break;
		case (byte)0x81:
			rtLog("EEG_POWER unsupported");
		break;
		case (byte)0x82:
			rtLog("0x82 "+getUnsigned32(buffer,pos));
		break;
		case (byte)0x83:
			parseASIC_EEG_POWER(buffer, pos);
		break;
		case (byte)0x86:
			rtLog("RRINTERVAL "+(((0xFF&(int)buffer[pos])<<8) | ((0xFF&(int)buffer[pos+1]))) );
		break;
		default:
			rtLog("UNPARSED "/* +excodeLevel+*/+" "+code);
			break;
		}
		
		return pos+dataLength;
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

// called only if buffer[i] == 0xAA	
	private int detectPacket0(byte[] buffer, int i) {
//		if (buffer.length <= i)
//			return PACKET_MAYBE;
//		if (buffer[i] != (byte)0xAA)
//			return PACKET_NO;
		if (buffer.length <= i+1)
			return PACKET_MAYBE;
		if (buffer[i+1] != (byte)0xAA)
			return PACKET_NO;
		if (buffer.length <= i+2)
			return PACKET_MAYBE;
		int pLength = 0xFF & (int)buffer[i+2];
		if (pLength > 169) {
			badSignals++;
			return PACKET_NO;
		}
		if (buffer.length < i+4+pLength)
			return PACKET_MAYBE;
		byte sum = 0;
		for (int j=0; j<pLength; j++)
			sum += buffer[i+3+j];
		packetCount++;
		if ((sum ^ buffer[i+3+pLength]) != (byte)0xFF) {
			badSignals++;
			rtLog("CSUMERROR "+sum+" vs "+buffer[i+3+pLength]);
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
		}
	}
	
	public void rtLog(String s) {
		if (mode < MODE_RAW)
			System.out.println(s);
	}

	public class Data {
		long t;
		long count;
		double[] power = new double[BrainFlex.POWER_NAMES.length];
		boolean havePower;
		boolean haveMeditation;
		boolean haveAttention;
		boolean haveRaw;
		double meditation;
		double attention;
		int raw;

		public Data(long t) {
			this.t = t;
			this.count = BrainFlex.this.packetCount - 1;
		}
	}
	
	public class Mark {
		long t;
		long count;
		
		public Mark() {
			this.t = System.currentTimeMillis()-t0;
			this.count = packetCount;
		}
	}
}
