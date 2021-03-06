package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import javax.inject.Inject;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.SingleValueSpecifier.SingleValueSpecifierCodec;
import dagger.ObjectGraph;

@ReloadCodec(SingleValueSpecifierCodec.class)
public class SingleValueSpecifier implements ValueSpecifier {

	@Inject
	public SingleValueSpecifier() {
	}

	@Override
	public boolean isMatching(DataValue value) {
		return value instanceof SingleValue;
	}

	static class SingleValueSpecifierCodec extends Codec<SingleValueSpecifier> {

		public SingleValueSpecifierCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(SingleValueSpecifier obj, ByteBuf buf, Object... params) throws CodecException {

		}

		@Override
		public SingleValueSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			return new SingleValueSpecifier();
		}

	}

}