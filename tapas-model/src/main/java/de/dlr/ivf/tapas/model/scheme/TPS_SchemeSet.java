/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.model.scheme;

import de.dlr.ivf.tapas.model.constants.TPS_PersonGroup;
import de.dlr.ivf.tapas.logger.legacy.LogHierarchy;
import de.dlr.ivf.tapas.logger.legacy.TPS_Logger;
import de.dlr.ivf.tapas.logger.legacy.HierarchyLogLevel;
import de.dlr.ivf.tapas.logger.legacy.SeverityLogLevel;
import de.dlr.ivf.tapas.model.distribution.TPS_DiscreteDistribution;
import de.dlr.ivf.tapas.model.person.TPS_Person;
import de.dlr.ivf.tapas.util.ExtendedWritable;
import de.dlr.ivf.tapas.util.NestedIterator;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * This class is a collection of all schemes collected by several scheme classes. Furthermore the scheme classes are
 * weighted by distributions created for different person groups.
 */
@LogHierarchy(hierarchyLogLevel = HierarchyLogLevel.PLAN)
@Component
public class TPS_SchemeSet implements Iterable<TPS_Scheme>, ExtendedWritable {
    /// Collection of scheme class distributions mapped by the person group
    private final SortedMap<TPS_PersonGroup, TPS_DiscreteDistribution<Integer>> distributionMap;

    /// Collection of scheme classes mapped by their own id
    private final Map<Integer, TPS_SchemeClass> schemeClasses;


    /**
     * Constructor
     */
    public TPS_SchemeSet() {
        this.schemeClasses = new HashMap<>();
        this.distributionMap = new TreeMap<>();
    }


    /**
     * This method adds a scheme class distribution for the given person group
     *
     * @param persGroup    person group
     * @param distribution probability distribution of scheme class ids
     */
    public void addDistribution(TPS_PersonGroup persGroup, TPS_DiscreteDistribution<Integer> distribution) {
        distributionMap.put(persGroup, distribution);
    }


    /**
     * @param key person group to search a distribution for
     * @return true if the scheme set contains a distribution for the given person group, false otherwise
     */
    public boolean containsDistribution(TPS_PersonGroup key) {
        return distributionMap.containsKey(key);
    }


    /**
     * Checks whether the scheme class identified by the id given is contained in the scheme set
     *
     * @param schemeClassId scheme class id corresponding to a scheme class
     * @return true if the scheme class is contained, false otherwise
     */
    public boolean containsSchemeClass(int schemeClassId) {
        return schemeClasses.containsKey(schemeClassId);
    }

    /**
     * Function determines the scheme to be used by the person specified. In case a specific type of scheme is to be
     * preferred, the selection probabilities are adopted.
     *
     * @param person person the scheme is to be selected for
     * @return the scheme which was found, null is also possible
     */
    public TPS_Scheme findScheme(TPS_Person person) {
        TPS_Scheme scheme = this.selectScheme(person);
        // report if wished
        if (TPS_Logger.isLogging(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG)) {
            TPS_Logger.log(HierarchyLogLevel.PERSON, SeverityLogLevel.DEBUG,
                    "Selected scheme (id=" + scheme.getId() + ") of schemeClass (id=" +
                            scheme.getSchemeClass().getId() + ")");
        }
        return scheme;
    }

    /**
     * This method returns the scheme class corresponding to the given id. If the scheme class does not exist yet it is
     * created.
     *
     * @param schemeClassId scheme class id corresponding to a scheme class
     * @return scheme class to the given id
     */
    public TPS_SchemeClass getSchemeClass(int schemeClassId) {
        TPS_SchemeClass sc;
        if (!containsSchemeClass(schemeClassId)) {
            sc = new TPS_SchemeClass(schemeClassId);
            sc.setSchemeSet(this);
            this.schemeClasses.put(sc.getId(), sc);
        } else {
            sc = this.schemeClasses.get(schemeClassId);
        }
        return sc;
    }

    public Integer[] getSchemeClassIdArray() {
        SortedSet<Integer> set = new TreeSet<>(this.schemeClasses.keySet());
        return set.toArray(new Integer[0]);
    }

    /**
     * @return Iterable over all scheme classes
     */
    public Iterable<TPS_SchemeClass> getSchemeClassIterable() {
        return this.schemeClasses.values();
    }

    /**
     * This method calls the init method of every scheme
     */
    public void init() {
        for (TPS_Scheme scheme : this) {
            scheme.init();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<TPS_Scheme> iterator() {
        return new NestedIterator<>(this.schemeClasses.values());
    }

    /**
     * First of all the distribution of the scheme classes for the given person is searched by the person group. Then
     * there is one scheme class drawn out of the distribution. At last, there is a scheme out of the scheme class
     * drawn.
     *
     * @param person person
     * @return the drawn scheme
     */
    public TPS_Scheme selectScheme(TPS_Person person) {
        TPS_DiscreteDistribution<Integer> scDis = this.distributionMap.get(person.getPersonGroup());
        Integer key = scDis.drawKey();
        TPS_SchemeClass sc = this.schemeClasses.get(key);
        return sc.draw();
    }

    public TPS_Scheme selectScheme(TPS_PersonGroup personGroup) {
        TPS_DiscreteDistribution<Integer> scDis = this.distributionMap.get(personGroup);
        Integer key = scDis.drawKey();
        TPS_SchemeClass sc = this.schemeClasses.get(key);
        return sc.draw();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toString("");
    }

    /*
     * (non-Javadoc)
     *
     * @see de.dlr.de.dlr.ivf.util.tapas.ivf.ExtendedWritable#toString(java.lang.String)
     */
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "TAPAS SchemeSet\n" + prefix + " SchemeClass distributions per PersonGroup\n");
        for (TPS_PersonGroup id : this.distributionMap.keySet()) {
            sb.append(prefix + " " + id.getCode() + " -> values=" + this.distributionMap.get(id).toString() + "\n");
        }
        for (TPS_SchemeClass schemeClass : this.schemeClasses.values()) {
            sb.append(schemeClass.toString(prefix + " ") + "\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
