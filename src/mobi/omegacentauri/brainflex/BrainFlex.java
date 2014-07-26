/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;

public class BrainFlex extends JFrame implements BrainFlexGUI {
	private static final long serialVersionUID = 211404378675000337L;
	private List<Mark> marks;
	private JTextField timeText;
	private JScrollBar scrollBar;
	Pause pause;
	static final double MIN_SCALE = 1/16.;
	static final double MAX_SCALE = 4.;
	static final double SCALE_MULT = 1.5;
	double scale;
	MindFlexReader mfr;
    static final private boolean rawDump = false;
	private boolean customBrainlinkFW = true; // use only with the custom firmware from https://github.com/arpruss/brainflex
    int mode = MindFlexReader.MODE_RAW;

	public BrainFlex(final String comPort) {
		DataLink dataLink = customBrainlinkFW ? new BrainLinkBridgeSerialLink(comPort) : new BrainLinkSerialLinkLL(comPort); 
		if (! dataLink.valid()) {
			mfr = null;
			dispose();
			return;
		}

		mfr = new MindFlexReader(this, dataLink, mode);
		marks = new ArrayList<Mark>();
		pause = new Pause();
		scale = 1.;
		
		setSize(640,480);
		
		addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				if (mfr != null)
					mfr.disconnect();
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		scrollBar = new JScrollBar(JScrollBar.HORIZONTAL);		

		final GraphPanel graph = mode < MindFlexReader.MODE_RAW ? new PowerGraphPanel(scrollBar, this) : new RawGraphPanel(scrollBar, this);	
		getContentPane().add(graph);
		
		getContentPane().add(scrollBar);
		scrollBar.setVisible(false);
		scrollBar.addAdjustmentListener(new AdjustmentListener() {
			
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				graph.repaint();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		
		JButton plusButton = new JButton(" + ");
		plusButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (scale / SCALE_MULT >= MIN_SCALE) {
					scale /= SCALE_MULT;
					graph.repaint();
				}
			}
		});
		
		JButton minusButton = new JButton(" - ");
		minusButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (scale * SCALE_MULT <= MAX_SCALE) {
					scale *= SCALE_MULT;
					graph.repaint();
				}
			}
		});
		

		JButton markButton = new JButton("Mark");
		markButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				Mark mark = new Mark((int)(System.currentTimeMillis()-mfr.t0), mfr.packetCount);
				System.out.println("Mark "+mark.t+ " "+mark.rawCount);
				synchronized(marks) {
					marks.add(mark);
				}
			}
		}); 	
		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				mfr.stop();
			}
		}); 	
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pause.point < 0) {
					pause.point = mfr.data.size();
					pause.pausedBadPacketCount = mfr.badPacketCount;
					pause.pausedPacketCount = mfr.packetCount;
				}
				else {
					pause.point = -1;
				}
				repaint();
			}
		}); 	
		timeText = new JTextField();
		setTime(0, 0, 0);
		timeText.setEditable(false);
		Dimension m = timeText.getMaximumSize();
		Dimension p = timeText.getPreferredSize();
		m.height = p.height;
		timeText.setMaximumSize(m);

		buttonPanel.add(minusButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(markButton);
		buttonPanel.add(exitButton);
		buttonPanel.add(timeText);
		
		getContentPane().add(buttonPanel);
		
		setVisible(true);
				
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
	
	@Override
	public void setTime(int t, int c, int bad) {
		timeText.setText(new DecimalFormat("0.000").format(t/1000.)+"s ("+c+" good packets, "+bad+" bad packets)");
	}
	
	public int getMode() {
		return mode;
	}
	
	public Pause getPause() {
		return pause;
	}
	
	public List<MindFlexReader.Data> getDataCopy() {
		synchronized (mfr.data) {
			return new ArrayList<MindFlexReader.Data>(mfr.data);
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

	public void update() {
		if (pause.point < 0)
			repaint();
	}
	
	public void terminate() {
		dispose();
	}
	
	public double getScale() {
		return scale;
	}

	@Override
	public MindFlexReader getMindFlexReader() {
		return mfr;
	}
}
