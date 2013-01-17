package ch.cern.atlas.apvs.client.event;

import ch.cern.atlas.apvs.client.domain.Ternary;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEvent;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;
import ch.cern.atlas.apvs.eventbus.shared.RequestRemoteEvent;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class ConnectionStatusChangedRemoteEvent extends
		RemoteEvent<ConnectionStatusChangedRemoteEvent.Handler> {

	private static final long serialVersionUID = 8865199851228810365L;

	public enum ConnectionType {
		audio("Audio Status"), video("Video Status"), daq("DAQ Status"), dosimeter(
				"Dosimeter Status"), database("Database Status");

		private String s;

		private ConnectionType(String s) {
			this.s = s;
		}

		public String getString() {
			return s;
		}
	}

	public interface Handler {
		/**
		 * Called when an event is fired.
		 * 
		 * @param event
		 *            an {@link MessageReceivedEvent} instance
		 */
		void onConnectionStatusChanged(ConnectionStatusChangedRemoteEvent event);
	}

	private static final Type<ConnectionStatusChangedRemoteEvent.Handler> TYPE = new Type<ConnectionStatusChangedRemoteEvent.Handler>();

	public static void fire(RemoteEventBus eventBus, ConnectionType type,
			Ternary status) {
		eventBus.fireEvent(new ConnectionStatusChangedRemoteEvent(type, status));
	}

	public static void fire(RemoteEventBus eventBus, ConnectionType type,
			boolean ok) {
		eventBus.fireEvent(new ConnectionStatusChangedRemoteEvent(type, ok ? Ternary.True : Ternary.False));
	}

	/**
	 * Register a handler for events on the eventbus.
	 * 
	 * @param eventBus
	 *            the {@link EventBus}
	 * @param handler
	 *            an Handler instance
	 * @return an {@link HandlerRegistration} instance
	 */
	public static HandlerRegistration register(RemoteEventBus eventBus,
			ConnectionStatusChangedRemoteEvent.Handler handler) {
		return eventBus.addHandler(TYPE, handler);
	}

	public static HandlerRegistration subscribe(RemoteEventBus eventBus,
			ConnectionStatusChangedRemoteEvent.Handler handler) {
		HandlerRegistration registration = register(eventBus, handler);

		eventBus.fireEvent(new RequestRemoteEvent(
				ConnectionStatusChangedRemoteEvent.class));

		return registration;
	}

	private ConnectionType connection;
	private Ternary status;

	public ConnectionStatusChangedRemoteEvent() {
	}

	public ConnectionStatusChangedRemoteEvent(ConnectionType connection, Ternary status) {
		this.connection = connection;
		this.status = status;
	}

	@Override
	public Type<ConnectionStatusChangedRemoteEvent.Handler> getAssociatedType() {
		return TYPE;
	}

	public ConnectionType getConnection() {
		return connection;
	}

	public Ternary getStatus() {
		return status;
	}

	@Override
	protected void dispatch(Handler handler) {
		handler.onConnectionStatusChanged(this);
	}

}
