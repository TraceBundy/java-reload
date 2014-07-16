package com.github.reload.net.encoders.header;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.content.errors.ErrorType;
import com.github.reload.net.encoders.header.ForwardingOption.ForwardingOptionType;

public class ForwardingOptionCodec extends Codec<ForwardingOption> {

	private static final int OPTION_LENGTH_FIELD = U_INT16;

	private static final byte FWD_CRITICAL_MASK = 0x01;
	private static final byte DST_CRITICAL_MASK = 0x02;
	private static final byte RES_COPY_MASK = 0x04;

	private final Codec<UnknownForwardingOption> unknownFwdCodec;

	public ForwardingOptionCodec(Configuration conf) {
		super(conf);
		unknownFwdCodec = getCodec(UnknownForwardingOption.class);
	}

	@Override
	public void encode(ForwardingOption obj, ByteBuf buf, Object... params) throws CodecException {
		buf.writeByte(obj.getType().code);
		buf.writeByte(getFlags(obj));

		Field lenFld = allocateField(buf, OPTION_LENGTH_FIELD);

		switch (obj.getType()) {
			case UNKNOWN_OPTION :
				unknownFwdCodec.encode((UnknownForwardingOption) obj, buf);
				break;

			default :
				break;
		}

		lenFld.updateDataLength();
	}

	private byte getFlags(ForwardingOption obj) {
		byte flags = 0;
		if (obj.isForwardCritical) {
			flags = FWD_CRITICAL_MASK;
		}
		if (obj.isDestinationCritical) {
			flags |= DST_CRITICAL_MASK;
		}
		if (obj.isResponseCopy) {
			flags |= RES_COPY_MASK;
		}
		return flags;
	}

	@Override
	public ForwardingOption decode(ByteBuf buf, Object... params) throws CodecException {
		ForwardingOptionType type = ForwardingOptionType.valueOf(buf.readByte());

		ForwardingOption option = null;

		byte flags = buf.readByte();

		boolean isFwdCritical = (flags & FWD_CRITICAL_MASK) == FWD_CRITICAL_MASK;
		boolean isDstCritical = (flags & DST_CRITICAL_MASK) == DST_CRITICAL_MASK;
		boolean isRspCopy = (flags & RES_COPY_MASK) == RES_COPY_MASK;

		if (type == ForwardingOptionType.UNKNOWN_OPTION && (isFwdCritical || isDstCritical))
			throw new UnsupportedFwdOptionException("Unknown forwarding option");

		ByteBuf data = readField(buf, OPTION_LENGTH_FIELD);

		switch (type) {
			case UNKNOWN_OPTION :
				option = unknownFwdCodec.decode(data);
				break;
		// May be extended
		}

		assert (option != null);

		option.isForwardCritical = isFwdCritical;
		option.isDestinationCritical = isDstCritical;
		option.isResponseCopy = isRspCopy;

		return option;
	}

	/**
	 * Indicates an unsupported forwarding option critical for header processing
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class UnsupportedFwdOptionException extends CodecException {

		public UnsupportedFwdOptionException(String message) {
			super(message);
		}

		@Override
		public ErrorType getErrorType() {
			return ErrorType.UNSUPPORTED_FWD_OPTION;
		}
	}
}