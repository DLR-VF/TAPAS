package de.dlr.ivf.tapas.model.plan;

import java.util.HashMap;
import java.util.Map;

/**
 * The StayHierarchies class represents a collection of stay hierarchies mapped to a scheme tour.
 */
public class StayHierarchies {

    private final Map<Integer,StayHierarchy> stayHierarchyMap;


    public StayHierarchies() {
        this.stayHierarchyMap = new HashMap<>();
    }

    /**
     * Retrieves the StayHierarchy object associated with the specified tour ID.
     *
     * @param tourId   The ID of the tour.
     * @return The StayHierarchy object associated with the tour ID,
     *         or null if no StayHierarchy is found.
     */
    public StayHierarchy getStayHierarchy(int tourId) {
        return stayHierarchyMap.get(tourId);
    }

    /**
     * Adds a StayHierarchy object to the collection, mapped to the specified tour ID.
     *
     * @param tourId   The ID of the tour.
     * @param stayHierarchy The StayHierarchy object to be added.
     */
    public void addStayHierarchy(int tourId, StayHierarchy stayHierarchy) {
        stayHierarchyMap.put(tourId, stayHierarchy);
    }
}
