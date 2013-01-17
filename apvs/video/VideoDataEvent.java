package ch.cern.atlas.apvs.client.video;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class VideoDataEvent extends Event<VideoDataEvent.Handler> {

	public interface Handler {
		/**
		 * Called when an event is fired.
		 * 
		 * @param event
		 *            an {@link MessageReceivedEvent} instance
		 */
		void onData(VideoDataEvent event);
	}

	private static final Type<VideoDataEvent.Handler> TYPE = new Type<VideoDataEvent.Handler>();

	public static void fire(EventBus eventBus, int id, String type) {
		eventBus.fireEvent(new VideoDataEvent(id, type));
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
			VideoDataEvent.Handler handler) {
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
	private String type;

	public VideoDataEvent() {
	}
	
	public VideoDataEvent(int id, String type) {
		this.id = id;
		this.type = type;
	}

	@Override
	public Type<VideoDataEvent.Handler> getAssociatedType() {
		return TYPE;
	}

	public int getId() {
		return id;
	}
	
	public String getType() {
		return type;
	}
		
	@Override
	protected void dispatch(Handler handler) {
		handler.onData(this);
	}
	
	@Override
	public String toString() {
		return "VideoData "+id+" "+type;
	}

}
