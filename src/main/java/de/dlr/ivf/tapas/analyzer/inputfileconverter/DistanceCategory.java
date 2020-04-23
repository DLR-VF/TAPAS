package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.util.List;

public enum DistanceCategory {
    CAT_1(0, 1000, "< 1 km"), CAT_2(1, 3000, "1-3 km"), CAT_3(2, 5000, "3-5 km"), CAT_4(3, 7000, "5-7 km"), CAT_5(4,
            10000, "7-10 km"), CAT_6(5, 25000, "10-25 km"), CAT_7(6, 100000, "25-100 km"), CAT_8(7, Double.MAX_VALUE,
            ">=100 km");

    private final int id;
    private final double maxDistance;
    private final String description;

    /**
     * @param id          die ID der Kategorie
     * @param maxDistance die maximale Distanz (ausschließlich, der angegebene Wert ist nicht mehr gültig)
     */
    DistanceCategory(int id, double maxDistance, String description) {
        this.id = id;
        this.maxDistance = maxDistance;
        this.description = description;

    }

    /**
     * Erzeugt aus den {@link DistanceCategory} einen einheitlichen Namen. z.B. Wird aus {@link DistanceCategory#CAT_2}
     * und {@link DistanceCategory#CAT_3} = 1-5 km
     *
     * @param categories
     * @return
     */
    public static String createDistanceCategoriesID(List<DistanceCategory> categories) {
        StringBuilder ret = new StringBuilder();
        for (DistanceCategory cat : categories) {
            ret.append(cat.getId()).append("u");
        }
        return ret.substring(0, ret.length() - 1);
    }

    /**
     * Erzeugt aus den {@link DistanceCategory} einen einheitlichen Namen. z.B. Wird aus {@link DistanceCategory#CAT_2} und {@link DistanceCategory#CAT_3} = 1-5 km
     *
     * @param categories Elemente müssen aufsteigend sortiert sein
     * @return
     */
    public static String createDistanceCategoriesName(List<DistanceCategory> categories) {
        if (categories.contains(DistanceCategory.CAT_1) && categories.contains(DistanceCategory.CAT_8)) {
            return "Beliebig";
        } else if (categories.contains(DistanceCategory.CAT_1)) {
            // Wenn die erste Kategorie (< 1km) enthalten ist, wird der neue Name: < xkm. Wobei x der größten Zahl in den Kategorien entspricht
            return "<" + categories.get(categories.size() - 1).getDescription().replaceFirst("(<|\\d+-)", "");
        } else if (categories.contains(DistanceCategory.CAT_8)) {
            return ">=" + categories.get(0).getDescription().replaceFirst("(>|(-\\d+))", "");
        } else {
            String descBeginning = categories.get(0).getDescription();
            String descEnd = categories.get(categories.size() - 1).getDescription();
            String beginning = descBeginning.substring(0, descBeginning.indexOf('-'));
            String end = descEnd.substring(descEnd.indexOf('-') + 1);
            return beginning + "-" + end;
        }
    }

    /**
     * @param dist
     * @return die DistanceCategory dessen obere Distanzgrenze am nächsten an der angebenen größe ist und gleichzeitig größer als die angebene ist. Sollte keine DistanceCategory eine größere Distance
     * haben wird immer die größte zurückgegeben.
     */
    public static DistanceCategory getByDistance(double dist) {
        for (DistanceCategory cat : DistanceCategory.values()) {
            if (dist < cat.getMaxDistance()) {
                return cat;
            }
        }
        return DistanceCategory.values()[DistanceCategory.values().length - 1];
    }

    /**
     * @param id
     * @return die DistanceCategory die die angegebene ID besitzt
     * @throws IllegalArgumentException wenn die ID keiner Kategorie zugeordnet werden kann
     */
    public static DistanceCategory getById(int id) throws IllegalArgumentException {
        for (DistanceCategory cat : DistanceCategory.values()) {
            if (cat.getId() == id) {
                return cat;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getDescription() {
        return description;
    }

    public int getId() {
        return id;
    }

    protected double getMaxDistance() {
        return maxDistance;
    }
}
