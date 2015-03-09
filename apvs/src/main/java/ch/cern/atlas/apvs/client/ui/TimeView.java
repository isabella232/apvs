package ch.cern.atlas.apvs.client.ui;

import ch.cern.atlas.apvs.client.ClientFactory;
import ch.cern.atlas.apvs.client.event.HistoryChangedEvent;
import ch.cern.atlas.apvs.client.event.PtuSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.event.SelectPtuEvent;
import ch.cern.atlas.apvs.client.settings.PtuSettings;
import ch.cern.atlas.apvs.client.widget.UpdateScheduler;
import ch.cern.atlas.apvs.domain.Device;
import ch.cern.atlas.apvs.domain.History;
import ch.cern.atlas.apvs.domain.InterventionMap;
import ch.cern.atlas.apvs.domain.Measurement;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent;
import ch.cern.atlas.apvs.event.InterventionMapChangedRemoteEvent;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;
import ch.cern.atlas.apvs.eventbus.shared.RequestEvent;

import com.github.highcharts4gwt.model.highcharts.option.api.ChartOptions;
import com.google.gwt.dom.client.Element;
import com.google.web.bindery.event.shared.EventBus;

public class TimeView extends SpecificTimeView implements Module {

//	private Logger log = LoggerFactory.getLogger(getClass().getName());

	private Device device = null;
	private PtuSettings settings;
	private InterventionMap interventions;
	private History history;

	private String measurementName = null;
	private EventBus cmdBus;
	private String options;

	private ClientFactory factory;

	private UpdateScheduler scheduler = new UpdateScheduler(this);

	public TimeView() {
	}

	@Override
	public boolean configure(Element element, ClientFactory clientFactory,
			Arguments args) {
		
		this.factory = clientFactory;

		RemoteEventBus eventBus = clientFactory.getRemoteEventBus();

		height = args.getArgInt(0);
		cmdBus = clientFactory.getEventBus(args.getArg(1));
		options = args.getArg(2);
		measurementName = args.getArg(3);

		this.title = !options.contains("NoTitle");
		this.export = !options.contains("NoExport");

		ConnectionStatusChangedRemoteEvent.subscribe(this,
				clientFactory.getRemoteEventBus(),
				new ConnectionStatusChangedRemoteEvent.Handler() {

					@Override
					public void onConnectionStatusChanged(
							ConnectionStatusChangedRemoteEvent event) {
						switch (event.getConnection()) {
						case databaseConnect:
							showGlass(!event.getStatus().isTrue());
							break;
						default:
							break;
						}
					}
				});

		PtuSettingsChangedRemoteEvent.subscribe(this, eventBus,
				new PtuSettingsChangedRemoteEvent.Handler() {

					@Override
					public void onPtuSettingsChanged(
							PtuSettingsChangedRemoteEvent event) {
						settings = event.getPtuSettings();
						scheduler.update();
					}
				});

		InterventionMapChangedRemoteEvent.subscribe(this, eventBus,
				new InterventionMapChangedRemoteEvent.Handler() {

					@Override
					public void onInterventionMapChanged(
							InterventionMapChangedRemoteEvent event) {
						interventions = event.getInterventionMap();
						scheduler.update();
					}
				});

		HistoryChangedEvent.subscribe(this, clientFactory,
				new HistoryChangedEvent.Handler() {

					@Override
					public void onHistoryChanged(HistoryChangedEvent event) {
						history= event.getHistory();
						scheduler.update();
					}
				});

		if (cmdBus != null) {
			SelectPtuEvent.subscribe(this, cmdBus, new SelectPtuEvent.Handler() {

				@Override
				public void onPtuSelected(final SelectPtuEvent event) {
					device = event.getPtu();
					scheduler.update();
				}
			});

			SelectMeasurementEvent.subscribe(this, cmdBus,
					new SelectMeasurementEvent.Handler() {

						@Override
						public void onSelection(SelectMeasurementEvent event) {
							measurementName = event.getName();
							scheduler.update();
						}
					});

			RequestEvent.register(this, cmdBus, new RequestEvent.Handler() {

				@Override
				public void onRequestEvent(RequestEvent event) {
					if (event.getRequestedClassName().equals(
							ColorMapChangedEvent.class.getName())) {
						cmdBus.fireEvent(new ColorMapChangedEvent(getColors()));
					}
				}
			});
		}

		return true;
	}

	@Override
	public boolean update() {
		unregister();
		removeChart();

		if (measurementName.equals("")) {
			return false;
		}

		if (history == null) {
			return false;
		}

		if (interventions == null) {
			return false;
		}

		if (device == null) {
			createChart(Measurement.getDisplayName(measurementName));
			add(chart);

			for (Device device : interventions.getPtus()) {
				if ((settings == null) || settings.isEnabled(device.getName())) {

					if (chart != null) {
						addSeries(device, getName(device, interventions), false);
						addHistory(history.get(device, measurementName));
						chart.setAnimation(false);
					}
				}

			}

			register(factory, measurementName, null);

			if (cmdBus != null) {
				ColorMapChangedEvent.fire(cmdBus, getColors());
			}
		} else {
			if ((settings == null) || settings.isEnabled(device.getName())) {

				createSingleChart(factory, measurementName, device, history,
						interventions, true);

				cmdBus.fireEvent(new ColorMapChangedEvent(getColors()));
			}
		}

		return false;
	}

}
