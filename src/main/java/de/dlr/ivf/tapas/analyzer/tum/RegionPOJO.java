/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.*;
import de.dlr.ivf.tapas.analyzer.tum.RegionPOJO.ModalSplitForDistanceCategory.ModalSplitForDistanceCategoryElement;
import de.dlr.ivf.tapas.analyzer.tum.RegionPOJO.ModalSplitForDistanceCategoryAndTripIntention.ModalSplitForDistanceCategoryAndTripIntentionElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class RegionPOJO implements Comparable<RegionPOJO> {
    private final RegionCode regionCode;
    private double medLength;
    private long medDur;
    private long nbTrips;
    private long nbPers;
    private long[] cntTripsPGViseva;
    private long[] cntDistTripsPGViseva;
    private long[] cntDurTripsPGViseva;
    private long[] cntTrips;
    private long[] cntDistTrips;
    private long[] cntDurTrips;
    private long[][][] distCatViseva;
    private long[] aggCounter;
    private Map<PersonGroup, Long> persBuffs;
    private double distMin;
    private double distMax;
    private double durMin;
    private double durMax;
    private long[][][] cntModeViseva;
    private long[][] cntTripIntentionPersonGroupViseva;
    private long[][] cntTripIntentionPersonGroupTapas;
    private long[] cntTripsPGTapas;
    private long[] cntDistTripsPGTapas;
    private long[] cntDurTripsPGTapas;
    private long[][][] distCatTapas;
    private long[][][] cntModeTapas;
    private long[][][] distModeTapas;
    public RegionPOJO(RegionCode regionCode) {
        this.regionCode = regionCode;
    }

    private long calcArraySum(long[] arr) {
        long counter = 0;
        for (long i : arr) {
            counter += i;
        }
        return counter;
    }

    public int compareTo(RegionPOJO compareObject) {
        return -(this.regionCode.getId() - compareObject.getRegionCode().getId());
    }

    public double getAvgNrOfTripsForPersonGroup(PersonGroup pg) {

        return saveDiv(cntTripsPGViseva[pg.getId()], persBuffs.get(pg));
    }

    public double getAvgNrOfTripsPerPerson() {

        return saveDiv(nbTrips, nbPers);
    }

    public long getAvgTripDurationForPersonGroup(PersonGroup pg) {

        return Math.round(saveDiv(cntDurTripsPGViseva[pg.getId()], cntTripsPGViseva[pg.getId()]));

    }

    public long getAvgTripDurationForTripIntention(TripIntention ti) {
        return Math.round(saveDiv(cntDurTrips[ti.getId()], cntTrips[ti.getId()]));
    }

    public double getAvgTripLengthForPersonGroup(PersonGroup pg) {

        return saveDiv(cntDistTripsPGViseva[pg.getId()], cntTripsPGViseva[pg.getId()]);

    }

    public double getAvgTripLengthForTripIntention(TripIntention ti) {
        return saveDiv(cntDistTrips[ti.getId()], cntTrips[ti.getId()]);
    }

    public long getCountOfTripsForTripIntention(TripIntention ti) {
        long[][] cntTripsPerDistCatAndPersonGroup = distCatViseva[ti.getId()];
        long cnt = 0;
        for (long[] tripsCntPerDistCat : cntTripsPerDistCatAndPersonGroup) {
            cnt += calcArraySum(tripsCntPerDistCat);
        }
        return cnt;
    }

    public long getCountOfTripsForTripIntentionAndPersonGroupTapas(TripIntention ti, Job pg) {
        long sum = 0;
        for (Mode m : Mode.values()) {
            sum += cntModeTapas[m.getId()][ti.getId()][pg.getId() - 1];
        }
        return sum;
    }

    public long getCountOfTripsForTripIntentionAndPersonGroupViseva(TripIntention ti, PersonGroup pg) {
        long sum = 0;
        for (Mode m : Mode.values()) {
            sum += cntModeViseva[m.getId()][ti.getId()][pg.getId()];
        }
        return sum;
    }

    public long getCountTripsByTripIntention(TripIntention ti) {
        return cntTrips[ti.getId()];
    }

    public long getCountTripsPGViseva(PersonGroup pg) {
        return cntTripsPGViseva[pg.getId()];
    }

    public double getDistMax() {
        return distMax;
    }

    public void setDistMax(double distMax) {
        this.distMax = distMax;
    }

    public double getDistMin() {
        return distMin;
    }

    public void setDistMin(double distMin) {
        this.distMin = distMin;
    }

    public double getDurMax() {
        return durMax;
    }

    public void setDurMax(double durMax) {
        this.durMax = durMax;
    }

    public double getDurMin() {
        return durMin;
    }

    public void setDurMin(double durMin) {
        this.durMin = durMin;
    }

    public long getMedDur() {
        return medDur;
    }

    public void setMedDur(long medDur) {
        this.medDur = medDur;
    }

    public double getMedLength() {
        return medLength;
    }

    public void setMedLength(double medLength) {
        this.medLength = medLength;
    }

    public double getModalSplit(Mode mode) {
        return saveDiv(getModalSplitAbs(mode), nbTrips) * 100.0;

    }

    public double getModalSplitAbs(Mode mode) {
        long counter = 0;
        for (long[] iA : cntModeViseva[mode.getId()]) {
            counter += calcArraySum(iA);
        }
        return (double) counter;
    }

    /**
     * Berechnet die Modalsplits für die {@link DistanceCategory} die in dem übergebenen Array true sind. Felder die hintereinander false sind werden mit dem nächsten true-Feld zusammengefasst. D.h.
     * ist das erste und 4. Feld selektiert, gibt es eine Kategorie 1 und eine Kategorie 2-4.
     *
     * @param modalSplitForDistanceCatFilter
     * @return
     */
    public Map<Mode, ModalSplitForDistanceCategory> getModalSplitForDistanceCategory(boolean[] modalSplitForDistanceCatFilter) {
        Map<Mode, ModalSplitForDistanceCategory> result = new HashMap<>();

        for (Mode mode : Mode.values()) {
            ModalSplitForDistanceCategory modalSplit = new ModalSplitForDistanceCategory();
            modalSplit.mode = mode;
            long otherAbs = 0;
            long tripsAll = 0;
            ModalSplitForDistanceCategoryElement element = null;
            for (DistanceCategory cat : DistanceCategory.values()) {

                if (element == null) element = new ModalSplitForDistanceCategoryElement();
                element.getDistanceCategories().add(cat);
                if (!modalSplitForDistanceCatFilter[cat.getId()]) {
                    // Wenn diese Kategorie nicht selektiert wurde, addiere die Werte zu otherAbs dazu
                    otherAbs += getModalSplitForDistanceCategoryAbs(mode, cat);

                    for (Mode m : Mode.values()) {
                        for (TripIntention ti : TripIntention.values()) {
                            tripsAll += distModeTapas[m.getId()][ti.getId()][cat.getId()];
                        }
                    }

                } else {
                    // Wenn diese Kategorie selektiert wurde, addiere die Werte zu otherAbs dazu und setze diese Werte + den relativen Anteil im
                    // element.
                    otherAbs += getModalSplitForDistanceCategoryAbs(mode, cat);
                    for (Mode m : Mode.values()) {
                        for (TripIntention ti : TripIntention.values()) {
                            tripsAll += distModeTapas[m.getId()][ti.getId()][cat.getId()];
                        }
                    }
                    element.modalSplit = saveDiv(otherAbs, tripsAll) * 100.0;
                    element.modalSplitAbs = otherAbs;
                    // Füge das Element dem Modalspli hinzu und setze wieder alle Zähler auf 0
                    modalSplit.getElements().add(element);
                    element = null;
                    otherAbs = 0;
                    tripsAll = 0;

                }
            }
            result.put(mode, modalSplit);
        }
        return result;
    }

    private double getModalSplitForDistanceCategory(Mode mode, DistanceCategory cat) {
        long trips = getModalSplitForDistanceCategoryAbs(mode, cat);
        long tripsAll = 0;
        for (Mode m : Mode.values()) {
            for (TripIntention ti : TripIntention.values()) {
                tripsAll += distModeTapas[m.getId()][ti.getId()][cat.getId()];
            }
        }
        if (trips == 0 || tripsAll == 0) return 0;// 66.39270042487985+24.968474148802017+12.29344159185774
        return saveDiv(trips, tripsAll) * 100.0;
    }

    private long getModalSplitForDistanceCategoryAbs(Mode mode, DistanceCategory cat) {
        long trips = 0;
        for (TripIntention ti : TripIntention.values()) {
            trips += distModeTapas[mode.getId()][ti.getId()][cat.getId()];
        }
        return trips;
    }

    /**
     * Berechnet die Modalsplits für die {@link DistanceCategory} und die {@link TripIntention} bei dem in dem jeweiligen übergebenen Array der Wert true ist. Alle {@link DistanceCategory} und
     * {@link TripIntention} die im Array false sind werden zur Kategorie "other" zusammengefasst
     *
     * @param modalSplitForDistanceCatAndTripIntentionFilterDistCat
     * @param modalSplitForDistanceCatAndTripIntentionFilterTripInt
     * @return
     */
    public Map<Mode, ModalSplitForDistanceCategoryAndTripIntention> getModalSplitForDistanceCategoryAndTripIntention(boolean[] modalSplitForDistanceCatAndTripIntentionFilterDistCat, boolean[] modalSplitForDistanceCatAndTripIntentionFilterTripInt) {

        Map<Mode, ModalSplitForDistanceCategoryAndTripIntention> result = new HashMap<>();

        for (Mode mode : Mode.values()) {
            ModalSplitForDistanceCategoryAndTripIntention modalSplit = new ModalSplitForDistanceCategoryAndTripIntention();
            modalSplit.mode = mode;
            long tripsAll = 0;
            Map<TripIntention, Long> tripsAllTripIntentionSeparated = new HashMap<>();

            ModalSplitForDistanceCategoryAndTripIntentionElement element = null;
            for (DistanceCategory cat : DistanceCategory.values()) {
                // Iteriere über alle Distanzkategorien
                // Ein neues Element wird immer erzeugt, wenn die aktuelle Distanzkategorie zu einer neuen Gruppe zusammengefasster Elemente gehört
                if (element == null) element = new ModalSplitForDistanceCategoryAndTripIntentionElement();
                element.getDistanceCategories().add(cat);
                // Wenn die Distanzkategorie in der UI nicht ausgewählt wurde, zähle "erstmal" nur die absoluten Werte hoch
                if (!modalSplitForDistanceCatAndTripIntentionFilterDistCat[cat.getId()]) {

                    for (TripIntention ti : TripIntention.values()) {
                        // Iteriere über alle Wegezwecke
                        long modalSplitAbs = getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti);

                        // Wenn der Wegezweck ausgewählt wurde, zähle die absoluten Werte für diesen Wegezweck hoch
                        if (modalSplitForDistanceCatAndTripIntentionFilterTripInt[ti.getId()]) {
                            long modalSplitAbsOld = element.containsModalSplitTripIntentionSeparatedAbs(ti) ? element
                                    .getModalSplitTripIntentionSeparatedAbs(ti) : 0;
                            element.putModalSplitTripIntentionSeparatedAbs(ti, modalSplitAbsOld + modalSplitAbs);

                            if (!tripsAllTripIntentionSeparated.containsKey(ti)) tripsAllTripIntentionSeparated.put(ti,
                                    0L);
                            for (Mode m : Mode.values()) {
                                tripsAllTripIntentionSeparated.put(ti, tripsAllTripIntentionSeparated.get(ti) +
                                        distModeTapas[m.getId()][ti.getId()][cat.getId()]);
                            }
                        } else {
                            // andernfalls Zähle die Werte für "sonstiges" hoch
                            element.modalSplitOtherAbs += modalSplitAbs;
                            for (Mode m : Mode.values()) {
                                tripsAll += distModeTapas[m.getId()][ti.getId()][cat.getId()];
                            }
                        }

                    }
                } else {

                    // Wenn diese Kategorie selektiert wurde zähle die absoluten Werte hoch und berechne daraus die relativen Werte
                    for (TripIntention ti : TripIntention.values()) {

                        long modalSplitAbs = getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti);

                        if (modalSplitForDistanceCatAndTripIntentionFilterTripInt[ti.getId()]) {
                            // Wenn der Wegezweck ausgewählt wurde, zähle die absoluten Werte für diesen Wegezweck hoch
                            // und berechne die relativen Anteile für diesen Wegezweck
                            long modalSplitAbsOld = element.containsModalSplitTripIntentionSeparatedAbs(ti) ? element
                                    .getModalSplitTripIntentionSeparatedAbs(ti) : 0;
                            element.putModalSplitTripIntentionSeparatedAbs(ti, modalSplitAbsOld + modalSplitAbs);

                            if (!tripsAllTripIntentionSeparated.containsKey(ti)) tripsAllTripIntentionSeparated.put(ti,
                                    0L);
                            for (Mode m : Mode.values()) {
                                tripsAllTripIntentionSeparated.put(ti, tripsAllTripIntentionSeparated.get(ti) +
                                        distModeTapas[m.getId()][ti.getId()][cat.getId()]);
                            }

                            element.putModalSplitTripIntentionSeparated(ti,
                                    saveDiv(modalSplitAbsOld + modalSplitAbs * 100,
                                            tripsAllTripIntentionSeparated.get(ti)));
                        } else {
                            // andernfalls Zähle die Werte für "sonstiges" hoch
                            // und berechne die relativen Anteile für "sonstiges"
                            element.modalSplitOtherAbs += modalSplitAbs;

                            for (Mode m : Mode.values()) {
                                tripsAll += distModeTapas[m.getId()][ti.getId()][cat.getId()];
                            }
                        }
                        element.modalSplitOther = saveDiv(element.modalSplitOtherAbs * 100, tripsAll);

                    }
                    // Füge das Element dem Modalsplit hinzu und setze wieder alle Zähler auf 0
                    modalSplit.getElements().add(element);
                    element = null;
                    tripsAllTripIntentionSeparated.clear();
                    tripsAll = 0;

                }
            }
            result.put(mode, modalSplit);
        }
        return result;

        // Map<Mode, ModalSplitForDistanceCategoryAndTripIntention> result = new HashMap<Mode, ModalSplitForDistanceCategoryAndTripIntention>();
        // for (Mode mode : Mode.values()) {
        // ModalSplitForDistanceCategoryAndTripIntention modalSplit = new ModalSplitForDistanceCategoryAndTripIntention();
        // for (DistanceCategory cat : DistanceCategory.values()) {
        // long otherAbs = 0;
        // double other = 0;
        // for (TripIntention ti : TripIntention.values()) {
        // ModalSplitForDistanceCategory modalSplitCat = new ModalSplitForDistanceCategory();
        // if (modalSplitForDistanceCatAndTripIntentionFilterDistCat[cat.getId()]) {
        // if (modalSplitForDistanceCatAndTripIntentionFilterTripInt[ti.getId()]) {
        // // Differenziert nach TripIntention und DistanceCategory
        // long modalSplitForDistanceCategoryTripIntentionAbs = getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti);
        // double modalSplitForDistanceCategoryAndTripIntention = getModalSplitForDistanceCategoryAndTripIntention(mode, cat, ti);
        // modalSplit.modalSplitsTripIntentionDistanceCategory.put(ti, cat, modalSplitForDistanceCategoryAndTripIntention);
        // modalSplit.modalSplitsTripIntentionDistanceCategoryAbs.put(ti, cat, modalSplitForDistanceCategoryTripIntentionAbs);
        //
        // } else {
        // // TripIntention nicht selektiert DistanceCategory selektiert: summiere other
        // otherAbs += getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti);
        // other += getModalSplitForDistanceCategoryAndTripIntention(mode, cat, ti);
        // if (!modalSplit.otherDistanceCategorySeperated.containsKey(ti)) {
        // modalSplit.otherDistanceCategorySeperatedAbs.put(cat, getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti));
        // modalSplit.otherDistanceCategorySeperated.put(cat, getModalSplitForDistanceCategoryAndTripIntention(mode, cat, ti));
        // } else {
        // modalSplit.otherDistanceCategorySeperatedAbs.put(cat,
        // modalSplit.otherTripIntentionSeperatedAbs.get(ti) + getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti));
        // modalSplit.otherDistanceCategorySeperated.put(cat, modalSplit.otherTripIntentionSeperated.get(ti) + getModalSplitForDistanceCategoryAndTripIntention(mode, cat, ti));
        // }
        // }
        // } else {
        // if (modalSplitForDistanceCatAndTripIntentionFilterTripInt[ti.getId()]) {
        // // TripIntention selektiert, DistanceCategory nicht selektiert
        // if (!modalSplit.otherTripIntentionSeperated.containsKey(ti)) {
        // modalSplit.otherTripIntentionSeperatedAbs.put(ti, getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti));
        // modalSplit.otherTripIntentionSeperated.put(ti, getModalSplitForDistanceCategoryAndTripIntention(mode, cat, ti));
        // } else {
        // modalSplit.otherTripIntentionSeperatedAbs.put(ti, modalSplit.otherTripIntentionSeperatedAbs.get(ti) + getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti));
        // modalSplit.otherTripIntentionSeperated.put(ti, modalSplit.otherTripIntentionSeperated.get(ti) + getModalSplitForDistanceCategoryAndTripIntention(mode, cat, ti));
        // }
        // } else {
        // // TripIntention nicht selektiert, DistanceCategory nicht selektiert
        // modalSplit.otherTripIntentionSeperatedAbs.put(ti, -1l);
        // modalSplit.otherTripIntentionSeperated.put(ti, -1d);
        // modalSplit.otherAllAbs += getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti);
        // modalSplit.otherAll += getModalSplitForDistanceCategoryAndTripIntention(mode, cat, ti);
        //
        // }
        //
        // }
        // }
        // }
        // result.put(mode, modalSplit);
        // }
        // return result;
    }

    private double getModalSplitForDistanceCategoryAndTripIntention(Mode mode, DistanceCategory cat, TripIntention ti) {
        long trips = getModalSplitForDistanceCategoryTripIntentionAbs(mode, cat, ti);
        long tripsAll = 0;
        for (Mode m : Mode.values()) {
            tripsAll += distModeTapas[m.getId()][ti.getId()][cat.getId()];
        }
        if (trips == 0 || tripsAll == 0) return 0;
        return saveDiv(trips, tripsAll) * 100.0;
    }

    private long getModalSplitForDistanceCategoryTripIntentionAbs(Mode mode, DistanceCategory cat, TripIntention ti) {
        return distModeTapas[mode.getId()][ti.getId()][cat.getId()];
    }

    public double getModalSplitForTripIntention(Mode m, TripIntention ti) {
        return saveDiv(getModalSplitForTripIntentionAbs(m, ti), getCountTripsByTripIntention(ti)) * 100.0;
    }

    public long getModalSplitForTripIntentionAbs(Mode m, TripIntention ti) {
        return calcArraySum(cntModeViseva[m.getId()][ti.getId()]);
    }

    public double getModalSplitForTripIntentionAndPersonGroupTapas(Mode m, TripIntention ti, Job pg) {

        return saveDiv(getModalSplitForTripIntentionAndPersonGroupTapasAbs(m, ti, pg),
                getCountOfTripsForTripIntentionAndPersonGroupTapas(ti, pg)) * 100.0;

    }

    public long getModalSplitForTripIntentionAndPersonGroupTapasAbs(Mode m, TripIntention ti, Job pg) {
        return cntModeTapas[m.getId()][ti.getId()][pg.getId() - 1];
    }

    public double getModalSplitForTripIntentionAndPersonGroupViseva(Mode m, TripIntention ti, PersonGroup pg) {
        return saveDiv(getModalSplitForTripIntentionAndPersonGroupVisevaAbs(m, ti, pg),
                getCountOfTripsForTripIntentionAndPersonGroupViseva(ti, pg)) * 100.0;

    }

    public long getModalSplitForTripIntentionAndPersonGroupVisevaAbs(Mode m, TripIntention ti, PersonGroup pg) {
        return cntModeViseva[m.getId()][ti.getId()][pg.getId()];
    }

    public long getNbPers() {
        return nbPers;
    }

    public void setNbPers(long nbPers) {
        this.nbPers = nbPers;
    }

    public long getNbTrips() {
        return nbTrips;
    }

    public void setNbTrips(long nbTrips) {
        this.nbTrips = nbTrips;
    }

    public long getNrOfPersonsForPersonGroup(PersonGroup pg) {
        long ret = 0;
        if (persBuffs.containsKey(pg)) {
            ret = persBuffs.get(pg);
        }
        return ret;
    }

    public long getNrOfTripsForPersonGroupTapas(Job pg) {
        return cntTripsPGTapas[pg.getId() - 1];
    }

    public long getNrOfTripsForPersonGroupTapasAndTripIntention(Job pg, TripIntention ti) {
        return cntTripIntentionPersonGroupTapas[ti.getId()][pg.getId() - 1];
    }

    public long getNrOfTripsForPersonGroupViseva(PersonGroup pg) {
        return cntTripsPGViseva[pg.getId()];
    }

    public long getNrOfTripsForPersonGroupVisevaAndTripIntention(PersonGroup pg, TripIntention ti) {
        return cntTripIntentionPersonGroupViseva[ti.getId()][pg.getId()];
    }

    public long getNrTripsForTripIntention(TripIntention ti) {
        return cntTrips[ti.getId()];
    }

    public double getPercentageOfTripAllocationForPersonGroupTapas(Job pg) {

        return saveDiv(cntTripsPGTapas[pg.getId() - 1], nbTrips) * 100.0;


    }

    public double getPercentageOfTripAllocationForPersonGroupViseva(PersonGroup pg) {
        return saveDiv(cntTripsPGViseva[pg.getId()], nbTrips) * 100.0;

    }

    public double getPercentageOfTripAllocationForTripIntention(TripIntention ti) {
        return saveDiv(getCountOfTripsForTripIntention(ti), nbTrips) * 100.0;

    }

    public double getPercentageOfTripLengthAllocationForTripIntentionAndPersonGroupsTapas(TripIntention ti, Job pg) {
        long cnt = cntTripIntentionPersonGroupTapas[ti.getId()][pg.getId() - 1];
        return saveDiv(cnt, nbTrips) * 100.0;
    }

    public double getPercentageOfTripLengthAllocationForTripIntentionAndPersonGroupsViseva(TripIntention ti, PersonGroup pg) {
        long cnt = cntTripIntentionPersonGroupViseva[ti.getId()][pg.getId()];
        return saveDiv(cnt, nbTrips) * 100.0;
    }

    public double getPercentageOfTripLengthAllocationForTripIntentions(TripIntention ti) {
        long[][] cntTripsPerDistCatAndPersonGroup = distCatViseva[ti.getId()];
        long cnt = 0;
        for (long[] tripsCntPerDistCat : cntTripsPerDistCatAndPersonGroup) {
            cnt += calcArraySum(tripsCntPerDistCat);
        }

        return saveDiv(cnt, nbTrips) * 100.0;

    }

    public double getPercentageOfTriplengthAllocationForDistanceCategory(DistanceCategory cat) {
        return saveDiv(aggCounter[cat.getId()], nbTrips) * 100.0;

    }

    public double getPercentageOfTriplengthAllocationForDistanceCategoryAndMode(Mode m, DistanceCategory cat) {
        long counter = 0;
        for (int i = 0; i < TripIntention.values().length; i++) {
            counter += distModeTapas[m.getId()][i][cat.getId()];
        }

        long all = 0;
        for (int i = 0; i < TripIntention.values().length; i++) {
            for (int j = 0; j < DistanceCategory.values().length; j++) {
                all += distModeTapas[m.getId()][i][j];

            }
        }

        return saveDiv(counter, all) * 100.0;

    }

    public double getPercentageOfTriplengthAllocationForDistanceCategoryAndTripIntention(TripIntention ti, DistanceCategory cat) {
        long counter = 0;
        for (long j : distCatViseva[ti.getId()][cat.getId()]) {
            counter += j;
        }

        return saveDiv(counter, cntTrips[ti.getId()]) * 100.0;


    }

    public double getPercentageOfTriplengthAllocationForDistanceCategoryAndTripIntentionAndPersonGroupTAPAS(TripIntention ti, DistanceCategory cat, Job pg) {
        return saveDiv(distCatTapas[ti.getId()][cat.getId()][pg.getId() - 1], cntTrips[ti.getId()]) * 100.0;
    }

    public double getPercentageOfTriplengthAllocationForDistanceCategoryAndTripIntentionAndPersonGroupViseva(TripIntention ti, DistanceCategory cat, PersonGroup pg) {

        return saveDiv(distCatViseva[ti.getId()][cat.getId()][pg.getId() - 1], cntTrips[ti.getId()]) * 100.0;

    }

    public RegionCode getRegionCode() {
        return regionCode;
    }

    private double saveDiv(long a, long b) {
        double d = 0.0;
        if (b != 0) d = (double) a / (double) b;
        return Double.isNaN(d) ? 0 : d;
    }

    private double saveDiv(double a, long b) {
        double d = 0.0;
        if (b != 0) d = a / (double) b;
        return Double.isNaN(d) ? 0 : d;
    }

    public void setAggCounter(long[] aggCounter) {
        this.aggCounter = aggCounter;
    }

    public void setCntDistTrips(long[] cntDistTrips) {
        this.cntDistTrips = cntDistTrips;
    }

    public void setCntDurTrips(long[] cntDurTrips) {
        this.cntDurTrips = cntDurTrips;
    }

    public void setCntDurTripsPGTapas(long[] cntDurTripsPGTapas) {
        this.cntDurTripsPGTapas = cntDurTripsPGTapas;
    }

    public void setCntDurTripsPGViseva(long[] cntDurTripsPG) {
        this.cntDurTripsPGViseva = cntDurTripsPG;
    }

    public void setCntModeTapas(long[][][] cntModeTapas) {
        this.cntModeTapas = cntModeTapas;
    }

    public void setCntModeViseva(long[][][] cntMode) {
        this.cntModeViseva = cntMode;
    }

    public void setCntTripIntentionPersonGroupTapas(long[][] cntTripIntentionPersonGroup) {
        this.cntTripIntentionPersonGroupTapas = cntTripIntentionPersonGroup;
    }

    public void setCntTripIntentionPersonGroupViseva(long[][] cntTripIntentionPersonGroup) {
        this.cntTripIntentionPersonGroupViseva = cntTripIntentionPersonGroup;
    }

    public void setCntTrips(long[] cntTrips) {
        this.cntTrips = cntTrips;
    }

    public void setCntTripsPGTapas(long[] cntTripsPGTapas) {
        this.cntTripsPGTapas = cntTripsPGTapas;
    }

    public void setCntTripsPGViseva(long[] cntTripsPG) {
        this.cntTripsPGViseva = cntTripsPG;
    }

    public void setDistCatTapas(long[][][] distCatTapas) {
        this.distCatTapas = distCatTapas;
    }

    public void setDistCatViseva(long[][][] distCat) {
        this.distCatViseva = distCat;
    }

    public void setDistModeTapas(long[][][] distModeTapas) {
        this.distModeTapas = distModeTapas;
    }

    public void setDistTripsPGTapas(long[] cntDistTripsPGTapas) {
        this.cntDistTripsPGTapas = cntDistTripsPGTapas;
    }

    public void setDistTripsPGViseva(long[] cntDistTripsPG) {
        this.cntDistTripsPGViseva = cntDistTripsPG;
    }

    public void setPersBuffs(Map<PersonGroup, Long> persBuffs) {
        this.persBuffs = persBuffs;
    }

    public enum RegionCode {
        REGION_0(0, "Gesamt"), REGION_1(1, "Agglomeration"), REGION_2(2, "städtisch"), REGION_3(3,
                "wenig verstädtert"), REGION_4(4, "ländlich"), REGION_5(5, "sehr ländlich");
        private final int id;
        private final String desc;

        RegionCode(int id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        public String getDescription() {
            return desc;
        }

        public int getId() {
            return id;
        }
    }

    /**
     * Der Modalsplit für einen bestimmten Mode.
     *
     * @author Marco
     */
    public static class ModalSplitForDistanceCategory {
        private Mode mode;
        private List<ModalSplitForDistanceCategoryElement> elements;

        public List<ModalSplitForDistanceCategoryElement> getElements() {
            if (elements == null) elements = new ArrayList<>();
            return elements;
        }

        public Mode getMode() {
            return mode;
        }

        /**
         * Ein Element des Modalsplits. Abhängig davon welche Distanzkategorien selektiert wurden, enthält dieses Element die Distanzkategorien die zusammengefasst wurden.
         *
         * @author Marco
         */
        public static class ModalSplitForDistanceCategoryElement {
            private double modalSplit;
            private long modalSplitAbs;
            private List<DistanceCategory> distanceCategories;

            public List<DistanceCategory> getDistanceCategories() {
                if (distanceCategories == null) distanceCategories = new ArrayList<>();
                return distanceCategories;
            }

            public double getModalSplit() {
                return modalSplit;
            }

            public long getModalSplitAbs() {
                return modalSplitAbs;
            }

        }

    }

    /**
     * Der Modalsplit für einen bestimmten Mode getrennt nach {@link DistanceCategory} und {@link TripIntention}.
     *
     * @author Marco
     */
    public static class ModalSplitForDistanceCategoryAndTripIntention {
        private Mode mode;
        private List<ModalSplitForDistanceCategoryAndTripIntentionElement> elements;

        public List<ModalSplitForDistanceCategoryAndTripIntentionElement> getElements() {
            if (elements == null) elements = new ArrayList<>();
            return elements;
        }

        public Mode getMode() {
            return mode;
        }

        /**
         * Ein Element des Modalsplits. Abhängig davon welche Distanzkategorien selektiert wurden, enthält dieses Element die Distanzkategorien die zusammengefasst wurden. Jedes Element wird des
         * weiteren nach {@link TripIntention} getrennt
         *
         * @author Marco
         */
        public static class ModalSplitForDistanceCategoryAndTripIntentionElement {
            private List<DistanceCategory> distanceCategories;
            private final Map<TripIntention, Double> modalSplitTripIntentionSeparated = new HashMap<>();
            private final Map<TripIntention, Long> modalSplitTripIntentionSeparatedAbs = new HashMap<>();
            private double modalSplitOther;
            private long modalSplitOtherAbs;

            public boolean containsModalSplitTripIntentionSeparated(TripIntention tripIntention) {
                return modalSplitTripIntentionSeparated.containsKey(tripIntention);
            }

            public boolean containsModalSplitTripIntentionSeparatedAbs(TripIntention tripIntention) {
                return modalSplitTripIntentionSeparatedAbs.containsKey(tripIntention);
            }

            public List<DistanceCategory> getDistanceCategories() {
                if (distanceCategories == null) distanceCategories = new ArrayList<>();
                return distanceCategories;
            }

            public double getModalSplitOther() {
                return modalSplitOther;
            }

            public long getModalSplitOtherAbs() {
                return modalSplitOtherAbs;
            }

            public double getModalSplitTripIntentionSeparated(TripIntention tripIntention) {
                return modalSplitTripIntentionSeparated.containsKey(tripIntention) ? modalSplitTripIntentionSeparated
                        .get(tripIntention) : -1;
            }

            public long getModalSplitTripIntentionSeparatedAbs(TripIntention tripIntention) {
                return modalSplitTripIntentionSeparatedAbs.containsKey(
                        tripIntention) ? modalSplitTripIntentionSeparatedAbs.get(tripIntention) : -1;
            }

            public void putModalSplitTripIntentionSeparated(TripIntention tripIntention, double modalSplit) {
                modalSplitTripIntentionSeparated.put(tripIntention, modalSplit);
            }

            public void putModalSplitTripIntentionSeparatedAbs(TripIntention tripIntention, long modalSplitAbs) {
                modalSplitTripIntentionSeparatedAbs.put(tripIntention, modalSplitAbs);
            }

        }

    }

}
