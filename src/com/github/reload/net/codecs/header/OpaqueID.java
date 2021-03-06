package com.github.reload.net.codecs.header;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.header.OpaqueID.OpaqueIdCodec;

/**
 * An opaque id used to substitute other ids by the local peer (also known as
 * compressed id)
 * 
 */
@ReloadCodec(OpaqueIdCodec.class)
public class OpaqueID extends RoutableID {

	private final byte[] id;

	private OpaqueID(byte[] id) {
		this.id = id;
	}

	public static OpaqueID valueOf(byte[] id) {
		return new OpaqueID(id);
	}

	@Override
	public byte[] getData() {
		return id;
	}

	@Override
	public DestinationType getType() {
		return DestinationType.OPAQUEID;
	}

	static class OpaqueIdCodec extends Codec<OpaqueID> {

		private static final int OPAQUE_LENGTH_FIELD = U_INT8;

		public OpaqueIdCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(OpaqueID obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			Field lenFld = allocateField(buf, OPAQUE_LENGTH_FIELD);
			buf.writeBytes(obj.id);
			lenFld.updateDataLength();
		}

		@Override
		public OpaqueID decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			ByteBuf data = readField(buf, OPAQUE_LENGTH_FIELD);
			byte[] id = new byte[data.readableBytes()];
			data.readBytes(id);
			data.release();
			return valueOf(id);
		}

	}
}
