package ch.cern.atlas.apvs.client.ui;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.client.ClientFactory;
import ch.cern.atlas.apvs.client.event.PtuSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.event.SelectPtuEvent;
import ch.cern.atlas.apvs.client.settings.PtuSettings;
import ch.cern.atlas.apvs.client.widget.ClickableHtmlColumn;
import ch.cern.atlas.apvs.client.widget.DurationCell;
import ch.cern.atlas.apvs.client.widget.EditableCell;
import ch.cern.atlas.apvs.client.widget.GlassPanel;
import ch.cern.atlas.apvs.client.widget.UpdateScheduler;
import ch.cern.atlas.apvs.domain.ClientConstants;
import ch.cern.atlas.apvs.domain.Device;
import ch.cern.atlas.apvs.domain.Intervention;
import ch.cern.atlas.apvs.domain.InterventionMap;
import ch.cern.atlas.apvs.domain.Ternary;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent.ConnectionType;
import ch.cern.atlas.apvs.event.InterventionMapChangedRemoteEvent;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.ListDataProvider;
import com.google.web.bindery.event.shared.EventBus;

public class GeneralInfoView extends GlassPanel implements Module {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ListDataProvider<String> dataProvider = new ListDataProvider<String>();
	private CellTable<String> table = new CellTable<String>();

	private EventBus cmdBus;

	private Device ptu;
	private Ternary serverOk = Ternary.Unknown;
	private String serverCause = "";
	private Ternary audioOk = Ternary.Unknown;
	private String audioCause = "";
	private Ternary videoOk = Ternary.Unknown;
	private String videoCause = "";
	private Ternary daqOk = Ternary.Unknown;
	private String daqCause = "";
	private Ternary dosimeterOk = Ternary.Unknown;
	private String dosimeterCause = "";
	private Ternary databaseConnectOk = Ternary.Unknown;
	private String databaseConnectCause = "";
	private Ternary databaseUpdateOk = Ternary.Unknown;
	private String databaseUpdateCause = "";

	private InterventionMap interventions;
	private PtuSettings ptuSettings;

	private boolean showHeader = true;

	private String options;

	private List<String> names = Arrays
			.asList(new String[] {
					ConnectionType.server.getString(),
					ConnectionType.audio.getString(),
					ConnectionType.video.getString(),
					ConnectionType.daq.getString(),
					// ConnectionType.dosimeter.getString(),
					ConnectionType.databaseConnect.getString(),
					ConnectionType.databaseUpdate.getString() });
	private List<Class<?>> classes = Arrays
			.asList(new Class<?>[] { TextCell.class,
					TextCell.class, TextCell.class,
					TextCell.class, // TextCell.class,
					TextCell.class, TextCell.class, DateCell.class,
					DurationCell.class, TextCell.class, TextCell.class });

	private UpdateScheduler scheduler = new UpdateScheduler(this);

	public GeneralInfoView() {
	}

	@Override
	public boolean configure(Element element, ClientFactory clientFactory,
			Arguments args) {

		cmdBus = clientFactory.getEventBus(args.getArg(0));
		options = args.getArg(1);

		showHeader = !options.contains("NoHeader");

		table.setWidth("100%");

		add(table);

		// name column
		ClickableHtmlColumn<String> name = new ClickableHtmlColumn<String>() {
			@Override
			public String getValue(String name) {
				return name;
			}
		};
		name.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		table.addColumn(name, showHeader ? new TextHeader("Name") : null);

		EditableCell cell = new EditableCell(classes, 50);
		cell.setDateFormat(ClientConstants.dateFormatNoSeconds);
		cell.setEnabled(false);
		Column<String, Object> column = new Column<String, Object>(cell) {
			public String getCause(String name) {
				if (name.equals(ConnectionType.server.getString())) {
					return serverCause;
				} else if (name.equals(ConnectionType.audio.getString())) {
					return audioCause;
				} else if (name.equals(ConnectionType.video.getString())) {
					return videoCause;
				} else if (name.equals(ConnectionType.daq.getString())) {
					return daqCause;
				} else if (name.equals(ConnectionType.dosimeter.getString())) {
					return dosimeterCause;
				} else if (name.equals(ConnectionType.databaseConnect
						.getString())) {
					return databaseConnectCause;
				} else if (name.equals(ConnectionType.databaseUpdate
						.getString())) {
					return databaseUpdateCause;
				} else if (name.equals("Start Time")) {
					return "";
				} else if (name.equals("Duration")) {
					return "";
				} else if (name.equals("Dosimeter")) {
					return "";
				} else if (name.equals("Wireless")) {
					return "";
				}
				log.warn("GeneralInfoView name unknown '" + name
						+ "'");
				return "";

			}

			@Override
			public Object getValue(String name) {
				if (name.equals(ConnectionType.server.getString())) {
					return serverOk;
				} else if (name.equals(ConnectionType.audio.getString())) {
					return audioOk;
				} else if (name.equals(ConnectionType.video.getString())) {
					return videoOk;
				} else if (name.equals(ConnectionType.daq.getString())) {
					return daqOk;
				} else if (name.equals(ConnectionType.dosimeter.getString())) {
					return dosimeterOk;
				} else if (name.equals(ConnectionType.databaseConnect
						.getString())) {
					return databaseConnectOk;
				} else if (name.equals(ConnectionType.databaseUpdate
						.getString())) {
					return databaseUpdateOk;
				} else if (name.equals("Start Time")) {
					return getStartTime();
				} else if (name.equals("Duration")) {
					Date startTime = getStartTime();
					return startTime != null ? new Date().getTime()
							- startTime.getTime() : null;
				} else if (name.equals("Dosimeter")) {
					return getDosimeterSerialNumber();
				} else if (name.equals("Wireless")) {
					return getBSSID();
				}
				log.warn("GeneralInfoView name unknown '" + name
						+ "'");
				return null;
			}

			@Override
			public void render(Context context, String name, SafeHtmlBuilder sb) {
				Object s = getValue(name);
				String c = getCause(name);
				if (s instanceof Ternary) {
					Ternary t = (Ternary) s;
					s = t.isTrue() ? "Ok" : t.isFalse() ? "Fail" : "Unknown";
					sb.append(SafeHtmlUtils.fromSafeConstant("<div title=\""
							+ c + "\" class=\"" + s.toString().toLowerCase()
							+ "\">"));
				}
				getCell().render(context, s, sb);
				if (s instanceof Ternary) {
					sb.append(SafeHtmlUtils.fromSafeConstant("</div>"));
				}
			}
		};
		column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		table.addColumn(column, showHeader ? new TextHeader("Value") : null);

		dataProvider.addDataDisplay(table);
		dataProvider.setList(names);

		ConnectionStatusChangedRemoteEvent.subscribe(this,
				clientFactory.getRemoteEventBus(),
				new ConnectionStatusChangedRemoteEvent.Handler() {

					@Override
					public void onConnectionStatusChanged(
							ConnectionStatusChangedRemoteEvent event) {
						switch (event.getConnection()) {
						case server:
							serverOk = event.getStatus();
							serverCause = event.getCause();
							break;
						case audio:
							audioOk = event.getStatus();
							audioCause = event.getCause();
							break;
						case video:
							videoOk = event.getStatus();
							videoCause = event.getCause();
							break;
						case daq:
							daqOk = event.getStatus();
							daqCause = event.getCause();
							break;
						case dosimeter:
							dosimeterOk = event.getStatus();
							dosimeterCause = event.getCause();
							break;
						case databaseConnect:
							databaseConnectOk = event.getStatus();
							databaseConnectCause = event.getCause();
							break;
						case databaseUpdate:
							databaseUpdateOk = event.getStatus();
							databaseUpdateCause = event.getCause();
							break;
						default:
							return;
						}
						scheduler.update();
					}
				});

		InterventionMapChangedRemoteEvent.subscribe(this,
				clientFactory.getRemoteEventBus(),
				new InterventionMapChangedRemoteEvent.Handler() {

					@Override
					public void onInterventionMapChanged(
							InterventionMapChangedRemoteEvent event) {
						interventions = event.getInterventionMap();
						scheduler.update();
					}
				});

		PtuSettingsChangedRemoteEvent.subscribe(this, clientFactory.getRemoteEventBus(), new PtuSettingsChangedRemoteEvent.Handler() {

			@Override
			public void onPtuSettingsChanged(PtuSettingsChangedRemoteEvent event) {
				ptuSettings = event.getPtuSettings();
				scheduler.update();
			}
		});

		if (cmdBus != null) {
			SelectPtuEvent.subscribe(this, cmdBus, new SelectPtuEvent.Handler() {

				@Override
				public void onPtuSelected(SelectPtuEvent event) {
					ptu = event.getPtu();
					scheduler.update();
				}
			});
		}

		return true;
	}

	private Date getStartTime() {
		if (ptu == null) {
			return null;
		}

		if (interventions == null) {
			return null;
		}

		Intervention intervention = interventions.get(ptu);
		if (intervention == null) {
			return null;
		}

		return intervention.getStartTime();
	}

	private String getDosimeterSerialNumber() {
		if (ptu == null) {
			return null;
		}

		if (ptuSettings == null) {
			return null;
		}

		return ptuSettings.getDosimeterSerialNumber(ptu.getName());
	}

	private String getBSSID() {
		if (ptu == null) {
			return null;
		}

		if (ptuSettings == null) {
			return null;
		}

		return ptuSettings.getBSSID(ptu.getName());
	}

	@Override
	public boolean update() {
		table.redraw();
		return false;
	}

}
