package de.dlr.ivf.tapas.util;

/**
 * Interface for extended writable objects.
 * 
 * @author mark_ma
 * 
 */
public interface ExtendedWritable {

	/**
	 * This method should print the object naturally with the given prefix.
	 * 
	 * @param prefix
	 * 
	 * @return prefix + toString();
	 */
    String toString(String prefix);
}
