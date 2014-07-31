package mobi.omegacentauri.brainflex;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import mobi.omegacentauri.brainflex.BrainFlexGUI.Mark;

public class RawGraphPanel extends GraphPanel {
	/**
	 *
	 */
	private static final long serialVersionUID = 8739434584974120909L;
	private static final int RAW_PER_SECOND = 515; // what an odd number!  Why not 512?
	public static final int VISIBLE = RAW_PER_SECOND * 3;
	private static final int WINDOW = 512;  // must be divisible by 2
	private static final double MAX_FREQ_PASS = 55; // Hz
	private static final double MIN_FREQ_PASS = 0; // HZ

	private static final int HR_WINDOW = 4096;
	private static final double HR_MAX_FREQ_PASS = 40;
	private static final double HR_MIN_FREQ_PASS = 0.2;
	
	private boolean filter = false;
	private boolean hr = true;
	private FastFourierTransformer fft;
//	private double primaryFreq;

	public RawGraphPanel(BrainFlexGUI gui, ViewerWindow w, List<?> data) {
		super(gui, w, data);

		hr = Preferences.userNodeForPackage(BrainFlex.class).getBoolean(BrainFlex.PREF_HEART_MODE, false);

		if (hr || filter)
			fft = new FastFourierTransformer(DftNormalization.STANDARD);
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

		int max = (int)endT;
		if (n < max)
			max = n;
		
		data = filter(data, max);

		double ySize = 0;
		for (int y: data) {
			if (y > ySize)
				ySize = y;
			else if (-y > ySize)
				ySize = -y;
		}

		ySize *= 2.2;
		
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

		for (int i=(int)startT; i<max; i++) {
			int d1 = data.get(i);
			if (0<i) {
				scaledLine(g2, i-1, ySize / 2 - d0, i, ySize / 2 - d1, 0);
			}
			d0 = d1;
		}

		if (hr) {
			double rate = hr(data, max);
			if ( 0 < rate ) {
				String rateLabel = "Rate: " + new DecimalFormat("0.00").format(rate);
				g2.drawChars(rateLabel.toCharArray(), 0, rateLabel.length(), 2, s.height - 2);
			}
		}
		
	}
        
	private double hr(List<Integer> data, int max) {
		if (max < HR_WINDOW)
			return -1;
		
		double signal[] = new double[HR_WINDOW * 2];
		for (int i = 0 ; i < HR_WINDOW ; i++)
			signal[i] = data.get(max - 1 - i);
		for (int i = HR_WINDOW ; i < 2 * HR_WINDOW ; i++)
			signal[i] = 0.;
		
		Complex[] trans = fft.transform(signal, TransformType.FORWARD);
		int cutOff = (int) (HR_WINDOW * HR_MAX_FREQ_PASS / RAW_PER_SECOND);
		for (int i = cutOff ; i < 2 * WINDOW ; i++)
			trans[i] = new Complex(0.);
		cutOff = (int)(HR_WINDOW * HR_MIN_FREQ_PASS / RAW_PER_SECOND);
		for (int i = 0 ; i < cutOff ; i++)
			trans[i] = new Complex(0.);
		
		trans = fft.transform(trans, TransformType.INVERSE);
		
		double m = 0;
		
		for (int i = 0 ; i < WINDOW ; i++) {
			signal[i] = trans[i].abs();
			if (signal[i] > m)
				m = signal[i];
		}
		
		IntervalCount best = null;
		
		for (double level = 0.2 * m ; level <= 0.85 * m ; level += m * .05) {
			IntervalCount c = new IntervalCount(signal, 0, HR_WINDOW, level);
//			System.out.println("Frequency: "+ ( RAW_PER_SECOND / c.averageLength ) );
//			System.out.println("Deviation: " + c.deviation);
//			System.out.println("Level: " + c.level / m);
			if (( best == null && ! Double.isInfinite(c.deviation)) || c.deviation < best.deviation) 
				best = c;
		}
		
//		System.out.println("Best frequency: "+ ( RAW_PER_SECOND / best.averageLength ) );
//		System.out.println("Deviation: " + best.deviation);
//		System.out.println("Level: " + best.level / m);
		
		if (best.deviation > 0.25)
			return -1;
		
		return 60 * RAW_PER_SECOND / best.averageLength;
	}

	private List<Integer> filter(List<Integer> data, int n) {
		if (!filter)
			return data;

		List<Integer> out = new ArrayList<Integer>();

		if (n % WINDOW != 0) {
			addFilteredWindow(data, 0, n % WINDOW, out);
		}
		for (int i = n % WINDOW; i < n; i += WINDOW) {
			addFilteredWindow(data, i, WINDOW, out);
		}

		return out;
	}

	// count must be less than WINDOW
	private void addFilteredWindow(List<Integer> in, int start, int count, List<Integer> out) {
		double[] signal = new double[2*WINDOW];

		for (int j = 0 ; j < 2*WINDOW ; j++) {
			if (start + j < in.size() && j < WINDOW)
				signal[j] = in.get(start + j);
			else
				signal[j] = 0;
		}
		Complex[] trans = fft.transform(signal, TransformType.FORWARD);
		
		int cutOff = (int) (WINDOW * MAX_FREQ_PASS / RAW_PER_SECOND);

		int j;
		for (j = cutOff ; j < WINDOW * 2 ; j++)
			trans[j] = new Complex(0);
		
		cutOff = (int)(WINDOW * MIN_FREQ_PASS / RAW_PER_SECOND);
		
		for (j=0; j < cutOff ; j++)
			trans[j] = new Complex(0);
		
		trans = fft.transform(trans, TransformType.INVERSE);

		for (j=0; j < count; j++)
			out.add((int)trans[j].getReal());
	}
	
	class IntervalCount {
		double averageLength;
		double deviation;
		int count;
		double level;
		
		public IntervalCount(double[] signal, int start, int dataCount, double level) {
			this.level = level;
			
			deviation = Double.POSITIVE_INFINITY;
			
			int safeStart;
			for (safeStart = start; safeStart < start + dataCount && signal[safeStart] >= level ; safeStart++);
			
			if (safeStart >= start + dataCount)
				return;
			
			int safeEnd;
			for (safeEnd = start + dataCount ; safeEnd > 0 && signal[safeEnd -1 ] >= level ; safeEnd--);
			
			int length = 0;
			int intervalStart = -1;
			boolean inPeak = false;
			
			List<Integer> lengths = new LinkedList<Integer>();
			
			for (int i = safeStart ; i < safeEnd ; i++) {
				if (intervalStart < 0 && signal[i] >= level) {
					intervalStart = i;
					inPeak = true;
				}
				else if (intervalStart >= 0 && ! inPeak && signal[i] >= level) {
					lengths.add(i - intervalStart);
					length += i - intervalStart;
					intervalStart = i;
					inPeak = true;
				}
				else if (signal[i] < level) {
					inPeak = false;
				}
			}
			
			if (lengths.size() == 0)
				return;
			
			averageLength = (double)length / lengths.size();

			this.deviation = 0;
			
			for (int l : lengths) {
				double dev = Math.abs(l - averageLength) / averageLength;
				if (dev > this.deviation)
					this.deviation = dev;
			}
		}
	}
}
