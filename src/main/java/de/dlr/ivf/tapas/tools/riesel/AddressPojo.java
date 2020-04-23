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
