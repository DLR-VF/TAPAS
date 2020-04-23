package de.dlr.ivf.tapas.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * This class provides a nested iterator, i.e. it is possible to iterate over all children without keeping their parents in
 * mind.
 *
 * @param <P> type of the parents
 * @param <C> type of the children
 * @author mark_ma
 */
public class NestedIterator<P extends Iterable<C>, C> implements Iterator<C>, Iterable<C> {

    /**
     * Internal iterator of one parent's children
     */
    private Iterator<C> cIt;

    /**
     * Internal iterator over all parents
     */
    private final Iterator<P> pIt;

    /**
     * Creates a parent iterator of the given collection an calls then the constructor NestedIterator(Iterator pIt)
     *
     * @param values collection of all parents
     */
    public NestedIterator(Collection<P> values) {
        this(values.iterator());
    }

    /**
     * Sets the internal parent iterator;
     *
     * @param pIt internal parent iterator
     */
    public NestedIterator(Iterator<P> pIt) {
        this.pIt = pIt;

    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        while (this.pIt.hasNext() && (this.cIt == null || !this.cIt.hasNext())) {
            this.cIt = this.pIt.next().iterator();
        }
        return this.cIt != null && this.cIt.hasNext();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<C> iterator() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    public C next() {
        if (this.hasNext()) return this.cIt.next();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    public void remove() {
    }

}
