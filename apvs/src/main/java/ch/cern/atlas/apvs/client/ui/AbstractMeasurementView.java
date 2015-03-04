package ch.cern.atlas.apvs.client.ui;

import java.util.Date;
import java.util.List;

import ch.cern.atlas.apvs.client.ClientFactory;
import ch.cern.atlas.apvs.client.event.HistoryChangedEvent;
import ch.cern.atlas.apvs.client.event.SelectPtuEvent;
import ch.cern.atlas.apvs.client.widget.GlassPanel;
import ch.cern.atlas.apvs.client.widget.UpdateScheduler;
import ch.cern.atlas.apvs.domain.ClientConstants;
import ch.cern.atlas.apvs.domain.Device;
import ch.cern.atlas.apvs.domain.History;
import ch.cern.atlas.apvs.domain.InterventionMap;
import ch.cern.atlas.apvs.domain.Measurement;
import ch.cern.atlas.apvs.domain.Ternary;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent;
import ch.cern.atlas.apvs.event.InterventionMapChangedRemoteEvent;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;
import ch.cern.atlas.apvs.ptu.shared.MeasurementChangedEvent;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

public abstract class AbstractMeasurementView extends GlassPanel implements
		Module {

	protected static NumberFormat format = NumberFormat.getFormat("0.00");

	protected History history;
	protected InterventionMap interventions;
	protected ListDataProvider<String> dataProvider = new ListDataProvider<String>();
	protected SingleSelectionModel<String> selectionModel;

	protected Device ptu = null;

	protected boolean showHeader = true;
	protected boolean showName = true;
	protected boolean selectable = true;
	protected boolean showDate = false;

	protected String options;

	private List<String> show = null;

	private RemoteEventBus remoteEventBus;
	private EventBus cmdBus;

	private HandlerRegistration measurementHandler;

	private UpdateScheduler scheduler = new UpdateScheduler(this);

	private Ternary daqOk = Ternary.Unknown;
	private Ternary databaseConnect = Ternary.Unknown;

	public AbstractMeasurementView() {
	}

	@Override
	public boolean configure(Element element, ClientFactory clientFactory,
			Arguments args) {

		remoteEventBus = clientFactory.getRemoteEventBus();
		cmdBus = clientFactory.getEventBus(args.getArg(0));
		options = args.getArg(1);
		show = args.getArgs(2);

		showHeader = !options.contains("NoHeader");
		showName = !options.contains("NoName");

		selectable = !options.contains("NoSelection");
		showDate = options.contains("Date");

		if (selectable) {
			selectionModel = new SingleSelectionModel<String>();
		}

		ConnectionStatusChangedRemoteEvent.subscribe(this, remoteEventBus,
				new ConnectionStatusChangedRemoteEvent.Handler() {

					@Override
					public void onConnectionStatusChanged(
							ConnectionStatusChangedRemoteEvent event) {
						switch (event.getConnection()) {
						case daq:
							daqOk = event.getStatus();
							break;
						case databaseConnect:
							databaseConnect = event.getStatus();
							break;
						default:
							break;
						}

						showGlass(daqOk.not().or(databaseConnect.not()).isTrue());
					}
				});

		InterventionMapChangedRemoteEvent.subscribe(this, remoteEventBus,
				new InterventionMapChangedRemoteEvent.Handler() {

					@Override
					public void onInterventionMapChanged(
							InterventionMapChangedRemoteEvent event) {
						interventions = event.getInterventionMap();
						changePtuId();
						scheduler.update();
					}
				});

		HistoryChangedEvent.subscribe(this, clientFactory,
				new HistoryChangedEvent.Handler() {

					@Override
					public void onHistoryChanged(HistoryChangedEvent event) {
						history = event.getHistory();
						changePtuId();
						scheduler.update();
					}
				});

		SelectPtuEvent.subscribe(this, cmdBus, new SelectPtuEvent.Handler() {

			@Override
			public void onPtuSelected(final SelectPtuEvent event) {
				ptu = event.getPtu();

				changePtuId();
				scheduler.update();
			}
		});

		return true;
	}

	/**
	 * Decorate with arrow up, down, left if a value went up, down or stayed the
	 * same. Only applies to last value. Also calls standard decorate method.
	 * 
	 * @param s
	 * @param current
	 * @param last
	 * @return
	 */
	public static SafeHtml decorate(String s, Measurement current,
			Measurement last) {
		if ((current != null) && (last != null) && (current.getDevice().getName() != null)
				&& (current.getDevice().getName().equals(last.getDevice().getName()))
				&& (current.getSensor() != null)
				&& current.getSensor().equals(last.getSensor())) {
			double c = current.getValue().doubleValue();
			double l = last.getValue().doubleValue();
			String a = (c == l) ? "&larr;" : (c > l) ? "&uarr;" : "&darr;";
			s = a + "&nbsp;<b>" + s + "</b>";
		}
		return decorate(s, current);
	}

	/**
	 * Adds date/time as tooltip. Shows future values (beyond 1 minute) in bold,
	 * values older than 5 minutes in italics and makes values older than a day
	 * more transparent.
	 * 
	 * @param s
	 * @param current
	 * @return
	 */
	public static SafeHtml decorate(String s, Measurement current) {
		long now = new Date().getTime();
		long future1min = now + (60 * 1000);
		long past5mins = now - (5 * 60 * 1000);
		long pastDay = now - (24 * 3600 * 1000);
		long time = current.getTime().getTime();
		if (time > future1min) {
			s = "<b>" + s + "</b>";
		} else if (time < past5mins) {
			s = "<i>" + s + "</i>";
		}

		// make text more transparent
		if (time < pastDay) {
			s = "<span style=\"opacity: 0.5;\">" + s + "</span>";
		}
		// Add date in tooltip
		s = "<div title=\""
				+ ClientConstants.dateFormat.format(current.getTime())
				+ "\">" + s + "</div>";
		return SafeHtmlUtils.fromSafeConstant(s);
	}

	private void changePtuId() {
		dataProvider.getList().clear();
		if (measurementHandler != null) {
			measurementHandler.removeHandler();
			measurementHandler = null;
		}

		if (interventions == null) {
			return;
		}

		if (history == null) {
			return;
		}

		for (Device ptu : interventions.getPtus()) {
			for (Measurement measurement : history.getMeasurements(ptu)) {
				replace(measurement);
			}
		}
		scheduler.update();

		measurementHandler = MeasurementChangedEvent.register(remoteEventBus,
				new MeasurementChangedEvent.Handler() {

					@Override
					public void onMeasurementChanged(
							MeasurementChangedEvent event) {
						Measurement measurement = event.getMeasurement();
						if (measurement.getDevice().equals(ptu)) {
							replace(measurement);
							scheduler.update();
						}
					}
				});

	}

	@Override
	public boolean update() {
		if (selectable) {
			String selection = selectionModel.getSelectedObject();

			if ((selection == null) && (dataProvider.getList().size() > 0)) {
				selection = dataProvider.getList().get(0);

				selectMeasurement(selection);
			}
		}

		return false;
	}

	private Measurement replace(Measurement measurement) {
		if (measurement == null) {
			return null;
		}
		
		// FIXME 611, remove when above works
		String sensor = measurement.getSensor();
		if (sensor == null) {
			return null;
		}
		if (ptu == null) {
			return null;
		}
		if (sensor.equalsIgnoreCase("BodyTemperature")) {
			return null;
		}
		if ((ptu.getName().equalsIgnoreCase("PTU-01") || (ptu.getName().equalsIgnoreCase("PTU-02"))) && sensor.equalsIgnoreCase("BarometricPressure")) {
			return null;
		}	
		if (sensor.equalsIgnoreCase("CO2")) {
			return null;
		}	

		List<String> list = dataProvider.getList();
		Measurement lastValue = history.getLastMeasurement(
				measurement.getDevice(), measurement.getSensor());
	
		if (!list.contains(measurement.getSensor())) {
			if ((show == null) || (show.size() == 0)
					|| (show.contains(measurement.getSensor()))) {
				list.add(measurement.getSensor());
				lastValue = measurement;
			} else {
				lastValue = null;
			}
		}

		return lastValue;
	}

	protected void selectMeasurement(String name) {
		SelectMeasurementEvent.fire(cmdBus, name);
	}
}
