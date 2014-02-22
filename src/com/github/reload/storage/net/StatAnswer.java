package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.StatAnswer.StatAnswerCodec;

@ReloadCodec(StatAnswerCodec.class)
public class StatAnswer extends Content {

	private final List<StatKindResponse> responses;

	public StatAnswer(List<StatKindResponse> responses) {
		this.responses = responses;
	}

	@Override
	public ContentType getType() {
		return ContentType.STAT_ANS;
	}

	public static class StatAnswerCodec extends Codec<StatAnswer> {

		private static final int RESPONSES_LENGTH_FIELD = U_INT32;

		private final Codec<StatKindResponse> respCodec;

		public StatAnswerCodec(Context context) {
			super(context);
			respCodec = getCodec(StatKindResponse.class);
		}

		@Override
		public void encode(StatAnswer obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			Field lenFld = allocateField(buf, RESPONSES_LENGTH_FIELD);

			for (StatKindResponse r : obj.responses) {
				respCodec.encode(r, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StatAnswer decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			ByteBuf resposeData = readField(buf, RESPONSES_LENGTH_FIELD);
			List<StatKindResponse> responses = new ArrayList<StatKindResponse>();

			while (resposeData.readableBytes() > 0) {
				responses.add(respCodec.decode(buf));
			}

			resposeData.release();

			return new StatAnswer(responses);
		}
	}
}