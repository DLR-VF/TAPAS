package de.dlr.ivf.tapas.distribution;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;

import java.util.*;

/**
 * This class represents a discrete probability distribution. Every probability value is referenced by a position/index
 * and a key (or singleton value).
 * The values do not necessarily have to sum up to 1. They will be normalized intrinsically.
 * The values have to be non-negative.
 *
 * @param <Key> key type for the index
 */
public class TPS_DiscreteDistribution<Key> {

    /**
     * value array
     */
    protected double[] values;

    /**
     * This flag indicates if the distribution is normalized
     */
    protected boolean isNormalized;
    protected double[] cumulativeValues;

    /**
     * array list of the keys of the distribution
     * order should be corresponding to the probability values
     * Important! This is only a reference of given list parameter
     * If you change the list or its values from the parameters they will be changed in here too
     */
    private ArrayList<Key> singletons;

    private TPS_DiscreteDistribution(int size) {
        this.setSize(size);
        this.isNormalized = false;
    }

    /**
     * This constructor initializes the indices with the given collection and creates a uniform distribution on its
     * values
     *
     * @param singletons indices
     */
    public TPS_DiscreteDistribution(Collection<Key> singletons) {
        this(singletons.size());
        this.setValues(1.0 / singletons.size()); // set values uniformly
        this.setSingletons(singletons);
    }

    /**
     * Initializes a distribution over singletons with the values as probabilities
     *
     * @param singletons object of the distribution, pay attention! if you change the singletons from the calling
     *                   object they will be changed here too
     * @param values     probability values as a double array
     */
    public TPS_DiscreteDistribution(ArrayList<Key> singletons, double[] values) {
        this(values.length);
        this.setValues(values);
        this.setSingletons(singletons);
    }

    /**
     * Initializes a distribution over singletons with the values as probabilities
     *
     * @param map mapping of singleton values to probability values
     */
    public TPS_DiscreteDistribution(Map<Key, ? extends Number> map) {
        this(new ArrayList<>(map.keySet()), map.values().stream().mapToDouble(Number::doubleValue).toArray());
    }

    /**
     * Initializes a distribution over singletons with the values as probabilities
     *
     * @param singletons object of the distribution
     * @param values     probability values as List of Number extending obejcts (like Integer or Double)
     */
    public TPS_DiscreteDistribution(ArrayList<Key> singletons, List<? extends Number> values) {
        this(singletons, values.stream().mapToDouble(Number::doubleValue).toArray());
    }

    /**
     * Looks for the smallest index in the cumulativeValues such that
     * cumulativeValues[index] >= randomValue and values[index]>0
     *
     * @param randomValue random value
     * @return index s.t. cumulativeValues[index] >= randomValue and values[index]>0
     */
    private int binarySearch(double randomValue) {
        // in case of randomValue==0.0
        // return the first element with a non-zero probability
        if (randomValue == 0.0) {
            for (int i = 0; i < this.size(); i++) {
                if (values[i] > 0.0) return i;
            }
        }
        int low = 0;
        int high = this.size() - 1;
        int index = (low + high) / 2;
        while (low < high) {
            if (cumulativeValues[index] == randomValue) { //found the exact value
                // roll back to the lowest index s.t. cumulativeValues[index] == randomValue
                while (index > 0) {
                    if (cumulativeValues[index - 1] == randomValue) index--;
                    else break;
                }
                return index;
            } else if (cumulativeValues[index] < randomValue) low = index + 1;
            else if (cumulativeValues[index] > randomValue) {
                if (index > 0 && cumulativeValues[index - 1] < randomValue) return index;
                high = index;
            }
            index = (low + high) / 2;
        }
        if (low > high) {
            if (cumulativeValues[index] < randomValue) TPS_Logger.log(SeverenceLogLevel.ERROR, "Something is wrong");
        }
        return index;
    }

    /**
     * This method draws a random value out of the distribution.
     *
     * @return index of the drawn value
     */
    public int draw() {
        return this.draw(Math.random());
    }

    /**
     * This method takes the index of the best fitting value to the given value. The distribution needs not to be
     * normalized.
     *
     * @param randomValue value between 0 and 1
     * @return index of the drawn value
     */
    public int draw(double randomValue) {
        if (this.size() == 0) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Empty distribution!");
            return 0;
        }
        //safety min/max if randomValue is not between 0 and 1
        randomValue = Math.max(0, Math.min(1, randomValue));
        if (!isNormalized()) {
            this.normalize(false);
        }
        if (!isNormalized()) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Distribution cannot be normalized because all" +
                    " values are 0 or there is an infinity or NaN value; sum of values: " + this.sum() +
                    " is Normalized: " + this.isNormalized);
        }
        return this.binarySearch(randomValue);
    }

    /**
     * This method draws a random value out of the distribution by using a random number generator.
     *
     * @param random Random number generator
     * @return index of the drawn value
     */
    public int draw(Random random) {
        if (random == null) {
            return this.draw();
        }
        return this.draw(random.nextDouble());
    }

    /**
     * This method draws a random key out of the distribution.
     *
     * @return a random Key from the singletons by the probabilities from the distribution's values
     */
    public Key drawKey() {
        return this.singletons.get(this.draw());
    }

    /**
     * This method draws a random key out of the distribution.
     *
     * @param randomValue between 0 and 1
     * @return a random Key from the singletons by the probabilities from the distribution's values
     */
    public Key drawKey(double randomValue) {
        return this.singletons.get(this.draw(randomValue));
    }

    /**
     * This method draws a random key out of the distribution.
     *
     * @param random Random number generator
     * @return a random Key from the singletons by the probabilities from the distribution's values
     */
    public Key drawKey(Random random) {
        return this.singletons.get(this.draw(random));
    }


    /**
     * Compares the array values given with the own array values
     *
     * @param obj distribution to compare with
     * @return true if the arrays have the same values, false otherwise
     */
    public boolean equals(TPS_DiscreteDistribution<Key> obj) {
        return Arrays.equals(this.values, obj.values);
    }

    /**
     * Returns the index of the distribution corresponding to the position given
     *
     * @param position position of the value in the distribution
     * @return Index at the position i
     */
    public Key getKey(int position) {
        if (position >= this.singletons.size()) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "Requested position " + position + " out of bounds: " + this.singletons.size());
        }
        return this.singletons.get(position);
    }

    /**
     * Returns an array containing all indices
     *
     * @return all singleton keys
     */
    public ArrayList<Key> getSingletons() {
        return this.singletons;
    }

    /**
     * Sets the keys list to the given list
     *
     * @param singletons keys
     */
    public void setSingletons(Collection<Key> singletons) {
        this.singletons = new ArrayList<>(singletons);
    }

    /**
     * Returns the distribution value for the given key
     *
     * @param key singleton value
     * @return value corresponding to the given key
     */
    public double getValueByKey(Key key) {
        return this.getValueByPosition(this.indexOf(key));
    }

    /**
     * Returns the distribution value for the given position index
     *
     * @param index position
     * @return value corresponding to the given index
     */
    public double getValueByPosition(int index) {
        return this.values[index];
    }

    /**
     * returns the probability values array
     *
     * @return probability array
     */
    public double[] getValues() {
        return values;
    }

    /**
     * Sets the probabilities to the given value
     *
     * @param value single probability value which is then the same for all probabilities
     */
    public void setValues(double value) {
        this.isNormalized = false;
        Arrays.fill(this.values, value);
    }

    /**
     * Sets the probabilities to the given values
     *
     * @param values probability values
     */
    public void setValues(double[] values) {
        this.isNormalized = false;
        if (values == null) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "Input is null!");
            return;
        }
        if (this.values == null) {
            TPS_Logger.log(SeverenceLogLevel.ERROR, "internal distribution is null!");
            return;
        }

        if (values.length != this.size()) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "Different array sizes! given: " + values.length + " expected: " + this.size());
            return;
        }
        System.arraycopy(values, 0, this.values, 0, this.size());
    }

    public int hashCode() {
        return this.singletons.hashCode() + Arrays.hashCode(this.values);
    }

    /**
     * This method returns the index of the given key in the array distribution
     *
     * @param key key
     * @return value between 0 and length-1 -> index of the value in the distribution corresponding to the given key <br>
     * -1 -> if the key doesn't exist
     */
    private int indexOf(Key key) {
        for (int i = 0; i < this.singletons.size(); i++) {
            if (this.singletons.get(i).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * This method normalizes the distribution, i.e. that the sum of all values is 1.
     * and computes the cumulative sum of the probability values
     * also cumulativeValues[this.size()-1] will equal 1
     *
     * @return true if the distribution is normalized, false otherwise
     */
    public boolean isNormalized() {
        return isNormalized;
    }

    public boolean normalize() {
        return normalize(true);
    }

    /**
     * This method normalizes the distribution and computes the cumulative sum,
     * i.e. cumulativeValues[this.size()-1] will equal 1
     *
     * @param normalizeValues if true it normalizes the values array too, otherwise the probability values will be left
     *                        untouched
     * @return true if normalizing was successful, false otherwise
     */
    public boolean normalize(boolean normalizeValues) {
        double sum = this.sum();
        if (sum > 0.0) {
            for (int i = 0; i < this.size(); i++) {
                this.cumulativeValues[i] = (i > 0 ? this.cumulativeValues[i - 1] : 0) + this.values[i] / sum;
                if (normalizeValues) this.values[i] = this.values[i] / sum; //normalize if true
            }
            this.isNormalized = true;
            return true;
        } else {
            if (TPS_Logger.isLogging(SeverenceLogLevel.ERROR)) {
                TPS_Logger.log(SeverenceLogLevel.ERROR, "Trying to normalize a distribution with a sum of zero!");
            }
        }
        return false;
    }

    /**
     * Initializes the arrays
     *
     * @param size array size
     */
    public void setSize(int size) {
        this.values = new double[size];
        this.cumulativeValues = new double[size];
    }

    /**
     * Returns the distribution value for the given key
     *
     * @param key   key
     * @param value probability value
     */
    public void setValueByKey(Key key, double value) {
        this.setValueByPosition(this.indexOf(key), value);
    }

    public void setValueByPosition(int index, double value) {
        this.isNormalized = false;
        if (index < 0 || index >= this.values.length) {
            throw new IllegalArgumentException(
                    "Index must be greater or equal than 0 or less than: " + this.values.length + " given index:" +
                            index);
        }
        this.values[index] = value;
    }

    public int size() {
        return this.values.length;
    }

    /**
     * Returns the sum of all values; if normalized this is 1
     *
     * @return sum of all values
     */
    public double sum() {
        double sum = Arrays.stream(this.values).sum();
        if (Double.isNaN(sum) || Double.isInfinite(sum)) {
            TPS_Logger.log(SeverenceLogLevel.ERROR,
                    "ERROR: NaN or infinity detected! Distribution: " + this.toString());
            throw new RuntimeException("ERROR: NaN or infinity detected! Index: Distribution: " + this.toString());
        }
        return sum;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.ivf.tapas.distribution.TPS_AbstractDiscreteDistribution#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Key index : this.singletons) {
            sb.append(index).append("->").append(this.getValueByKey(index)).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("]");
        return sb.toString();
    }

}
