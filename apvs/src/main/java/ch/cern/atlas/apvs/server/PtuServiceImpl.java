package ch.cern.atlas.apvs.server;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.client.domain.HistoryMap;
import ch.cern.atlas.apvs.client.event.ServerSettingsChangedRemoteEvent;
import ch.cern.atlas.apvs.client.service.PtuService;
import ch.cern.atlas.apvs.client.service.ServiceException;
import ch.cern.atlas.apvs.client.settings.ServerSettings;
import ch.cern.atlas.apvs.domain.Measurement;
import ch.cern.atlas.apvs.domain.Order;
import ch.cern.atlas.apvs.eventbus.shared.RemoteEventBus;
import ch.cern.atlas.apvs.ptu.server.PtuPipelineFactory;

/**
 * @author Mark Donszelmann
 */
@SuppressWarnings("serial")
public class PtuServiceImpl extends DbServiceImpl implements PtuService {

	private Logger log = LoggerFactory.getLogger(getClass().getName());

	private static final int DEFAULT_PTU_PORT = 4005;

	private String ptuUrl;

	private RemoteEventBus eventBus;
	private PtuClientHandler ptuClientHandler;

	public PtuServiceImpl() {
		log.info("Creating PtuService...");
		eventBus = APVSServerFactory.getInstance().getEventBus();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		log.info("Starting PtuService...");

		//FIXME - Change the declaring location before ServerSettingsChangedRemoteEvent
		// Configure the client.
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		//FIXME - Change the declaring location before ServerSettingsChangedRemoteEvent
		ptuClientHandler = new PtuClientHandler(bootstrap, eventBus);
		
		
	
		
		
		ServerSettingsChangedRemoteEvent.subscribe(eventBus,
				new ServerSettingsChangedRemoteEvent.Handler() {

					@Override
					public void onServerSettingsChanged(
							ServerSettingsChangedRemoteEvent event) {
						ServerSettings settings = event.getServerSettings();
						if (settings != null) {
							String url = settings
									.get(ServerSettings.Entry.ptuUrl.toString());
							if ((url != null) && !url.equals(ptuUrl)) {
								ptuUrl = url;
								String[] s = ptuUrl.split(":", 2);
								String host = s[0];
								int port = s.length > 1 ? Integer
										.parseInt(s[1]) : DEFAULT_PTU_PORT;

								log.info("Setting PTU to " + host + ":" + port);
								//#FIXME
								ptuClientHandler.connect(new InetSocketAddress(
										host, port));
							}
						}
					}
				});
		
		
		 
		Timer timer = new HashedWheelTimer();

		// Configure the pipeline factory.
		bootstrap.setPipelineFactory(new PtuPipelineFactory(timer, ptuClientHandler));
		
	}
	
	@Override
	public List<Measurement> getMeasurements(List<String> ptuIdList, String name)
			throws ServiceException {
		try {
			return dbHandler.getMeasurements(ptuIdList, name);
		} catch (SQLException e) {
			throw new ServiceException(e.getMessage());
		}
	}

	@Override
	public List<Measurement> getMeasurements(String ptuId, String name)
			throws ServiceException {
		try {
			return dbHandler.getMeasurements(ptuId, name);
		} catch (SQLException e) {
			throw new ServiceException(e.getMessage());
		}
	}

	@Override
	public HistoryMap getHistoryMap(List<String> ptuIdList, Date from)
			throws ServiceException {
		try {
			return dbHandler.getHistoryMap(ptuIdList, from);
		} catch (SQLException e) {
			throw new ServiceException(e.getMessage());
		}
	}
		
	@Override
	public void handleOrder(Order order) throws ServiceException {
		System.err.println("Handle "+order);
		
		ptuClientHandler.sendOrder(order);
	}
}
