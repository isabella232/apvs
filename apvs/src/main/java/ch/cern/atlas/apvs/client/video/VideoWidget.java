package ch.cern.atlas.apvs.client.video;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import elemental.client.Browser;
import elemental.dom.LocalMediaStream;
import elemental.dom.MediaStream;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.NavigatorUserMediaSuccessCallback;
import elemental.html.VideoElement;
import elemental.js.util.JsMappable;
import elemental.util.Mappable;

public class VideoWidget extends Widget {

	private VideoElement videoElement;
	private EventBus eventBus;
	private MediaStream stream;
	private int id = -1;

	// delayed events
	private boolean play = false;
	private boolean deactivate = false;
	private float offset;

	public VideoWidget(final EventBus eventBus) {
		super();
		this.videoElement = Browser.getDocument().createVideoElement();
		setElement(castVideo());
		this.eventBus = eventBus;

		videoElement.setControls(true);

		videoElement.addEventListener("timeupdate", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoTimeUpdateEvent.fire(eventBus, id, getTime());
			}
		}, false);

		videoElement.addEventListener("seeking", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoSeekingEvent.fire(eventBus, id);
			}
		}, false);

		// NOTE: seeked may deliver some delayed events
		videoElement.addEventListener("seeked", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoSeekedEvent.fire(eventBus, id, getTime());

				if (play) {
					VideoPlayEvent.fire(eventBus, id);
					play = false;
				}

				if (deactivate) {
					VideoDeactivateEvent.fire(eventBus, id);
					deactivate = false;
				}
			}
		}, false);

		videoElement.addEventListener("pause", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoPauseEvent.fire(eventBus, id);
			}
		}, false);

		videoElement.addEventListener("play", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoPlayEvent.fire(eventBus, id);
			}
		}, false);

		videoElement.addEventListener("playing", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoPlayingEvent.fire(eventBus, id);
			}
		}, false);

		// NOTE establishes the "activated" video, the one in charge.
		// If we are seeking, we need to keep this activated until the seek
		// has ended, then give control to the other widget (if any).
		// For thgis reason, activations may overlap.
		videoElement.setOnmouseover(new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoActivateEvent.fire(eventBus, id);
			}
		});

		// we let the deactivate hang if we are still seeking.
		videoElement.setOnmouseout(new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				if (isSeeking()) {
					deactivate = true;
				} else {
					VideoDeactivateEvent.fire(eventBus, id);
				}
			}
		});

		// NOTE: when we drag the slider, we wait, we seek, seeked, and release,
		// then we use this event to get the other videos to play again if this
		// one was not paused
		// On top of this we let the play hang until we have seeked.
		videoElement.setOnmouseup(new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				if (!videoElement.isPaused()) {
					if (isSeeking()) {
						play = true;
					} else {
						VideoPlayEvent.fire(eventBus, id);
					}
				}
			}
		});

		videoElement.addEventListener("loadstart", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoDataEvent.fire(eventBus, id, "loadstart");
			}
		}, false);
		videoElement.addEventListener("progress", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
//				VideoDataEvent.fire(eventBus, id, "progress");
			}
		}, false);
		videoElement.addEventListener("suspend", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
//				VideoDataEvent.fire(eventBus, id, "suspend");
			}
		}, false);
		videoElement.addEventListener("abort", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoDataEvent.fire(eventBus, id, "abort");
			}
		}, false);
		videoElement.addEventListener("error", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoDataEvent.fire(eventBus, id, "error");
			}
		}, false);
		videoElement.addEventListener("emptied", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoDataEvent.fire(eventBus, id, "emptied");
			}
		}, false);
		videoElement.addEventListener("stalled", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoDataEvent.fire(eventBus, id, "stalled");
			}
		}, false);
		videoElement.addEventListener("loadedmetadata", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				reset();
				VideoDataEvent.fire(eventBus, id, "loadedmetadata");
			}
		}, false);
		videoElement.addEventListener("loadeddata", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoDataEvent.fire(eventBus, id, "loadeddata");
			}
		}, false);
		videoElement.addEventListener("waiting", new EventListener() {

			@Override
			public void handleEvent(Event evt) {
				VideoDataEvent.fire(eventBus, id, "waiting");
			}
		}, false);
	}

	// package
	void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void playLocalMedia() {
		final Mappable map = (Mappable) JsMappable.createObject();
		map.setAt("video", true);
		map.setAt("audio", true);
		Browser.getWindow()
				.getNavigator()
				.webkitGetUserMedia(map,
						new NavigatorUserMediaSuccessCallback() {
							public boolean onNavigatorUserMediaSuccessCallback(
									LocalMediaStream stream) {
								setMediaStream(stream);
								play();
								return true;
							}
						});
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		setMediaStream(null);
		tryStop();
	}

	public MediaStream getStream() {
		return stream;
	}

	private final native Element castVideo() /*-{
		return this.@ch.cern.atlas.apvs.client.video.VideoWidget::videoElement;
	}-*/;

	public int getVideoHeight() {
		return videoElement.getVideoHeight();
	}

	public int getVideoWidth() {
		return videoElement.getVideoWidth();
	}

	public void setMediaStream(MediaStream mediaStream) {
		this.stream = mediaStream;
		videoElement.setSrc(StringUtil
				.createUrl((JavaScriptObject) mediaStream));
	}

	public void reset() {
		setTime(0);
	}

	public boolean play() {
		if (videoElement.isPaused() && !videoElement.isEnded()) {
			videoElement.play();
			return false;
		}
		return true;
	}

	public boolean pause() {
		if (!videoElement.isPaused() && !videoElement.isEnded()) {
			videoElement.pause();
			return false;
		}
		return true;
	}

	public void setTime(float time) {
		time = time + offset;
		float startTime = videoElement.getStartTime();
		float endTime = startTime + videoElement.getDuration();
		if (time < startTime) {
			time = startTime;
		} else if (time > endTime) {
			time = endTime;
		}
		if (videoElement.getCurrentTime() != time) {
			videoElement.setCurrentTime(time);
		}
	}

	public float getTime() {
		return videoElement.getCurrentTime() - offset;
	}

	public void tryStop() {
		if (stream instanceof LocalMediaStream) {
			((LocalMediaStream) stream).stop();
		}
	}

	public boolean isSeeking() {
		return videoElement.isSeeking();
	}

	public void setSrc(String src) {
		videoElement.setSrc(src);
	}

	public void setOffset(float offset) {
		this.offset = offset;
	}

	public int getNetworkState() {
		return videoElement.getNetworkState();
	}

	public int getReadyState() {
		return videoElement.getReadyState();
	}
}