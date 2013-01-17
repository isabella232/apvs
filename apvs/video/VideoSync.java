package ch.cern.atlas.apvs.client.video;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.web.bindery.event.shared.EventBus;

public class VideoSync extends SimplePanel {

	private List<VideoWidget> videoWidgets = new ArrayList<VideoWidget>();
	private TextArea msg = new TextArea();
	private int activeId = -1;
	private int nextActiveId = -1;
	
	public VideoSync(EventBus eventBus) {

		msg.setVisibleLines(25);
		msg.setWidth("50%");
		add(msg);

		VideoTimeUpdateEvent.register(eventBus,
				new VideoTimeUpdateEvent.Handler() {

					@Override
					public void onTimeUpdate(VideoTimeUpdateEvent event) {

						// set current time on all "other" video widgets if the
						// current one is seeking
						if (activeId == event.getId() && videoWidgets.get(activeId).isSeeking()) {
							addMsg("" + event);
							for (VideoWidget w : videoWidgets) {
								if (w.getId() != activeId) {
									w.pause();
									w.setTime(event.getTime());
								}
							}
						} else {
							addMsg("*"+event);
						}
					}
				});

		VideoSeekingEvent.register(eventBus, new VideoSeekingEvent.Handler() {

			@Override
			public void onSeeking(VideoSeekingEvent event) {
				if (activeId != event.getId()) return;				
				addMsg("" + event);
			}
		});

		VideoSeekedEvent.register(eventBus, new VideoSeekedEvent.Handler() {

			@Override
			public void onSeeked(VideoSeekedEvent event) {
// NOTE: we seem to have to sync by all
//				if (activeId != event.getId()) return;				
				addMsg("" + event);

				// set current time on all "other" video widgets
				for (VideoWidget w : videoWidgets) {
					if (w.getId() != activeId) {
						w.setTime(event.getTime());
					}
				}
			}
		});

		VideoPlayEvent.register(eventBus, new VideoPlayEvent.Handler() {

			@Override
			public void onPlay(VideoPlayEvent event) {
				if (activeId != event.getId()) return;
				addMsg("" + event);
				
				// invoke play on all "other" video widgets
				for (VideoWidget w : videoWidgets) {
					if (w.getId() != activeId) {
						w.play();
					}					
				}
			}
		});

		VideoPauseEvent.register(eventBus, new VideoPauseEvent.Handler() {

			@Override
			public void onPause(VideoPauseEvent event) {
				if (activeId != event.getId()) return;
				addMsg("" + event);
				
				// invoke pause on all "other" video widgets
				for (VideoWidget w : videoWidgets) {
					if (w.getId() != activeId) {
						w.pause();
					}					
				}
			}
		});
		
		VideoActivateEvent.register(eventBus, new VideoActivateEvent.Handler() {

			@Override
			public void onActivate(VideoActivateEvent event) {
				if (activeId >= 0) {
					nextActiveId = event.getId();
				} else {
					activeId = event.getId();
				}
			}
		});

		VideoDeactivateEvent.register(eventBus, new VideoDeactivateEvent.Handler() {

			@Override
			public void onDeactivate(VideoDeactivateEvent event) {
				activeId = nextActiveId;
				nextActiveId = -1;
			}
		});
		
		VideoDataEvent.register(eventBus, new VideoDataEvent.Handler() {
			
			@Override
			public void onData(VideoDataEvent event) {
				addMsg(""+event+" "+videoWidgets.get(event.getId()).getNetworkState()+" "+videoWidgets.get(event.getId()).getReadyState());
			}
		});
	}

	private int line = 1;

	private void addMsg(String text) {
		msg.setText(msg.getText() + "\n" + line + " " + text);
		msg.getElement().setScrollTop(msg.getElement().getScrollHeight());
		line++;
	}

	public void add(VideoWidget widget) {
		widget.setId(videoWidgets.size());
		videoWidgets.add(widget);
	}

	public void reset() {
		for (VideoWidget w : videoWidgets) {
			w.pause();
			w.reset();
		}
	}

	public void play() {
		for (VideoWidget w : videoWidgets) {
			w.play();
		}
	}

	public void pause() {
		for (VideoWidget w : videoWidgets) {
			w.pause();
		}
	}

}
