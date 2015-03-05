package ch.cern.atlas.apvs.client.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.cern.atlas.apvs.client.ClientFactory;
import ch.cern.atlas.apvs.client.event.HistoryChangedEvent;
import ch.cern.atlas.apvs.client.event.PtuSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.event.SelectPtuEvent;
import ch.cern.atlas.apvs.client.settings.PtuSettings;
import ch.cern.atlas.apvs.client.widget.ClickableHtmlColumn;
import ch.cern.atlas.apvs.client.widget.ActiveClickableTextCell;
import ch.cern.atlas.apvs.client.widget.ClickableTextColumn;
import ch.cern.atlas.apvs.client.widget.GlassPanel;
import ch.cern.atlas.apvs.client.widget.UpdateScheduler;
import ch.cern.atlas.apvs.domain.Device;
import ch.cern.atlas.apvs.domain.History;
import ch.cern.atlas.apvs.domain.InterventionMap;
import ch.cern.atlas.apvs.domain.Measurement;
import ch.cern.atlas.apvs.domain.Ternary;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent;
import ch.cern.atlas.apvs.event.InterventionMapChangedRemoteEvent;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;
import ch.cern.atlas.apvs.ptu.shared.MeasurementChangedEvent;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.web.bindery.event.shared.EventBus;

public class PtuView extends GlassPanel implements Module {

//	private Logger log = LoggerFactory.getLogger(getClass().getName());

	private static NumberFormat format = NumberFormat.getFormat("0.00");

	private RemoteEventBus eventBus;
	private ListDataProvider<String> dataProvider = new ListDataProvider<String>();
	private CellTable<String> table = new CellTable<String>();

	private List<Device> ptus;
	private Measurement last = null;
	private Map<String, ClickableTextColumn<String>> columns;
	private SingleSelectionModel<String> selectionModel;
	private Map<Device, String> colorMap = new HashMap<Device, String>();

	private PtuSettings settings;
	private InterventionMap interventions;
	private EventBus cmdBus;

	private UpdateScheduler scheduler = new UpdateScheduler(this);

	private Ternary daqOk = Ternary.Unknown;
	private Ternary databaseConnect = Ternary.Unknown;

	private History history;

	private void init() {
		columns = new HashMap<String, ClickableTextColumn<String>>();
	}

	public PtuView() {
	}

	@Override
	public boolean configure(Element element, ClientFactory clientFactory,
			Arguments args) {

		eventBus = clientFactory.getRemoteEventBus();

		init();

		cmdBus = clientFactory.getEventBus(args.getArg(0));
		
		table.setWidth("100%");

		add(table);

		// name column
		ClickableHtmlColumn<String> name = new ClickableHtmlColumn<String>() {
			@Override
			public String getValue(String name) {
				return Measurement.getDisplayName(name);
			}
		};
		name.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		name.setSortable(true);
		name.setFieldUpdater(new FieldUpdater<String, String>() {

			@Override
			public void update(int index, String name, String value) {
				selectMeasurementAndPtu(name, null);
			}
		});
		table.addColumn(name, "Name");

		// unit column
		ClickableHtmlColumn<String> unit = new ClickableHtmlColumn<String>() {
			@Override
			public String getValue(String name) {
				return history != null ? history.getUnit(name) : "";
			}
		};
		unit.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		unit.setFieldUpdater(new FieldUpdater<String, String>() {

			@Override
			public void update(int index, String name, String value) {
				selectMeasurementAndPtu(name, null);
			}
		});
		table.addColumn(unit, "Unit");

		dataProvider.addDataDisplay(table);
		dataProvider.setList(new ArrayList<String>());

		ListHandler<String> columnSortHandler = new ListHandler<String>(
				dataProvider.getList());
		columnSortHandler.setComparator(name, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1 != null ? o1.compareTo(o2) : -1;
			}
		});
		table.addColumnSortHandler(columnSortHandler);
		table.getColumnSortList().push(name);

		selectionModel = new SingleSelectionModel<String>();
		table.setSelectionModel(selectionModel);

		ConnectionStatusChangedRemoteEvent.subscribe(this, 
				clientFactory.getRemoteEventBus(),
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

		MeasurementChangedEvent.register(eventBus,
				new MeasurementChangedEvent.Handler() {

					@Override
					public void onMeasurementChanged(
							MeasurementChangedEvent event) {
						Measurement measurement = event.getMeasurement();
						last = addOrReplaceMeasurement(measurement);
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

		HistoryChangedEvent.subscribe(this, clientFactory,
				new HistoryChangedEvent.Handler() {

					@Override
					public void onHistoryChanged(HistoryChangedEvent event) {
						history = event.getHistory();
						configChanged();
						scheduler.update();
					}
				});		

		if (cmdBus != null) {
			ColorMapChangedEvent.subscribe(this, cmdBus,
					new ColorMapChangedEvent.Handler() {
						@Override
						public void onColorMapChanged(ColorMapChangedEvent event) {
							colorMap = event.getColorMap();
							scheduler.update();
						}

					});
		}

		return true;
	}

	private Measurement addOrReplaceMeasurement(Measurement measurement) {
		
		Device ptu = measurement.getDevice();
		String sensor = measurement.getSensor();
		if ((ptus == null) || !ptus.contains(ptu))
			return null;

		Measurement lastValue = history != null ? history.getLastMeasurement(ptu, sensor) : measurement;

		addRow(sensor);

		return lastValue;
	}

	private void addRow(String name) {
		if (!dataProvider.getList().contains(name)) {
			dataProvider.getList().add(name);
		}
	}

	private void addColumn(final Device ptu) {
		ClickableTextColumn<String> column = new ClickableTextColumn<String>() {
			@Override
			public String getValue(String name) {
				Measurement m = history != null ? history.getCurrentMeasurement(ptu, name) : null;
				if (m == null) {
					return "";
				}
				return format.format(m.getValue());
			}

			@Override
			public void render(Context context, String name, SafeHtmlBuilder sb) {
				String color = colorMap.get(ptu);
				boolean isSelected = selectionModel.isSelected(name);
				if ((color != null) && isSelected) {
					sb.append(SafeHtmlUtils
							.fromSafeConstant("<div style=\"background:"
									+ color + "\">"));
				}

				Measurement m = history != null ? history.getCurrentMeasurement(ptu, name) : null;
				if (m != null) {
					((ActiveClickableTextCell) getCell()).render(context,
							MeasurementView.decorate(getValue(name), m, last),
							sb);
				}
				if ((color != null) && isSelected) {
					sb.append(SafeHtmlUtils.fromSafeConstant("</div>"));
				}
			}
		};
		column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		column.setFieldUpdater(new FieldUpdater<String, String>() {

			@Override
			public void update(int index, String name, String value) {
				selectMeasurementAndPtu(name, ptu);
			}
		});

		table.addColumn(column, new TextHeader("") {
			@Override
			public String getValue() {
				String name = interventions != null ? interventions.get(ptu) != null ? interventions
						.get(ptu).getName() : null
						: null;
				return name != null ? name + "<br/>(" + ptu.getName() + ")"
						: ptu.getName();
			}

			@Override
			public void render(Context context, SafeHtmlBuilder sb) {
				((TextCell) getCell()).render(context,
						SafeHtmlUtils.fromSafeConstant(getValue()), sb);
			}
		});

		columns.put(ptu.getName(), column);
	}

	private void configChanged() {

		if (ptus == null) {
			init();
			return;
		}
		
		// Remove any dead columns
		for (Iterator<Map.Entry<String, ClickableTextColumn<String>>> i = columns
				.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String, ClickableTextColumn<String>> entry = i.next();
			if (ptus.contains(entry.getKey()))
				continue;

			ClickableTextColumn<String> column = entry.getValue();
			table.removeColumn(column);
			i.remove();
		}

		// add any new columns...
		for (Iterator<Device> i = ptus.iterator(); i.hasNext();) {
			Device ptu = i.next();
			ClickableTextColumn<String> column = columns.get(ptu);

			if ((settings == null) || settings.isEnabled(ptu.getName())) {
				if (column == null) {
					addColumn(ptu);
				}
				
				if (history != null) {
					for (Measurement measurement: history.getMeasurements(ptu)) {
						// #515 null in case no value was every recorded
						if (measurement != null) {
							addRow(measurement.getSensor());
						}
					}
				}
				
			} else {
				// remove a column that was disabled
				if (column != null) {
					table.removeColumn(column);
				}
			}
		}
	}

	@Override
	public boolean update() {
		ColumnSortEvent.fire(table, table.getColumnSortList());
		table.redraw();

		String selection = selectionModel.getSelectedObject();
		if ((selection == null) && (dataProvider.getList().size() > 0)) {
			selection = dataProvider.getList().get(0);

			selectMeasurementAndPtu(selection, null);
		}

		// re-set the selection as the async update may have changed the
		// rendering
		if (selection != null) {
			selectionModel.setSelected(selection, true);
		}
		return false;
	}

	private void selectMeasurementAndPtu(String name, Device ptu) {
		SelectMeasurementEvent.fire(cmdBus, name);
		SelectPtuEvent.fire(cmdBus, ptu);
	}
}
