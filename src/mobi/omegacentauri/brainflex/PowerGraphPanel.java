package mobi.omegacentauri.brainflex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.List;

import javax.swing.JScrollBar;

import mobi.omegacentauri.brainflex.BrainFlexGUI.Mark;

public class PowerGraphPanel extends GraphPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4623488847975233096L;
	private static final int VISIBLE=512;
	private static final int SPACING = 3;

	public PowerGraphPanel(BrainFlexGUI gui, ViewerWindow w) {
		super(gui, w);
	}
	
	@Override
	protected
	void draw(Graphics2D g2, Dimension s, List<MindFlexReader.Data> data, List<BrainFlexGUI.Mark> marks, int n) {
		if (n<2)
			return;

		calculateTSize(s, (double)data.get(n-1).t, w.scale * VISIBLE, 1000., 10.);

		double ySize = 0;
		for (MindFlexReader.Data d: data) 
			for (double y: d.power)
				if (y > ySize)
					ySize = y;

		double subgraphContentHeight = (s.getHeight() - SPACING * (1+MindFlexReader.POWER_NAMES.length) ) / (2+MindFlexReader.POWER_NAMES.length);
		subgraphHeight = subgraphContentHeight + SPACING;
		yScale = subgraphContentHeight / ySize;

		g2.setColor(Color.BLUE);
		for (Mark m: marks) {
			Line2D lin = new Line2D.Double(scaleT(m.t), 0,
					scaleT(m.t), s.getHeight());
			g2.draw(lin);
		}

		g2.setColor(Color.GREEN);
		for (int j = 0 ; j < MindFlexReader.POWER_NAMES.length + 1 ; j++) {
			Line2D lin = new Line2D.Double(0, subgraphHeight * ( j + 1 ) + SPACING / 2,
					s.getWidth(), subgraphHeight * ( j + 1 ) + SPACING / 2);
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
