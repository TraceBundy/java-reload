package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.ice.IPv4AddressPort.IPv4AddresPortCodec;

@ReloadCodec(IPv4AddresPortCodec.class)
public class IPv4AddressPort extends IPAddressPort {

	public IPv4AddressPort(InetAddress addr, int port) {
		super(addr, port);
	}

	@Override
	protected AddressType getAddressType() {
		return AddressType.IPv4;
	}

	public static class IPv4AddresPortCodec extends Codec<IPv4AddressPort> {

		private static final int ADDR_LENGTH = U_INT32;

		public IPv4AddresPortCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(IPv4AddressPort obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			buf.writeBytes(obj.getAddress().getAddress());
			buf.writeShort(obj.getPort());
		}

		@Override
		public IPv4AddressPort decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			return new IPv4AddressPort(decodeAddr(buf), decodePort(buf));
		}

		private InetAddress decodeAddr(ByteBuf buf) {
			byte[] tmpAddr = new byte[ADDR_LENGTH];
			buf.readBytes(tmpAddr);
			try {
				return InetAddress.getByAddress(tmpAddr);
			} catch (UnknownHostException e) {
				throw new DecoderException("Invalid IPv4 address");
			} catch (ClassCastException e) {
				throw new DecoderException("Invalid IPv4 address");
			}
		}

		public int decodePort(ByteBuf buf) {
			return buf.readUnsignedShort();
		}

	}
}
