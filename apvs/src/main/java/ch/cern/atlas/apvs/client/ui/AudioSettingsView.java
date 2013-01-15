package ch.cern.atlas.apvs.client.ui;

import java.util.ArrayList;
import java.util.List;

import ch.cern.atlas.apvs.client.ClientFactory;
import ch.cern.atlas.apvs.client.event.AsteriskStatusRemoteEvent;
import ch.cern.atlas.apvs.client.event.AudioSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.event.InterventionMapChangedRemoteEvent;
import ch.cern.atlas.apvs.client.settings.AudioSettings;
import ch.cern.atlas.apvs.client.widget.ActiveCheckboxCell;
import ch.cern.atlas.apvs.client.widget.DynamicSelectionCell;
import ch.cern.atlas.apvs.client.widget.StringList;
import ch.cern.atlas.apvs.client.widget.TextInputSizeCell;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;

public class AudioSettingsView extends VerticalPanel implements Module{
	
	private CellTable<String> table = new CellTable<String>(); //n a partida
	private AudioSettings voipAccounts =  new AudioSettings();	//s a partida
	private List<String> activePtuIds = new ArrayList<String>(); //n a partida
	private List<String> usersList = new ArrayList<String>();	//s a partida
	private ListDataProvider<String> dataProvider = new ListDataProvider<String>(); //n a partida

	
	public AudioSettingsView() {
	}

	@Override
	public boolean configure(Element element, ClientFactory clientFactory, Arguments args) {
		final RemoteEventBus eventBus = clientFactory.getRemoteEventBus();
		 
		//Call List Users
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
		
		add(table);
		
		//ACTIVE
		Column<String,Boolean> active = new Column<String,Boolean>(new ActiveCheckboxCell()) {
			
			public Boolean getValue(String object) {
				return activePtuIds.contains(object);
			}
		};
		table.addColumn(active, "Active");
		
		//PTU ID
		Column<String, String> ptuId = new Column<String, String>(new TextCell()) {

			@Override
			public String getValue(String object) {
				return object;
			}
		};
		ptuId.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		table.addColumn(ptuId, "Ptu ID");

		// NAME
		Column<String, String> name = new Column<String, String>(
				new TextCell()) {
			@Override
			public String getValue(String object) {
				return voipAccounts.getUsername(object);
			}
		};
		name.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		table.addColumn(name, "Name");
		
		//Number
		Column<String, String> number = new Column<String, String>(new DynamicSelectionCell(new StringList<String>(usersList))){
			
			@Override
			public String getValue(String object) {
				return voipAccounts.getNumber(object);
			}
					
		};
		number.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		table.addColumn(number, "Number");
				
		number.setFieldUpdater(new FieldUpdater<String, String>() {
			
			@Override
			public void update(int index, String object, String value) {
				voipAccounts.setNumber(object, value);
				eventBus.fireEvent(new AudioSettingsChangedRemoteEvent(voipAccounts));
			}
		});
		
		//Impact Activity
		Column<String, String> activity = new Column<String, String>(new TextInputSizeCell(40)) {
			
			@Override
			public String getValue(String object) {
				return voipAccounts.getActivity(object);
			}
		};
		activity.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		table.addColumn(activity, "Impact Activity");
		
		activity.setFieldUpdater(new FieldUpdater<String, String>() {
			
			@Override
			public void update(int index, String object, String value) {
				voipAccounts.setActivity(object, value);
				eventBus.fireEvent(new AudioSettingsChangedRemoteEvent(voipAccounts));
			}
		});
		
		dataProvider.addDataDisplay(table);
		dataProvider.setList(new ArrayList<String>());
		
		//*****************************
		//Events Registration
		InterventionMapChangedRemoteEvent.subscribe(eventBus,new InterventionMapChangedRemoteEvent.Handler() {

			@Override
			public void onInterventionMapChanged(InterventionMapChangedRemoteEvent event){
				System.err.println("PTU IDS changed");
				activePtuIds = event.getInterventionMap().getPtuIds();
			}
		});
		
		AudioSettingsChangedRemoteEvent.subscribe(eventBus,new AudioSettingsChangedRemoteEvent.Handler() {
			
			@Override
			public void onAudioSettingsChanged(AudioSettingsChangedRemoteEvent event) {
				System.out.println("Audio Settings changed");
				voipAccounts = event.getAudioSettings();

				dataProvider.getList().clear();
				dataProvider.getList().addAll(voipAccounts.getPtuIds());
			}
		}); 		
		
		AsteriskStatusRemoteEvent.register(eventBus, new AsteriskStatusRemoteEvent.Handler() {
			
			@Override
			public void onAsteriskStatusChange(AsteriskStatusRemoteEvent event) {
				System.out.println("Asterisk Event");
				usersList.clear();
				usersList.addAll(event.getAsteriskUsersList());
			}
		});	
		
		return true;
	}
		
	@Override
	public boolean update() {
		return false;
	}
}
