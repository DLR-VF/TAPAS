/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.util;

import de.dlr.ivf.tapas.log.LogHierarchy;
import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.HierarchyLogLevel;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;

import java.util.TreeSet;

/**
 * This class represents a time line. It is possible to add entries from a start to an end point. These entries build up a
 * linked list in both directions. The time line holds the entries consistent, i.e. there can be no entries added which
 * overlaps already added entries.
 *
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class Timeline {

    private final TreeSet<Entry> entries;

    /**
     * The Constructor initialises the member values
     */
    public Timeline() {
        entries = new TreeSet<>();
    }

    /**
     * Small test for the time line
     *
     * @param args
     */
    public static void main(String[] args) {
        Timeline t = new Timeline();

        System.out.println(t.add(10, 20));
        System.out.println(t.contains(10,20));
        System.out.println(t.contains(10,30));
        System.out.println(t.contains(5,20));

        System.out.println(t.add(10, 30));
        System.out.println(t.add(5, 20));
        System.out.println(t.add(21, 22));
        System.out.println(t.add(25, 30));
        System.out.println(t.add(22, 24));


        t.clash(3, 5);
        t.add(2, 8);
        System.out.println(t);
        t.clash(3, 5);
        t.add(6, 9);
        System.out.println(t);
        t.add(8, 9);
        System.out.println(t);
        t.add(0, 2);
        t.add(14, 15);
        System.out.println(t);
        t.add(15, 15);
        System.out.println(t);
        t.add(10, 11);
        System.out.println(t);
        t.add(15, 16);
        System.out.println(t);
        if (t.contains(8, 9)) {
            System.out.println("[8,9] ist vorhanden und wird entfernt");
            t.remove(8, 9);
        } else {
            System.out.println("[8,9] ist nicht da");
        }
        if (t.contains(14, 16)) {
            System.out.println("[14, 16] ist vorhanden und wird entfernt");
            t.remove(8, 9);
        } else {
            System.out.println("[14, 16] ist nicht da");
        }

        System.out.println(t);
        t.add(8, 10);
        System.out.println(t);
        System.out.println("ende");
    }

    /**
     * This method tries to add an entry to the time line in a consistent way
     *
     * @param start
     * @param end
     * @return true if an entry was added, false otherwise
     * @throws IllegalArgumentException when start >= end
     */
    public boolean add(int start, int end) {
        return add(new Entry(start, end));
    }

    public boolean add(Entry newElement){
        if (isItPossibleToAddElement(newElement)) return entries.add(newElement);
        else return false;
    }

    public boolean isItPossibleToAddElement(Entry newElement){
        if (entries.contains(newElement)) return false;
        Entry floor = entries.floor(newElement);
        Entry higher = entries.higher(newElement);

        // if floor and/or higher are null they will not overlap with newElement and therefore the new element can be added
        return !newElement.overlaps(floor) && !newElement.overlaps(higher);
    }

    /**
     * This method checks if the given interval overlaps an existing entries
     *
     * @param start
     * @param end
     * @return true if the intervals overlaps
     */
    public boolean clash(int start, int end) {
        return !isItPossibleToAddElement(new Entry(start, end));
    }

    /**
     * This method checks if the Timeline contains an entry with exactly the given start and stop times
     *
     * @param start
     * @param end
     * @return true if an entry exists
     */

    public boolean contains(int start, int end) {
        return contains(new Entry(start, end));
    }

    public boolean contains(Entry e){
        return entries.contains(e);
    }

    /**
     * This method removes the entry with exactly the given start and stop times from this timeline
     *
     * @param start
     * @param end
     * @return true if entry was removed
     */

    public boolean remove(int start, int end) {
        return remove(new Entry(start,end));
    }

    public boolean remove(Entry e){
        return entries.remove(e);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return entries.toString();
    }

    /**
     * This class represents an entry of the time line. It refers to the next and the previous entry and stores the start and
     * the end time of the entry.
     */
    private class Entry implements Comparable<Entry>{

        /**
         * end time
         */
        public int end;

        /**
         * start time
         */
        public int start;

        /**
         * Constructor sets start and end time
         *
         * @param start
         * @param end
         */
        public Entry(int start, int end) throws IllegalArgumentException{
            if (start > end) {
                if (TPS_Logger.isLogging(SeverenceLogLevel.ERROR)) {
                    TPS_Logger.log(SeverenceLogLevel.ERROR,
                            "Start is greater or equal to end (start=" + start + ", end=" + end + ")");
                }
                throw new IllegalArgumentException();
            }
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(Entry entry) {
            if (start-entry.start != 0) return start-entry.start;
            else return end-entry.end;
        }

        /**
         * Checks if two intervals/entries overlap
         * We consider two elements not overlapping if one of them is null
         *
         * @param entry
         * @return
         */
        public boolean overlaps(Entry entry) {
            if (entry == null) return false;
            if (this.compareTo(entry) == 0 ) {
                return true;
            }
            else if (this.compareTo(entry) < 0 ) {
                return this.end >= entry.start;
            }
            else if (this.compareTo(entry) > 0 ) {
                return entry.end >= this.start;
            }
            return false;
        }
    }
}
