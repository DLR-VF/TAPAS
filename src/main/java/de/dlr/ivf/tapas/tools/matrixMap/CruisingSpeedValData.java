/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.matrixMap;

public class CruisingSpeedValData {
    private String recordName = null;
    private String matricesTable = null;
    private String terrain = null;
    private String tazTable = null;
    private String path = null;
    private boolean indirectWayFactor;
    private String[] referenceDist = null;
    private String[] referenceTT = null;

    public CruisingSpeedValData() {
        indirectWayFactor = true;
        setReferenceDist(new String[4]);
        setReferenceTT(new String[4]);
    }

    public String getMatricesTable() {
        return matricesTable;
    }

    public void setMatricesTable(String matricesTable) {
        this.matricesTable = matricesTable;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRecordName() {
        return recordName;
    }

    public void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    public String[] getReferenceDist() {
        return referenceDist;
    }

    public void setReferenceDist(String[] referenceDist) {
        this.referenceDist = referenceDist;
    }

    public String[] getReferenceTT() {
        return referenceTT;
    }

    public void setReferenceTT(String[] referenceTT) {
        this.referenceTT = referenceTT;
    }

    public String getTazTable() {
        return tazTable;
    }

    public void setTazTable(String tazTable) {
        this.tazTable = tazTable;
    }

    public String getTerrain() {
        return terrain;
    }

    public void setTerrain(String terrain) {
        this.terrain = terrain;
    }

    public boolean isIndirectWayFactor() {
        return indirectWayFactor;
    }

    public void setIndirectWayFactor(boolean indirectWayFactor) {
        this.indirectWayFactor = indirectWayFactor;
    }

}
