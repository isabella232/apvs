package ch.cern.atlas.apvs.client;

import ch.cern.atlas.apvs.client.widget.VerticalFlowPanel;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;

import com.google.gwt.user.client.ui.HTML;

public class CameraView extends VerticalFlowPanel {

	// FIXME
//	private final String cameraURL = "rtsp://pcatlaswpss02:8554/worker1";
//	private final String cameraURL = "http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8";
	private final String cameraURL = "http://devimages.apple.com/iphone/samples/bipbop/gear4/prog_index.m3u8";
	private int videoWidth = 480;
	private int videoHeight = 360;
	private String videoPoster = "camera.jpg"; // FIXME
	@SuppressWarnings("unused")
	private RemoteEventBus eventBus;

	public CameraView(RemoteEventBus eventBus) {
		this.eventBus = eventBus;

		String source = cameraURL;
		System.err.println(source);
		add(new HTML("<video width='" + videoWidth + "' height='"
				+ videoHeight + "' poster='" + videoPoster + "' controls autoplay>"
				+ "<source src='" + source + "'></source>" + "</video>"));

	}
}
