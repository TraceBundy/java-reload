package com.github.reload.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import com.github.reload.Service;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.codecs.Message;
import com.github.reload.net.codecs.MessageBuilder;
import com.github.reload.net.codecs.content.AppAttachMessage;
import com.github.reload.net.codecs.content.ContentType;
import com.github.reload.net.codecs.content.Error.ErrorType;
import com.github.reload.net.codecs.header.DestinationList;
import com.github.reload.net.ice.HostCandidate;
import com.github.reload.net.ice.ICEHelper;
import com.github.reload.net.ice.NoSuitableCandidateException;
import com.github.reload.routing.MessageHandlers;
import com.github.reload.routing.MessageHandlers.MessageHandler;
import com.github.reload.services.AppAttachService.AppAttachModule;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dagger.Module;

/**
 * Provides attach functionalities for local peer and answers remote requests
 */
@Service({AppAttachModule.class})
public class AppAttachService {

	private static final Logger logger = Logger.getRootLogger();

	@Inject
	MessageBuilder msgBuilder;

	@Inject
	MessageRouter router;

	@Inject
	ICEHelper iceHelper;

	private final Map<Integer, InetSocketAddress> registeredServers = Maps.newConcurrentMap();

	@Inject
	public AppAttachService(MessageHandlers msgHandlers) {
		msgHandlers.register(this);
	}

	/**
	 * Register an application server that is already listening to the specified
	 * application-id port. Only one application-id can be registered, the
	 * registration fails if there is already a registered server for the
	 * related application-id.
	 * 
	 * @param applicationID
	 *            the application identificator of a running application server
	 */
	public void registerApplicativeServer(InetSocketAddress serviceAddr) {
		logger.info("Applicative server registered at " + new InetSocketAddress(serviceAddr.getPort()));
		registeredServers.put(serviceAddr.getPort(), serviceAddr);
	}

	public void registerApplicativeServer(int servicePort) {
		registerApplicativeServer(new InetSocketAddress(servicePort));
	}

	/**
	 * Unregister the application server related to the specified applicationID
	 * 
	 * @param applicationID
	 *            The {@link ApplicationID} of the applicative server to
	 *            unregister
	 * @return true if the server was unregistered, false otherwise
	 */
	public boolean unregisterApplicativeServer(int servicePort) {
		if (registeredServers.remove(servicePort) == null)
			return false;

		logger.info(String.format("Applicative server on port %s unregistered", servicePort));
		return true;

	}

	/**
	 * Create an application level connection to the node responsible for the
	 * specified destination using the RELOAD appAttach message. The returned
	 * sockets are generated using the default socket factory.
	 * 
	 * @param destinationList
	 *            The destination list to be used to reach the destination node
	 * @param applicationId
	 *            The {@link ApplicationID} of the application that will use
	 *            this connection
	 * 
	 * @return A socket used to communicate with the connected node
	 * @throws IOException
	 *             if some error occurs while connecting to the node
	 * @throws InterruptedException
	 *             if the caller thread is interrupted while waiting for the
	 *             response
	 */
	public ListenableFuture<InetSocketAddress> requestApplicationAddress(DestinationList destinationList, final int servicePort) {
		AppAttachMessage.Builder appAttachReqBuilder = new AppAttachMessage.Builder(servicePort);

		Message req = msgBuilder.newMessage(appAttachReqBuilder.buildRequest(), destinationList);

		ListenableFuture<Message> ansFut = router.sendRequestMessage(req);

		final SettableFuture<InetSocketAddress> addrFut = SettableFuture.create();

		Futures.addCallback(ansFut, new FutureCallback<Message>() {

			@Override
			public void onSuccess(Message result) {
				AppAttachMessage answer = (AppAttachMessage) result.getContent();
				try {
					addrFut.set(iceHelper.testAndSelectCandidate(answer.getCandidates()).getSocketAddress());
				} catch (NoSuitableCandidateException e) {
					addrFut.setException(new IOException("No suitable direct connection parameters found"));
				}
			}

			@Override
			public void onFailure(Throwable t) {
				addrFut.setException(t);
			}
		});

		return addrFut;
	}

	/**
	 * @return The application servers registered for the local peer
	 */
	public Map<Integer, InetSocketAddress> getRegisteredServers() {
		return Collections.unmodifiableMap(registeredServers);
	}

	@MessageHandler(ContentType.APPATTACH_REQ)
	private void handleAppAttachRequest(Message requestMessage) {
		AppAttachMessage request = (AppAttachMessage) requestMessage.getContent();

		InetSocketAddress addr = registeredServers.get(request.getApplicationID());

		if (addr == null) {
			router.sendError(requestMessage.getHeader(), ErrorType.NOT_FOUND, "Application " + request.getApplicationID() + " not registered");
			return;
		}

		List<? extends HostCandidate> candidates = iceHelper.getCandidates(addr);

		router.sendAnswer(requestMessage.getHeader(), new AppAttachMessage.Builder(request.getApplicationID()).candidates(candidates).buildAnswer());

	}

	@Module(injects = {AppAttachService.class}, complete = false)
	public static class AppAttachModule {

	}
}
