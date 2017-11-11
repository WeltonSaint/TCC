import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;
import org.sourceforge.jlibeps.epsgraphics.EpsGraphics2D;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

public class Main {

	final static int[] ids = { 1, 2, 3, 4, 5, 6, 7, 8, 12 };
	final static String DISTANCE = "Distance";
	final static String SOCIALSTRENGTH = "SocialStrength";
	final static String MOTION = "PhysicalActivity";
	final static SimpleDateFormat df = new SimpleDateFormat("dd/MM-HH:mm:ss.SSS");
	final static int PLOT_SOCIAL_STRENGTH = 1000;
	final static int PLOT_ENCOUNTERS = 2000;
	final static int PLOT_ENCOUNTER_DURATION = 3000;
	final static int PLOT_PROPINQUITY = 4000;
	final static int MINUTES_INTERVAL = 1;
	final static int HOURS_INTERVAL = 60;
	final static int DAYS_INTERVAL = 1440;
	final static Color[] seriesColors = { Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.ORANGE,
			Color.YELLOW, Color.GRAY, new Color(163, 73, 164) };	
	public static HashMap<Long, HashMap<String, Nodo>> prop;
	public static ArrayList<Integer> presentSeries;
	private static boolean exportPDF = true;

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
				
				long date = dateLast.getTime()/60000;
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
		
		for (int i = 0; i < presentSeries.size(); i++) {
			int index = (presentSeries.get(i) > 8) ? ids.length - 1 : presentSeries.get(i) - 1;
			renderer.setSeriesShape(i, getShape(index));
			renderer.setSeriesShapesVisible(i, true);
		}

		plot.setRenderer(renderer);

		// NumberAxis xAxis = new NumberAxis();
		HourOfDayAxis xAxis = new HourOfDayAxis();
		xAxis.setTickUnit(new NumberTickUnit(interval));
		xAxis.setLowerMargin(0);
		xAxis.setLowerBound(start);
		xAxis.setUpperBound(end);

		plot.setDomainAxis(xAxis);

		/*
		 * File lineChart = new File("graficos/" + chartTitle + ".jpeg"); try {
		 * ChartUtilities.saveChartAsJPEG(lineChart, lineChartObject, width,
		 * height); } catch (IOException e) { e.printStackTrace(); }
		 */

		if (!exportPDF) {
			Graphics2D g = new EpsGraphics2D();
			lineChartObject.draw(g, new Rectangle(width, height));
			Writer out;
			try {
				out = new FileWriter("graficos/" + chartTitle + ".eps");
				out.write(g.toString());
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {

			BufferedOutputStream out = null;
			com.itextpdf.text.Rectangle pagesize = new com.itextpdf.text.Rectangle(width, height); 
			Document document = new Document(pagesize, 50, 50, 50, 50); 
			try {
				out =  new BufferedOutputStream(new FileOutputStream("graficos/" + chartTitle + ".pdf"));
				
				PdfWriter writer = PdfWriter.getInstance(document, out);
				document.addAuthor("JFreeChart");
				document.open();

				PdfContentByte cb = writer.getDirectContent();
				PdfTemplate tp = cb.createTemplate(width, height);
				Graphics2D g2 = tp.createGraphics(width, height);

				lineChartObject.draw(g2, new Rectangle(width, height));
				g2.dispose();
				cb.addTemplate(tp, 0, 0);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (DocumentException e) {
				e.printStackTrace();
			} finally {
				document.close();
			}

		}
	}
	
	private static Shape getShape(int index){
		Shape s  = null;
		
		switch (index) {
		case 0:
			s = ShapeUtilities.createDiamond(2);
			break;
		case 1:
			s = ShapeUtilities.createUpTriangle(2);
			break;			
		case 2:
			s = ShapeUtilities.createDownTriangle(2);
			break;
		case 3:
			s = ShapeUtilities.createDiagonalCross(1, 2);
			break;
		case 4:
			s = ShapeUtilities.createRegularCross(4, 2);
			break;
		case 5:
			s =  new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0);
			break;
		case 6:
			s = ShapeUtilities.createDiagonalCross(2, 2);
			break;
		case 7:
			s = ShapeUtilities.createRegularCross(2, 2);
			break;
		case 8:
			s =  new Ellipse2D.Double(-2.0, -2.0, 4.0, 4.0);
			break;
		default:
			break;
		}
		
		return s;		
	}

	public static long getDayStartValue(int day) {
		
		String string = "1"+( 1 + Math.abs(day))+"/09-00:00:00.000";	      
	      try {
	         Date date = df.parse(string);
	         return date.getTime()/60000;
	      } 
	      catch (ParseException e) {
	         e.printStackTrace();
	         return 0;
	      }   
	   
	}

	public static long getDayEndValue(int day) {
		String string = "1"+( 2 + Math.abs(day))+"/09-00:00:00.000";	  
	     
	      try {
	         Date date = df.parse(string);
	         return date.getTime()/60000;
	      } 
	      catch (ParseException e) {
	         e.printStackTrace();
	         return 0;
	      } 
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
