package ch.cern.atlas.apvs.client.ui;

import java.util.List;

import org.gwtbootstrap3.client.ui.Column;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Row;
import org.gwtbootstrap3.client.ui.constants.ColumnSize;
import org.gwtbootstrap3.client.ui.html.Div;

import ch.cern.atlas.apvs.client.ClientFactory;
import ch.cern.atlas.apvs.client.event.HistoryChangedEvent;
import ch.cern.atlas.apvs.client.event.PtuSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.settings.PtuSettings;
import ch.cern.atlas.apvs.client.widget.UpdateScheduler;
import ch.cern.atlas.apvs.domain.Device;
import ch.cern.atlas.apvs.domain.History;
import ch.cern.atlas.apvs.domain.InterventionMap;
import ch.cern.atlas.apvs.event.InterventionMapChangedRemoteEvent;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;

import com.github.highcharts4gwt.client.view.widget.HighchartsLayoutPanel;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class CameraTable extends Div implements Module {

	private PtuSettings settings;
	private InterventionMap interventions;
	private History history;
	private List<Device> ptus;

	private UpdateScheduler scheduler = new UpdateScheduler(this);

	private ClientFactory factory;

	@Override
	public boolean configure(Element element, ClientFactory clientFactory,
			Arguments args) {

		this.factory = clientFactory;

		RemoteEventBus eventBus = clientFactory.getRemoteEventBus();

		InterventionMapChangedRemoteEvent.subscribe(this, eventBus,
				new InterventionMapChangedRemoteEvent.Handler() {

					@Override
					public void onInterventionMapChanged(
							InterventionMapChangedRemoteEvent event) {
						interventions = event.getInterventionMap();

						ptus = interventions.getPtus();

						configChanged();
						scheduler.update();
					}

				});

		PtuSettingsChangedRemoteEvent.subscribe(this, eventBus,
				new PtuSettingsChangedRemoteEvent.Handler() {

					@Override
					public void onPtuSettingsChanged(
							PtuSettingsChangedRemoteEvent event) {
						settings = event.getPtuSettings();
						configChanged();
						scheduler.update();
					}
				});

		HistoryChangedEvent.subscribe(this, clientFactory,
				new HistoryChangedEvent.Handler() {

					@Override
					public void onHistoryChanged(HistoryChangedEvent event) {
						history = event.getHistory();
						configChanged();
						scheduler.update();
					}
				});

		return true;
	}

	@Override
	public boolean update() {
		return false;
	}

	private Widget getCameraView(String title, Widget camera) {
		PanelHeader header = new PanelHeader();
		header.add(new HTML("<h3 class=\"panel-title\">" + title + "</h3>"));

		PanelBody body = new PanelBody();
		body.add(camera);

		Panel defaultPanel = new Panel();
		defaultPanel.add(header);
		defaultPanel.add(body);
		return defaultPanel;
	}

	private void configChanged() {
		clear();

		if ((ptus == null) || (settings == null) || (history == null)) {
			return;
		}

		Row row = new Row();
		add(row);

		int column = 0;
		for (Device ptu : ptus) {

			Widget helmet = new ImageView(factory, settings.getCameraUrl(
					ptu.getName(), CameraView.HELMET, factory.getProxy()));

			Column col = new Column(getColumnSize(ptus.size()));
			row.add(col);
			col.add(getCameraView(ptu.getName(), helmet));

			SpecificTimeView timeView = new SpecificTimeView();
			HighchartsLayoutPanel chart = timeView.createSingleChart(factory, "DoseRate", ptu,
					history, interventions, false);
			col.add(chart);
			column++;

			row = addRow(row, column, ptus.size());

			// Widget hand = new ImageView(factory, settings.getCameraUrl(ptu,
			// CameraView.HAND, factory.getProxy()));
		}
	}

	private Row addRow(Row row, int column, int count) {
		switch (count) {
		case 1:
		case 2:
			break;
		case 3:
		case 5:
		case 6:
			if ((column % 3) == 0) {
				row = new Row();
				add(row);
			}
			break;
		case 4:
		case 7:
		case 8:
		default:
			if ((column % 4) == 0) {
				row = new Row();
				add(row);
			}
			break;
		}
		return row;
	}
	
	private ColumnSize getColumnSize(int count) {
		switch (count) {
		case 1:
			return ColumnSize.MD_12;
		case 2:
			return ColumnSize.MD_6;
		case 3:
		case 5:
		case 6:
			return ColumnSize.MD_4;
		case 4:
		case 7:
		case 8:
		default:
			return ColumnSize.MD_3;
		}
	}
}
