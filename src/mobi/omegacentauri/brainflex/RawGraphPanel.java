package mobi.omegacentauri.brainflex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import mobi.omegacentauri.brainflex.BrainFlexGUI.Mark;

public class RawGraphPanel extends GraphPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8739434584974120909L;
	private static final int RAW_PER_SECOND = 515; // what an odd number!  Why not 512?
	public static final int VISIBLE = RAW_PER_SECOND * 3;

	public RawGraphPanel(BrainFlexGUI gui, ViewerWindow w, List<?> data) {
		super(gui, w, data);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void draw(Graphics2D g2, Dimension s, 
			List<BrainFlexGUI.Mark> marks) {
		List<Integer> data = new ArrayList<Integer>((List<Integer>) origData);
		
		int n = w.pause.point < 0 ? data.size() : w.pause.point;
		if (n<1)
			return;
		
		w.setTime(n * 1000 / RAW_PER_SECOND, n, w.pause.point < 0 ? mfr.badPacketCount : w.pause.pausedBadPacketCount );
				
		if (n<2)
			return;

		calculateTSize(s, n-1, w.scale * VISIBLE, 16., 1.);

		double ySize = 0;
		for (int y: data) {
			if (y > ySize)
				ySize = y;
			else if (-y > ySize)
				ySize = -y;
		}
		
		ySize *= 2;
		
		if (ySize < 0)
			ySize = 1;
		
		yScale = s.getHeight() / ySize;
		subgraphHeight = 0;
		
		g2.setColor(Color.BLUE);
		for (Mark m: marks) {
			if (startT <= m.rawCount && m.rawCount < endT) {
				Line2D lin = new Line2D.Double(scaleT(m.rawCount), 0,
						scaleT(m.rawCount), s.getHeight());
				g2.draw(lin);
			}
		}

		g2.setColor(Color.BLACK);
		int d0 = 0;

		for (int i=(int)startT; i<endT && i<n; i++) {
			int d1 = data.get(i);
			if (0<i) { 
				scaledLine(g2, i-1, ySize / 2 - d0, i, ySize / 2 - d1, 0);
			}
			d0 = d1;
		}		
	}
}
