package ch.cern.atlas.apvs.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.client.service.ServerService;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;

/**
 * @author Mark Donszelmann
 */
@SuppressWarnings("serial")
public class ServerServiceImpl extends ResponsePollService implements
		ServerService {

	private Logger log = LoggerFactory.getLogger(getClass().getName());
	
	private RemoteEventBus eventBus;
	private ServerSettingsStorage serverSettingsStorage;

	public ServerServiceImpl() {
		log.info("Creating ServerService...");
		eventBus = APVSServerFactory.getInstance().getEventBus();
		serverSettingsStorage = ServerSettingsStorage.getInstance(eventBus);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		log.info("Starting ServerService...");

		// FIXME, move this back to AudioSettings as ServerSettingsStorage is now created in ServerFactory
		//AudioUsersSettingsStorage.getInstance(eventBus);
		//AudioSupervisorSettingsStorage.getInstance(eventBus);
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	@Override
	public void isAlive() {
		// do nothing
	}
	
	/**
	 * return false if not supervisor
	 */
	@Override
	public boolean isReady(String supervisorPassword) {
		String pwd = System.getenv("APVSpwd");
		if (pwd == null) {
			log.error("NO Supervisor Password set!!! Set enviroment variable 'APVSpwd'");
			return false;
		}
		return (supervisorPassword != null) && supervisorPassword.equals(pwd);
	}
	
	@Override
	public void setPassword(String name, String password) {
		serverSettingsStorage.setPassword(name, password);
	}
}
