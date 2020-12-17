/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.riesel;

public class AddressPojo {

    public static AddressPojo POISON_ELEMENT = new AddressPojo(-1, -1);

    private long key;
    private int inhabitants;

    public AddressPojo(long key, int inhabitants) {
        super();
        this.key = key;
        this.inhabitants = inhabitants;
    }

    public int getInhabitants() {
        return inhabitants;
    }

    public void setInhabitants(int inhabitants) {
        this.inhabitants = inhabitants;
    }

    public long getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void incInhabitants() {
        this.inhabitants++;
    }

}
