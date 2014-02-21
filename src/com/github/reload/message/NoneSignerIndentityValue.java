package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.NoneSignerIndentityValue.NoneSignerIdentityValueCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(NoneSignerIdentityValueCodec.class)
class NoneSignerIndentityValue extends SignerIdentityValue {

	public static class NoneSignerIdentityValueCodec extends Codec<NoneSignerIndentityValue> {

		public NoneSignerIdentityValueCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(NoneSignerIndentityValue obj, ByteBuf buf, Object... params) throws CodecException {
			// No data
		}

		@Override
		public NoneSignerIndentityValue decode(ByteBuf buf, Object... params) throws CodecException {
			return new NoneSignerIndentityValue();
		}

	}

}