package de.dlr.ivf.tapas.runtime.client.Graphics.ActivityTimeChart;

import org.jfree.data.category.DefaultCategoryDataset;

import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("rawtypes")
public class ActivityTimeDataset extends DefaultCategoryDataset {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final EnumMap<HourInterval, Number> reference = new EnumMap<>(HourInterval.class);
    private final EnumMap<HourInterval, Number> model = new EnumMap<>(HourInterval.class);

    public ActivityTimeDataset(HashMap<Date, Number> reference, HashMap<Date, Number> model) {
        for (Entry<Date, Number> e : reference.entrySet()) {
            this.reference.put(HourInterval.getInterval(e.getKey()), e.getValue());
        }

        for (Entry<Date, Number> e : model.entrySet()) {
            this.model.put(HourInterval.getInterval(e.getKey()), e.getValue());
        }

    }

    @Override
    public int getColumnCount() {
        return HourInterval.values().length;
    }

    @Override
    public int getColumnIndex(Comparable key) {

        if (key instanceof HourInterval) {
            return ((HourInterval) key).ordinal();
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Comparable getColumnKey(int column) {
        return HourInterval.getById(column);
    }

    @Override
    public List getColumnKeys() {
        return Arrays.asList(HourInterval.values());
    }

    @Override
    public int getRowCount() {
        return RowKey.values().length;
    }

    @Override
    public int getRowIndex(Comparable key) {

        if (key instanceof RowKey) {
            return ((RowKey) key).ordinal();
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Comparable getRowKey(int row) {
        return RowKey.values()[row];
    }

    @Override
    public List getRowKeys() {
        return Arrays.asList(RowKey.values());
    }

    @Override
    public Number getValue(Comparable rowKey, Comparable columnKey) {
        if (rowKey instanceof RowKey && columnKey instanceof HourInterval) {
            switch ((RowKey) rowKey) {
                case MODEL:
                    return model.get(columnKey);
                case REFERENCE:
                    return reference.get(columnKey);
                default:
                    return null;
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Number getValue(int row, int column) {
        return getValue(RowKey.values()[row], HourInterval.values()[column]);
    }

    /**
     * This enumeration covers a whole day with hour wide intervals. The first
     * is <code>[23:30, 0:30)</code> and the last is <code>[22:30, 23:30)</code>
     * .
     *
     * @author boec_pa
     */
    public enum HourInterval {
        I_00(0), I_01(1), I_02(2), I_03(3), I_04(4), I_05(5), I_06(6), I_07(7), I_08(8), I_09(9), I_10(10), I_11(
                11), I_12(12), I_13(13), I_14(14), I_15(15), I_16(16), I_17(17), I_18(18), I_19(19), I_20(20), I_21(
                21), I_22(22), I_23(23);

        private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

        /**
         * redundant at the moment. May be used when {@link HourInterval} does
         * not cover the whole day.
         */
        private final int interval;
        private final long milliInterval;

        HourInterval(int time) {
            this.interval = time;

            // time and 30 minutes more
            milliInterval = this.interval * 60 * 60 * 1000 + 30 * 60 * 1000;
        }

        public static HourInterval getById(int i) {
            return values()[i];
        }

        public static HourInterval getInterval(Date time) {

            // TODO check for time zone/summer time problems
            long t = time.getTime() % MILLIS_PER_DAY;

            for (HourInterval h : values()) {
                if (t < h.milliInterval) return h;
            }

            // if 23:31 ...
            return I_00;
        }

        @Override
        public String toString() {
            return Integer.toString(interval);
        }

    }

    public enum RowKey {
        REFERENCE, MODEL
    }
}
