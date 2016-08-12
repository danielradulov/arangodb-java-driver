package com.arangodb.velocypack;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.arangodb.velocypack.VPackBuilder.BuilderOptions;
import com.arangodb.velocypack.exception.VPackException;
import com.arangodb.velocypack.exception.VPackParserException;
import com.arangodb.velocypack.internal.DefaultVPackBuilderOptions;
import com.arangodb.velocypack.internal.VPackCache;
import com.arangodb.velocypack.internal.VPackCache.FieldInfo;
import com.arangodb.velocypack.internal.VPackDeserializers;
import com.arangodb.velocypack.internal.VPackInstanceCreators;
import com.arangodb.velocypack.internal.VPackKeyMapAdapters;
import com.arangodb.velocypack.internal.VPackSerializers;

/**
 * @author Mark - mark@arangodb.com
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class VPack {

	private static final String ATTR_KEY = "key";
	private static final String ATTR_VALUE = "value";

	private final Map<Type, VPackSerializer<?>> serializers;
	private final Map<Type, VPackDeserializer<?>> deserializers;
	private final Map<Type, VPackInstanceCreator<?>> instanceCreators;
	private final Map<Type, VPackKeyMapAdapter<?>> keyMapAdapters;

	private final BuilderOptions builderOptions;
	private final VPackCache cache;
	private final VPackSerializationContext serializationContext;
	private final VPackDeserializationContext deserializationContext;
	private final boolean serializeNullValues;

	public static class Builder {
		private final Map<Type, VPackSerializer<?>> serializers;
		private final Map<Type, VPackDeserializer<?>> deserializers;
		private final Map<Type, VPackInstanceCreator<?>> instanceCreators;
		private final BuilderOptions builderOptions;
		private boolean serializeNullValues;
		private VPackFieldNamingStrategy fieldNamingStrategy;

		public Builder() {
			super();
			serializers = new HashMap<>();
			deserializers = new HashMap<>();
			instanceCreators = new HashMap<>();
			builderOptions = new DefaultVPackBuilderOptions();
			serializeNullValues = false;

			instanceCreators.put(Collection.class, VPackInstanceCreators.COLLECTION);
			instanceCreators.put(List.class, VPackInstanceCreators.LIST);
			instanceCreators.put(Set.class, VPackInstanceCreators.SET);
			instanceCreators.put(Map.class, VPackInstanceCreators.MAP);

			serializers.put(String.class, VPackSerializers.STRING);
			serializers.put(Boolean.class, VPackSerializers.BOOLEAN);
			serializers.put(boolean.class, VPackSerializers.BOOLEAN);
			serializers.put(Integer.class, VPackSerializers.INTEGER);
			serializers.put(int.class, VPackSerializers.INTEGER);
			serializers.put(Long.class, VPackSerializers.LONG);
			serializers.put(long.class, VPackSerializers.LONG);
			serializers.put(Short.class, VPackSerializers.SHORT);
			serializers.put(short.class, VPackSerializers.SHORT);
			serializers.put(Double.class, VPackSerializers.DOUBLE);
			serializers.put(double.class, VPackSerializers.DOUBLE);
			serializers.put(Float.class, VPackSerializers.FLOAT);
			serializers.put(float.class, VPackSerializers.FLOAT);
			serializers.put(BigInteger.class, VPackSerializers.BIG_INTEGER);
			serializers.put(BigDecimal.class, VPackSerializers.BIG_DECIMAL);
			serializers.put(Number.class, VPackSerializers.NUMBER);
			serializers.put(Character.class, VPackSerializers.CHARACTER);
			serializers.put(char.class, VPackSerializers.CHARACTER);

			deserializers.put(String.class, VPackDeserializers.STRING);
			deserializers.put(Boolean.class, VPackDeserializers.BOOLEAN);
			deserializers.put(boolean.class, VPackDeserializers.BOOLEAN);
			deserializers.put(Integer.class, VPackDeserializers.INTEGER);
			deserializers.put(int.class, VPackDeserializers.INTEGER);
			deserializers.put(Long.class, VPackDeserializers.LONG);
			deserializers.put(long.class, VPackDeserializers.LONG);
			deserializers.put(Short.class, VPackDeserializers.SHORT);
			deserializers.put(short.class, VPackDeserializers.SHORT);
			deserializers.put(Double.class, VPackDeserializers.DOUBLE);
			deserializers.put(double.class, VPackDeserializers.DOUBLE);
			deserializers.put(Float.class, VPackDeserializers.FLOAT);
			deserializers.put(float.class, VPackDeserializers.FLOAT);
			deserializers.put(BigInteger.class, VPackDeserializers.BIG_INTEGER);
			deserializers.put(BigDecimal.class, VPackDeserializers.BIG_DECIMAL);
			deserializers.put(Number.class, VPackDeserializers.NUMBER);
			deserializers.put(Character.class, VPackDeserializers.CHARACTER);
			deserializers.put(char.class, VPackDeserializers.CHARACTER);
		}

		public <T> VPack.Builder registerSerializer(final Type type, final VPackSerializer<T> serializer) {
			serializers.put(type, serializer);
			return this;
		}

		public <T> VPack.Builder registerDeserializer(final Type type, final VPackDeserializer<T> deserializer) {
			deserializers.put(type, deserializer);
			return this;
		}

		public <T> VPack.Builder regitserInstanceCreator(final Type type, final VPackInstanceCreator<T> creator) {
			instanceCreators.put(type, creator);
			return this;
		}

		public VPack.Builder buildUnindexedArrays(final boolean buildUnindexedArrays) {
			builderOptions.setBuildUnindexedArrays(buildUnindexedArrays);
			return this;
		}

		public VPack.Builder buildUnindexedObjects(final boolean buildUnindexedObjects) {
			builderOptions.setBuildUnindexedObjects(buildUnindexedObjects);
			return this;
		}

		public VPack.Builder serializeNullValues(final boolean serializeNullValues) {
			this.serializeNullValues = serializeNullValues;
			return this;
		}

		public VPack.Builder fieldNamingStrategy(final VPackFieldNamingStrategy fieldNamingStrategy) {
			this.fieldNamingStrategy = fieldNamingStrategy;
			return this;
		}

		public VPack build() {
			return new VPack(serializers, deserializers, instanceCreators, builderOptions, serializeNullValues,
					fieldNamingStrategy);
		}

	}

	private VPack(final Map<Type, VPackSerializer<?>> serializers, final Map<Type, VPackDeserializer<?>> deserializers,
		final Map<Type, VPackInstanceCreator<?>> instanceCreators, final BuilderOptions builderOptions,
		final boolean serializeNullValues, final VPackFieldNamingStrategy fieldNamingStrategy) {
		super();
		this.serializers = serializers;
		this.deserializers = deserializers;
		this.instanceCreators = instanceCreators;
		this.builderOptions = builderOptions;
		this.serializeNullValues = serializeNullValues;
		keyMapAdapters = new HashMap<>();
		cache = new VPackCache(fieldNamingStrategy);
		serializationContext = (builder, attribute, entity) -> VPack.this.serialize(attribute, entity, builder,
			new HashMap<String, Object>());
		deserializationContext = new VPackDeserializationContext() {
			@Override
			public <T> T deserialize(final VPackSlice vpack, final Class<T> type) throws VPackParserException {
				return VPack.this.deserialize(vpack, type);
			}
		};
		keyMapAdapters.put(String.class, VPackKeyMapAdapters.STRING);
		keyMapAdapters.put(Boolean.class, VPackKeyMapAdapters.BOOLEAN);
		keyMapAdapters.put(Integer.class, VPackKeyMapAdapters.INTEGER);
		keyMapAdapters.put(Long.class, VPackKeyMapAdapters.LONG);
		keyMapAdapters.put(Short.class, VPackKeyMapAdapters.SHORT);
		keyMapAdapters.put(Double.class, VPackKeyMapAdapters.DOUBLE);
		keyMapAdapters.put(Float.class, VPackKeyMapAdapters.FLOAT);
		keyMapAdapters.put(BigInteger.class, VPackKeyMapAdapters.BIG_INTEGER);
		keyMapAdapters.put(BigDecimal.class, VPackKeyMapAdapters.BIG_DECIMAL);
		keyMapAdapters.put(Number.class, VPackKeyMapAdapters.NUMBER);
		keyMapAdapters.put(Character.class, VPackKeyMapAdapters.CHARACTER);
	}

	public <T> T deserialize(final VPackSlice vpack, final Type type) throws VPackParserException {
		final T entity;
		try {
			entity = (T) getValue(vpack, type, null);
		} catch (final Exception e) {
			throw new VPackParserException(e);
		}
		return entity;
	}

	private <T> T deserializeObject(final VPackSlice vpack, final Type type) throws InstantiationException,
			IllegalAccessException, NoSuchMethodException, InvocationTargetException, VPackException {
		final T entity;
		final VPackDeserializer<?> deserializer = deserializers.get(type);
		if (deserializer != null) {
			entity = ((VPackDeserializer<T>) deserializer).deserialize(vpack, deserializationContext);
		} else {
			entity = createInstance(type);
			deserializeFields(entity, vpack);
		}
		return entity;
	}

	private <T> T createInstance(final Type type) throws InstantiationException, IllegalAccessException {
		final T entity;
		final VPackInstanceCreator<?> creator = instanceCreators.get(type);
		if (creator != null) {
			entity = (T) creator.createInstance();
		} else if (type instanceof ParameterizedType) {
			entity = createInstance(((ParameterizedType) type).getRawType());
		} else {
			entity = ((Class<T>) type).newInstance();
		}
		return entity;
	}

	private void deserializeFields(final Object entity, final VPackSlice vpack) throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, InstantiationException, VPackException {
		final Map<String, FieldInfo> fields = cache.getFields(entity.getClass());
		for (final Iterator<VPackSlice> iterator = vpack.iterator(); iterator.hasNext();) {
			final VPackSlice attr = iterator.next();
			final FieldInfo fieldInfo = fields.get(attr.makeKey().getAsString());
			if (fieldInfo != null && fieldInfo.isDeserialize()) {
				deserializeField(attr, entity, fieldInfo);
			}
		}
	}

	private void deserializeField(final VPackSlice vpack, final Object entity, final FieldInfo fieldInfo)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException,
			VPackException {
		final VPackSlice attr = new VPackSlice(vpack.getVpack(), vpack.getStart() + vpack.getByteSize());
		if (!attr.isNone()) {
			final Object value = getValue(attr, fieldInfo.getType(), fieldInfo);
			fieldInfo.set(entity, value);
		}
	}

	private <T> Object getValue(final VPackSlice vpack, final Type type, final FieldInfo fieldInfo)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException,
			VPackException {
		final Object value;
		if (vpack.isNull()) {
			value = null;
		} else {
			final VPackDeserializer<?> deserializer = deserializers.get(type);
			if (deserializer != null) {
				value = ((VPackDeserializer<Object>) deserializer).deserialize(vpack, deserializationContext);
			} else if (type instanceof ParameterizedType) {
				final ParameterizedType pType = ParameterizedType.class.cast(type);
				final Type rawType = pType.getRawType();
				if (Collection.class.isAssignableFrom((Class<?>) rawType)) {
					value = deserializeCollection(vpack, type, pType.getActualTypeArguments()[0]);
				} else if (Map.class.isAssignableFrom((Class<?>) rawType)) {
					final Type[] parameterizedTypes = pType.getActualTypeArguments();
					value = deserializeMap(vpack, type, parameterizedTypes[0], parameterizedTypes[1]);
				} else {
					value = deserializeObject(vpack, type);
				}
			} else if (((Class) type).isArray()) {
				value = deserializeArray(vpack, type, fieldInfo);
			} else if (((Class) type).isEnum()) {
				value = Enum.valueOf((Class<? extends Enum>) type, vpack.getAsString());
			} else {
				value = deserializeObject(vpack, type);
			}
		}
		return value;
	}

	private <T> Object deserializeArray(final VPackSlice vpack, final Type type, final FieldInfo fieldInfo)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException,
			VPackException {
		final int length = (int) vpack.getLength();
		final Class<?> componentType = ((Class<?>) type).getComponentType();
		final Object value = Array.newInstance(componentType, length);
		for (int i = 0; i < length; i++) {
			Array.set(value, i, getValue(vpack.get(i), componentType, null));
		}
		return value;
	}

	private <T, C> Object deserializeCollection(final VPackSlice vpack, final Type type, final Type contentType)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException,
			VPackException {
		final Collection value = (Collection) createInstance(type);
		final long length = vpack.getLength();
		if (length > 0) {
			for (int i = 0; i < length; i++) {
				value.add(getValue(vpack.get(i), contentType, null));
			}
		}
		return value;
	}

	private <T, K, C> Object deserializeMap(
		final VPackSlice vpack,
		final Type type,
		final Type keyType,
		final Type valueType) throws InstantiationException, IllegalAccessException, NoSuchMethodException,
			InvocationTargetException, VPackException {
		final int length = (int) vpack.getLength();
		final Map value = (Map) createInstance(type);
		if (length > 0) {
			final VPackKeyMapAdapter<Object> keyMapAdapter = getKeyMapAdapter(keyType);
			if (keyMapAdapter != null) {
				for (final Iterator<VPackSlice> iterator = vpack.iterator(); iterator.hasNext();) {
					final VPackSlice key = iterator.next();
					final VPackSlice valueAt = new VPackSlice(key.getVpack(), key.getStart() + key.getByteSize());
					value.put(keyMapAdapter.deserialize(key.makeKey().getAsString()),
						getValue(valueAt, valueType, null));
				}
			} else {
				for (int i = 0; i < vpack.getLength(); i++) {
					final VPackSlice entry = vpack.get(i);
					final Object mapKey = getValue(entry.get(ATTR_KEY), keyType, null);
					final Object mapValue = getValue(entry.get(ATTR_VALUE), valueType, null);
					value.put(mapKey, mapValue);
				}
			}
		}
		return value;
	}

	public VPackSlice serialize(final Object entity) throws VPackParserException {
		return serialize(entity, new HashMap<String, Object>());
	}

	public VPackSlice serialize(final Object entity, final Map<String, Object> additionalFields)
			throws VPackParserException {
		final VPackBuilder builder = new VPackBuilder(builderOptions);
		serialize(null, entity, builder, new HashMap<>(additionalFields));
		return builder.slice();
	}

	private void serialize(
		final String name,
		final Object entity,
		final VPackBuilder builder,
		final Map<String, Object> additionalFields) throws VPackParserException {
		try {
			addValue(name, entity.getClass(), entity, builder, null, additionalFields);
		} catch (final Exception e) {
			throw new VPackParserException(e);
		}
	}

	public VPackSlice serialize(final Object entity, final Type type) throws VPackParserException {
		return serialize(entity, type, new HashMap<>());
	}

	public VPackSlice serialize(final Object entity, final Type type, final Map<String, Object> additionalFields)
			throws VPackParserException {
		final VPackBuilder builder = new VPackBuilder(builderOptions);
		try {
			addValue(null, type, entity, builder, null, new HashMap<>(additionalFields));
		} catch (final Exception e) {
			throw new VPackParserException(e);
		}
		return builder.slice();
	}

	private void serializeObject(
		final String name,
		final Object entity,
		final VPackBuilder builder,
		final Map<String, Object> additionalFields)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, VPackException {

		final VPackSerializer<?> serializer = serializers.get(entity.getClass());
		if (serializer != null) {
			((VPackSerializer<Object>) serializer).serialize(builder, name, entity, serializationContext);
		} else {
			builder.add(name, new Value(ValueType.OBJECT));
			serializeFields(entity, builder, additionalFields);
			if (!additionalFields.isEmpty()) {
				additionalFields.clear();
				builder.close(true);
			} else {
				builder.close(false);
			}
		}
	}

	private void serializeFields(
		final Object entity,
		final VPackBuilder builder,
		final Map<String, Object> additionalFields)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, VPackException {
		final Map<String, FieldInfo> fields = cache.getFields(entity.getClass());
		for (final FieldInfo fieldInfo : fields.values()) {
			if (fieldInfo.isSerialize()) {
				serializeField(entity, builder, fieldInfo, additionalFields);
			}
		}
		for (final Entry<String, Object> entry : additionalFields.entrySet()) {
			final String key = entry.getKey();
			if (!fields.containsKey(key)) {
				final Object value = entry.getValue();
				addValue(key, value != null ? value.getClass() : null, value, builder, null, additionalFields);
			}
		}
	}

	private void serializeField(
		final Object entity,
		final VPackBuilder builder,
		final FieldInfo fieldInfo,
		final Map<String, Object> additionalFields)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, VPackException {

		final String fieldName = fieldInfo.getFieldName();
		final Type type = fieldInfo.getType();
		final Object value = fieldInfo.get(entity);
		addValue(fieldName, type, value, builder, fieldInfo, additionalFields);
	}

	private void addValue(
		final String name,
		final Type type,
		final Object value,
		final VPackBuilder builder,
		final FieldInfo fieldInfo,
		final Map<String, Object> additionalFields)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, VPackException {

		if (value == null) {
			if (serializeNullValues) {
				builder.add(name, new Value(ValueType.NULL));
			}
		} else {
			final VPackSerializer<?> serializer = serializers.get(type);
			if (serializer != null) {
				((VPackSerializer<Object>) serializer).serialize(builder, name, value, serializationContext);
			} else if (type instanceof ParameterizedType) {
				final ParameterizedType pType = ParameterizedType.class.cast(type);
				final Type rawType = pType.getRawType();
				if (Iterable.class.isAssignableFrom((Class<?>) rawType)) {
					serializeIterable(name, value, builder, additionalFields);
				} else if (Map.class.isAssignableFrom((Class<?>) rawType)) {
					serializeMap(name, value, builder, pType.getActualTypeArguments()[0], additionalFields);
				} else {
					serializeObject(name, value, builder, additionalFields);
				}
			} else if (Iterable.class.isAssignableFrom((Class<?>) type)) {
				serializeIterable(name, value, builder, additionalFields);
			} else if (Map.class.isAssignableFrom((Class<?>) type)) {
				serializeMap(name, value, builder, String.class, additionalFields);
			} else if (((Class) type).isArray()) {
				serializeArray(name, value, builder, additionalFields);
			} else if (((Class) type).isEnum()) {
				builder.add(name, new Value(Enum.class.cast(value).name()));
			} else {
				serializeObject(name, value, builder, additionalFields);
			}
		}
	}

	private void serializeArray(
		final String name,
		final Object value,
		final VPackBuilder builder,
		final Map<String, Object> additionalFields)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, VPackException {
		builder.add(name, new Value(ValueType.ARRAY));
		for (int i = 0; i < Array.getLength(value); i++) {
			final Object element = Array.get(value, i);
			addValue(null, element.getClass(), element, builder, null, additionalFields);
		}
		builder.close();
	}

	private void serializeIterable(
		final String name,
		final Object value,
		final VPackBuilder builder,
		final Map<String, Object> additionalFields)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, VPackException {
		builder.add(name, new Value(ValueType.ARRAY));
		for (final Iterator iterator = Iterable.class.cast(value).iterator(); iterator.hasNext();) {
			final Object element = iterator.next();
			addValue(null, element.getClass(), element, builder, null, additionalFields);
		}
		builder.close();
	}

	private void serializeMap(
		final String name,
		final Object value,
		final VPackBuilder builder,
		final Type keyType,
		final Map<String, Object> additionalFields)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, VPackException {
		final Map map = Map.class.cast(value);
		if (map.size() > 0) {
			final VPackKeyMapAdapter<Object> keyMapAdapter = getKeyMapAdapter(keyType);
			if (keyMapAdapter != null) {
				builder.add(name, new Value(ValueType.OBJECT));
				final Set<Entry<?, ?>> entrySet = map.entrySet();
				for (final Entry<?, ?> entry : entrySet) {
					final Object entryValue = entry.getValue();
					addValue(keyMapAdapter.serialize(entry.getKey()),
						entryValue != null ? entryValue.getClass() : Object.class, entry.getValue(), builder, null,
						additionalFields);
				}
				builder.close();
			} else {
				builder.add(name, new Value(ValueType.ARRAY));
				final Set<Entry<?, ?>> entrySet = map.entrySet();
				for (final Entry<?, ?> entry : entrySet) {
					builder.add(null, new Value(ValueType.OBJECT));
					addValue(ATTR_KEY, entry.getKey().getClass(), entry.getKey(), builder, null, additionalFields);
					addValue(ATTR_VALUE, entry.getValue().getClass(), entry.getValue(), builder, null,
						additionalFields);
					builder.close();
				}
				builder.close();
			}
		} else {
			builder.add(name, new Value(ValueType.OBJECT));
			builder.close();
		}
	}

	private VPackKeyMapAdapter<Object> getKeyMapAdapter(final Type type) {
		VPackKeyMapAdapter<?> adapter = keyMapAdapters.get(type);
		if (adapter == null && Enum.class.isAssignableFrom((Class<?>) type)) {
			adapter = VPackKeyMapAdapters.createEnumAdapter(type);
		}
		return (VPackKeyMapAdapter<Object>) adapter;
	}

}