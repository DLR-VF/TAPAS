package de.dlr.ivf.tapas.model.constants;

import lombok.Builder;
import lombok.Singular;

import java.util.Map;

@Builder
public class AgeClasses {

    /**
     * maps ids from the db to {@link TPS_AgeClass} objects constructed from corresponding db data
     */
    @Singular
    private final Map<Integer, TPS_AgeClass> ageClasses;

    /**
     * Static constant which represents all ages [0, infinite[
     */
    private final TPS_AgeClass NON_RELEVANT = TPS_AgeClass.builder()
            .id(-1)
            .min(0)
            .max(2000)
            .build();

    /**
     * This method searches the corresponding age class to the given age., When there exists no appropriate age class the
     * default age class NON_RELEVANT is returned.
     *
     * @param age should be a non-negative integer
     * @return corresponding age class
     */
    public TPS_AgeClass getClassByAge(int age){

        return ageClasses.values()
                .stream()
                .filter(ageClass -> fits(age, ageClass))
                .filter(ageClass -> !ageClass.equals(NON_RELEVANT))
                .findFirst()
                .orElse(NON_RELEVANT);
    }

    /**
     * @param age should be a non-negative integer
     * @return true if the given age is inside the interval [minimum, maximum], false otherwise
     */
    private boolean fits(int age, TPS_AgeClass ageClass) {

        return ageClass.getMin() <= age && age <= ageClass.getMax();
    }
}
