package ch.cern.atlas.apvs.daq.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.ptu.server.MessageEvent;

import com.google.gwt.event.shared.EventBus;

public class DebugHandler {
	private Logger log = LoggerFactory.getLogger(getClass().getName());
	
	public DebugHandler(EventBus bus) {
		bus.addHandler(MessageEvent.TYPE, new MessageEvent.Handler() {

			@Override
			public void onMessageReceived(MessageEvent event) {
				log.info("BUS " + event);
			}
		});
	}

}
