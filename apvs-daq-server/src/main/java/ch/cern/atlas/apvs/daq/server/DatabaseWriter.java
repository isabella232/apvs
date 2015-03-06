package ch.cern.atlas.apvs.daq.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.db.Database;
import ch.cern.atlas.apvs.domain.Event;
import ch.cern.atlas.apvs.domain.GeneralConfiguration;
import ch.cern.atlas.apvs.domain.Measurement;
import ch.cern.atlas.apvs.domain.MeasurementConfiguration;
import ch.cern.atlas.apvs.domain.Message;
import ch.cern.atlas.apvs.domain.Packet;
import ch.cern.atlas.apvs.ptu.server.MessageEvent;

import com.google.gwt.event.shared.EventBus;

public class DatabaseWriter {

	private Logger log = LoggerFactory.getLogger(getClass().getName());

	private EventFilter eventFilter = new TypeEventFilter();

	public DatabaseWriter(EventBus bus) {
		final Database database = Database.getInstance();

		bus.addHandler(MessageEvent.TYPE, new MessageEvent.Handler() {

			@Override
			public void onMessageReceived(MessageEvent messageEvent) {
				System.err.println("DB "+messageEvent);
				if (messageEvent.getPrefix().equals("IN")) {
					Packet packet = messageEvent.getPacket();

					for (Message msg : packet.getMessages()) {
						switch (msg.getType()) {
						case "Measurement":
							Measurement measurement = (Measurement) msg;

							database.saveOrUpdate(measurement);
							log.debug("Written to DB: " + measurement);

							break;

						case "Event":
							Event event = (Event) msg;

							if (eventFilter.filter(event)) {
								database.saveOrUpdate(event);
								log.debug("Written to DB: " + event);
							} else {
								log.debug("Discarded: " + event);
							}

							break;

						case "GeneralConfiguration":
							GeneralConfiguration gc = (GeneralConfiguration) msg;

							database.saveOrUpdate(gc);
							log.debug("Written to DB: " + gc);

							break;

						case "MeasurementConfiguration":
							MeasurementConfiguration mc = (MeasurementConfiguration) msg;

							database.saveOrUpdate(mc);
							log.debug("Written to DB: " + mc);

							break;

						default:
							log.warn("Unhandled message type: " + msg);
							break;
						}
					}
				}
			}
		});
	}
}
