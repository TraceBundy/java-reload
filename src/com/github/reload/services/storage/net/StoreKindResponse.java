package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.header.NodeID;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.net.StoreKindResponse.StoreKindResponseCodec;

@ReloadCodec(StoreKindResponseCodec.class)
public class StoreKindResponse {

	private final DataKind kind;
	private final BigInteger generation;
	private final List<NodeID> replicas;

	public StoreKindResponse(DataKind kind, BigInteger generation, List<NodeID> replicas) {
		this.kind = kind;
		this.generation = generation;
		this.replicas = replicas;
	}

	public BigInteger getGeneration() {
		return generation;
	}

	public DataKind getKind() {
		return kind;
	}

	public List<NodeID> getReplicas() {
		return replicas;
	}

	@Override
	public String toString() {
		return "StoreKindResponse [kind=" + kind.getKindId() + ", generation=" + generation + ", replicas=" + replicas + "]";
	}

	static class StoreKindResponseCodec extends Codec<StoreKindResponse> {

		private static final int GENERATION_FIELD = U_INT64;
		private static final int REPLICAS_LENGTH_FIELD = U_INT16;

		private final Codec<DataKind> kindCodec;
		private final Codec<NodeID> nodeIdCodec;

		public StoreKindResponseCodec(ObjectGraph ctx) {
			super(ctx);
			kindCodec = getCodec(DataKind.class);
			nodeIdCodec = getCodec(NodeID.class);
		}

		@Override
		public void encode(StoreKindResponse obj, ByteBuf buf, Object... params) throws CodecException {
			kindCodec.encode(obj.kind, buf);

			byte[] genBytes = toUnsigned(obj.generation);

			// Make sure the field is always the fixed size by padding with
			// zeros
			buf.writeZero(GENERATION_FIELD - genBytes.length);

			buf.writeBytes(genBytes);

			Field lenFld = allocateField(buf, REPLICAS_LENGTH_FIELD);

			for (NodeID n : obj.replicas) {
				nodeIdCodec.encode(n, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StoreKindResponse decode(ByteBuf buf, Object... params) throws CodecException {
			DataKind kind = kindCodec.decode(buf);

			byte[] genCounterData = new byte[GENERATION_FIELD];
			buf.readBytes(genCounterData);
			BigInteger genCounter = new BigInteger(1, genCounterData);

			List<NodeID> replicas = decodeReplicas(buf);
			return new StoreKindResponse(kind, genCounter, replicas);
		}

		private List<NodeID> decodeReplicas(ByteBuf buf) throws com.github.reload.net.codecs.Codec.CodecException {
			ByteBuf replicasFld = readField(buf, REPLICAS_LENGTH_FIELD);

			List<NodeID> replicas = new ArrayList<NodeID>();

			while (replicasFld.readableBytes() > 0) {
				replicas.add(nodeIdCodec.decode(replicasFld));
			}

			replicasFld.release();

			return replicas;
		}

	}
}
