package ch.cern.atlas.apvs.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.eventbus.server.ServerEventBus;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;

public class APVSServerFactory {

	private Logger log = LoggerFactory.getLogger(getClass().getName());

	private static APVSServerFactory factory = new APVSServerFactory();
	
	private RemoteEventBus eventBus;
	
	public APVSServerFactory() {
		log.info("Creating ServerFactory...");
		
		eventBus = ServerEventBus.getInstance();
		
		ServerSettingsStorage.getInstance(eventBus);
	}
	
	public static APVSServerFactory getInstance() {
		return factory;
	}
	
	public RemoteEventBus getEventBus() {
		return eventBus;
	}
	
}
