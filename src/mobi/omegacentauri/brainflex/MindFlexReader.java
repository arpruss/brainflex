package mobi.omegacentauri.brainflex;

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
	public List<Data> data;
	public long t0;
	int badPacketCount;
	private int rawPacketCount;
	private int lastSignal;
	private Data curData;
	int packetCount;
	private long lastPaintTime;
	public static final int MODE_NORMAL = 0;
	public static final int MODE_RAW = 0x02; // 0x02;
    public boolean done;
	
	private DataLink dataLink;
	private BrainFlexGUI gui;
	private int mode;

    public MindFlexReader(BrainFlexGUI gui, DataLink dataLink, int mode) {
    	this.mode = mode;
    	this.gui = gui;
    	this.dataLink = dataLink;
		t0 = System.currentTimeMillis();
		packetCount = 0;
		data = new ArrayList<Data>();
		lastSignal = 100;
		badPacketCount = 0;
    }
    

	void readData() throws IOException {
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
		
		while (!done && dataLink != null) {
			try {
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
			catch(Exception e) {				
			}
		} 

		System.out.println("Terminated");
		if (dataLink != null) {
			dataLink.stop();
			dataLink = null;
		}
		if (gui!=null)
			gui.terminate();
	}

	private void parsePacket(byte[] buffer, int pos, int packetLength) {
		curData = new Data((int) (System.currentTimeMillis()-t0), rawPacketCount);

		//System.out.println("Asked to parse "+pos+" "+packetLength+" of "+buffer.length);
		int end = pos + packetLength - 1;
		pos += 3;

		rtLog("TIME "+System.currentTimeMillis());

		while((pos = parseRow(buffer, pos, end)) < end);

		if (curData.haveRaw || ( lastSignal == 0 && ( curData.havePower || curData.haveAttention || curData.haveMeditation ) ) ) {
			synchronized(data) {
				data.add(curData);
			}
			
			if (curData.haveRaw)
				rawPacketCount++;
			
			if (System.currentTimeMillis() - lastPaintTime > 250) {
				lastPaintTime = System.currentTimeMillis();
				gui.update();
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
//			rtLog("RAW " + curData.raw);
		break;
		case (byte)0x81:
			rtLog("EEG_POWER unsupported");
		break;
		case (byte)0x82:
//			rtLog("0x82 "+getUnsigned32(buffer,pos));
			curData.raw = (short)(((0xFF&(int)buffer[pos+2])<<8) | ((0xFF&(int)buffer[pos+3])));
			curData.haveRaw = true;
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
			badPacketCount++;
			packetCount++;
			return PACKET_NO;
		}
		if (buffer.length < i+4+pLength)
			return PACKET_MAYBE;
		
		packetCount++;

		byte sum = 0;
		for (int j=0; j<pLength; j++)
			sum += buffer[i+3+j];
		if ((sum ^ buffer[i+3+pLength]) != (byte)0xFF) {
			badPacketCount++;
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
		int t;
		int rawCount;
		double[] power = new double[MindFlexReader.POWER_NAMES.length];
		boolean havePower;
		boolean haveMeditation;
		boolean haveAttention;
		boolean haveRaw;
		double meditation;
		double attention;
		int raw;
		

		public Data(int t, int rawPacketCount) {
			this.t = t;
			this.rawCount = MindFlexReader.this.rawPacketCount;
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
