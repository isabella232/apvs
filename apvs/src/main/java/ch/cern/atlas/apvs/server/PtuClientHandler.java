package ch.cern.atlas.apvs.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.client.event.PtuSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.settings.PtuSettings;
import ch.cern.atlas.apvs.db.Database;
import ch.cern.atlas.apvs.db.Scale;
import ch.cern.atlas.apvs.db.SensorMap;
import ch.cern.atlas.apvs.domain.APVSException;
import ch.cern.atlas.apvs.domain.Device;
import ch.cern.atlas.apvs.domain.Error;
import ch.cern.atlas.apvs.domain.Event;
import ch.cern.atlas.apvs.domain.GeneralConfiguration;
import ch.cern.atlas.apvs.domain.Measurement;
import ch.cern.atlas.apvs.domain.MeasurementConfiguration;
import ch.cern.atlas.apvs.domain.Message;
import ch.cern.atlas.apvs.domain.Order;
import ch.cern.atlas.apvs.domain.Packet;
import ch.cern.atlas.apvs.domain.Report;
import ch.cern.atlas.apvs.domain.Ternary;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent.ConnectionType;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;
import ch.cern.atlas.apvs.eventbus.shared.RequestRemoteEvent;
import ch.cern.atlas.apvs.ptu.server.PtuJsonWriter;
import ch.cern.atlas.apvs.ptu.server.PtuReconnectHandler;
import ch.cern.atlas.apvs.ptu.shared.EventChangedEvent;
import ch.cern.atlas.apvs.ptu.shared.MeasurementChangedEvent;
import ch.cern.atlas.apvs.ptu.shared.MeasurementConfigurationChangedEvent;

import com.google.gwt.user.client.rpc.SerializationException;

@Sharable
public class PtuClientHandler extends PtuReconnectHandler {

	private Logger log = LoggerFactory.getLogger(getClass().getName());
	private final RemoteEventBus eventBus;

	private List<Measurement> measurementChanged = new ArrayList<Measurement>();

	private Ternary dosimeterOk = Ternary.Unknown;
	private String dosimeterCause = "Dosimeter not yet read by PTU";

	private PtuSettings settings;

	private Database database;
	private SensorMap sensorMap;
	private Map<String, Device> deviceMap;

	public PtuClientHandler(Bootstrap bootstrap, final RemoteEventBus eventBus)
			throws SerializationException {
		super(bootstrap, "DAQ");
		this.eventBus = eventBus;

		database = Database.getInstance();

		RequestRemoteEvent.register(this, eventBus, new RequestRemoteEvent.Handler() {

			@Override
			public void onRequestEvent(RequestRemoteEvent event) {
				String type = event.getRequestedClassName();

				if (type.equals(ConnectionStatusChangedRemoteEvent.class
						.getName())) {
					ConnectionStatusChangedRemoteEvent.fire(eventBus,
							ConnectionType.daq, isConnected(), getCause());
					ConnectionStatusChangedRemoteEvent.fire(eventBus,
							ConnectionType.dosimeter,
							isConnected() ? dosimeterOk : Ternary.False,
							dosimeterCause);
				}
			}
		});

		PtuSettingsChangedRemoteEvent.subscribe(this, eventBus,
				new PtuSettingsChangedRemoteEvent.Handler() {

					@Override
					public void onPtuSettingsChanged(
							PtuSettingsChangedRemoteEvent event) {
						settings = event.getPtuSettings();
					}
				});
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ConnectionStatusChangedRemoteEvent.fire(eventBus, ConnectionType.daq,
				true, "");
		ConnectionStatusChangedRemoteEvent.fire(eventBus,
				ConnectionType.dosimeter, dosimeterOk, dosimeterCause);
		super.channelActive(ctx);

		sensorMap = database.getSensorMap();

		deviceMap = database.getDeviceMap();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ConnectionStatusChangedRemoteEvent.fire(eventBus, ConnectionType.daq,
				false, getCause());
		ConnectionStatusChangedRemoteEvent.fire(eventBus,
				ConnectionType.dosimeter, dosimeterOk, dosimeterCause);
		super.channelInactive(ctx);
	}

	public ChannelFuture sendOrder(Order order) throws IOException {
		log.info("=====> " + PtuJsonWriter.objectToJson(order));

		if (isConnected()) {
			ChannelFuture future = getChannel().write(
					new Packet("apvs-daq-server", 0, false, order));
			getContext().flush();
			return future;
		} else {
			throw new IOException("Channel Closed");
		}
	}

	private final static boolean DEBUG = true;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet)
			throws Exception {
		log.info("READ " + packet);

		Device device = deviceMap.get(packet.getSender());
		List<Message> list = packet.getMessages();
		if (device == null) {
			log.warn("Messages (" + list.size() + ") from unknown device: "
					+ packet.getSender());
			return;
		}

		if (DEBUG) {
			log.debug("# of mesg: " + list.size());
		}
		for (Iterator<Message> i = list.iterator(); i.hasNext();) {
			Message message = i.next();
			if (DEBUG) {
				log.debug(message.toString());
			}

			try {
				if (message instanceof Measurement) {
					handleMessage((Measurement) message);
				} else if (message instanceof Report) {
					handleMessage((Report) message);
				} else if (message instanceof Event) {
					handleMessage((Event) message);
				} else if (message instanceof Error) {
					handleMessage((Error) message);
				} else if (message instanceof GeneralConfiguration) {
					handleMessage((GeneralConfiguration) message);
				} else if (message instanceof MeasurementConfiguration) {
					handleMessage((MeasurementConfiguration) message);
				} else {
					log.warn("Error: unknown Message Type: "
							+ message.getType());
				}
			} catch (APVSException e) {
				log.warn("Could not add measurement", e);
			} catch (SerializationException e) {
				log.error("Could not serialize event", e);
			}
		}
	}

	private final static long SECOND = 1000;
	private final static long MINUTE = 60 * SECOND;

	private void handleMessage(Measurement message) throws APVSException,
			SerializationException {
		// Quick fix for #371
		Date now = new Date();
		if (message.getTime().getTime() < (now.getTime() - 5 * MINUTE)) {
			log.warn("UPDATE IGNORED, too old " + message.getTime() + " " + now
					+ " " + message);
			return;
		}

		Device ptu = message.getDevice();
		String sensor = message.getSensor();

		if (!sensorMap.isEnabled(ptu, sensor)) {
			log.warn("UPDATE IGNORED, disabled measurement " + ptu.getName() + " " + sensor);
			return;
		}

		String unit = message.getUnit();
		Double value = message.getValue();
		Double low = message.getDownThreshold();
		Double high = message.getUpThreshold();

		// Scale down to microSievert
		value = Scale.getValue(value, unit);
		low = Scale.getDownThreshold(low, unit);
		high = Scale.getUpThreshold(high, unit);
		unit = Scale.getUnit(sensor, unit);

		message = new Measurement(message.getDevice(), sensor, value, low,
				high, unit, message.getSamplingRate(), "OneShoot",
				message.getTime());

		measurementChanged.add(message);

		sendEvents();
	}

	private void handleMessage(Event message) throws SerializationException {
		Device device = message.getDevice();
		String sensor = message.getSensor();

		// log.info("EVENT " + message);

		eventBus.fireEvent(new EventChangedEvent(new Event(device, sensor,
				message.getEventType(), message.getValue(), message
						.getThreshold(), message.getUnit(), message.getTime())));

		if (message.getEventType().equals("DosConnectionStatus_OFF")) {
			dosimeterOk = Ternary.False;
			dosimeterCause = "Dosimeter not connected to PTU";
			ConnectionStatusChangedRemoteEvent.fire(eventBus,
					ConnectionType.dosimeter, dosimeterOk, dosimeterCause);
		} else if (message.getEventType().equals("DosConnectionStatus_ON")) {
			dosimeterOk = Ternary.True;
			dosimeterCause = "";
			ConnectionStatusChangedRemoteEvent.fire(eventBus,
					ConnectionType.dosimeter, dosimeterOk, dosimeterCause);
		}
	}

	private void handleMessage(GeneralConfiguration message)
			throws SerializationException {
		if (settings != null) {
			PtuSettingsStorage.updateFromDatabase(settings);

			eventBus.fireEvent(new PtuSettingsChangedRemoteEvent(settings));
		}
	}

	private void handleMessage(MeasurementConfiguration message)
			throws SerializationException {
		eventBus.fireEvent(new MeasurementConfigurationChangedEvent(message));
	}

	private void handleMessage(Report report) {
		log.warn(report.getType() + " NOT YET IMPLEMENTED, see #23 and #112");
	}

	private void handleMessage(Error error) {
		log.warn(error.getType() + " NOT YET IMPLEMENTED, see #114");
	}

	private synchronized void sendEvents() throws SerializationException {

		for (Iterator<Measurement> i = measurementChanged.iterator(); i
				.hasNext();) {
			Measurement m = i.next();
			eventBus.fireEvent(new MeasurementChangedEvent(m));
		}

		measurementChanged.clear();
	}
}
