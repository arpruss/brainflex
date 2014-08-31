/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class BrainFlex extends BrainFlexGUI {
	private List<JFrame> windows;
	MindFlexReader mfr;
    static final private boolean rawDump = false;
	public static final String PREF_SERIAL_PORT = "serialPort";
	public static final String PREF_RAW = "raw";
	public static final String PREF_POWER = "power";
	public static final String PREF_CUSTOM_FW = "customFW";
	public static final String PREF_LOG_WINDOW = "logWindow";
	public static final String PREF_FILE_NAME = "fileName";
	public static final String PREF_SAVE_BINARY = "saveBinary";
	public static final String PREF_HEART_MODE = "ecg";
	public static final String PREF_IN_MODE = "inMode";
	public int mode;
	LogWindow logWindow;

	private class MyDispatcher implements KeyEventDispatcher {
		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			if (e.getID() == KeyEvent.KEY_TYPED) {
				char c = e.getKeyChar();
				if (Character.isAlphabetic(c) || Character.isDigit(c)) {
					Mark mark = new Mark((int)(System.currentTimeMillis()-mfr.t0), 
							mfr.rawData.size(), Character.toString(c));
					System.out.println("Mark "+mark.t+ " "+mark.rawCount+" "+c);
					addMark(mark);
				}
				return true;
			}
			return false;
		}
	}

	public BrainFlex(File saveFile) {
		super();
		
		Preferences pref = Preferences.userNodeForPackage(BrainFlex.class);

//		JFrame mainWindow = new JFrame();
//		mainWindow.setSize(640, 480);
//		mainWindow.setVisible(true);
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new MyDispatcher());
		
		windows = new LinkedList<JFrame>();
				
		if (pref.getBoolean(PREF_LOG_WINDOW, true)) {
			logWindow = new LogWindow();
			windows.add(logWindow);
		}
		
		DataLink dataLink;

		try {
			int inMode = pref.getInt(PREF_IN_MODE, 0);
			if (Options.LINKS[inMode] == FileDataLink.class) {
				DataInputStream in = new DataInputStream(new FileInputStream(new File(pref.get(PREF_FILE_NAME, null))));
				readMarks(in);
				dataLink = new FileDataLink(in);
			}
			else if (Options.LINKS[inMode] == BrainLinkBridgeSerialLink.class){
				dataLink = new BrainLinkBridgeSerialLink(pref.get(PREF_SERIAL_PORT, null));
			}
			else if (Options.LINKS[inMode] == BrainLinkSerialLinkLL.class){
				dataLink = new BrainLinkSerialLinkLL(pref.get(PREF_SERIAL_PORT, null));
			}
			else if (Options.LINKS[inMode] == SerialLink57600.class){
				dataLink = new SerialLink57600(pref.get(PREF_SERIAL_PORT, null));
			}
			else {
				dataLink = new MindWaveMobile(pref.get(PREF_SERIAL_PORT, null));
			}
			
			log(dataLink.getClass().toString());
		}
		catch(Exception e) {
			log(""+e);
			mfr = null;
			System.out.println(""+e);
			System.exit(1);
			return;
		}

		if (pref.getBoolean(PREF_RAW, true) || pref.getBoolean(PREF_HEART_MODE, false)) {
			mode = MindFlexReader.MODE_RAW;
		}
		else {
			mode = MindFlexReader.MODE_NORMAL;
		}

		mfr = new MindFlexReader(this, dataLink, mode, saveFile);

		if (!pref.getBoolean(PREF_HEART_MODE, false) && pref.getBoolean(PREF_POWER, true))
			windows.add(new ViewerWindow(this, false));

		if (pref.getBoolean(PREF_RAW, true) || pref.getBoolean(PREF_HEART_MODE, false))
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
			if (w instanceof mobi.omegacentauri.brainflex.ViewerWindow) {
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
