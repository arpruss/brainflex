/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

public class BrainFlex implements BrainFlexGUI {
	private List<Mark> marks;
	private List<ViewerWindow> windows;
	MindFlexReader mfr;
    static final private boolean rawDump = false;
	private boolean customBrainlinkFW = true; // use only with the custom firmware from https://github.com/arpruss/brainflex
    int mode = MindFlexReader.MODE_RAW;

	public BrainFlex(final String comPort) {
		DataLink dataLink = customBrainlinkFW ? new BrainLinkBridgeSerialLink(comPort) : new BrainLinkSerialLinkLL(comPort); 
		//DataLink dataLink = new FileDataLink(comPort);
		if (! dataLink.valid()) {
			mfr = null;
			return;
		}

		mfr = new MindFlexReader(this, dataLink, mode);
		marks = new ArrayList<Mark>();
		
		windows = new LinkedList<ViewerWindow>();
		
		if (mode == MindFlexReader.MODE_RAW) {
			windows.add(new ViewerWindow(this, false));
			windows.add(new ViewerWindow(this, true));
		}
		else {
			windows.add(new ViewerWindow(this, false));
		}
				
		Thread reader = new Thread() {
			@Override 
			public void run() {
				try {
					mfr.readData();
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
	
	public int getMode() {
		return mode;
	}
	
	public List<MindFlexReader.PowerData> getPowerDataCopy() {
		synchronized (mfr.powerData) {
			return new ArrayList<MindFlexReader.PowerData>(mfr.powerData);
		}
	}
	
	public List<Integer> getRawDataCopy() {
		synchronized (mfr.powerData) {
			return new ArrayList<Integer>(mfr.rawData);
		}
	}
	
	public List<Mark> getMarksCopy() {
		synchronized (marks) {
			return new ArrayList<Mark>(marks);
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
			dataLink.preStart(9600, new byte[] { MindFlexReader.MODE_RAW });
			dataLink.start(57600);

			long t0 = System.currentTimeMillis();
			while(System.currentTimeMillis() < t0 + 20000) {
				byte[] data = dataLink.receiveBytes();
				if (data != null)
					o.write(data);
			}
			dataLink.stop();
			o.close();
			System.out.println("Done");
		}
		else {
			new BrainFlex(comPort);
		}
	}

	public void updateGraphs() {
		for (ViewerWindow w: windows) {
			w.updateGraph();
		}
	}
	
	public void terminate() {
		for (ViewerWindow w: windows)
			w.dispose();
	}
	
	@Override
	public MindFlexReader getMindFlexReader() {
		return mfr;
	}

	public void addMark(Mark mark) {
		synchronized(marks) {
			marks.add(mark);
		}		
	}

	public void closing(ViewerWindow toClose) {
		for (ViewerWindow w: windows) {
			if (w == toClose) {
				windows.remove(w);
			}
		}
		if (windows.size() == 0)
			mfr.disconnect();
	}
}
