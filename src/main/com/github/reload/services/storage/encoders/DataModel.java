package com.github.reload.services.storage.encoders;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.services.storage.encoders.DataModel.DataValue;

public abstract class DataModel<T extends DataValue> {

	private static final Map<String, DataModel<? extends DataValue>> models = new HashMap<String, DataModel<? extends DataValue>>();

	public static final Class<SingleModel> SINGLE = SingleModel.class;
	public static final Class<ArrayModel> ARRAY = ArrayModel.class;
	public static final Class<DictionaryModel> DICTIONARY = DictionaryModel.class;

	public static <T extends DataModel<? extends DataValue>> T getInstance(Class<T> clazz) {
		String name = getModelName(clazz);

		@SuppressWarnings("unchecked")
		T model = (T) models.get(name);

		if (model == null) {
			try {
				model = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			models.put(name, model);
		}

		return model;
	}

	private static String getModelName(Class<? extends DataModel<? extends DataValue>> clazz) {
		return clazz.getAnnotation(ModelName.class).value().toLowerCase();
	}

	public DataModel() {
	}

	/**
	 * Syntetic data value used to indicate non existent data in particular
	 * situations
	 */
	public abstract T getNonExistentValue();

	public abstract DataValueBuilder<T> newValueBuilder();

	protected abstract Class<T> getValueClass();

	public abstract Metadata<T> newMetadata(T value, HashAlgorithm hashAlg);

	protected abstract Class<? extends Metadata<T>> getMetadataClass();

	public abstract ValueSpecifier newSpecifier();

	protected abstract Class<? extends ValueSpecifier> getSpecifierClass();

	@SuppressWarnings("unchecked")
	public String getName() {
		return getModelName((Class<? extends DataModel<? extends DataValue>>) this.getClass());
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * The value that will be stored
	 */
	public interface DataValue {

		long getSize();

		ValueSpecifier getMatchingSpecifier();

	}

	/**
	 * The metadata associated with a data value
	 */
	public interface Metadata<T extends DataValue> extends DataValue {

	}

	/**
	 * A builder used to create {@link DataValue}
	 * 
	 */
	public interface DataValueBuilder<T extends DataValue> {

		T build();

	}

	/**
	 * A model specifier used to query for a {@link DataValue}
	 */
	public interface ValueSpecifier {

		boolean isMatching(DataValue value);

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ModelName {

		public String value();
	}
}
