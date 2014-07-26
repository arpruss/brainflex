/**
*
* Copyright (c) 2014 Alexander Pruss
* Distributed under the GNU GPL v3 or later. For full terms see the file COPYING.
*
*/

package mobi.omegacentauri.brainflex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Line2D;
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
	private List<Mark> marks;
    private int pause = -1;
	private int pausedBadPacketCount;
	private int pausedPacketCount;
	private static final long serialVersionUID = 1L;
	private JTextField timeText;
	private JScrollBar scrollBar;
	static final int VISIBLE_RAW=1500;
	static final int VISIBLE_POWER=512;
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

		final MyPanel graph = new MyPanel();	
		getContentPane().add(graph);
		
		scrollBar = new JScrollBar(JScrollBar.HORIZONTAL);		
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
				Mark mark = new Mark();
				System.out.println("Mark "+mark.t+ " "+mark.rawCount);
				marks.add(mark);
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
				if (pause < 0) {
					pause = mfr.data.size();
					pausedBadPacketCount = mfr.badPacketCount;
					pausedPacketCount = mfr.packetCount;
				}
				else {
					pause = -1;
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
	
	private void setTime(long t, long c, long bad) {
		timeText.setText(new DecimalFormat("0.000").format(t/1000.)+"s ("+c+" good packets, "+bad+" bad packets)");
	}

	private class MyPanel extends JPanel {
		private static final long serialVersionUID = -1055183524854368685L;
		private static final int POWER_SPACING = 3;
		double startT;
		double endT;
		double maxT;
		double tScale;
		double yScale;
		double subgraphHeight;
		
		public MyPanel() {
			super();
		}
		
		double scaleT(double t) {
			return (t - startT) * tScale;
		}
		
		void calculateTSize( Dimension s, double currentSize, double visibleLimit, double minVisible, double scrollBarScale ) {
			maxT = currentSize;
		
			double tSize;
			
			if (maxT >= visibleLimit) {
				tSize = visibleLimit;

				if (! scrollBar.isVisible() || scrollBar.getMaximum() != maxT) {
					scrollBar.setMaximum((int)(scrollBarScale * maxT+0.5));
					scrollBar.setVisibleAmount((int)(scrollBarScale * visibleLimit+0.5));
					scrollBar.setMinimum(0);
					scrollBar.setVisible(true);
					startT = maxT-visibleLimit;
				}
				else {					
					startT = scrollBar.getValue() / scrollBarScale;
				}
			}
			else {
				scrollBar.setVisible(false);
				startT = 0;
				tSize = Math.pow(2, Math.ceil(log2(Math.max(maxT - startT, 16.))));
			}
			endT = startT + tSize;
			tScale = s.getWidth() / tSize;
		}
		
		void scaledLine(Graphics2D g2, double t1, double y1, double t2, double y2, int subgraph) {
			g2.draw(new Line2D.Double(scaleT(t1), 
					y1 * yScale + subgraphHeight * subgraph,
					scaleT(t2), 
					y2 * yScale + subgraphHeight * subgraph));
		}

//		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D) g;
			Dimension s = getSize();
			
			ArrayList<MindFlexReader.Data> dataCopy;
			synchronized (mfr.data) {
				dataCopy = new ArrayList<MindFlexReader.Data>(mfr.data);
			}
			
			int n = pause < 0 ? dataCopy.size() : pause;
			
			if (mode == MindFlexReader.MODE_RAW) {
				drawRaw(g2, s, dataCopy, n);
			}
			else {
				drawPower(g2, s, dataCopy, n);
			}
			
			long t = n > 0 ? dataCopy.get(n-1).t : 0;
			
			if (pause < 0) {
				setTime(t, mfr.packetCount, mfr.badPacketCount);
			}
			else {
				setTime(t, pausedPacketCount, pausedBadPacketCount);
			}
		}
		
		private void drawRaw(Graphics2D g2, Dimension s, List<MindFlexReader.Data> data, int n) {
			if (n<2)
				return;

			calculateTSize(s, (double)data.get(n-1).rawCount, scale * VISIBLE_RAW, 16., 1.);

			double ySize = 0;
			for (MindFlexReader.Data d: data) {
				if (d.raw > ySize)
					ySize = d.raw;
				else if (-d.raw > ySize)
					ySize = -d.raw;
			}
			
			ySize *= 2;
			
			if (ySize < 0)
				ySize = 1;
			
			yScale = s.getHeight() / ySize;
			subgraphHeight = 0;
			
			g2.setColor(Color.BLUE);
			for (Mark m: marks) {
				Line2D lin = new Line2D.Double(scaleT(m.rawCount), 0,
						scaleT(m.rawCount), s.getHeight());
				g2.draw(lin);
			}

			g2.setColor(Color.BLACK);
			MindFlexReader.Data d0 = null;

			for (int i=0; i<n; i++) {
				MindFlexReader.Data d1 = data.get(i);
				if (0<i && d0.haveRaw && d1.haveRaw &&
						d0.rawCount >= startT && d1.rawCount <= endT
						) { 
					
					scaledLine(g2, d0.rawCount, ySize / 2 - d0.raw, d1.rawCount, ySize / 2 - d1.raw, 0);
				}
				d0 = d1;
			}
		}

		private void drawPower(Graphics2D g2, Dimension s, List<MindFlexReader.Data> data, int n) {
			if (n<2)
				return;

			calculateTSize(s, (double)data.get(n-1).t, scale * VISIBLE_POWER, 1000., 10.);

			double ySize = 0;
			for (MindFlexReader.Data d: data) 
				for (double y: d.power)
					if (y > ySize)
						ySize = y;

			double subgraphContentHeight = (s.getHeight() - POWER_SPACING * (1+MindFlexReader.POWER_NAMES.length) ) / (2+MindFlexReader.POWER_NAMES.length);
			subgraphHeight = subgraphContentHeight + POWER_SPACING;
			yScale = subgraphContentHeight / ySize;

			g2.setColor(Color.BLUE);
			for (Mark m: marks) {
				Line2D lin = new Line2D.Double(scaleT(m.t), 0,
						scaleT(m.t), s.getHeight());
				g2.draw(lin);
			}

			g2.setColor(Color.GREEN);
			for (int j = 0 ; j < MindFlexReader.POWER_NAMES.length + 1 ; j++) {
				Line2D lin = new Line2D.Double(0, subgraphHeight * ( j + 1 ) + POWER_SPACING / 2,
						s.getWidth(), subgraphHeight * ( j + 1 ) + POWER_SPACING / 2);
				g2.draw(lin);
			}
			
			for (int j = 0 ; j < MindFlexReader.POWER_NAMES.length ; j++) {
				g2.drawChars(MindFlexReader.POWER_NAMES[j].toCharArray(), 0, MindFlexReader.POWER_NAMES[j].length(), 
						0, (int)(j * subgraphHeight + ySize * .5 * yScale));
			}
			g2.drawChars("Attention".toCharArray(), 0, "Attention".length(), 
					0, (int)(MindFlexReader.POWER_NAMES.length * subgraphHeight + ySize * .5 * yScale));
			g2.drawChars("Meditation".toCharArray(), 0, "Meditation".length(), 
					0, (int)((1+MindFlexReader.POWER_NAMES.length) * subgraphHeight + ySize * .5 * yScale));

			g2.setColor(Color.BLACK);
			MindFlexReader.Data d0 = null;

			for (int i=0; i<n; i++) {
				MindFlexReader.Data d1 = data.get(i);
				if (0<i && startT <= d0.t && d1.t <= endT) { 
					if (d0.havePower && d1.havePower) { 
						for (int j=0; j<MindFlexReader.POWER_NAMES.length; j++) {
							scaledLine(g2, d0.t, ySize - d0.power[j], d1.t, ySize - d1.power[j], j);
						}
					}
					if (d0.haveAttention && d1.haveAttention) {
						scaledLine(g2, d0.t, (1 - d0.attention)*ySize, d1.t, (1-d0.attention)*ySize, 
									MindFlexReader.POWER_NAMES.length);
					}
					if (d0.haveMeditation && d1.haveMeditation) {
						scaledLine(g2, d0.t, (1 - d0.meditation)*ySize, d1.t, (1-d0.meditation)*ySize, 
								MindFlexReader.POWER_NAMES.length + 1);
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

	public double log2(double d) {
		return Math.log(d)/Math.log(2);
	}
	
	public void update() {
		if (pause < 0)
			repaint();
	}
	
	public void terminate() {
		dispose();
	}
	
	public class Mark {
		int t;
		int rawCount;
		
		public Mark() {
			this.t = (int)(System.currentTimeMillis()-mfr.t0);
			this.rawCount = mfr.packetCount;
		}
	}
}
