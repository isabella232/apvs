package ch.cern.atlas.apvs.client.video;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class VideoTimeUpdateEvent extends Event<VideoTimeUpdateEvent.Handler> {

	public interface Handler {
		/**
		 * Called when an event is fired.
		 * 
		 * @param event
		 *            an {@link MessageReceivedEvent} instance
		 */
		void onTimeUpdate(VideoTimeUpdateEvent event);
	}

	private static final Type<VideoTimeUpdateEvent.Handler> TYPE = new Type<VideoTimeUpdateEvent.Handler>();

	public static void fire(EventBus eventBus, int id, float time) {
		eventBus.fireEvent(new VideoTimeUpdateEvent(id, time));
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
			VideoTimeUpdateEvent.Handler handler) {
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
	private float time;

	public VideoTimeUpdateEvent() {
	}
	
	public VideoTimeUpdateEvent(int id, float time) {
		this.id = id;
		this.time = time;
	}

	@Override
	public Type<VideoTimeUpdateEvent.Handler> getAssociatedType() {
		return TYPE;
	}

	public int getId() {
		return id;
	}
		
	public float getTime() {
		return time;
	}
	
	@Override
	protected void dispatch(Handler handler) {
		handler.onTimeUpdate(this);
	}
	
	@Override
	public String toString() {
		return "VideoTimeUpdate "+id+" "+time;
	}

}
