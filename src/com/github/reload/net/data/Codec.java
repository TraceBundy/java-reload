package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import com.github.reload.Context;
import com.github.reload.message.errors.Error;
import com.github.reload.message.errors.Error.ErrorType;

/**
 * Encode and decode the object on the given buffer
 * 
 * @param <T>
 *            The object type handled by this codec
 */
public abstract class Codec<T> {

	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>8</sup>-1
	 */
	protected static final int U_INT8 = 1;
	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>16</sup>-1
	 */
	protected static final int U_INT16 = 2;
	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>24</sup>-1
	 */
	protected static final int U_INT24 = 3;
	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>32</sup>-1
	 */
	protected static final int U_INT32 = 4;
	/**
	 * The amount of bytes needed to represent an unsigned integer up to
	 * 2<sup>64</sup>-1
	 */
	protected static final int U_INT64 = 8;

	protected final Context context;

	public Codec(Context context) {
		this.context = context;
	}

	/**
	 * Get an instance of the codec associated with the given class. The given
	 * class must be annotated with the {@link ReloadCodec} annotation to
	 * declare the codec class.
	 * The new codec will be initialized with the given {@link Context}.
	 * 
	 * @param clazz
	 *            the class that the codec is associated with
	 * @param ctx
	 *            the context used to initialize the codec
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Codec<T> getCodec(Class<T> clazz, Context ctx) {
		ReloadCodec codecAnn = clazz.getAnnotation(ReloadCodec.class);
		if (codecAnn == null)
			throw new IllegalStateException("No RELOAD codec associated with class " + clazz.toString());

		try {
			Constructor<? extends Codec<?>> codecConstr = codecAnn.value().getConstructor(Context.class);
			return (Codec<T>) codecConstr.newInstance(ctx);
		} catch (Exception e) {
			throw new IllegalStateException("Codec instantiation failed for class " + clazz.toString(), e);
		}
	}

	/**
	 * Encode object to the given byte buffer
	 * 
	 * @param data
	 * @param buf
	 */
	public abstract void encode(T obj, ByteBuf buf) throws CodecException;

	/**
	 * Decode object from the given byte buffer
	 * 
	 * @param buf
	 * @return
	 */
	public abstract T decode(ByteBuf buf) throws CodecException;

	/**
	 * Allocate a field at the current write index that can hold at most the
	 * data of the given amount of bytes.
	 * This method is meant to be used with
	 * {@link #setVariableLengthField(ByteBuf, int, int)} to set the length
	 * subfield after the data have been written to the buffer
	 * 
	 * @param buf
	 *            the buffer
	 * @param maxDataLength
	 *            the data maximum bytes size in power of two
	 */
	protected static Field allocateField(ByteBuf buf, int maxDataLength) {
		return new Field(buf, maxDataLength);
	}

	/**
	 * Returns the data stored in a variable-length field at the current read
	 * index and move the readIndex after the field.
	 * The returned buffer is a {@link ByteBuf#slice()} of the original buffer,
	 * it remains backed to the original buffer.
	 * 
	 * @param buf
	 *            the buffer
	 * @param maxDataLength
	 *            the data maximum bytes size in power of two
	 * 
	 * @see {@link ByteBuf#slice()}
	 */
	protected static ByteBuf readData(ByteBuf buf, int maxDataLength) {
		int dataLength = 0;
		int baseOffset = (maxDataLength - 1) * 8;
		for (int i = 0; i < maxDataLength; i++) {
			int offset = baseOffset - (i * 8);
			dataLength += (buf.readByte() & 0xff) << offset;
		}

		ByteBuf data = buf.slice(buf.readerIndex(), dataLength);
		buf.readerIndex(data.readableBytes());
		return data;
	}

	protected static class Field {

		private final ByteBuf buf;
		private final int fieldPos;
		private final int maxDataLength;

		public Field(ByteBuf buf, int maxDataLength) {
			this.buf = buf;
			fieldPos = buf.writerIndex();
			this.maxDataLength = maxDataLength;
			buf.writerIndex(fieldPos + maxDataLength);
		}

		/**
		 * Set data length of the field, its calculated starting from the end of
		 * the length subfield up to the current buffer write index
		 * 
		 * @return
		 *         the length of data subfield in bytes
		 */
		public int updateDataLength() {
			// current position - (start of data subfield)
			int writtenDataLength = buf.writerIndex() - (fieldPos + maxDataLength);
			// Set actual written data subfield length into the length subfield
			buf.writerIndex(fieldPos);
			encodeLength(maxDataLength, writtenDataLength, buf);

			// Reset 1 byte after original position (after the field)
			buf.writerIndex(buf.writerIndex() + writtenDataLength + 1);

			return writtenDataLength;
		}

		/**
		 * Encode length subfield
		 */
		private static void encodeLength(int maxDataLength, int dataLength, ByteBuf buf) {
			for (int i = 0; i < maxDataLength; i++) {
				int offset = (maxDataLength - 1 - i);
				buf.writeByte(dataLength >>> offset);
			}
		}
	}

	/**
	 * @return the hexadecimal string representation of the passed bytes
	 */
	public static String hexDump(byte[] bytes) {
		BigInteger bi = new BigInteger(1, bytes);
		return String.format("%#x", bi);
	}

	/**
	 * @return the hexadecimal string representation of the passed value without
	 *         sign
	 */
	public static String hexDump(long val) {
		BigInteger bi = BigInteger.valueOf(val).abs();
		return String.format("%#x", bi);
	}

	/**
	 * This exception can be thrown to indicate an error in the en/decoding
	 * process
	 */
	public static class CodecException extends Exception {

		public CodecException() {
			super();
		}

		public CodecException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public CodecException(String message, Throwable cause) {
			super(message, cause);
		}

		public CodecException(String message) {
			super(message);
		}

		public CodecException(Throwable cause) {
			super(cause);
		}

		/**
		 * @return An error response to send to the sender node with the cause
		 *         of the error
		 */
		public Error getErrorResponse() {
			return new Error(ErrorType.INVALID_MESSAGE, getMessage());
		}

	}
}