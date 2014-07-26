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
	static final int MAX_VISIBLE_RAW=1500;
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
		private static final int GRAPH_SPACING = 3;
		
		public MyPanel() {
			super();
		}

		@Override
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

			int maxT = data.get(n-1).rawCount;
			int startT;
			double tSize;

			if (maxT > MAX_VISIBLE_RAW) {
				tSize = MAX_VISIBLE_RAW;

				if (! scrollBar.isVisible() || scrollBar.getMaximum() != maxT) {
					scrollBar.setMaximum(maxT);
					scrollBar.setVisibleAmount(MAX_VISIBLE_RAW);
					scrollBar.setMinimum(0);
					scrollBar.setVisible(true);
				}
				
				startT = scrollBar.getValue();
			}
			else {
				scrollBar.setVisible(false);
				startT = 0;
				tSize = Math.pow(2, Math.ceil(log2(maxT - startT + 16)));
			}
			double tScale = s.getWidth() / tSize;

			double ySize = 0;
			for (MindFlexReader.Data d: data) {
				if (d.raw > ySize)
					ySize = d.raw;
				else if (-d.raw > ySize)
					ySize = -d.raw;
			}
			
			ySize *= 2;
			
			double yScale = s.getHeight() / ySize;
			
			g2.setColor(Color.BLUE);
			for (Mark m: marks) {
				Line2D lin = new Line2D.Double((m.rawCount - startT) * tScale, 0,
						m.rawCount * tScale, s.getHeight());
				g2.draw(lin);
			}

			g2.setColor(Color.BLACK);
			MindFlexReader.Data d0 = null;

			for (int i=0; i<n; i++) {
				MindFlexReader.Data d1 = data.get(i);
				if (0<i && d0.rawCount >= startT && d0.haveRaw && d1.haveRaw) { 
					Line2D lin = new Line2D.Double((d0.rawCount - startT) * tScale, 
							(ySize / 2 - d0.raw) * yScale,
							(d1.rawCount - startT) * tScale, 
							(ySize / 2 - d1.raw) * yScale);
					g2.draw(lin);					
				}
				d0 = d1;
			}
		}

		private void drawPower(Graphics2D g2, Dimension s, List<MindFlexReader.Data> data, int n) {
			if (n<2)
				return;
			double tSize = Math.pow(2, Math.ceil(log2(data.get(n-1).t + 1000 )));
			double tScale = s.getWidth() / tSize;
			double ySize = 0;
			for (MindFlexReader.Data d: data) 
				for (double y: d.power)
					if (y > ySize)
						ySize = y;

			double subgraphContentHeight = (s.getHeight() - GRAPH_SPACING * (1+MindFlexReader.POWER_NAMES.length) ) / (2+MindFlexReader.POWER_NAMES.length);
			double subgraphHeight = subgraphContentHeight + GRAPH_SPACING;
			double yScale = subgraphContentHeight / ySize;

			g2.setColor(Color.BLUE);
			for (Mark m: marks) {
				Line2D lin = new Line2D.Double(m.t * tScale, 0,
						m.t * tScale, s.getHeight());
				g2.draw(lin);
			}

			g2.setColor(Color.GREEN);
			for (int j = 0 ; j < MindFlexReader.POWER_NAMES.length + 1 ; j++) {
				Line2D lin = new Line2D.Double(0, subgraphHeight * ( j + 1 ) + GRAPH_SPACING / 2,
						s.getWidth(), subgraphHeight * ( j + 1 ) + GRAPH_SPACING / 2);
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
				if (0<i) { 
					if (d0.havePower && d1.havePower) { 
						for (int j=0; j<MindFlexReader.POWER_NAMES.length; j++) {
							Line2D lin = new Line2D.Double(d0.t * tScale, 
									(ySize - d0.power[j]) * yScale + j * subgraphHeight,
									d1.t * tScale, 
									(ySize - d1.power[j]) * yScale + j * subgraphHeight);
							g2.draw(lin);
						}
					}
					if (d0.haveAttention && d1.haveAttention) {
						Line2D lin = new Line2D.Double(d0.t * tScale, 
								(1 - d0.attention) * subgraphContentHeight + MindFlexReader.POWER_NAMES.length * subgraphHeight,
								d1.t * tScale, 
								(1 - d1.attention) * subgraphContentHeight + MindFlexReader.POWER_NAMES.length * subgraphHeight);
						g2.draw(lin);
					}
					if (d0.haveMeditation && d1.haveMeditation) {
						Line2D lin = new Line2D.Double(d0.t * tScale, 
								(1 - d0.meditation) * subgraphContentHeight + (1+MindFlexReader.POWER_NAMES.length) * subgraphHeight,
								d1.t * tScale, 
								(1 - d1.meditation) * subgraphContentHeight + (1+MindFlexReader.POWER_NAMES.length) * subgraphHeight);
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
			final BrainFlex bf = new BrainFlex(comPort);
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
