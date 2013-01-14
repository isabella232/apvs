package ch.cern.atlas.apvs.client.video;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class VideoSeekedEvent extends Event<VideoSeekedEvent.Handler> {

	public interface Handler {
		/**
		 * Called when an event is fired.
		 * 
		 * @param event
		 *            an {@link MessageReceivedEvent} instance
		 */
		void onSeeked(VideoSeekedEvent event);
	}

	private static final Type<VideoSeekedEvent.Handler> TYPE = new Type<VideoSeekedEvent.Handler>();

	public static void fire(EventBus eventBus, int id, float time) {
		eventBus.fireEvent(new VideoSeekedEvent(id, time));
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
			VideoSeekedEvent.Handler handler) {
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

	public VideoSeekedEvent() {
	}
	
	public VideoSeekedEvent(int id, float time) {
		this.id = id;
		this.time = time;
	}

	@Override
	public Type<VideoSeekedEvent.Handler> getAssociatedType() {
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
		handler.onSeeked(this);
	}
	
	@Override
	public String toString() {
		return "VideoSeeked "+id+" "+time;
	}

}
