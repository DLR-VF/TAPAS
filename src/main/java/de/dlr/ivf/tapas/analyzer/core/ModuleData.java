/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.dlr.ivf.tapas.analyzer.core;

/**
 * Container für die Daten, die zwischen den Modulen hin und her geschickt werden.
 *
 * @author Martin
 * @version $Revision$
 */
public abstract class ModuleData {

    public static final ModuleData NO_RESULT = new ModuleData("no.result", "0.0.0") {
    };

    private final String id;
    private final String version;
    private boolean last = true;

    /**
     * Erstellt eine Instanz von {@link ModuleData} mit einer id und einer version um die Kompatibilität der Daten zu
     * garantieren.
     *
     * @param id      - der Moduldaten
     * @param version - der Moduldaten
     */
    public ModuleData(String id, String version) {
        this.id = id;
        this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ModuleData) {
            ModuleData compare = (ModuleData) obj;
            return compare.getId().equals(getId()) && compare.getVersion().equals(getVersion());
        }
        return false;
    }

    /**
     * @return ID der Moduldaten
     */
    public String getId() {
        return id;
    }

    /**
     * @return Version der Moduldaten
     */
    public String getVersion() {
        return version;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
