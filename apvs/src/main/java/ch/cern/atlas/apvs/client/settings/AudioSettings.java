package ch.cern.atlas.apvs.client.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioSettings implements Serializable {

	private static final long serialVersionUID = 1L;
	private Map<String, Entry> entries = new HashMap<String, Entry>();

	public static class Entry implements Serializable {
		
		private static final long serialVersionUID = 1L;
		String  username;
		String  number;
		String  destUser;
		String  destPTU;
		String  channel;
		String  activity;
		Boolean  status;
		
		// Private Call
		Boolean onCall;
		
		// Conference Call
		String  room;
		Boolean onConference;
		Boolean mute;
		
		public Entry() {
			username = "";
			number = "";
			channel = "";
			destUser = "";
			destPTU = "";
			status = false;
			activity = "";
			
			onCall = false;
			
			room = "";
			mute = false;
			onConference = false;
		}

		public String toString() {
			return ("VoipAccount: username=" + username + " number= " + number
					+ " channel=" + channel + " destUser= " + destUser
					+ " destPTU=" + destPTU + " status=" + status 
					+ " activity=" + activity + " onCall=" + onCall 
					+ " room=" + room + " onConference="+ onConference
					+ " mute=" + mute);
		}
	}

	public AudioSettings() {
	}

	/*********************************************
	 * Methods
	 *********************************************/
	
	// Username Methods
	public String getUsername(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.username : "");
	}

	public boolean setUsername(String ptuId, String username) {
		if (entries.get(ptuId).username.equals(username)) {
			return false;
		}
		entries.get(ptuId).username = username;
		return true;
	}

	
	// Number Methods
	public String getNumber(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.number : "");
	}

	public void setNumber(String ptuId, String number) {
		entries.get(ptuId).number = number;
	}

	
	// Channel Methods
	public String getChannel(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.channel : "");
	}

	public void setChannel(String ptuId, String channel) {
		entries.get(ptuId).channel = channel;
	}

	
	// Destination User Methods
	public String getDestUser(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.destUser : "");
	}

	public String getDestPtu(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.destPTU : "");
	}
	
	public void setDestUser(String ptuId, String userDest) {
		entries.get(ptuId).destUser = userDest;
	}
	
	public void setDestPTUser(String ptuId, String userDest, String PTUDest) {
		entries.get(ptuId).destUser = userDest;
		entries.get(ptuId).destPTU  = PTUDest;
	}

	
	// Status Methods
	public Boolean getStatus(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.status : false);
	}

	public void setStatus(String ptuId, Boolean status) {
		entries.get(ptuId).status = status;
	}

	
	// On Call Status Methods
	public Boolean getOnCall(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.onCall : false);
	}

	public void setOnCall(String ptuId, Boolean onCall) {
		entries.get(ptuId).onCall = onCall;
	}

	
	// On Conference Status Methods
	public Boolean getOnConference(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.onConference : false);
	}

	public void setOnConference(String ptuId, Boolean onConference) {
		entries.get(ptuId).onConference = onConference;
	}

	
	// Room Methods
	public String getRoom(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.room : "");
	}

	public void setRoom(String ptuId, String conferenceRoom) {
		entries.get(ptuId).room = conferenceRoom;
	}
	
	
	// Mute Methods
	public boolean getMute(String ptuId){
		Entry entry = entries.get(ptuId);
		return (entry != null? entry.mute : true);
	}
	
	public void setMute(String ptuId, boolean mute){
		entries.get(ptuId).mute = mute;
	}
	
	
	// Activity Methods
	public String getActivity(String ptuId) {
		Entry entry = entries.get(ptuId);
		return (entry != null ? entry.activity : "");
	}

	public void setActivity(String ptuId, String activity) {
		entries.get(ptuId).activity = activity;
	}

	
	// PTU Ids
	public List<String> getPtuIds() {
		List<String> list = new ArrayList<String>();
		list.addAll(entries.keySet());
		return list;
	}

	
	// Add PTU
	public boolean add(String ptuId) {
		System.err.println("Adding " + ptuId);
		if (!entries.containsKey(ptuId)) {
			entries.put(ptuId, new Entry());
			return true;
		}
		return false;
	}

	
	// Check PTU is in the list
	public boolean contains(String ptuId) {
		if (entries.containsKey(ptuId)) {
			return true;
		}
		return false;
	}

	
	// Return PTU ID of given number
	public String getPtuId(String number) {
		List<String> ptuList = new ArrayList<String>(this.getPtuIds());
		for (int i = 0; i < ptuList.size(); i++) {
			if (this.getNumber(ptuList.get(i)).equals(number)) {
				return ptuList.get(i);
			}
		}
		return null;
	}

	// Return users phone numbers of given activity
	public List<String> getNumbersActivity(String activity) {
		List<String> ptuList = new ArrayList<String>(this.getPtuIds());
		List<String> numbers = new ArrayList<String>();
		for (int i = 0; i < ptuList.size(); i++) {
			if (this.getActivity(ptuList.get(i)).equals(activity)) {
				numbers.add(this.getNumber(ptuList.get(i)));
			}
		}
		return numbers;
	}
	
	// Return active channels of given activity
	public List<String> getActiveChannelsActivity(String activity) {
		List<String> ptuList = new ArrayList<String>(this.getPtuIds());
		List<String> channels = new ArrayList<String>();
		for (int i = 0; i < ptuList.size(); i++) {
			if (this.getActivity(ptuList.get(i)).equals(activity) && !this.getChannel(ptuList.get(i)).isEmpty()) {
				channels.add(this.getChannel(ptuList.get(i)));
			}
		}
		return channels;
	}
	
}
