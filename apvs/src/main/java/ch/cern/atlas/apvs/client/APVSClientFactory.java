package ch.cern.atlas.apvs.client;

import org.atmosphere.gwt.client.AtmosphereGWTSerializer;

import ch.cern.atlas.apvs.client.event.EventSerializer;
import ch.cern.atlas.apvs.client.service.AudioServiceAsync;
import ch.cern.atlas.apvs.client.service.DbServiceAsync;
import ch.cern.atlas.apvs.client.service.EventServiceAsync;
import ch.cern.atlas.apvs.client.service.FileServiceAsync;
import ch.cern.atlas.apvs.client.service.InterventionServiceAsync;
import ch.cern.atlas.apvs.client.service.PtuServiceAsync;
import ch.cern.atlas.apvs.client.service.ServerServiceAsync;
import ch.cern.atlas.apvs.client.tablet.CameraPanel;
import ch.cern.atlas.apvs.client.tablet.CameraUI;
import ch.cern.atlas.apvs.client.tablet.ImagePanel;
import ch.cern.atlas.apvs.client.tablet.ImageUI;
import ch.cern.atlas.apvs.client.tablet.MainMenuList;
import ch.cern.atlas.apvs.client.tablet.MainMenuUI;
import ch.cern.atlas.apvs.client.tablet.ModelPanel;
import ch.cern.atlas.apvs.client.tablet.ModelUI;
import ch.cern.atlas.apvs.client.tablet.ProcedureMenuPanel;
import ch.cern.atlas.apvs.client.tablet.ProcedureMenuUI;
import ch.cern.atlas.apvs.client.tablet.ProcedureNavigator;
import ch.cern.atlas.apvs.client.tablet.ProcedurePanel;
import ch.cern.atlas.apvs.client.tablet.ProcedureUI;
import ch.cern.atlas.apvs.client.ui.Arguments;
import ch.cern.atlas.apvs.client.ui.MeasurementView;
import ch.cern.atlas.apvs.client.ui.ProcedureView;
import ch.cern.atlas.apvs.client.ui.PtuSelector;
import ch.cern.atlas.apvs.eventbus.client.AtmosphereEventBus;
import ch.cern.atlas.apvs.eventbus.client.PollEventBus;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;
import ch.cern.atlas.apvs.server.ServerSettingsStorage;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.web.bindery.event.shared.EventBus;

public class APVSClientFactory implements ClientFactory {

	@SuppressWarnings("unused")
	private AtmosphereEventBus atmosphereEventBus;
	private RemoteEventBus localEventBus;
	private final PlaceController placeController;
	private final ServerServiceAsync serverService = ServerServiceAsync.Util.getInstance();
	private final FileServiceAsync fileService = FileServiceAsync.Util.getInstance();
	private final PtuServiceAsync ptuService = PtuServiceAsync.Util.getInstance();
	private final AudioServiceAsync audioService = AudioServiceAsync.Util.getInstance();
	private final DbServiceAsync dbService = DbServiceAsync.Util.getInstance();
	private final EventServiceAsync eventService = EventServiceAsync.Util.getInstance();
	private final InterventionServiceAsync interventionService = InterventionServiceAsync.Util.getInstance();

	private MainMenuUI homeView;
	private ModelUI modelView;
	private ProcedureMenuUI procedureView;
	private PtuSelector ptuSelector;
	private MeasurementView measurementView;

	public APVSClientFactory() {
		// atmosphereEventBus keeps track of connections, not used for actual polling of events
// FIXME #284, re-enable, but reload gives NPE onDisconnect in atmosphere-gwt
//		AtmosphereGWTSerializer serializer = GWT.create(EventSerializer.class);
//		atmosphereEventBus = new AtmosphereEventBus(serializer);

		AuthenticatingRequestBuilder requestBuilder = new AuthenticatingRequestBuilder();

		((ServiceDefTarget)serverService).setRpcRequestBuilder(requestBuilder);
		((ServiceDefTarget)fileService).setRpcRequestBuilder(requestBuilder);
		((ServiceDefTarget)ptuService).setRpcRequestBuilder(requestBuilder);
		((ServiceDefTarget)audioService).setRpcRequestBuilder(requestBuilder);
		((ServiceDefTarget)dbService).setRpcRequestBuilder(requestBuilder);
		((ServiceDefTarget)eventService).setRpcRequestBuilder(requestBuilder);
		((ServiceDefTarget)interventionService).setRpcRequestBuilder(requestBuilder);

		// used for events
		RemoteEventBus remoteEventBus = new PollEventBus(requestBuilder);
		NamedEventBus.put("remote", remoteEventBus);
		placeController = new PlaceController(remoteEventBus);
		
		// specially for now in iPad app
		localEventBus = new RemoteEventBus();
	}

	@Override
	public EventBus getEventBus(String name) {
		return NamedEventBus.get(name);
	}

	@Override
	public RemoteEventBus getRemoteEventBus() {
		return (RemoteEventBus)NamedEventBus.get("remote");
	}

	@Override
	public PlaceController getPlaceController() {
		return placeController;
	}

	@Override
	public FileServiceAsync getFileService() {
		return fileService;
	}
	
	@Override
	public PtuServiceAsync getPtuService() {
		return ptuService;
	}


	@Override
	public MainMenuUI getHomeView() {
		if (homeView== null) {
			homeView = new MainMenuList(this);
		}
		return homeView;
	}

	@Override
	public ModelUI getModelView() {
		if (modelView == null) {
			modelView = new ModelPanel(this);
		}
		return modelView;
	}

	@Override
	public PtuSelector getPtuSelector() {
//		if (ptuSelector == null) {
			ptuSelector = new PtuSelector(getRemoteEventBus(), getEventBus("remote"));
//		}
		return ptuSelector;
	}

	@Override
	public MeasurementView getMeasurementView() {
//		if (measurementView == null) {
			measurementView = new MeasurementView();
			measurementView.configure(null, this, new Arguments());
//		}
		return measurementView;
	}

	@Override
	public CameraUI getCameraView(String type) {
		return new CameraPanel(this, type);
	}

	@Override
	public ProcedureMenuUI getProcedureMenuView() {
		if (procedureView == null) {
			procedureView = new ProcedureMenuPanel(this);
		}
		return procedureView;
	}
	
	@Override
	public ImageUI getImagePanel(String url) {
		return new ImagePanel(url);
	}

	@Override
	public ProcedureUI getProcedurePanel(String url, String name, String step) {
		return new ProcedurePanel(this, url, name, step);
	}
	
	@Override
	public ProcedureNavigator getProcedureNavigator() {
		return new ProcedureNavigator(localEventBus);
	}

	@Override
	public ProcedureView getProcedureView(String width, String height) {
		// FIXME #178 width and height ignored
		ProcedureView v = new ProcedureView();
		v.configure(null, this, new Arguments());
		return v;
	}

	@Override
	public ProcedureView getProcedureView(
			String width, String height, String url, String name, String step) {
		// FIXME #178 width and height and name and step ignored
		ProcedureView v = new ProcedureView();
		v.configure(null, this, new Arguments());
		return v;
	}

	@Override
	public ServerServiceAsync getServerService() {
		return serverService;
	}

	@Override
	public AudioServiceAsync getAudioService() {
		return audioService;
	}

	@Override
	public DbServiceAsync getDbService() {
		return dbService;
	}

	@Override
	public EventServiceAsync getEventService() {
		return eventService;
	}

	@Override
	public InterventionServiceAsync getInterventionService() {
		return interventionService;
	}
}
