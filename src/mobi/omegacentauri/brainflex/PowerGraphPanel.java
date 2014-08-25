package mobi.omegacentauri.brainflex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mobi.omegacentauri.brainflex.MindFlexReader.PowerData;

public class PowerGraphPanel extends GraphPanel {
	private static final long serialVersionUID = -4623488847975233096L;
	private static final int VISIBLE=512 * 1000;
	private static final int SPACING = 3;

	public PowerGraphPanel(BrainFlexGUI gui, ViewerWindow w, List<?> data) {
		super(gui, w, data, 0.15);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected
	void draw(Graphics2D g2, Dimension s, List<Mark> marks) {
		List<MindFlexReader.PowerData> data = new ArrayList<MindFlexReader.PowerData>((List<MindFlexReader.PowerData>)origData);
		
		int n = w.pause.point < 0 ? data.size() : w.pause.point;
		if (n<1)
			return;

		calculateTSize(s, (double)data.get(n-1).t, w.scale * VISIBLE, 1000., 1.);
		marks.add(new Mark(data.get(n-1).t, -1, "end"));
		Collections.sort(marks);

		double ySize = 0;

		for (int i=0; i<n; i++) 
			for (double y: data.get(i).power)
				if (y > ySize)
					ySize = y;
		
		double effectiveHeight = s.getHeight() - 16;

		double subgraphContentHeight = (effectiveHeight - SPACING * (1+MindFlexReader.POWER_NAMES.length) ) / (2+MindFlexReader.POWER_NAMES.length);
		subgraphHeight = subgraphContentHeight + SPACING;
		yScale = subgraphContentHeight / ySize;

		g2.setColor(Color.BLUE);
		g2.setFont(new Font("default", Font.BOLD, 12));


		for (int i = 0 ; i < marks.size() ; i++) {
			Mark m = marks.get(i);
			if (startT <= m.t && m.t < endT) {
				double x = scaleT(m.t);
				if (m.rawCount >= 0) {
					Line2D lin = new Line2D.Double(x, 0,
							x, effectiveHeight);
					g2.draw(lin);
					if (m.descriptor.length() > 0) {
						g2.drawChars(m.descriptor.toCharArray(), 0, m.descriptor.length(), (int)x, (int)s.getHeight()-14);
					}
				}
			}
		}
		
//		int fromTime;
//		String avgDesc;
//		if (lastM == null) {
//			fromTime = 0;
//			avgDesc = "start";
//		}
//		else {
//			fromTime = lastM.t;
//			if (lastM.descriptor.length() == 0) 
//				avgDesc = "unnamed";
//			else
//				avgDesc = lastM.descriptor;
//		}
//		
//		avgDesc += "-";
//
//		int toTime;
//		if (nextM == null) {
//			toTime = data.get(n-1).t;
//			avgDesc += "end";
//		}
//		else {
//			toTime = nextM.t;
//			if (nextM.descriptor.length() == 0) 
//				avgDesc += "unnamed";
//			else
//				avgDesc += nextM.descriptor;
//		}
//		avgDesc += ": ";

		g2.setColor(Color.GREEN);
		for (int j = 0 ; j < MindFlexReader.POWER_NAMES.length + 1 ; j++) {
			Line2D lin = new Line2D.Double(0, subgraphHeight * ( j + 1 ) + SPACING / 2,
					s.getWidth(), subgraphHeight * ( j + 1 ) + SPACING / 2);
			g2.draw(lin);
		}
		
		g2.setColor(new Color(0f,0.5f,0f));
		g2.setFont(new Font("default", Font.BOLD, 12));
		for (int j = 0 ; j < MindFlexReader.POWER_NAMES.length ; j++) {
			g2.drawChars(MindFlexReader.POWER_NAMES[j].toCharArray(), 0, MindFlexReader.POWER_NAMES[j].length(), 
					0, (int)(j * subgraphHeight + ySize * .5 * yScale + 6));
		}
		g2.drawChars("Attention".toCharArray(), 0, "Attention".length(), 
				0, (int)(MindFlexReader.POWER_NAMES.length * subgraphHeight + ySize * .5 * yScale + 6));
		g2.drawChars("Meditation".toCharArray(), 0, "Meditation".length(), 
				0, (int)((1+MindFlexReader.POWER_NAMES.length) * subgraphHeight + ySize * .5 * yScale + 6));

		g2.setColor(Color.BLACK);
		MindFlexReader.PowerData d0 = null;

		int first = -1;
		int last = -1;
		
		for (int i=0; i<n; i++) {
			MindFlexReader.PowerData d1 = data.get(i);
			if (first < 0 && startT <= d1.t)
				first = d1.t;
			if (last < 0 && (endT <= d1.t || i == n-1) ) {
				if (d0 != null)
					last = d0.t;
				else
					last = d1.t;					
			}
			if (0<i && startT <= d0.t && d1.t <= endT) { 
				if (d0.havePower && d1.havePower) { 
					for (int j=0; j<MindFlexReader.POWER_NAMES.length; j++) {
						scaledLine(g2, d0.t, ySize - d0.power[j], d1.t, ySize - d1.power[j], j);
					}
				}
				if (d0.haveAttention && d1.haveAttention) {
					scaledLine(g2, d0.t, (1 - d0.attention)*ySize, d1.t, (1-d1.attention)*ySize, 
								MindFlexReader.POWER_NAMES.length);
				}
				if (d0.haveMeditation && d1.haveMeditation) {
					scaledLine(g2, d0.t, (1 - d0.meditation)*ySize, d1.t, (1-d1.meditation)*ySize, 
							MindFlexReader.POWER_NAMES.length + 1);
				}
			}
			d0 = d1;
		}
		
		w.setTimeRange(first, last);
	}

	private double getAveragePower(List<PowerData> data, int n, int fromTime, int toTime,
			int power) {
		double sum = 0;
		int count = 0;
		for (int i = 0 ; i < n ; i++) {
			PowerData d = data.get(i);
			if (d.havePower && fromTime <= d.t && d.t < toTime ) {
				sum += d.power[power];
				count++;
			}
		}
		return sum/count;
	}
	
	private double getAverageAttention(List<PowerData> data, int n, int fromTime, int toTime) {
		double sum = 0;
		int count = 0;
		for (int i = 0 ; i < n ; i++) {
			PowerData d = data.get(i);
			if (d.haveAttention && fromTime <= d.t && d.t < toTime ) {
				sum += d.attention;
				count++;
			}
		}
		return sum/count;
	}
	
	private double getAverageMeditation(List<PowerData> data, int n, int fromTime, int toTime) {
		double sum = 0;
		int count = 0;
		for (int i = 0 ; i < n ; i++) {
			PowerData d = data.get(i);
			if (d.haveMeditation && fromTime <= d.t && d.t < toTime ) {
				sum += d.meditation;
				count++;
			}
		}
		return sum/count;
	}
	
}
