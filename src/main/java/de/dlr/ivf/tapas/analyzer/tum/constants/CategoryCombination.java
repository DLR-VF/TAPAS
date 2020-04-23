package de.dlr.ivf.tapas.analyzer.tum.constants;

import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Immutable object used as key for different splits.
 *
 * @author boec_pa
 */
@SuppressWarnings("rawtypes")
public class CategoryCombination {

    final int hashCode;
    final Enum[] categories;

    /**
     * @param categories combination of different enum entries. The order is important
     *                   for the <code>toString</code> method only. Example:
     *                   [Mode.BIKE, RegionCode.REGION_0]
     */
    public CategoryCombination(Enum... categories) {
        this.categories = Arrays.copyOf(categories, categories.length);
        int hash = 0;
        for (Enum cat : categories) {
            if (!Categories.isValid(cat)) {
                throw new IllegalArgumentException(cat.getClass().getName() + " is no valid category.");
            }
            hash = hash ^ cat.hashCode();

        }
        hashCode = hash;

    }

    /**
     * Returns a list of all possible combinations of the given categories.
     *
     * @param categories
     */
    public static ArrayList<CategoryCombination> listAllCombinations(Categories... categories) {

        int nCat = categories.length;
        int[] idx = new int[nCat];
        int[] catLength = new int[nCat];

        for (int i = 0; i < nCat; i++) {
            catLength[i] = categories[i].getEnumeration().getEnumConstants().length;
        }

        ArrayList<CategoryCombination> result = new ArrayList<>();

        if (nCat == 0) {
            return result;
        }

        Enum[] tmpCats = new Enum[nCat];
        while (idx[nCat - 1] < catLength[nCat - 1]) {
            if (idx[0] == catLength[0]) {// overrun 59 -> 60
                int i = 0;
                while (idx[i] == catLength[i] && i < nCat - 1) {
                    idx[i] = 0;
                    idx[++i]++;
                }
            }

            if (idx[nCat - 1] < catLength[nCat - 1]) {
                // fill array
                for (int i = 0; i < nCat; i++) {
                    tmpCats[i] = (Enum) categories[i].getEnumeration().getEnumConstants()[idx[i]];
                }
                result.add(new CategoryCombination(tmpCats));
                idx[0]++;
            }
        }

        return result;
    }

    public boolean contains(Categories category) {
        for (Enum cat : categories) {
            if (Categories.getByClass(cat.getClass()) == category) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CategoryCombination)) return false;
        CategoryCombination cc = (CategoryCombination) obj;
        if (cc.categories.length != this.categories.length) return false;

        // TODO saver method to do equals
        return cc.hashCode == this.hashCode;
    }

    public Enum[] getCategories() {
        return categories;
    }

    /**
     * Returns the <code>Enum</code> element of the given <code>category</code>.
     *
     * @param category
     * @return <code>null</code> if the category is not found.
     */
    public Enum getCategory(Categories category) {
        for (Enum cat : categories) {
            if (Categories.getByClass(cat.getClass()) == category) return cat;
        }
        return null;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        for (Enum e : categories) {
            s.append(e.toString()).append(",");
        }
        s.setCharAt(s.length() - 1, '}');// replace last comma with bracket
        return s.toString();
    }
}