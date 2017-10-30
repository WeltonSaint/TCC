import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.sourceforge.jlibeps.epsgraphics.EpsGraphics2D;

public class Main {

	final static int[] ids = { 1, 2, 3, 4, 5, 6, 7, 8, 12 };
	final static String DISTANCE = "Distance";
	final static String SOCIALSTRENGTH = "SocialStrength";
	final static String MOTION = "PhysicalActivity";
	final static SimpleDateFormat df = new SimpleDateFormat("dd/MM-HH:mm:ss.S");
	final static int PLOT_SOCIAL_STRENGTH = 1000;
	final static int PLOT_ENCOUNTERS = 2000;
	final static int PLOT_ENCOUNTER_DURATION = 3000;
	final static int PLOT_PROPINQUITY = 4000;
	final static int MINUTES_INTERVAL = 1;
	final static int HOURS_INTERVAL = 60;
	final static int DAYS_INTERVAL = 1440;
	final static Color[] seriesColors = { Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.ORANGE,
			Color.YELLOW, Color.GRAY, new Color(163, 73, 164)};
	public static HashMap<Long, HashMap<String, Nodo>> prop;
	public static ArrayList<Integer> presentSeries;

	public static void readFile(int id, String type, long start, long end, long interval) {

		BufferedReader reader;

		try {
			String line = "";
			Date dateFirst = null, dateLast = null;
			reader = new BufferedReader(new FileReader("arquivos/Copelabs" + id + "/Source/" + type + ".dat"));
			while ((line = reader.readLine()) != null) {

				String[] values = line.split("\t");

				if (dateFirst != null) {
					if (values[0].trim().length() > 18)
						values[0] = values[0].trim().substring(values[0].trim().length() - 18,
								values[0].trim().length() - 1);
					dateLast = (Date) df.parse(values[0].trim());
				} else {
					if (values[0].trim().length() > 18)
						values[0] = values[0].trim().substring(values[0].trim().length() - 18,
								values[0].trim().length() - 1);
					dateFirst = (Date) df.parse(values[0].trim());
					dateLast = (Date) df.parse(values[0].trim());
				}
				long date = TimeUnit.MILLISECONDS.toMinutes(dateLast.getTime() - dateFirst.getTime());
				double value;

				if (date >= start && date <= end) {

					switch (type) {
					case DISTANCE:
						if (line.contains("Copelabs")) {

							value = Double.parseDouble(values[values.length - 1]);
							if (value > 0) {

								if (prop.get(date) == null) {
									prop.put(date, new HashMap<>());
									for (int i : ids) {
										if (prop.get(date).get("Copelabs" + i) == null)
											prop.get(date).put("Copelabs" + i, new Nodo());
									}

								}

								if (prop.get(date).get(values[1]) == null)
									prop.get(date).put(values[1], new Nodo());

								prop.get(date).get(values[1]).distance = value;

								prop.get(date).get(values[1]).encounters = prop.get(date).get(values[1]).encounters + 1;

							}
						}
						break;

					case SOCIALSTRENGTH:

						if (line.contains("Copelabs")) {

							double valueSocialStrength = Double.parseDouble(values[values.length - 1]);
							double valueEncounterDuration = Double.parseDouble(values[2]);

							if (valueSocialStrength >= 0.0 || valueEncounterDuration >= 0.0) {

								if (prop.get(date) == null) {
									prop.put(date, new HashMap<>());
									for (int i : ids) {
										if (prop.get(date).get("Copelabs" + i) == null)
											prop.get(date).put("Copelabs" + i, new Nodo());
									}

								}

								if (prop.get(date).get(values[1]) == null)
									prop.get(date).put(values[1], new Nodo());

								prop.get(date).get(values[1]).socialStrength = valueSocialStrength;
								prop.get(date).get(values[1]).durationEncounter = valueEncounterDuration;
							}

						}

						break;

					case MOTION:

						if (prop.get(date) == null) {
							prop.put(date, new HashMap<>());
							for (int i : ids) {
								if (prop.get(date).get("Copelabs" + i) == null)
									prop.get(date).put("Copelabs" + i, new Nodo());
							}
						}

						for (int i : ids) {

							if (prop.get(date).get("Copelabs" + i) == null)
								prop.get(date).put("Copelabs" + i, new Nodo());

							prop.get(date)
									.get("Copelabs"
											+ i).motion = (values[values.length - 1].trim().equals("STATIONARY"))
													? 1
													: (values[values.length - 1].trim().equals("STATIONARY")
															&& !values[values.length - 1].trim().equals("WALKING")) ? 2
																	: 3;

							prop.get(date)
									.get("Copelabs" + i).propinquity = prop.get(date).get("Copelabs" + i).socialStrength
											/ ((prop.get(date).get("Copelabs" + i).distance + 1)
													* prop.get(date).get("Copelabs" + i).motion);

						}

						break;

					default:
						break;
					}

				}

			}

			if (type == MOTION) {

				plotGraph(PLOT_ENCOUNTER_DURATION, id, start, end, interval);
				plotGraph(PLOT_ENCOUNTERS, id, start, end, interval);
				plotGraph(PLOT_SOCIAL_STRENGTH, id, start, end, interval);
				plotGraph(PLOT_PROPINQUITY, id, start, end, interval);

			}

			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void plotGraph(int option, int id, long start, long end, long interval) {

		String titleChart = null, unit = null;
		XYSeriesCollection dataset = new XYSeriesCollection();
		HashMap<String, XYSeries> series = new HashMap<>();

		switch (option) {
		case PLOT_ENCOUNTERS:
			titleChart = "Número de Encontros entre Copelabs" + id + " e Outros Dispositivos (Total)";
			unit = "Número de Encontros";
			break;
		case PLOT_SOCIAL_STRENGTH:
			titleChart = "Força Social entre Copelabs" + id + " e Outros Dispositivos";
			unit = "Força Social";
			break;
		case PLOT_ENCOUNTER_DURATION:
			titleChart = "Duração Média de Encontros entre Copelabs" + id + " e Outros Dispositivos";
			unit = "Duração Média";
			break;
		case PLOT_PROPINQUITY:
			titleChart = "Propinência (Propinquity) entre Copelabs" + id + " e Outros Dispositivos";
			unit = "Propinência (Propinquity)";
			break;
		default:
			break;
		}

		JFreeChart lineChart = ChartFactory.createXYLineChart(titleChart, "Tempo", unit, dataset,
				PlotOrientation.VERTICAL, true, true, false);
		SortedSet<Long> keys = new TreeSet<Long>(prop.keySet());
		HashMap<String, Integer> aux = new HashMap<>();
		for (Long key : keys) {

			if (key >= start && key <= end) {

				for (int i : ids) {

					if (i != id) {

						if (series.get("Copelabs" + i) == null)
							series.put("Copelabs" + i, new XYSeries("Copelabs" + i));

						switch (option) {
						case PLOT_ENCOUNTERS:

							if (aux.get("Copelabs" + i) == null)
								aux.put("Copelabs" + i, 0);

							aux.put("Copelabs" + i,
									aux.get("Copelabs" + i) + prop.get(key).get("Copelabs" + i).encounters);

							if (!series.get("Copelabs" + i).isEmpty() || aux.get("Copelabs" + i) > 0)
								series.get("Copelabs" + i).add(key, aux.get("Copelabs" + i));

							break;

						case PLOT_SOCIAL_STRENGTH:

							if (!series.get("Copelabs" + i).isEmpty()
									|| prop.get(key).get("Copelabs" + i).socialStrength > 0)
								series.get("Copelabs" + i).add(((long) key),
										prop.get(key).get("Copelabs" + i).socialStrength);

							break;

						case PLOT_ENCOUNTER_DURATION:

							if (!series.get("Copelabs" + i).isEmpty()
									|| prop.get(key).get("Copelabs" + i).durationEncounter > 0)
								series.get("Copelabs" + i).add(((long) key),
										prop.get(key).get("Copelabs" + i).durationEncounter);

							break;

						case PLOT_PROPINQUITY:

							if (!series.get("Copelabs" + i).isEmpty()
									|| prop.get(key).get("Copelabs" + i).propinquity > 0)
								series.get("Copelabs" + i).add(((long) key),
										prop.get(key).get("Copelabs" + i).propinquity);

							break;

						default:
							break;

						}
					}
				}
			}
		}
		
		presentSeries = new ArrayList<>();

		for (int i : ids) {
			if (i != id && series.get("Copelabs" + i) != null && !series.get("Copelabs" + i).isEmpty()) {
				presentSeries.add(i);
				dataset.addSeries(series.get("Copelabs" + i));
			}
		}

		saveImageChart(lineChart, titleChart, start, end, interval);

	}

	private static void saveImageChart(JFreeChart lineChartObject, String chartTitle, long start, long end,
			long interval) {

		int width = 1080;
		int height = 640;

		XYPlot plot = (XYPlot) lineChartObject.getPlot();
		
		

		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.DARK_GRAY);

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		for (int i = 0; i < 8; i++)
			renderer.setSeriesStroke(i, new BasicStroke(2.0f));
		
		for (int i = 0; i < presentSeries.size(); i++) {
			int index = (presentSeries.get(i) > 8) ? ids.length - 1 : presentSeries.get(i) - 1;
			renderer.setSeriesPaint(i, seriesColors[index]);
		}
		
		plot.setRenderer(renderer);

		// NumberAxis xAxis = new NumberAxis();
		HourOfDayAxis xAxis = new HourOfDayAxis();
		xAxis.setTickUnit(new NumberTickUnit(interval));
		xAxis.setLowerMargin(0);
		xAxis.setLowerBound(start);
		xAxis.setUpperBound(end);

		plot.setDomainAxis(xAxis);

		/*File lineChart = new File("graficos/" + chartTitle + ".jpeg");
		try {
			ChartUtilities.saveChartAsJPEG(lineChart, lineChartObject, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		
		Graphics2D g = new EpsGraphics2D();
		lineChartObject.draw(g,new Rectangle(width,height));
        Writer out;
		try {
			out = new FileWriter("graficos/" + chartTitle + ".eps");
			out.write(g.toString());
	        out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        

	}

	public static int getDayStartValue(int day) {
		return (Math.abs(day) - 1) * DAYS_INTERVAL;
	}

	public static int getDayEndValue(int day) {
		return Math.abs(day) * DAYS_INTERVAL;
	}

	public static void plotPerDay(int id, int day) {
		readFile(id, DISTANCE, getDayStartValue(day), getDayEndValue(day), HOURS_INTERVAL);
		readFile(id, SOCIALSTRENGTH, getDayStartValue(day), getDayEndValue(day), HOURS_INTERVAL);
		readFile(id, MOTION, getDayStartValue(day), getDayEndValue(day), HOURS_INTERVAL);
	}

	public static void plotAllInterval(int id, int interval) {
		readFile(id, DISTANCE, 0, 12 * DAYS_INTERVAL, interval);
		readFile(id, SOCIALSTRENGTH, 0, 12 * DAYS_INTERVAL, interval);
		readFile(id, MOTION, 0, 12 * DAYS_INTERVAL, interval);
	}

	public static void main(String[] args) {
		for (int id : ids) {
			prop = new HashMap<>();			
			System.out.println("Copelabs" + id);
			plotPerDay(id, 2);
			// plotAllInterval(id, DAYS_INTERVAL);
		}
	}

}
