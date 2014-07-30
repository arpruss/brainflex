/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class BrainFlex implements BrainFlexGUI {
	private List<Mark> marks;
	private List<JFrame> windows;
	MindFlexReader mfr;
    static final private boolean rawDump = false;
	public static final String PREF_SERIAL_PORT = "serialPort";
	public static final String PREF_RAW = "raw";
	public static final String PREF_POWER = "power";
	public static final String PREF_CUSTOM_FW = "customFW";
	public static final String PREF_LOG_WINDOW = "logWindow";
	public static final String PREF_FILE_MODE = "inFile";
	public static final String PREF_FILE_NAME = "fileName";
	public static final String PREF_SAVE_BINARY = "saveBinary";
	public int mode;
	LogWindow logWindow;

	public BrainFlex(File saveFile) {
		Preferences pref = Preferences.userNodeForPackage(BrainFlex.class);
		windows = new LinkedList<JFrame>();
				
		if (pref.getBoolean(PREF_LOG_WINDOW, true)) {
			logWindow = new LogWindow();
			windows.add(logWindow);
		}
		
		DataLink dataLink;

		try {
			if (pref.getBoolean(PREF_FILE_MODE, false)) {
				dataLink = new FileDataLink(pref.get(PREF_FILE_NAME, null));
			}
			else if (pref.getBoolean(PREF_CUSTOM_FW, false)) { 
				dataLink = new BrainLinkBridgeSerialLink(pref.get(PREF_SERIAL_PORT, null));
			}
			else {
				dataLink = new BrainLinkSerialLinkLL(pref.get(PREF_SERIAL_PORT, null));
			}
			log(dataLink.getClass().toString());
		}
		catch(Exception e) {
			log(""+e);
			mfr = null;
			return;
		}

		if (pref.getBoolean(PREF_RAW, true)) {
			mode = MindFlexReader.MODE_RAW;
		}
		else {
			mode = MindFlexReader.MODE_NORMAL;
		}

		mfr = new MindFlexReader(this, dataLink, mode, saveFile);
		marks = new ArrayList<Mark>();
		
		if (pref.getBoolean(PREF_POWER, true))
			windows.add(new ViewerWindow(this, false));

		if (pref.getBoolean(PREF_RAW, true))
			windows.add(new ViewerWindow(this, true));
				
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
			log("Cannot set max priority for reader thread.");
		}
		
		for (final JFrame w : windows) {
			w.addWindowListener(new WindowListener() {
				
				@Override
				public void windowOpened(WindowEvent arg0) {
				}
				
				@Override
				public void windowIconified(WindowEvent arg0) {
				}
				
				@Override
				public void windowDeiconified(WindowEvent arg0) {
				}
				
				@Override
				public void windowDeactivated(WindowEvent arg0) {
				}
				
				@Override
				public void windowClosing(WindowEvent arg0) {
					windows.remove(w);
					if (windows.size() == 0) {
						if (mfr != null) {
							mfr.stop();
						}
					}
				}
				
				@Override
				public void windowClosed(WindowEvent arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void windowActivated(WindowEvent arg0) {
					// TODO Auto-generated method stub
					
				}
			});
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
		if (rawDump) {
			Preferences prefs = Preferences.userNodeForPackage(BrainFlex.class);
			DataLink dataLink;
			FileOutputStream o = new FileOutputStream("data.raw");

			final String comPort = JOptionPane.showInputDialog(null, "Brainlink serial port?", 
					prefs.get(PREF_SERIAL_PORT, ""));
			if (comPort == null)
				return;
			prefs.put(PREF_SERIAL_PORT, comPort);

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
		    try {
		        UIManager.setLookAndFeel(
		            UIManager.getSystemLookAndFeelClassName());
		    }
		    catch (Exception e) {
		    	
		    }
			new Options();
		}
	}

	public void updateGraphs() {
		for (JFrame w: windows) {
			if (w.getType().equals(ViewerWindow.class)) {
				((ViewerWindow)w).updateGraph();
			}
		}
	}
	
	// This assumes the MindFlexReader instance has already terminated.
	public void terminate() {
		for (JFrame w: windows)
			w.dispose();
		System.exit(0);
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

//	public void closing(ViewerWindow toClose) {
//		for (JFrame w: windows) {
//			if (w == toClose) {
//				windows.remove(w);
//			}
//		}
//		if (windows.size() == 0)
//			mfr.stop();
//	}

	public void log(String s) {
		if (logWindow != null && s != null)
			logWindow.log(s);
	}
}
