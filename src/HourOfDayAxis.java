import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.awt.Color;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

public class HourOfDayAxis extends NumberAxis {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2455114288828927514L;

	@SuppressWarnings("unchecked")
	protected AxisState drawTickMarksAndLabels(Graphics2D g2, double cursor, Rectangle2D plotArea, Rectangle2D dataArea,
			RectangleEdge edge) {
		AxisState state = new AxisState(cursor);

		g2.setFont(getTickLabelFont());

		double ol = getTickMarkOutsideLength();
		int y = (int) (Math.round(cursor - ol));
		LineMetrics lineMetrics = g2.getFont().getLineMetrics("Ápr", g2.getFontRenderContext());
		int h = (int) (lineMetrics.getHeight() + 6);

		List<ValueTick> ticks = refreshTicks(g2, state, dataArea, edge);
		state.setTicks(ticks);

		/* Last x point */
		ValueTick tick = (ticks.size() != 0) ? ticks.get(ticks.size() - 1) : ticks.get(0);
		float[] prevAnchorPoint = calculateAnchorPoint(tick, cursor, dataArea, edge);
		double xmax = prevAnchorPoint[0];
		double max_minute = tick.getValue();

		/* First x point */
		tick = ticks.get(0);
		prevAnchorPoint = calculateAnchorPoint(tick, cursor, dataArea, edge);
		double xmin = Math.round(prevAnchorPoint[0]);
		double min_minute = tick.getValue();
		double minutes_visible = max_minute - min_minute + 1;
		
		/* 0.1 day horizontal gap. */
		double gap = 0.1 * (xmax - xmin) / minutes_visible;
		
		g2.setFont(getTickLabelFont());
		g2.setColor(Color.DARK_GRAY);
		int start_minute = (int) min_minute;
		for (int hours = 0; hours < 24; hours++) {
			
			int end_minute = start_minute + 60;			
			if ((start_minute >= min_minute) && (start_minute <= max_minute) && (end_minute >= min_minute) && (end_minute <= max_minute)) {
				
				double factor_x1 = (start_minute - min_minute) / minutes_visible;
				double x1 = xmin + (xmax - xmin) * factor_x1;
				double factor_x2 = (end_minute - min_minute) / minutes_visible;
				double x2 = xmin + (xmax - xmin) * factor_x2;
				
				g2.setColor(new Color(0xFF, 0xFF, 0xFF));
				g2.fill3DRect((int) (x1 + gap), y, (int) (x2 - x1 - 2 * gap), h, true);
				g2.setColor(Color.DARK_GRAY);
				TextUtilities.drawAlignedString(String.valueOf(hours), g2, (float) ((x1 + x2) / 2), (float) (y + ol),
						TextAnchor.TOP_CENTER);
			}
			
			start_minute += 60;
			
		}
		
		return state;
		
	}

}
