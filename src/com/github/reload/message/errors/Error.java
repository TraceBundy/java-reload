package com.github.reload.message.errors;

import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.errors.Error.ErrorCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

/**
 * An RELOAD error message
 * 
 */
@ReloadCodec(ErrorCodec.class)
public class Error extends Content {

	public static final Charset MSG_CHARSET = Charset.forName("US-ASCII");

	private final ErrorType error;
	private final byte[] info;

	public Error(ErrorType type, String info) {
		error = type;
		this.info = info.getBytes(MSG_CHARSET);
	}

	public Error(ErrorType type, byte[] info) {
		error = type;
		this.info = info;
	}

	public ErrorType getErrorType() {
		return error;
	}

	public String getInfo() {
		return new String(info, MSG_CHARSET);
	}

	@Override
	public String toString() {
		return "Error " + error.getCode() + " (" + error + "): " + getInfo();
	}

	@Override
	public ContentType getType() {
		return ContentType.ERROR;
	}

	public Exception toException() {
		return error.toException(this);
	}

	@Override
	public boolean isAnswer() {
		return true;
	}

	public static class ErrorCodec extends Codec<Error> {

		private static final int INFO_LENGTH_FIELD = U_INT16;

		public ErrorCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(Error obj, ByteBuf buf) throws CodecException {
			buf.writeShort(obj.error.getCode());

			Field lenFld = allocateField(buf, INFO_LENGTH_FIELD);
			buf.writeBytes(obj.info);
			lenFld.updateDataLength();
		}

		@Override
		public Error decode(ByteBuf buf) throws CodecException {
			ErrorType error = ErrorType.valueOf(buf.readShort());

			ByteBuf info = readField(buf, INFO_LENGTH_FIELD);
			byte[] infoData = new byte[info.readableBytes()];
			info.readBytes(infoData);

			return new Error(error, infoData);

		}

	}
}
