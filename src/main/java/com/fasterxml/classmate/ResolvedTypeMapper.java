package com.fasterxml.classmate;

import com.fasterxml.classmate.types.ResolvedArrayType;
import com.fasterxml.classmate.types.ResolvedRecursiveType;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Utility for converting a {@link ResolvedType} into a standard Java {@link Type}
 * that can be used with the reflection API.
 *
 * <p><strong>Limitations:</strong></p>
 * <ul>
 *   <li><b>Type variables and wildcards are not fully supported.</b>
 *       Since {@link ResolvedType} does not preserve full type variable or wildcard details,
 *       this mapper resolves them to their <em>upper bound</em> (or {@link Object} if no upper bound is known).</li>
 *   <li>For example:
 *     <ul>
 *       <li>A field declared as {@code E extends List} will map to {@code List} when the type is unbound.</li>
 *       <li>A wildcard like {@code ? super List} or a type variable like {@code E super List} will map to {@code Object}.</li>
 *       <li>If a concrete type binding is provided in the {@link TypeBindings},
 *           for example {@code E extends List} bound to {@code ArrayList},
 *           this mapper will return {@code ArrayList}.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>
 * For further discussion, see
 * <a href="https://github.com/FasterXML/java-classmate/issues/69">issue #69</a>.
 * </p>
 */

@SuppressWarnings("serial")
public class ResolvedTypeMapper implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Method for mapping {@link ResolvedType} to java types.

	 * @param resolvedType The resolved type to map to a java {@link Type}
	 * @return The type with all generics resolved, OR with the generics replaced with the upper bounds AND with all wildcards resolved to the upper bound.
	 */
	public Type map(ResolvedType resolvedType) {
		if (resolvedType instanceof ResolvedArrayType) {
			ResolvedArrayType arrayType = (ResolvedArrayType) resolvedType;
			ResolvedType arrayElementType = arrayType.getArrayElementType();
			if (arrayElementType == null) {
				throw new IllegalStateException("Missing array element type");
			}
			Type elementType = map(arrayElementType);
			//GenericArrayType is only used for parameterized types or TypeVariables, but we don't have TypeVariables
			if (elementType instanceof ParameterizedType) {
				return new GenericArrayTypeImpl(elementType);
			}
			return arrayType.getErasedType();
		}
		// Extract recursive type
		if (resolvedType instanceof ResolvedRecursiveType) {
			ResolvedRecursiveType recursiveType = (ResolvedRecursiveType) resolvedType;
			ResolvedType selfReferencedType = recursiveType.getSelfReferencedType();
			if (selfReferencedType == null) {
				throw new IllegalStateException("Missing self referenced type");
			}
			return map(selfReferencedType);
		}
		// no generics present, so the erased type is equal to the real type
		if (resolvedType.getTypeParameters() == null || resolvedType.getTypeParameters().isEmpty()) {
			return resolvedType.getErasedType();
		}
		// Parameters are present, so we need to create a parameterized
		// Wildcard types are not supported, since we store the upperbound
		return _mapParameterizedType(resolvedType);
	}

	private ParameterizedTypeImpl _mapParameterizedType(ResolvedType objectType) {
		Class<?> erasedType = objectType.getErasedType();
		List<Type> list = new ArrayList<>();
		for (ResolvedType resolvedType : objectType.getTypeParameters()) {
			Type mappedArrayType = map(resolvedType);
			list.add(mappedArrayType);
		}
		return new ParameterizedTypeImpl(erasedType, list.toArray(new Type[0]), erasedType.getEnclosingClass());
	}

	/**
	 * Implementation of ParameterizedType for classmate type creation
	 */
	static final class ParameterizedTypeImpl implements ParameterizedType {
		private final Type rawType;
		private final Type[] actualTypeArguments;
		private final Type ownerType;

		private ParameterizedTypeImpl(Type rawType, Type[] actualTypeArguments, Type ownerType) {
			this.rawType = rawType;
			this.actualTypeArguments = actualTypeArguments;
			this.ownerType = ownerType;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Type[] getActualTypeArguments() {
			return actualTypeArguments;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Type getRawType() {
			return rawType;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Type getOwnerType() {
			return ownerType;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ParameterizedType)) {
				return false;
			}
			ParameterizedType that = (ParameterizedType) o;
			return Objects.equals(rawType, that.getRawType()) && Objects.deepEquals(actualTypeArguments, that.getActualTypeArguments()) && Objects.equals(ownerType, that.getOwnerType());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			return Objects.hash(rawType, Arrays.hashCode(actualTypeArguments), ownerType);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return rawType + "<" + Arrays.stream(actualTypeArguments).map(Type::getTypeName).collect(Collectors.joining(", ")) + ">";
		}
	}

	/**
	 * Implementation of GenericArrayType for classmate type creation
	 */
	static final class GenericArrayTypeImpl implements GenericArrayType {
		private final Type genericComponentType;

		private GenericArrayTypeImpl(Type genericComponentType) {
			this.genericComponentType = genericComponentType;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Type getGenericComponentType() {
			return genericComponentType;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof GenericArrayType)) {
				return false;
			}
			GenericArrayType that = (GenericArrayType) o;
			return Objects.equals(genericComponentType, that.getGenericComponentType());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			return Objects.hashCode(genericComponentType);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return genericComponentType.getTypeName() + "[]";
		}
	}
}
