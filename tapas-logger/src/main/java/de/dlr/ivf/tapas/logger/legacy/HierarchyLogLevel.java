package de.dlr.ivf.tapas.logger.legacy;

public     /**
 * The HierarchyLogLevel: specifies the Hierarchy within the application
 *
 * @author hein_mh
 */
enum HierarchyLogLevel {
    OFF(0), APPLICATION(1), CLIENT(2), THREAD(3), HOUSEHOLD(4), PERSON(5), PLAN(6), EPISODE(7), ALL(8);

    private final int index;

    private final int value;

    private final int mask;

    /**
     * Constructor, which initialises the internal variables for the given index.
     *
     * @param index
     */
    HierarchyLogLevel(int index) {
        this.index = index;
        this.mask = (int) Math.pow(2, index) - 1;
        this.value = (int) Math.pow(2, Math.max(0, index - 1));
    }

    /**
     * Getter for the index of this instance. The higher the more important.
     *
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     * This method returns true if the instance is included in the logging
     *
     * @param hLog
     * @return
     */
    public boolean includes(HierarchyLogLevel hLog) {
        return (this.mask & hLog.value) > 0;
    }
}
