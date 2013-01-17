package ch.cern.atlas.apvs.client.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.client.ClientFactory;
import ch.cern.atlas.apvs.client.domain.InterventionMap;
import ch.cern.atlas.apvs.client.event.AsteriskStatusRemoteEvent;
import ch.cern.atlas.apvs.client.event.AudioSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.event.InterventionMapChangedRemoteEvent;
import ch.cern.atlas.apvs.client.event.PtuSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.settings.AudioSettings;
import ch.cern.atlas.apvs.client.settings.PtuSettings;
import ch.cern.atlas.apvs.client.widget.DynamicSelectionCell;
import ch.cern.atlas.apvs.client.widget.GlassPanel;
import ch.cern.atlas.apvs.client.widget.StringList;
import ch.cern.atlas.apvs.client.widget.TextInputSizeCell;
import ch.cern.atlas.apvs.client.widget.UpdateScheduler;
import ch.cern.atlas.apvs.domain.Order;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.ActionCell.Delegate;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.ListDataProvider;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Shows a list of PTU settings which are alive. A list of ever alive PTU
 * settings is persisted.
 * 
 * @author duns
 * 
 */
public class PtuSettingsView extends GlassPanel implements Module {

	private Logger log = LoggerFactory.getLogger(getClass().getName());

	private ListDataProvider<String> dataProvider = new ListDataProvider<String>();
	private CellTable<String> table = new CellTable<String>();
	private ListHandler<String> columnSortHandler;

	protected PtuSettings settings = new PtuSettings();
	protected InterventionMap interventions = new InterventionMap();
	protected List<String> dosimeterSerialNumbers = new ArrayList<String>();

	private UpdateScheduler scheduler = new UpdateScheduler(this);
	
	/*
	 * Addition Variables declaration
	 */
	private List<String> usersList = new ArrayList<String>();
	private AudioSettings voipAccounts =  new AudioSettings();
	/*
	 * End of addition
	 */
	
	public PtuSettingsView() {
	}

	@Override
	public boolean configure(Element element, final ClientFactory clientFactory,
			Arguments args) {

		final RemoteEventBus eventBus = clientFactory.getRemoteEventBus();

		/*
		 * Addition of Asterisk account listing event
		 */
		//Call RPC Method to List SIP Accounts
		clientFactory.getAudioService().usersList(new AsyncCallback<Void>() {
			
			@Override
			public void onSuccess(Void result) {
				System.err.println("Asterisk SIP users listed...");
			}
			
			@Override
			public void onFailure(Throwable caught) {
				System.err.println("Fail to list Asterisk SIP users " + caught);				
			}
		});
		/*
		 * End of addition
		 */
		
		add(table, CENTER);

		// ENABLED
		Column<String, Boolean> enabled = new Column<String, Boolean>(
				new CheckboxCell()) {
			@Override
			public Boolean getValue(String object) {
				return settings.isEnabled(object);
			}
		};
		enabled.setFieldUpdater(new FieldUpdater<String, Boolean>() {

			@Override
			public void update(int index, String object, Boolean value) {
				settings.setEnabled(object, value);
				fireSettingsChangedEvent(eventBus, settings);
			}
		});
		enabled.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		enabled.setSortable(true);
		table.addColumn(enabled, "Enabled");

		// PTU ID
		Column<String, String> ptuId = new Column<String, String>(
				new TextCell()) {
			@Override
			public String getValue(String object) {
				return object;
			}
		};
		ptuId.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		ptuId.setSortable(true);
		table.addColumn(ptuId, "PTU ID");

		// NAME
		Column<String, String> name = new Column<String, String>(new TextCell()) {
			@Override
			public String getValue(String object) {
				return interventions.get(object) != null ? interventions.get(
						object).getName() : "";
			}
		};
		name.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		name.setSortable(true);
		table.addColumn(name, "Name");

		// DOSIMETER
		Column<String, String> dosimeter;
		boolean useCompositeCell = false;

		final Delegate<String> setDosimeterSerialId = new Delegate<String>() {
			@Override
			public void execute(String object) {
				System.out.println("Action program "
						+ settings.getDosimeterSerialNumber(object) + " into "
						+ object);
				final Order order = new Order(object, "DosimeterID",
						settings.getDosimeterSerialNumber(object));
				clientFactory.getPtuService().handleOrder(order,
						new AsyncCallback<Void>() {

							@Override
							public void onSuccess(Void result) {
								log.info("Order " + order + " set");
							}

							@Override
							public void onFailure(Throwable caught) {
								log.warn("Order " + order + " failed " + caught);
							}
						});
			}
		};

		if (useCompositeCell) {
			// FIXME #275 could be done with cells, but could not get button and
			// inout next to eachother, something with
			// block level and inline elements AND table cells
			List<HasCell<String, ?>> cells = new ArrayList<HasCell<String, ?>>();
			cells.add(new HasCell<String, String>() {
				Cell<String> action = new ActionCell<String>("Set to PTU",
						setDosimeterSerialId);

				@Override
				public Cell<String> getCell() {
					return action;
				}

				@Override
				public FieldUpdater<String, String> getFieldUpdater() {
					return null;
				}

				@Override
				public String getValue(String object) {
					return object;
				}

			});
			cells.add(new HasCell<String, String>() {
				Cell<String> cell = new TextInputSizeCell(10);

				@Override
				public Cell<String> getCell() {
					return cell;
				}

				@Override
				public FieldUpdater<String, String> getFieldUpdater() {
					return new FieldUpdater<String, String>() {
						@Override
						public void update(int index, String object,
								String value) {
							settings.setDosimeterSerialNumber(object, value);
							fireSettingsChangedEvent(eventBus, settings);
						}
					};
				}

				@Override
				public String getValue(String object) {
					return settings.getDosimeterSerialNumber(object);
				}
			});
			dosimeter = new Column<String, String>(new CompositeCell<String>(
					cells)) {

				@Override
				public String getValue(String object) {
					return object;
				}
			};
		} else {
			dosimeter = new Column<String, String>(new TextInputSizeCell(15)) {
				@Override
				public String getValue(String object) {
					return settings.getDosimeterSerialNumber(object);
				}
			};
			dosimeter.setFieldUpdater(new FieldUpdater<String, String>() {

				@Override
				public void update(int index, String object, String value) {
					settings.setDosimeterSerialNumber(object, value);
					fireSettingsChangedEvent(eventBus, settings);
				}
			});
		}
		dosimeter.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		dosimeter.setSortable(useCompositeCell);
		table.addColumn(dosimeter, "Dosimeter #");

		if (!useCompositeCell) {
			Column<String, String> dosimeterSet = new Column<String, String>(
					new ActionCell<String>("Set to PTU", setDosimeterSerialId)) {

				@Override
				public String getValue(String object) {
					return object;
				}
			};
			dosimeterSet.setSortable(false);
			dosimeterSet
					.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
			table.addColumn(dosimeterSet, "Dosimeter #");

		}

		// HELMET URL
		Column<String, String> helmetUrl = new Column<String, String>(
				new TextInputSizeCell(50)) {
			@Override
			public String getValue(String object) {
				return settings.getCameraUrl(object, CameraView.HELMET);
			}
		};
		helmetUrl.setFieldUpdater(new FieldUpdater<String, String>() {

			@Override
			public void update(int index, String object, String value) {
				settings.setCameraUrl(object, CameraView.HELMET, value);
				fireSettingsChangedEvent(eventBus, settings);
			}
		});
		helmetUrl.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		helmetUrl.setSortable(true);
		table.addColumn(helmetUrl, "Helmet Camera URL");

		// HAND URL
		Column<String, String> handUrl = new Column<String, String>(
				new TextInputSizeCell(50)) {
			@Override
			public String getValue(String object) {
				return settings.getCameraUrl(object, CameraView.HAND);
			}
		};
		handUrl.setFieldUpdater(new FieldUpdater<String, String>() {

			@Override
			public void update(int index, String object, String value) {
				settings.setCameraUrl(object, CameraView.HAND, value);
				fireSettingsChangedEvent(eventBus, settings);
			}
		});
		handUrl.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		handUrl.setSortable(true);
		table.addColumn(handUrl, "Hand Camera URL");

		dataProvider.addDataDisplay(table);
		dataProvider.setList(new ArrayList<String>());
		
		/*
		 * Addition on SIP Account
		 */
		//SIP Number 
		Column<String, String> number = new Column<String, String>(new DynamicSelectionCell(new StringList<String>(usersList))){
			
			@Override
			public String getValue(String object) {
				return voipAccounts.getNumber(object);
			}
					
		};
		number.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		table.addColumn(number, "SIP Account");
				
		number.setFieldUpdater(new FieldUpdater<String, String>() {
			
			@Override
			public void update(int index, String object, String value) {
				voipAccounts.setNumber(object, value);
				eventBus.fireEvent(new AudioSettingsChangedRemoteEvent(voipAccounts));
			}
		});
		/*
		 * End of addition
		 */
		
		/*
		 * Addition on Activity
		 */
		//Impact Activity Name
		Column<String, String> activity = new Column<String, String>(new TextInputSizeCell(30)) {
			
			@Override
			public String getValue(String object) {
				return voipAccounts.getActivity(object);
			}
		};
		activity.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		table.addColumn(activity, "Impact Activity Name");
		
		activity.setFieldUpdater(new FieldUpdater<String, String>() {
			
			@Override
			public void update(int index, String object, String value) {
				voipAccounts.setActivity(object, value);
				eventBus.fireEvent(new AudioSettingsChangedRemoteEvent(voipAccounts));
			}
		});
		/*
		 * End of addition
		 */
		
		// SORTING
		columnSortHandler = new ListHandler<String>(dataProvider.getList());
		columnSortHandler.setComparator(ptuId, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1 != null ? o1.compareTo(o2) : -1;
			}
		});
		columnSortHandler.setComparator(enabled, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return settings.isEnabled(o1).compareTo(settings.isEnabled(o2));
			}
		});
		columnSortHandler.setComparator(name, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return interventions.get(o1).getName()
						.compareTo(interventions.get(o2).getName());
			}
		});
		columnSortHandler.setComparator(dosimeter, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return settings.getDosimeterSerialNumber(o1).compareTo(
						settings.getDosimeterSerialNumber(o2));
			}
		});
		columnSortHandler.setComparator(helmetUrl, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return settings.getCameraUrl(o1, CameraView.HELMET).compareTo(
						settings.getCameraUrl(o2, CameraView.HELMET));
			}
		});
		columnSortHandler.setComparator(handUrl, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return settings.getCameraUrl(o1, CameraView.HAND).compareTo(
						settings.getCameraUrl(o2, CameraView.HAND));
			}
		});
		table.addColumnSortHandler(columnSortHandler);
		table.getColumnSortList().push(ptuId);

		PtuSettingsChangedRemoteEvent.subscribe(eventBus,
				new PtuSettingsChangedRemoteEvent.Handler() {
					@Override
					public void onPtuSettingsChanged(
							PtuSettingsChangedRemoteEvent event) {
						log.info("PTU Settings changed");
						settings = event.getPtuSettings();
						dataProvider.getList().clear();
						dataProvider.getList().addAll(settings.getPtuIds());

						scheduler.update();
					}
				});

		InterventionMapChangedRemoteEvent.subscribe(eventBus,
				new InterventionMapChangedRemoteEvent.Handler() {

					@Override
					public void onInterventionMapChanged(
							InterventionMapChangedRemoteEvent event) {
						interventions = event.getInterventionMap();
						scheduler.update();
					}
				});
		
		/*
		 * Addition of Audio settings change
		 */
		AudioSettingsChangedRemoteEvent.subscribe(eventBus,new AudioSettingsChangedRemoteEvent.Handler() {
			
			@Override
			public void onAudioSettingsChanged(AudioSettingsChangedRemoteEvent event) {
				System.out.println("Audio Settings changed");
				voipAccounts = event.getAudioSettings();

				dataProvider.getList().clear();
				dataProvider.getList().addAll(voipAccounts.getPtuIds());
			}
		}); 		
		
		/*
		 * End of addition
		 */
		
		/*
		 * Addition of Asterisk account listing event
		 */
		AsteriskStatusRemoteEvent.register(eventBus, new AsteriskStatusRemoteEvent.Handler() {
			
			@Override
			public void onAsteriskStatusChange(AsteriskStatusRemoteEvent event) {
				System.out.println("Asterisk Event");
				usersList.clear();
				usersList.addAll(event.getAsteriskUsersList());
			}
		});	
		/*
		 * End of addition
		 */
		

//		DosimeterSerialNumbersChangedEvent.subscribe(eventBus,
//				new DosimeterSerialNumbersChangedEvent.Handler() {
//
//					@Override
//					public void onDosimeterSerialNumbersChanged(
//							DosimeterSerialNumbersChangedEvent event) {
//						dosimeterSerialNumbers.clear();
//						dosimeterSerialNumbers.addAll(event
//								.getDosimeterSerialNumbers());
//						log.info("DOSI changed "
//								+ dosimeterSerialNumbers.size());
//
//						// FIXME, allow for setting not available as DOSI #
//						scheduler.update();
//					}
//				});

		scheduler.update();

		return true;
	}

	@Override
	public boolean update() {
		// Resort the table
		ColumnSortEvent.fire(table, table.getColumnSortList());

		table.redraw();

		return false;
	}

	private void fireSettingsChangedEvent(EventBus eventBus,
			PtuSettings settings) {

		((RemoteEventBus) eventBus).fireEvent(new PtuSettingsChangedRemoteEvent(
				settings));
//		((RemoteEventBus) eventBus).fireEvent(new DosimeterPtuChangedEvent(
//				settings.getDosimeterToPtuMap()));
	}
}
