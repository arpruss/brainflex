package mobi.omegacentauri.brainflex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.List;

import javax.swing.JScrollBar;

import mobi.omegacentauri.brainflex.BrainFlexGUI.Mark;
import mobi.omegacentauri.brainflex.MindFlexReader.Data;

public class RawGraphPanel extends GraphPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8739434584974120909L;
	public static final int VISIBLE = 1500;

	public RawGraphPanel(JScrollBar scrollBar, BrainFlexGUI gui) {
		super(scrollBar, gui);
	}

	@Override
	protected void draw(Graphics2D g2, Dimension s, List<MindFlexReader.Data> data,
			List<BrainFlexGUI.Mark> marks, int n) {
		if (n<2)
			return;

		calculateTSize(s, (double)data.get(n-1).rawCount, gui.getScale() * VISIBLE, 16., 1.);

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
}
