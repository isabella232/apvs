package ch.cern.atlas.apvs.client.video;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.web.bindery.event.shared.EventBus;

public class VideoControlWidget extends HorizontalPanel {

	private Label label;
	private int id;

	public VideoControlWidget(EventBus eventBus, int id) {	
		this.id = id;
		
		label = new Label(" ");
		add(label);
		
		VideoTimeUpdateEvent.register(eventBus, new VideoTimeUpdateEvent.Handler() {
			
			@Override
			public void onTimeUpdate(VideoTimeUpdateEvent event) {
				setTime(event.getId(), event.getTime());
			}
		});
		
		VideoSeekedEvent.register(eventBus, new VideoSeekedEvent.Handler() {
			
			@Override
			public void onSeeked(VideoSeekedEvent event) {
				setTime(event.getId(), event.getTime());				
			}
		});
	}
	
	private void setTime(int id, float time) {
		if (this.id == id) {
			label.setText(id+" "+time);
		}
	}
}
