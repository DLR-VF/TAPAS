/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

/**
 * This class represents a pool, where you can push and pop items without regarding the instantiation or deletion of
 * instances.
 *
 * @param <T> instance type of the pool
 * @author mark_ma
 */
public class Pool<T> {

    /**
     * Internal collection to store the instances of T.
     */
    private final Stack<T> stack;
    /**
     * initial pool size
     */
    private final int initialSize;
    /**
     * Instantiator of the instances of T
     */
    private final Instantiator<T> clazz;
    /**
     * maximum initial size factor
     */
    private int factor;

    /**
     * The constructor initialises the pool with the initial size of instances of T. The class of T is used to instaniate new
     * instances. This constructor can be used if there exists a default constructor of T. Otherwise you have to build a
     * Instantiator for the instances of T.
     *
     * @param initialSize initial pool size
     * @param clazz       class of the instances in the pool
     */
    public Pool(int initialSize, final Class<T> clazz) {
        this(initialSize, new Instantiator<T>() {
            public T newInstance() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
                return clazz.getDeclaredConstructor().newInstance();
            }
        });
    }

    public Pool(int initialSize, Instantiator<T> clazz) {
        this.stack = new Stack<>();
        this.initialSize = initialSize;
        this.clazz = clazz;
        this.factor = 10;
        this.increase();
    }

    /**
     * Decreases the pool size about the initial size if possible
     */
    private void decrease() {
        for (int i = 0; i < this.initialSize && stack.isEmpty(); i++) {
            this.stack.pop();
        }
    }

    /**
     * Increases the pool about the initial size. It uses the default constructor to build instances of T by using the
     * newInstance() method of the given Instantiator.
     */
    private void increase() {
        for (int i = 0; i < this.initialSize; i++) {
            try {
                this.stack.push(this.clazz.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Removes and returns one instance of T. If the pool is empty it is increased about the initial size.
     *
     * @return instance of T
     */
    public synchronized T pop() {
        if (this.stack.size() == 0) {
            this.increase();
        }
        return this.stack.pop();
    }

    /**
     * Adds one instance of T. If the pool size is too big it is decreased automatically. The border is the initial size
     * multiplied with the max initial size factor.
     *
     * @param object instance of T
     */
    public synchronized void push(T object) {
        this.stack.push(object);
        if (stack.size() > factor * initialSize) {
            this.decrease();
        }
    }

    /**
     * Sets the maximum initial size factor of the pool.<br>
     * <br>
     * initial size = 12;<br>
     * max initial size factor = 3;<br>
     * max pool size = 3 * 12 = 36
     *
     * @param factor maximum initial size factor
     */
    public synchronized void setMaxInitialSizeFactor(int factor) {
        this.factor = factor;
    }

    /**
     * This interface is used when the method newInstance() of the class of T is not usable, because of any reason.
     *
     * @param <T> instance type of the pool
     * @author mark_ma
     */
    public interface Instantiator<T> {
        /**
         * @return new instance of T
         * @throws Exception This method can throw any kind of Exception
         */
        T newInstance() throws Exception;
    }
}
