package mobi.omegacentauri.brainflex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MindFlexReader {
	static final int PACKET_NO = -1;
	static final int PACKET_MAYBE = 0;
	public static final String[] POWER_NAMES = { "delta", "theta", "low-alpha", "high-alpha", "low-beta", "high-beta",
		"low-gamma", "mid-gamma"
	};
	public List<PowerData> powerData;
	public List<Integer> rawData;
	public long t0;
	int badPacketCount;
	private int lastSignal;
	private PowerData curPowerData;
	private int curRaw;
	private boolean haveRaw;
	private long lastPaintTime;
	public static final int MODE_NORMAL = 0;
	public static final int MODE_RAW = 0x02; // 0x02;
    public boolean done;
	
	private DataLink dataLink;
	private BrainFlexGUI gui;
	private int mode;
	static private final boolean LOG = false;
	static private final boolean DUMP = false;

    public MindFlexReader(BrainFlexGUI gui, DataLink dataLink, int mode) {
    	this.mode = mode;
    	this.gui = gui;
    	this.dataLink = dataLink;
		t0 = System.currentTimeMillis();
		powerData = new ArrayList<PowerData>();
		rawData = new ArrayList<Integer>();
		lastSignal = 100;
		badPacketCount = 0;
    }
    

	void readData() throws IOException {
		FileOutputStream out;
		if (DUMP) {
			File f = new File("data.bin");
			f.delete();
			out = new FileOutputStream(f);
		}
		byte[] buffer = new byte[0];

		System.out.println("CONNECTING");
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
		
		t0 = System.currentTimeMillis();

		while (!done && dataLink != null && 
				(!DUMP || System.currentTimeMillis() - t0 < 30000) ) {
			try {
				byte[] data = dataLink.receiveBytes();
				if (data != null) {
					if (DUMP)
						out.write(data);
					buffer = concat(buffer, data);
	
					int i;
					int n = buffer.length;
					
					for (i = 0; i < n; i++) {
						if (buffer[i] == (byte)0xAA) {
							int length = detectPacket0(buffer, i);
							if (length == PACKET_MAYBE) {
								// possible start of unfinished packet
								byte[] newBuffer = new byte[n - i];
								System.arraycopy(buffer, i, newBuffer, 0, buffer.length - i);
								buffer = newBuffer;
								break;
							}
							else if (length > 0) {
								parsePacket(buffer, i, length);
								i += length - 1;
							}
						}
					}
					
					if (i >= n)
						buffer = new byte[0];
				}
			}
			catch(Exception e) {				
			}
		} 

		System.out.println("Terminated");
		System.out.println("Received "+rawData.size()+" raw packets over "+curPowerData.t/1000.+" sec: "+(1000.*rawData.size()/curPowerData.t));
		System.out.println("Received "+powerData.size()+" processed packets over "+curPowerData.t/1000.+" sec: "+(1000.*powerData.size()/curPowerData.t));
		if (dataLink != null) {
			dataLink.stop();
			dataLink = null;
		}
		if (gui!=null)
			gui.terminate();
	}

	private void parsePacket(byte[] buffer, int pos, int packetLength) {
		curPowerData = new PowerData((int) (System.currentTimeMillis()-t0));
		haveRaw = false;

		//System.out.println("Asked to parse "+pos+" "+packetLength+" of "+buffer.length);
		int end = pos + packetLength - 1;
		pos += 3;

		while((pos = parseRow(buffer, pos, end)) < end);
		
		if (haveRaw) {
			synchronized(rawData) {
				rawData.add(curRaw);
			}
//				if (rawData.size() % 10000 == 0) {
//					System.out.println("Have "+rawData.size()+" raw packets over "+curPowerData.t/1000.+" sec: "+(1000.*rawData.size()/curPowerData.t));
//				}
		}

		if (lastSignal < 50 && ( curPowerData.havePower || curPowerData.haveAttention || curPowerData.haveMeditation ) ) {
			synchronized(powerData) {
				powerData.add(curPowerData);
				log("TIME "+curPowerData.t);
			}
		}
		if (System.currentTimeMillis() - lastPaintTime > 250) {
			lastPaintTime = System.currentTimeMillis();
			gui.updateGraphs();
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
//			log("UNPARSED "+excodeLevel+" "+code);
//			return pos + dataLength;
//		}

		int v;

		switch(code) {
		case (byte)0x02:
			log("POOR_SIGNAL "+(0xFF&(int)buffer[pos]));
		lastSignal = (0xFF)&(int)buffer[pos];
		break;
		case (byte)0x03:
			log("HEART_RATE "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x04:
			v = 0xFF&(int)buffer[pos];
		log("ATTENTION "+v);
		curPowerData.attention = v / 100.;
		curPowerData.haveAttention = true;
		break;
		case (byte)0x05:
			v = 0xFF&(int)buffer[pos];
		log("MEDITATION "+v);
		curPowerData.meditation = v / 100.;
		curPowerData.haveMeditation = true;
		break;
		case (byte)0x06:
			log("8BIT_RAW "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x07:
			log("RAW_MARKER "+(0xFF&(int)buffer[pos]));
		break;
		case (byte)0x80:
			curRaw = (short)(((0xFF&(int)buffer[pos])<<8) | ((0xFF&(int)buffer[pos+1])));
			haveRaw = true;
//			rtLog("RAW " + curData.raw);
		break;
		case (byte)0x81:
			log("EEG_POWER unsupported");
		break;
		case (byte)0x82:
//			rtLog("0x82 "+getUnsigned32(buffer,pos));
			curRaw = (short)(((0xFF&(int)buffer[pos+2])<<8) | ((0xFF&(int)buffer[pos+3])));
			haveRaw = true;
		break;
		case (byte)0x83:
			parseASIC_EEG_POWER(buffer, pos);
		break;
		case (byte)0x86:
			log("RRINTERVAL "+(((0xFF&(int)buffer[pos])<<8) | ((0xFF&(int)buffer[pos+1]))) );
		break;
		default:
			log("UNPARSED "/* +excodeLevel+*/+" "+code);
			break;
		}
		
		return pos+dataLength;
	}

//	private long getUnsigned32(byte[] buffer, int pos) {
//		return ((0xFF&(long)buffer[pos]) << 24) |
//				((0xFF&(long)buffer[pos+1]) << 16) |
//				((0xFF&(long)buffer[pos+2]) << 8) |
//				((0xFF&(long)buffer[pos+3]));
//	}

	private void parseASIC_EEG_POWER(byte[] buffer, int pos) {
		double sum = 0;
		for (int i=0; i<POWER_NAMES.length; i++) {
			int v = getUnsigned24(buffer, pos + 3 * i);
			log(POWER_NAMES[i]+" "+v);
			curPowerData.power[i] = v;
			sum += v;
		}
		for (int i=0; i<POWER_NAMES.length; i++)
			curPowerData.power[i] /= sum;
		curPowerData.havePower = true;
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
			badPacketCount++;
			return PACKET_NO;
		}
		if (buffer.length < i+4+pLength)
			return PACKET_MAYBE;
		
		byte sum = 0;
		for (int j=0; j<pLength; j++)
			sum += buffer[i+3+j];
		if ((sum ^ buffer[i+3+pLength]) != (byte)0xFF) {
			badPacketCount++;
			if (mode < MODE_RAW) 
				log("CSUMERROR "+sum+" vs "+buffer[i+3+pLength]);
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

//	private static void sleep(final int millis)
//	{
//		try
//		{
//			Thread.sleep(millis);
//		}
//		catch (InterruptedException e)
//		{
//		}
//	}
	
	public void log(String s) {
		if (LOG)
			System.out.println(s);
	}

	public class PowerData {
		int t;
		double[] power = new double[MindFlexReader.POWER_NAMES.length];
		boolean havePower;
		boolean haveMeditation;
		boolean haveAttention;
		double meditation;
		double attention;
		
		public PowerData(int t) {
			this.t = t;
		}
	}
	
	public void stop() {
		done = true;
	}

	public void disconnect() {
		synchronized(dataLink) {
			if (dataLink != null) {
				dataLink.stop();
				dataLink = null;
			}
		}
	}
}
