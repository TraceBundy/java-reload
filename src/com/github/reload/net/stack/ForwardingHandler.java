package com.github.reload.net.stack;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import com.github.reload.Overlay;
import com.github.reload.conf.Configuration;
import com.github.reload.net.ConnectionManager;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.codecs.ForwardMessage;
import com.github.reload.net.codecs.content.Error.ErrorType;
import com.github.reload.net.codecs.header.DestinationList;
import com.github.reload.net.codecs.header.NodeID;
import com.github.reload.net.codecs.header.OpaqueID;
import com.github.reload.net.codecs.header.RoutableID;
import com.github.reload.routing.PathCompressor;
import com.github.reload.routing.PathCompressor.UnknownOpaqueIdException;
import com.github.reload.routing.TopologyPlugin;

@Sharable
@Singleton
public class ForwardingHandler extends ChannelInboundHandlerAdapter {

	private static final Logger l = Logger.getRootLogger();

	@Inject
	Overlay overlay;

	@Inject
	MessageRouter router;

	@Inject
	TopologyPlugin plugin;

	@Inject
	ConnectionManager connMgr;

	@Inject
	Configuration conf;

	@Inject
	PathCompressor compressor;

	@Inject
	public ForwardingHandler() {
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ForwardMessage message = (ForwardMessage) msg;

		DestinationList destList = message.getHeader().getDestinationList();

		NodeID localId = overlay.getLocalNodeId();

		try {
			processDestination(destList, localId);
		} catch (UnknownOpaqueIdException e) {
			l.debug(e.getMessage());
			return;
		}

		if (!isValidDestination(destList, localId)) {
			l.debug(String.format("Invalid message %#x destination dropped...", message.getHeader().getTransactionId()));
			return;
		}

		RoutableID nextHop = destList.get(0);

		if (isLocalPeerResponsible(nextHop)) {
			l.debug(String.format("Passing message %#x for local peer to upper layer...", message.getHeader().getTransactionId()));
			ctx.fireChannelRead(message);
		} else {

			short ttl = message.getHeader().getTimeToLive();
			if (ttl == 0 || ttl > conf.get(Configuration.INITIAL_TTL)) {
				if (ttl == 0) {
					router.sendError(message.getHeader(), ErrorType.TLL_EXCEEDED, "Expired message TTL");
				} else if (ttl > conf.get(Configuration.INITIAL_TTL)) {
					router.sendError(message.getHeader(), ErrorType.TLL_EXCEEDED, "Message TTL greater than initial overlay TTL");
				}

				l.debug(String.format("Expired message %#x not forwarded", message.getHeader().getTransactionId()));
				return;
			}

			l.debug(String.format("Forwarding message %#x to neighbor...", message.getHeader().getTransactionId()));
			router.forwardMessage(message);
		}
	}

	private void processDestination(DestinationList destList, NodeID localId) throws UnknownOpaqueIdException {
		RoutableID nextHop = destList.get(0);

		switch (nextHop.getType()) {
			case OPAQUEID :
				decompressDestinationList(destList);
				processDestination(destList, localId);

			case NODEID :
				if (nextHop.equals(localId) && destList.size() > 1) {
					destList.remove(0);
					processDestination(destList, localId);
				}

			case RESOURCEID :
				return;
		}

		throw new IllegalStateException();
	}

	private boolean isValidDestination(DestinationList destList, NodeID localId) {
		RoutableID nextHop = destList.get(0);

		switch (nextHop.getType()) {
			case OPAQUEID :
				return true;

			case RESOURCEID :
				return destList.size() == 1;

			case NODEID :
				if (nextHop.equals(localId) || nextHop.isWildcard())
					return true;

				return connMgr.isNeighbor((NodeID) nextHop);
		}

		throw new IllegalStateException();
	}

	private void decompressDestinationList(DestinationList destList) throws UnknownOpaqueIdException {
		OpaqueID compressed = (OpaqueID) destList.get(0);

		DestinationList original = compressor.decompress(compressed);
		destList.addAll(0, original);
	}

	private boolean isLocalPeerResponsible(RoutableID dest) {

		if (dest.isWildcard())
			return true;

		if (dest.equals(overlay.getLocalNodeId()))
			return true;

		return plugin.isLocalPeerResponsible(dest);
	}
}
