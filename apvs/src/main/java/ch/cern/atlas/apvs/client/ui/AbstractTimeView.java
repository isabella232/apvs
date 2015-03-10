package ch.cern.atlas.apvs.client.ui;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.cern.atlas.apvs.client.widget.GlassPanel;
import ch.cern.atlas.apvs.domain.ClientConstants;
import ch.cern.atlas.apvs.domain.Device;

import com.github.highcharts4gwt.client.view.widget.HighchartsLayoutPanel;
import com.github.highcharts4gwt.model.array.api.Array;
import com.github.highcharts4gwt.model.factory.api.HighchartsOptionFactory;
import com.github.highcharts4gwt.model.factory.jso.JsoHighchartsOptionFactory;
import com.github.highcharts4gwt.model.highcharts.option.api.ChartOptions;
import com.github.highcharts4gwt.model.highcharts.option.api.Series;
import com.github.highcharts4gwt.model.highcharts.option.api.series.Data;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;

public class AbstractTimeView extends GlassPanel {

	private static final int POINT_LIMIT = 2000; // 5 seconds basis, 12
													// pnts/min, 720/hour
	private static final String[] color = { "#AA4643", "#89A54E", "#80699B",
			"#3D96AE", "#DB843D", "#92A8CD", "#A47D7C", "#B5CA92", "#4572A7" };
	private Map<Device, Integer> pointsById;
	private Map<Device, Series> seriesById;
	private Map<Device, String> colorsById;
	private Map<Device, Series> limitSeriesById;

	protected HighchartsOptionFactory factory;
	protected HighchartsLayoutPanel chart;
	protected ChartOptions options;
	protected Integer height = null;
	protected boolean export = true;
	protected boolean title = true;

	public AbstractTimeView() {
		super();
		factory = new JsoHighchartsOptionFactory();
		
		pointsById = new HashMap<Device, Integer>();
		seriesById = new HashMap<Device, Series>();
		colorsById = new HashMap<Device, String>();
		limitSeriesById = new HashMap<Device, Series>();

		// Fix for #289
//		if (false) {
		Window.addResizeHandler(new ResizeHandler() {

			@Override
			public void onResize(ResizeEvent event) {
				if ((chart != null) && (chart.isVisible())) {
					// table, with 100% width, will be the same as old chart
					Widget parent = chart.getParent();
					if (parent != null) {
						// div, corrected width
						parent = parent.getParent();
					}
					if (parent != null) {
						int width = parent.getOffsetWidth();
						if (width > 0) {
//							chart.setSize(width, chart.getOffsetHeight(), false);
							chart.setWidth(width+"px");
						}
					}
				}
			}
		});
//		}
	}

	public Map<Device, String> getColors() {
		return colorsById;
	}

	protected void removeChart() {
		if (chart != null) {
			remove(chart);
			chart = null;
			pointsById.clear();
			seriesById.clear();
			colorsById.clear();
			limitSeriesById.clear();
		}
	}

	protected void createChart(String name) {
		removeChart();
		
		chart = new HighchartsLayoutPanel();
		options = factory.createChartOptions();
		
				// same as above
				// FIXME String.format not supported
		options.colors().push("#AA4643");
		options.colors().push("#89A54E");
		options.colors().push("#80699B");
		options.colors().push("#3D96AE");
		options.colors().push("#DB843D");
		options.colors().push("#92A8CD");
		options.colors().push("#A47D7C");
		options.colors().push("#B5CA92");
		options.colors().push("#4572A7");

		if (title) {
			options.title().text(name).setFieldAsJsonObject("fontSize", "12px");
		}
		
		options.plotOptions().bar().dataLabels().enabled(true);
		options.plotOptions().line().shadowAsBoolean(false).marker().enabled(false);
		options.exporting().enabled(export);
		options.legend().enabled(false);
		options.credits().enabled(false);

		options.chart().zoomType("x");
		options.chart().marginRight(10.0);
		options.chart().animationAsBoolean(false);
					
//				.sizeToMatchContainer()
//				.setWidth100()
//				.setHeight100()
			
//		options.setToolTip(
//						new ToolTip().setCrosshairs(true, true).setFormatter(
//								new ToolTipFormatter() {
//									@Override
//									public String format(ToolTipData toolTipData) {
//										return "<b>"
//												+ toolTipData.getSeriesName()
//												+ "</b><br/>"
//												+ getDateTime(toolTipData
//														.getXAsLong())
//												+ "<br/>"
//												+ NumberFormat
//														.getFormat("0.00")
//														.format(toolTipData
//																.getYAsDouble());
//									}
//								}));

		if (height != null) {
			chart.setHeight(height+"px");
		}

		options.xAxis().type("datetime");
		
//		.labels(). (
//		// Fix one hour offset in time labels...
//				new XAxisLabels().setFormatter(new AxisLabelsFormatter() {
//
//					@Override
//					public String format(AxisLabelsData axisLabelsData) {
//						return getDateTime(axisLabelsData.getValueAsLong());
//					}
//				}));
		options.xAxis().title().text("Time");
		options.yAxis().allowDecimals(true).title().text("");
	}

	protected void addSeries(Device device, String name, boolean showLimits) {
	
		Series series = factory.createSeries();
		series.name(name);
		series.type("line");
		options.plotOptions().series().animation(false);

		pointsById.put(device, 0);
		seriesById.put(device, series);
		colorsById.put(device, color[options.series().length()]);

		if (showLimits) {
			Series limitSeries = factory.createSeries();
			limitSeries.type("arearange");
			options.plotOptions().series()
					.animation(false)
					.color("#3482d4")
					.enableMouseTracking(false);
			limitSeriesById.put(device, limitSeries);

			options.series().addToEnd(limitSeries);
		}
		options.series().addToEnd(series);
		chart.renderChart(options);
	}

	protected void setData(Device device, Number[][] data, Number[][] limits) {
		Series series = seriesById.get(device);
	
		// FIXME, below may be very slow
//		series.setPoints(data != null ? data : new Number[0][2], false);		
		if (data != null) {
			Array<Data> d = series.dataAsArrayObject();
			for (int i=0; i<data.length; i++) {
				d.addToEnd(factory.createData().x(data[i][0].doubleValue()).y(data[i][1].doubleValue()));
			}
		}
		
		pointsById.put(device, data != null ? data.length : 0);

		Series limitSeries = limitSeriesById.get(device);
		if (limitSeries != null) {
//			limitSeries.setPoints(limits != null ? limits : new Number[0][3],
//					false);
			Array<Data> d = series.dataAsArrayObject();
			for (int i=0; i<data.length; i++) {
				// FIXME handle upThreshold
				d.addToEnd(factory.createData().x(data[i][0].doubleValue()).y(data[i][1].doubleValue()));				
			}
		}
	}
	
	protected void addPoint(Device device, long time, Number value,
			Number downThreshold, Number upThreshold) {
		Series series = seriesById.get(device);
		if (series == null) {
			return;
		}
		Integer numberOfPoints = pointsById.get(device);
		if (numberOfPoints == null) {
			numberOfPoints = 0;
		}
		boolean shift = numberOfPoints >= POINT_LIMIT;
		if (!shift) {
			pointsById.put(device, numberOfPoints + 1);
		}
		options.plotOptions().line().marker().enabled(!shift);

		Series limitSeries = limitSeriesById.get(device);
		if (limitSeries != null) {
//			limitSeries.addPoint(new Point(time, downThreshold, upThreshold), false,
//					shift, false);
			// FIXME missing upThreshold
			limitSeries.dataAsArrayObject().addToEnd(factory.createData().x(time).y(downThreshold.doubleValue()));
		}

//		Point p = new Point(time, value);
//		series.addPoint(p, true, shift, false);
		// FIXME, what is shift, true and false for
		series.dataAsArrayObject().addToEnd(factory.createData().x(time).y(value.doubleValue()));
		chart.renderChart(options);
	}

	private static final DateTimeFormat ddMMM = DateTimeFormat
			.getFormat("dd MMM");
	private static final DateTimeFormat ddMMMyyyy = DateTimeFormat
			.getFormat("dd MMM yyyy");

	private static final long DAY = 24 * 60 * 60 * 1000L;

	@SuppressWarnings("deprecation")
	private String getDateTime(long time) {
		Date today = new Date();
		long now = today.getTime();
		long nextMinute = now + (60 * 1000);
		long yesterday = now - DAY;
		long tomorrow = now + DAY;
		Date date = new Date(time);

		String prefix = "";
		String postfix = "";
		String newline = "<br/>";
		if (time > nextMinute) {
			prefix = "<b>";
			postfix = "</b>";
		} else if (time < yesterday) {
			prefix = "<i>";
			postfix = "</i>";
		}

		String dateTime = ClientConstants.timeFormat.format(date);
		if ((time < yesterday) || (time > tomorrow)) {
			if (date.getYear() == today.getYear()) {
				dateTime += newline + ddMMM.format(date);
			} else {
				dateTime += newline + ddMMMyyyy.format(date);
			}
		}

		return prefix + dateTime + postfix;
	}

	protected void setUnit(Device device, String unit) {
		Series series = seriesById.get(device);
		if (series == null) {
			return;
		}

		if (chart == null) {
			return;
		}

		// fix #96 to put unicode in place of &deg; and &micro;
		unit = unit.replaceAll("\\&deg\\;", "\u00B0");
		unit = unit.replaceAll("\\&micro\\;", "\u00B5");

		options.yAxis().title().text(unit);
	}

	public void redraw() {
		if (chart != null) {
			chart.renderChart(options);
		}
	}

}