package de.dlr.ivf.api.converter;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toList;

/**
 * This functional interface provides a single method {@link #convert(S someObject)} that converts an object {@link S}
 * to an object {@link T}.
 * Additionally, the interface provides a set of default methods that convert some form of {@link T} to some form of {@link S}.
  *
 * @param <S> type of object that should be converted
 * @param <T> type of the converted object
 *
 * @author Alain Schengen
 */
@FunctionalInterface
public interface Converter<S,T> {
    /**
     * Converts an object {@link S} to an object {@link T}.
     *
     * @param dto data transfer object to convert
     * @return the converted object
     */
    T convert(@NonNull S dto);

    /**
     * A default method that converts a {@code List<S>} to a {@code List<T>}.
     * @param dtoCollection list of data transfer objects to convert
     * @return list of converted objects
     */
    default Collection<T> convertCollection(@NonNull Collection<S> dtoCollection){
        return dtoCollection.stream()
                .map(this::convert)
                .collect(toList());
    }

    /**
     * A default method that converts a {@code List<S>} to a {@code Map<K, List<T>>}. {@code List<S>} will be
     * converted to a {@code List<T>} first and then {@code List<T>} will be aggregated to a {@code Map<K, List<T>>}
     * using a {@code Function<T, K>}.
     * @param dtoCollection list of data transfer objects to convert
     * @param aggregationKey a function returning the key to group by with.
     * @return a map containing a {@code List<T>} for each {@link T} that maps to {@link K}.
     * @param <K> return type of aggregation function
     */
    default <K> Map<K, Collection<T>> convertCollectionToMapWithTargetKey(@NonNull Collection<S> dtoCollection, @NonNull Function<T,K> aggregationKey){
        return convertCollection(dtoCollection)
                .stream()
                .collect(groupingBy(aggregationKey, toCollection(ArrayList::new)));
    }


    /**
     * A default method that converts a {@code List<S>} to a {@code Map<K, List<T>>}. {@code List<S>} will be
     * aggregated using a {@code Function<S, K>} and the result will be converted from a {@code List<S>} to a
     * {@code List<T>}.
     * @param dtoCollection list of data transfer objects to convert
     * @param aggregationKey a function returning the key to group by with.
     * @return a map containing a {@code List<T>} for each {@link T} that maps to {@link K}.
     * @param <K> return type of aggregation function
     */
    default <K> Map<K,Collection<T>> convertCollectionToMapWithSourceKey(@NonNull Collection<S> dtoCollection, @NonNull Function<S,K> aggregationKey){
        return dtoCollection.stream()
                .collect(groupingBy(aggregationKey, mapping(this::convert, toCollection(ArrayList::new))));
    }
}

