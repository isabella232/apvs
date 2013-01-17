package ch.cern.atlas.apvs.ptu;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import ch.cern.atlas.apvs.domain.Message;
import ch.cern.atlas.apvs.ptu.server.PtuJsonReader;

public class PtuJsonReaderTest {

	String json = "{"+
			"\"Sender\":\"PTU_88\",\"Receiver\":\"Broadcast\",\"FrameID\":\"0\",\"Acknowledge\":\"false\",\"Messages\":["+
			"{\"Type\":\"Measurement\",\"Sensor\":\"Humidity\",\"Time\":\"11/9/2012 10:02:25\",\"Method\":\"OneShoot\",\"Value\":\"33.19684099267707\",\"SamplingRate\":\"10000\",\"Unit\":\"ppm\",\"DownThreshold\":\"33.0\",\"UpThreshold\":\"35.7\"},"+
			"{\"Type\":\"Measurement\",\"Sensor\":\"Humidity\",\"Time\":\"11/9/2012 10:07:10\",\"Method\":\"OneShoot\",\"Value\":\"35.45927608218701\",\"SamplingRate\":\"15000\",\"Unit\":\"ppm\",\"DownThreshold\":\"33.0\",\"UpThreshold\":\"35.7\"}"+
			"]}";
	
	String msg0 = "Measurement(PTU_88): name=Humidity value=33.19684099267707 unit=ppm sampling rate=10000 date: Tue Sep 11 10:02:25 CEST 2012";
	String msg1 = "Measurement(PTU_88): name=Humidity value=35.45927608218701 unit=ppm sampling rate=15000 date: Tue Sep 11 10:07:10 CEST 2012";
	
	@Test
	public void test() {
		List<Message> list = (List<Message>)PtuJsonReader.toJava(json);
		Assert.assertEquals(2, list.size());
//		System.err.println(list.get(0).toString());
//		System.err.println(list.get(1).toString());
		Assert.assertEquals(msg0, list.get(0).toString());
		Assert.assertEquals(msg1, list.get(1).toString());
	}
}
