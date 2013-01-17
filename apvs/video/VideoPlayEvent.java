package ch.cern.atlas.apvs.client.video;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class VideoPlayEvent extends Event<VideoPlayEvent.Handler> {

	public interface Handler {
		/**
		 * Called when an event is fired.
		 * 
		 * @param event
		 *            an {@link MessageReceivedEvent} instance
		 */
		void onPlay(VideoPlayEvent event);
	}

	private static final Type<VideoPlayEvent.Handler> TYPE = new Type<VideoPlayEvent.Handler>();

	public static void fire(EventBus eventBus, int id) {
		eventBus.fireEvent(new VideoPlayEvent(id));
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
			VideoPlayEvent.Handler handler) {
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

	public VideoPlayEvent() {
	}
	
	public VideoPlayEvent(int id) {
		this.id = id;
	}

	@Override
	public Type<VideoPlayEvent.Handler> getAssociatedType() {
		return TYPE;
	}

	public int getId() {
		return id;
	}
		
	@Override
	protected void dispatch(Handler handler) {
		handler.onPlay(this);
	}
	
	@Override
	public String toString() {
		return "VideoPlay "+id;
	}

}
