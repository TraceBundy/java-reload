package com.github.reload.storage.net;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.storage.net.FindAnswer.FindAnswerCodec;

@ReloadCodec(FindAnswerCodec.class)
public class FindAnswer extends Content {

	private final List<FindKindData> data;

	public FindAnswer(List<FindKindData> data) {
		this.data = data;
	}

	@Override
	public ContentType getType() {
		return ContentType.FIND_ANS;
	}

	public List<FindKindData> getData() {
		return data;
	}

	@Override
	public String toString() {
		return "FindAnswer [data=" + data + "]";
	}

	public static class FindAnswerCodec extends Codec<FindAnswer> {

		private static final int LIST_LENGTH_FIELD = U_INT16;

		private final Codec<FindKindData> kindDataCodec;

		public FindAnswerCodec(Configuration conf) {
			super(conf);
			kindDataCodec = getCodec(FindKindData.class);
		}

		@Override
		public void encode(FindAnswer obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, LIST_LENGTH_FIELD);

			for (FindKindData d : obj.data) {
				kindDataCodec.encode(d, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FindAnswer decode(ByteBuf buf, Object... params) throws CodecException {
			ByteBuf kindDataBuf = readField(buf, LIST_LENGTH_FIELD);

			List<FindKindData> kindData = new ArrayList<FindKindData>();

			while (kindDataBuf.readableBytes() > 0) {
				kindData.add(kindDataCodec.decode(kindDataBuf));
			}

			kindDataBuf.release();

			return new FindAnswer(kindData);
		}

	}
}