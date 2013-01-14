package ch.cern.atlas.apvs.client.video;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class VideoDeactivateEvent extends Event<VideoDeactivateEvent.Handler> {

	public interface Handler {
		/**
		 * Called when an event is fired.
		 * 
		 * @param event
		 *            an {@link MessageReceivedEvent} instance
		 */
		void onDeactivate(VideoDeactivateEvent event);
	}

	private static final Type<VideoDeactivateEvent.Handler> TYPE = new Type<VideoDeactivateEvent.Handler>();

	public static void fire(EventBus eventBus, int id) {
		eventBus.fireEvent(new VideoDeactivateEvent(id));
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
	public static HandlerRegistration register(EventBus eventBus,
			VideoDeactivateEvent.Handler handler) {
		return eventBus.addHandler(TYPE, handler);
	}

	// FIXME later
//	public static HandlerRegistration subscribe(EventBus eventBus,
//			VideoTimeUpdateEvent.Handler handler) {
//		HandlerRegistration registration = register(eventBus, handler);
//		
//		eventBus.fireEvent(new RequestEvent(VideoTimeUpdateEvent.class));
//		
//		return registration;
//	}

	private int id;

	public VideoDeactivateEvent() {
	}
	
	public VideoDeactivateEvent(int id) {
		this.id = id;
	}

	@Override
	public Type<VideoDeactivateEvent.Handler> getAssociatedType() {
		return TYPE;
	}

	public int getId() {
		return id;
	}
		
	@Override
	protected void dispatch(Handler handler) {
		handler.onDeactivate(this);
	}
	
	@Override
	public String toString() {
		return "VideoDeactivate "+id;
	}

}
