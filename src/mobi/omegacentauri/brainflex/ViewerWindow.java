package mobi.omegacentauri.brainflex;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;

import mobi.omegacentauri.brainflex.BrainFlexGUI.Mark;

public class ViewerWindow extends JFrame {
	private static final long serialVersionUID = 7102164930300849382L;
	public JScrollBar scrollBar;
	private final BrainFlex bf;
	private final MindFlexReader mfr;
	public JTextField timeText;
	static final double MIN_SCALE = 1/16.;
	static final double MAX_SCALE = 4.;
	static final double SCALE_MULT = 1.5;
	double scale;
	Pause pause;
	List<?> data;

	public ViewerWindow(BrainFlex bf, boolean raw) {
		this.bf = bf;
		this.mfr = bf.mfr;
		scale = 1.;
		pause = new Pause();
		setSize(640,480);
		setLocationByPlatform(true);
		
		if (raw) {
			setTitle("Raw MindFlex data");
			data = mfr.rawData;
		}
		else {
			setTitle("Processed MindFlex data");
			data = mfr.powerData;
		}
		
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
				ViewerWindow.this.bf.closing(ViewerWindow.this);
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

		final GraphPanel graph = raw ? new RawGraphPanel(bf, this, data) : new PowerGraphPanel(bf, this, data);	
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
				Mark mark = new Mark((int)(System.currentTimeMillis()-ViewerWindow.this.mfr.t0), 
						ViewerWindow.this.mfr.rawData.size());
				System.out.println("Mark "+mark.t+ " "+mark.rawCount);
				ViewerWindow.this.bf.addMark(mark);
			}
		}); 	
		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				ViewerWindow.this.mfr.stop();
			}
		}); 	
		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pause.point < 0) {
					pause.point = ViewerWindow.this.data.size();
					pause.pausedBadPacketCount = ViewerWindow.this.mfr.badPacketCount;
				}
				else {
					pause.point = -1;
				}
				repaint();
			}
		}); 	
		timeText = new JTextField();
		timeText.setEditable(false);
		setTime(0, 0, 0);
		Dimension m = timeText.getMaximumSize();
		Dimension p = timeText.getPreferredSize();
		m.height = p.height;
		timeText.setMaximumSize(m);

		buttonPanel.add(plusButton);
		buttonPanel.add(minusButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(markButton);
		buttonPanel.add(exitButton);
		buttonPanel.add(timeText);
		
		getContentPane().add(buttonPanel);
		
		setVisible(true);
	}

	public void setTime(int t, int c, int bad) {
		timeText.setText(new DecimalFormat("0.000").format(t/1000.)+"s ("+c+" packets; "+bad+" bad packets)");
	}
	
	public class Pause {
	    int point = -1;
		int pausedBadPacketCount;
	}

	public void updateGraph() {
		if (pause.point < 0)
			repaint();
	}
}
