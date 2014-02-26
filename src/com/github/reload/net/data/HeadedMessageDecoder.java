package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.message.Header;

/**
 * Decoder used to process only the Forwarding Header part of the message
 */
public class HeadedMessageDecoder extends ByteToMessageDecoder {

	private final Codec<Header> hdrCodec;

	public HeadedMessageDecoder(Configuration conf) {
		hdrCodec = Codec.getCodec(Header.class, conf);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			HeadedMessage message = new HeadedMessage();
			message.header = hdrCodec.decode(in);
			message.payload = in.slice();
			in.retain();
			out.add(message);
		} finally {
			in.clear();
		}
	}

	@Override
	protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// Ignore
	}
}
