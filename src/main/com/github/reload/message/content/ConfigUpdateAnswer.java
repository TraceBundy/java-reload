package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.ConfigUpdateAnswer.ConfigUpdateAnsCodec;

@ReloadCodec(ConfigUpdateAnsCodec.class)
public class ConfigUpdateAnswer extends Content {

	@Override
	public ContentType getType() {
		return ContentType.CONFIG_UPDATE_ANS;
	}

	public static class ConfigUpdateAnsCodec extends Codec<ConfigUpdateAnswer> {

		public ConfigUpdateAnsCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(ConfigUpdateAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			// No data carried
		}

		@Override
		public ConfigUpdateAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			return new ConfigUpdateAnswer();
		}

	}

}