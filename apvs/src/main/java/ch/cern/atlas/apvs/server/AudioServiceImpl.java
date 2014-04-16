package ch.cern.atlas.apvs.server;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.live.MeetMeRoom;
import org.asteriskjava.live.MeetMeUser;
import org.asteriskjava.live.OriginateCallback;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.HangupAction;
import org.asteriskjava.manager.action.MonitorAction;
import org.asteriskjava.manager.action.SipPeersAction;
import org.asteriskjava.manager.event.BridgeEvent;
import org.asteriskjava.manager.event.ConnectEvent;
import org.asteriskjava.manager.event.DisconnectEvent;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.MeetMeEndEvent;
import org.asteriskjava.manager.event.MeetMeJoinEvent;
import org.asteriskjava.manager.event.MeetMeLeaveEvent;
import org.asteriskjava.manager.event.NewChannelEvent;
import org.asteriskjava.manager.event.PeerEntryEvent;
import org.asteriskjava.manager.event.PeerStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.SerializationException;

import ch.cern.atlas.apvs.client.AudioException;
import ch.cern.atlas.apvs.client.domain.Conference;
import ch.cern.atlas.apvs.client.event.AudioSupervisorSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.event.AudioSupervisorStatusRemoteEvent;
import ch.cern.atlas.apvs.client.event.AudioUsersSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.event.AudioUsersStatusRemoteEvent;
import ch.cern.atlas.apvs.client.event.MeetMeRemoteEvent;
import ch.cern.atlas.apvs.client.event.ServerSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.service.AudioService;
import ch.cern.atlas.apvs.client.settings.AudioSettings;
import ch.cern.atlas.apvs.client.settings.ConferenceRooms;
import ch.cern.atlas.apvs.client.settings.ServerSettings;
import ch.cern.atlas.apvs.client.settings.VoipAccount;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent;
import ch.cern.atlas.apvs.event.ConnectionStatusChangedRemoteEvent.ConnectionType;
import ch.cern.atlas.apvs.eventbus.shared.ConnectionUUIDsChangedEvent;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;
import ch.cern.atlas.apvs.eventbus.shared.RequestRemoteEvent;
import ch.cern.atlas.apvs.ptu.shared.EventChangedEvent;

public class AudioServiceImpl extends ResponsePollService implements
		AudioService, ManagerEventListener {

	private Logger log = LoggerFactory.getLogger(getClass().getName());

	private static final long serialVersionUID = 1L;

	private ManagerConnection managerConnection;

	private AsteriskServer asteriskServer;
	private AudioSettings voipAccounts;
	private VoipAccount supervisorAccount;
	private ConferenceRooms conferenceRooms;
	private List<VoipAccount> usersList;
	private List<VoipAccount> supervisorsList;

	private ScheduledExecutorService executorService;
	private ScheduledFuture<?> future;

	private boolean audioOk;
	private boolean asteriskConnected;

	// Asterisk Placing Calls Details
	private static final String CONTEXT = "internal";
	private static final int PRIORITY = 1;
	private static final int TIMEOUT = 20000;
	private static final long ASTERISK_POLLING = 5000;

	private String asteriskUrl;
	private String asteriskPwd;
	private String asteriskUser;
	private String asteriskAddress;

	private static RemoteEventBus eventBus;


	public class AsteriskConnect extends Thread {
		public void run() {
			if (!asteriskConnected) {
				audioOk = false;
				if (managerConnection != null) {
					managerConnection
							.removeEventListener(AudioServiceImpl.this);
				}

				// Asterisk Connection Manager
				ManagerConnectionFactory factory = new ManagerConnectionFactory(
						asteriskUrl, asteriskUser, asteriskPwd);
				AudioServiceImpl.this.managerConnection = factory
						.createManagerConnection();

				// Eases the communication with asterisk server
				asteriskServer = new DefaultAsteriskServer(managerConnection);

				// Event handler
				managerConnection.addEventListener(AudioServiceImpl.this);
				voipAccounts.setUnknownStatus();
				supervisorAccount.setStatus(false);
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
								voipAccounts));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorSettingsChangedRemoteEvent(
								supervisorAccount));

				log.info("Trying login in Asterisk Server on "
						+ asteriskUrl + " ...");
				try {
					login();
				} catch (AudioException e) {
					log.error("Fail to login: "+e.getMessage());
				}
			} else {
				boolean audioFormerState = audioOk;
				if (new AsteriskPing(managerConnection).isAlive()) {
					audioOk = true;
				} else {
					asteriskConnected = false;
					audioOk = false;
					log.warn("Asterisk Server: " + asteriskUrl
							+ " is not available...");
				}
				if ((audioFormerState != audioOk)) {
					ConnectionStatusChangedRemoteEvent.fire(eventBus,
							ConnectionType.audio, audioOk, "TBD");
				}
			}
		}
	}

	/*********************************************
	 * Constructor
	 * @throws SerializationException 
	 **********************************************/
	public AudioServiceImpl() throws SerializationException {
		if (eventBus != null)
			return;

		log.info("Creating AudioService...");
		eventBus = APVSServerFactory.getInstance().getEventBus();
		ServerSettingsStorage.getInstance(eventBus);
		AudioUsersSettingsStorage.getInstance(eventBus);
		AudioSupervisorSettingsStorage.getInstance(eventBus);

		executorService = Executors.newSingleThreadScheduledExecutor();

		RequestRemoteEvent.register(this, eventBus, new RequestRemoteEvent.Handler() {

			@Override
			public void onRequestEvent(RequestRemoteEvent event) {
				String type = event.getRequestedClassName();

				if (type.equals(ConnectionStatusChangedRemoteEvent.class
						.getName())) {
					ConnectionStatusChangedRemoteEvent.fire(eventBus,
							ConnectionType.audio, audioOk, "TBD");
				}
			}
		});
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		log.info("Starting Audio Service...");

		voipAccounts = new AudioSettings();
		supervisorAccount = new VoipAccount();
		conferenceRooms = new ConferenceRooms();
		audioOk = false;
		asteriskConnected = false;

		ServerSettingsChangedRemoteEvent.subscribe(this, eventBus,
				new ServerSettingsChangedRemoteEvent.Handler() {

					@Override
					public void onServerSettingsChanged(
							ServerSettingsChangedRemoteEvent event) {
						ServerSettings settings = event.getServerSettings();

						if (settings != null) {

							String url = settings
									.get(ServerSettings.Entry.audioUrl
											.toString());
							String pwd = ServerSettingsStorage
									.getInstance(eventBus)
									.getPasswords()
									.get(ServerSettings.Entry.audioUrl
											.toString());

							if (((url != null) && !url.equals(asteriskAddress))
									|| ((pwd != null) && !pwd
											.equals(asteriskPwd))) {
								if (managerConnection != null) {
									managerConnection
											.removeEventListener(AudioServiceImpl.this);
									future.cancel(false);
								}

								asteriskAddress = url;
								int pos = asteriskAddress.indexOf("@");
								if (pos > 0) {
									asteriskUrl = url.substring(pos + 1);
									asteriskUser = url.substring(0, pos);
									asteriskPwd = pwd;
									asteriskConnected = false;
									if ((asteriskUrl != null)
											&& (asteriskUser != null))
										future = executorService
												.scheduleAtFixedRate(
														new AsteriskConnect(),
														0, ASTERISK_POLLING,
														TimeUnit.MILLISECONDS);
								} else {
									log.error
											("Audio URL \""
													+ asteriskAddress
													+ "\" does not follow the correct format -> username@asterisk_hostname");
								}
							}
						}
					}
				});

		// FOR #281, uids may still change in the future, related to #284, at
		// this moment any reload generated more uids... none is taken away on
		// disconnect
		ConnectionUUIDsChangedEvent.subscribe(this, eventBus,
				new ConnectionUUIDsChangedEvent.Handler() {

					@Override
					public void onConnectionUUIDchanged(
							ConnectionUUIDsChangedEvent event) {
						log.error
								("****** Supervisor Connect or Disconnect "
										+ event);
					}
				});

		AudioUsersSettingsChangedRemoteEvent.subscribe(this, eventBus,
				new AudioUsersSettingsChangedRemoteEvent.Handler() {

					@Override
					public void onAudioUsersSettingsChanged(
							AudioUsersSettingsChangedRemoteEvent event) {
						voipAccounts = event.getAudioSettings();
					}
				});

		AudioSupervisorSettingsChangedRemoteEvent.subscribe(this, eventBus,
				new AudioSupervisorSettingsChangedRemoteEvent.Handler() {

					@Override
					public void onAudioSupervisorSettingsChanged(
							AudioSupervisorSettingsChangedRemoteEvent event) {
						supervisorAccount = event.getSupervisorSettings();
					}
				});

		MeetMeRemoteEvent.subscribe(this, eventBus, new MeetMeRemoteEvent.Handler() {

			@Override
			public void onMeetMeEvent(MeetMeRemoteEvent event) {
				conferenceRooms = event.getConferenceRooms();
			}
		});

		EventChangedEvent.register(eventBus, new EventChangedEvent.Handler() {

			@Override
			public void onEventChanged(EventChangedEvent event) {
				String type = event.getEvent().getEventType();
				if (type.equals("Panic")) {
					List<String> channels = new ArrayList<String>();
					channels.add(supervisorAccount.getChannel());

					String ptuId = event.getEvent().getDevice().getName();
					channels.add(voipAccounts.getChannel(ptuId));

					// Hangup Supervisor and PTU User from active calls
					try {
						hangupMultiple(channels);
					} catch (AudioException e) {
						log.error("Failed to Hangup Channel"
								+ e.getMessage());
					}
					call(voipAccounts.getNumber(ptuId),
							supervisorAccount.getNumber());
				}
			}
		});

	}

	public void login() throws AudioException {
		try {
			managerConnection.login();
		} catch (IllegalStateException e) {
			throw new AudioException(e.getMessage());
		} catch (IOException e) {
			throw new AudioException(e.getMessage());
		} catch (AuthenticationFailedException e) {
			throw new AudioException("Failed login to Asterisk Manager: "
					+ e.getMessage());
		} catch (TimeoutException e) {
			throw new AudioException("Login to Asterisk Timeout: "
					+ e.getMessage());
		}
	}

	public void logoff() throws AudioException {
		try {
			managerConnection.logoff();
		} catch (IllegalStateException e) {
			throw new AudioException(e.getMessage());
		}
	}

	public String filterNumber(String channel) {
		return channel.substring(0, channel.indexOf("-"));
	}

	/*********************************************
	 * RPC Methods
	 *********************************************/
	@Override
	public void call(String callerOriginater, String callerDestination) {
		asteriskServer.originateToExtension(callerOriginater, CONTEXT,
				callerDestination, PRIORITY, TIMEOUT);
	}

	@Override
	public void hangup(String channel) throws AudioException {
		// HangupAction hangupCall = new HangupAction(channel);
		try {
			managerConnection.sendAction(new HangupAction(channel));
		} catch (IllegalArgumentException e) {
			throw new AudioException(e.getMessage());
		} catch (IllegalStateException e) {
			throw new AudioException(e.getMessage());
		} catch (IOException e) {
			throw new AudioException(e.getMessage());
		} catch (TimeoutException e) {
			throw new AudioException("Timeout: " + e.getMessage());
		}
	}

	@Override
	public void hangupMultiple(List<String> channels) throws AudioException {
		for (int i = 0; i < channels.size(); i++) {
			hangup(channels.get(i));
		}
	}

	@Override
	public void newConference(List<String> participantsNumber) {
		MeetMeRoom room = asteriskServer.getMeetMeRoom(conferenceRooms
				.newRoom());
		for (int i = 0; i < participantsNumber.size(); i++) {
			addToConference(participantsNumber.get(i), room.getRoomNumber()
					+ ",qdr");
		}
	}

	@Override
	public void addToConference(String participant, String roomAndParam) {
		asteriskServer.originateToApplicationAsync(participant, "MeetMe",
				roomAndParam, TIMEOUT, new OriginateCallback() {

					@Override
					public void onSuccess(AsteriskChannel arg0) {
						log.info("User added to conference call...");
					}

					@Override
					public void onNoAnswer(AsteriskChannel arg0) {
						log.warn("No answer...");
					}

					@Override
					public void onFailure(LiveException arg0) {
						log.error("Failure to load...");
					}

					@Override
					public void onDialing(AsteriskChannel arg0) {
						log.info("User currently dialing someone...");
					}

					@Override
					public void onBusy(AsteriskChannel arg0) {
						log.info("User Busy...");
					}
				});
	}

	@Override
	public void muteUser(String room, String channel, String ptu) {
		MeetMeRoom meetMeRoom = asteriskServer.getMeetMeRoom(room);
		List<MeetMeUser> meetMeUsersList = (List<MeetMeUser>) meetMeRoom
				.getUsers();
		for (int i = 0; i < meetMeUsersList.size(); i++) {
			if (meetMeUsersList.get(i).getChannel().getName().equals(channel)) {
				meetMeUsersList.get(i).mute();
				voipAccounts.setMute(ptu, true);
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
								voipAccounts));
			}
		}
	}

	@Override
	public void unMuteUser(String room, String channel, String ptu){
		MeetMeRoom meetMeRoom = asteriskServer.getMeetMeRoom(room);
		List<MeetMeUser> meetMeUsersList = (List<MeetMeUser>) meetMeRoom
				.getUsers();
		for (int i = 0; i < meetMeUsersList.size(); i++) {
			if (meetMeUsersList.get(i).getChannel().getName().equals(channel)) {
				meetMeUsersList.get(i).unmute();
				voipAccounts.setMute(ptu, false);
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
								voipAccounts));
			}
		}
	}

	@Override
	public void usersList() throws AudioException {
		usersList = new ArrayList<VoipAccount>();
		supervisorsList = new ArrayList<VoipAccount>();
		if (managerConnection != null) {
			try {
				managerConnection.sendAction(new SipPeersAction());
			} catch (IllegalArgumentException e) {
				usersList.add(new VoipAccount("Not available"));
				supervisorsList.add(new VoipAccount("Not available"));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersStatusRemoteEvent(usersList));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorStatusRemoteEvent(
								supervisorsList));
				throw new AudioException(e.getMessage());
			} catch (IllegalStateException e) {
				usersList.add(new VoipAccount("Not available"));
				supervisorsList.add(new VoipAccount("Not available"));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersStatusRemoteEvent(usersList));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorStatusRemoteEvent(
								supervisorsList));
				throw new AudioException(e.getMessage());
			} catch (IOException e) {
				usersList.add(new VoipAccount("Not available"));
				supervisorsList.add(new VoipAccount("Not available"));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersStatusRemoteEvent(usersList));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorStatusRemoteEvent(
								supervisorsList));
				throw new AudioException(e.getMessage());
			} catch (TimeoutException e) {
				usersList.add(new VoipAccount("Not available"));
				supervisorsList.add(new VoipAccount("Not available"));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersStatusRemoteEvent(usersList));
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorStatusRemoteEvent(
								supervisorsList));
				throw new AudioException("Timeout: " + e.getMessage());
			}
		} else {
			usersList.add(new VoipAccount("Not available"));
			supervisorsList.add(new VoipAccount("Not available"));
			((RemoteEventBus) eventBus)
					.fireEvent(new AudioUsersStatusRemoteEvent(usersList));
			((RemoteEventBus) eventBus)
					.fireEvent(new AudioSupervisorStatusRemoteEvent(
							supervisorsList));
		}
	}

	/*********************************************
	 * Event Handler
	 *********************************************/
	@Override
	public void onManagerEvent(ManagerEvent event) {
		// NewChannelEvent
		if (event instanceof NewChannelEvent) {
			NewChannelEvent channel = (NewChannelEvent) event;
			newChannelEvent(channel);
			// BridgeEvent
		} else if (event instanceof BridgeEvent) {
			BridgeEvent bridge = (BridgeEvent) event;
			bridgeEvent(bridge);
			// HangupEvent
		} else if (event instanceof HangupEvent) {
			HangupEvent hangup = (HangupEvent) event;
			hangupEvent(hangup);
			// PeerStatusEvent
		} else if (event instanceof PeerStatusEvent) {
			PeerStatusEvent peer = (PeerStatusEvent) event;
			peerStatusEvent(peer);
			// MeetMeJoinEvent
		} else if (event instanceof MeetMeJoinEvent) {
			MeetMeJoinEvent meetJoin = (MeetMeJoinEvent) event;
			meetMeJoin(meetJoin);
			// MeetMeLeaveEvent
		} else if (event instanceof MeetMeLeaveEvent) {
			MeetMeLeaveEvent meetLeave = (MeetMeLeaveEvent) event;
			meetMeLeave(meetLeave);
			// MeetMeEndEvent
		} else if (event instanceof MeetMeEndEvent) {
			MeetMeEndEvent meetEnd = (MeetMeEndEvent) event;
			meetMeEnd(meetEnd);
			// PeerEntryEvent
		} else if (event instanceof PeerEntryEvent) {
			PeerEntryEvent peer = (PeerEntryEvent) event;
			peerEntryEvent(peer);
			// ConnectEvent
		} else if (event instanceof ConnectEvent) {
			asteriskConnected = true;
			log.info("Connected to Asterisk server");
			// DisconnectEvent
		} else if (event instanceof DisconnectEvent) {
			asteriskConnected = false;
			log.info("Disconnected from Asterisk server");
		}
	}

	/*********************************************
	 * Event Methods
	 *********************************************/
	// New Channel
	public void newChannelEvent(NewChannelEvent event) {
		DateFormat dateFormat = new SimpleDateFormat("yyMMdd-HH.mm.ss");
		Date date = new Date();
		log.info(dateFormat.format(date));
		MonitorAction record = new MonitorAction();

		String channel = event.getChannel();
		String number = filterNumber(channel);
		if (number.matches("SIP/1[0-9]{3}")) {
			String ptuId = voipAccounts.getPtuId(number);
			if (ptuId != null) {
				voipAccounts.setChannel(ptuId, channel);
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
								voipAccounts));
				record = new MonitorAction(channel, voipAccounts
						.getIntervention(ptuId).getId()
						+ "-"
						+ dateFormat.format(date), "wav", true);
			}
		} else if (number.matches("SIP/2[0-9]{3}")) {
			if (number.equals(supervisorAccount.getAccount())) {
				log.info("Channel for supervisor created");
				supervisorAccount.setChannel(channel);
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorSettingsChangedRemoteEvent(
								supervisorAccount));
				record = new MonitorAction(channel, "Supervisor -"
						+ dateFormat.format(date), "wav", true);
			}
		} else {
			log.error("#NewChannelEvent - NO PTU FOUND WITH NUMBER "
					+ number);
			return;
		}

		try {
			managerConnection.sendAction(record);
		} catch (IllegalArgumentException e) {
			e.getMessage();
		} catch (IllegalStateException e) {
			e.getMessage();
		} catch (IOException e) {
			e.getMessage();
		} catch (TimeoutException e) {
			e.getMessage();
		}

		// TODO
		// asteriskServer.originateToApplication(channel, "MixMonitor",
		// "Audiozinho.wav,V(-1)v(2) a,/", TIMEOUT);

	}

	// Bridge of Call Channels - Event only valid for private call
	public void bridgeEvent(BridgeEvent event) {
		String channel1 = event.getChannel1();
		String number1 = filterNumber(channel1);

		String channel2 = event.getChannel2();
		String number2 = filterNumber(channel2);

		if (number1.matches("SIP/1[0-9]{3}")) {
			String ptuId1 = voipAccounts.getPtuId(number1);
			if (ptuId1 != null) {
				if (number2.matches("SIP/1[0-9]{3}")) {
					String ptuId2 = voipAccounts.getPtuId(number2);
					voipAccounts.setDestPTUser(ptuId1,
							voipAccounts.getUsername(ptuId2), ptuId2);
					voipAccounts.setOnCall(ptuId1, true);
					voipAccounts.setDestPTUser(ptuId2,
							voipAccounts.getUsername(ptuId1), ptuId1);
					voipAccounts.setOnCall(ptuId2, true);

				} else if (number2.matches("SIP/2[0-9]{3}")) {
					voipAccounts.setDestPTUser(ptuId1, "Supervisor",
							"Supervisor");
					voipAccounts.setOnCall(ptuId1, true);
					supervisorAccount.setDestPTU(ptuId1);
					supervisorAccount.setDestUser(voipAccounts
							.getUsername(ptuId1));
					supervisorAccount.setOnCall(true);
				}
			} else {
				log.error("#BridgeEvent - ERROR IN ASSIGNMENT "
						+ number1 + " & " + number2);
				return;
			}
		} else if (number1.matches("SIP/2[0-9]{3}")) {
			if (number2.matches("SIP/1[0-9]{3}")) {
				String ptuId2 = voipAccounts.getPtuId(number2);
				if (ptuId2 != null) {
					voipAccounts.setDestPTUser(ptuId2, "Supervisor",
							"Supervisor");
					voipAccounts.setOnCall(ptuId2, true);
					supervisorAccount.setDestPTU(ptuId2);
					supervisorAccount.setDestUser(voipAccounts
							.getUsername(ptuId2));
					supervisorAccount.setOnCall(true);
				} else {
					log.error("#BridgeEvent - ERROR IN ASSIGNMENT "
							+ number1 + " & " + number2);
					return;
				}

			}
		} else {
			log.error("#BridgeEvent - ERROR IN ASSIGNMENT " + number1
					+ " & " + number2);
			return;
		}
		((RemoteEventBus) eventBus)
				.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
						voipAccounts));
		((RemoteEventBus) eventBus)
				.fireEvent(new AudioSupervisorSettingsChangedRemoteEvent(
						supervisorAccount));
	}

	// Hangup Call Event
	public void hangupEvent(HangupEvent event) {
		String channel = event.getChannel();
		String number = filterNumber(channel);

		if (number.equals(supervisorAccount.getAccount())) {
			supervisorAccount.setChannel("");
			supervisorAccount.setDestPTU(null);
			// FIXME
			supervisorAccount.setDestUser(voipAccounts.getUsername(""));
			supervisorAccount.setOnCall(false);
			supervisorAccount.setOnConference(false);
			((RemoteEventBus) eventBus)
					.fireEvent(new AudioSupervisorSettingsChangedRemoteEvent(
							supervisorAccount));
			return;
		} else {
			String ptuId = voipAccounts.getPtuId(number);
			if (ptuId != null) {
				voipAccounts.setChannel(ptuId, "");
				voipAccounts.setDestPTUser(ptuId, "", "");
				voipAccounts.setOnCall(ptuId, false);
				voipAccounts.setOnConference(ptuId, false);
				voipAccounts.setRoom(ptuId, "");
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
								voipAccounts));
				return;
			}
		}
		log.error("#HangupEvent - NO DEVICE FOUND WITH NUMBER "
				+ number);
	}

	// Users Register and Unregister
	public void peerStatusEvent(PeerStatusEvent event) {
		String number = event.getPeer();
		String peerStatus = event.getPeerStatus();

		if (number.matches("SIP/1[0-9]{3}")) {
			if (voipAccounts.getPtuId(number) != null) {
				String ptuId = voipAccounts.getPtuId(number);
				if (peerStatus.equals("Registered"))
					voipAccounts.setStatus(ptuId, true);
				else
					voipAccounts.setStatus(ptuId, false);

				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
								voipAccounts));
				return;
			}
		} else if (number.matches("SIP/2[0-9]{3}")) {
			if (number.equals(supervisorAccount.getAccount())) {
				if (peerStatus.equals("Registered"))
					supervisorAccount.setStatus(true);
				else
					supervisorAccount.setStatus(false);

				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorSettingsChangedRemoteEvent(
								supervisorAccount));
				return;
			}
		}

				log.error("#PeerStatusEvent - NO PTU OR SUPERVISOR FOUND OR ASSIGNED WITH NUMBER "
						+ number);
	}

	// MeetMe Join Event
	public void meetMeJoin(MeetMeJoinEvent event) {
		String channel = event.getChannel();
		String room = event.getMeetMe();
		String number = filterNumber(channel);
		String ptuId = voipAccounts.getPtuId(number);

		if (!conferenceRooms.roomExist(room)) {
			conferenceRooms.put(room, new Conference());
			conferenceRooms.get(room).setActivity(
					voipAccounts.getActivity(ptuId));
		}
		if (number.matches("SIP/1[0-9]{3}")) {
			if (ptuId != null) {
				voipAccounts.setChannel(ptuId, channel);
				voipAccounts.setRoom(ptuId, room);
				voipAccounts.setOnConference(ptuId, true);
				conferenceRooms.get(room).setUserNum(
						conferenceRooms.get(room).getUserNum() + 1);
				conferenceRooms.get(room).addPtu(ptuId);
				conferenceRooms.get(room).addUsername(
						voipAccounts.getUsername(ptuId));

				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
								voipAccounts));
				((RemoteEventBus) eventBus).fireEvent(new MeetMeRemoteEvent(
						conferenceRooms));
				return;
			}
		} else if (number.matches("SIP/2[0-9]{3}")) {
			if (number.equals(supervisorAccount.getAccount())) {
				supervisorAccount.setOnConference(true);
				supervisorAccount.setRoom(room);
				supervisorAccount.setChannel(channel);
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorSettingsChangedRemoteEvent(
								supervisorAccount));
				return;
			}
		}

		log.error("#MeetMeJoinEvent - NO PTU FOUND WITH NUMBER "
				+ number);
	}

	// MeetMe Leave Event
	public void meetMeLeave(MeetMeLeaveEvent event) {

		String channel = event.getChannel();
		String room = event.getMeetMe();
		String number = filterNumber(channel);
		String ptuId = voipAccounts.getPtuId(number);

		if (number.matches("SIP/1[0-9]{3}")) {
			if (ptuId != null) {
				if (voipAccounts.getChannel(ptuId).equals(channel))
					voipAccounts.setChannel(ptuId, "");
				if (voipAccounts.getRoom(ptuId).equals(room))
					voipAccounts.setRoom(ptuId, "	");

				voipAccounts.setOnConference(ptuId, false);

				conferenceRooms.get(room).setUserNum(
						conferenceRooms.get(room).getUserNum() - 1);
				int index = conferenceRooms.get(room).getPtus()
						.indexOf(ptuId);
				conferenceRooms.get(room).getPtus().remove(index);
				conferenceRooms.get(room).getUsernames().remove(index);

				((RemoteEventBus) eventBus)
						.fireEvent(new AudioUsersSettingsChangedRemoteEvent(
								voipAccounts));
				((RemoteEventBus) eventBus).fireEvent(new MeetMeRemoteEvent(
						conferenceRooms));
				return;
			}
		} else if (number.matches("SIP/2[0-9]{3}")) {
			if (number.equals(supervisorAccount.getAccount())) {
				supervisorAccount.setOnConference(false);
				supervisorAccount.setRoom("");
				supervisorAccount.setChannel("");
				((RemoteEventBus) eventBus)
						.fireEvent(new AudioSupervisorSettingsChangedRemoteEvent(
								supervisorAccount));
				return;
			}
		}
		log.error("#MeetMeLeaveEvent - NO PTU FOUND WITH NUMBER "
				+ number);
	}

	// MeetMe End Event
	public void meetMeEnd(MeetMeEndEvent event) {
		String room = event.getMeetMe();
		conferenceRooms.remove(room);
	}

	// Peer Entry Event
	public void peerEntryEvent(PeerEntryEvent event) {
		String number = "SIP/" + event.getObjectName();
		Boolean status = (event.getIpAddress() != null ? true : false);
		// Workers Accounts
		if (event.getObjectName().matches("1[0-9]{3}")) {
			usersList.add(new VoipAccount(number, status));
			((RemoteEventBus) eventBus)
					.fireEvent(new AudioUsersStatusRemoteEvent(usersList));
			// Supervisor Accounts
		} else if (event.getObjectName().matches("2[0-9]{3}")) {
			supervisorsList.add(new VoipAccount(number, status));
			((RemoteEventBus) eventBus)
					.fireEvent(new AudioSupervisorStatusRemoteEvent(
							supervisorsList));
		}
	}

}
