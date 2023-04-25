/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model;

import de.dlr.ivf.tapas.logger.LogHierarchy;
import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;
import de.dlr.ivf.tapas.logger.HierarchyLogLevel;

/**
 * This class represents a time line. It is possible to add entries from a start to an end point. These entries build up a
 * linked list in both directions. The time line holds the entries consistent, i.e. there can be no entries added which
 * overlaps already added entries.
 *
 * @author mark_ma
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
public class Timeline {

    /**
     * The entrance entry
     */
    private final Entry entrance;
    /**
     * Internal storage for a pair of references
     */
    private final Entry[] entries;
    /**
     * The exit entry
     */
    private final Entry exit;

    /**
     * The Constructor initialises the member values
     */
    public Timeline() {
        this.entries = new Entry[2];
        this.entrance = new Entry(Integer.MIN_VALUE, Integer.MIN_VALUE);
        this.exit = new Entry(Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.entrance.next = this.exit;
        this.exit.previous = this.entrance;
    }

    /**
     * Small test for the time line
     *
     * @param args
     */
    public static void main(String[] args) {
        Timeline t = new Timeline();

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
        if (start > end) {
            if (TPS_Logger.isLogging(SeverityLogLevel.WARN)) {
                TPS_Logger.log(SeverityLogLevel.WARN,
                        "Start is greater or equal to end (start=" + start + ", end=" + end + ")");
            }
            return false;
        }
        Entry[] entryArray = getSurroundingEntries(start, end);
        if (entryArray == null) {
            return false;
        }
        Entry entry = new Entry(start, end);
        entryArray[0].next = entry;
        entry.previous = entryArray[0];
        entryArray[1].previous = entry;
        entry.next = entryArray[1];
        return true;
    }

    /**
     * This method checks if the given interval overlaps an existing entries
     *
     * @param start
     * @param end
     * @return true if the intervals overlaps
     */
    public boolean clash(int start, int end) {
        return getSurroundingEntries(start, end) == null;
    }

    /**
     * This method checks if the Timeline contains an entry with exactly the given start and stop times
     *
     * @param start
     * @param end
     * @return true if an entry exists
     */

    public boolean contains(int start, int end) {
        boolean entryFound = false;

        for (Entry e = entrance.next; e != this.exit; e = e.next) {
            if (e.start == start && e.end == end) {
                entryFound = true;
                break;
            }
        }

        return entryFound;
    }

    /**
     * This method returns the surrounding entries to the given interval. When the interval don't overlaps another one, then
     * the next and the previous interval is returned.
     *
     * @param start
     * @param end
     * @return next and previous entry to the given interval
     */
    private Entry[] getSurroundingEntries(int start, int end) {
        Entry e;
        this.entries[0] = null;
        this.entries[1] = null;
        for (e = entrance; e != null && (this.entries[0] == null || this.entries[1] == null); e = e.next) {
            if (this.entries[0] == null && e.end <= start && e.next.start >= end) this.entries[0] = e;
            if (this.entries[1] == null && e.start >= end && e.previous.end <= start) this.entries[1] = e;
        }

        if (this.entries[0] == null || this.entries[1] == null || this.entries[0].next != this.entries[1]) {
            return null;
        }

        return this.entries;
    }

    /**
     * This method removes the entry with exactly the given start and stop times from this timeline
     *
     * @param start
     * @param end
     * @return true if entry was removed
     */

    public boolean remove(int start, int end) {
        boolean entryRemoved = false;

        for (Entry e = entrance.next; e != this.exit && !entryRemoved; e = e.next) {
            if (e.start == start && e.end == end) {
                //unlink this element from the list
                e.previous.next = e.next;
                e.next.previous = e.previous;
                entryRemoved = true;
            }
        }

        return entryRemoved;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + "{");
        for (Entry e = entrance.next; e != this.exit; e = e.next) {
            sb.append("[" + e.start + "," + e.end + "]");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * This class represents an entry of the time line. It refers to the next and the previous entry and stores the start and
     * the end time of the entry.
     *
     * @author mark_ma
     */
    private class Entry {

        /**
         * end time
         */
        public int end;

        /**
         * reference to the next entry
         */
        public Entry next = null;

        /**
         * reference to the previous entry
         */
        public Entry previous = null;

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
        public Entry(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
