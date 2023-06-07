package de.dlr.ivf.api.io.updater;

public interface DataUpdater<T> {

    void update(int id, T objectToUpdate);
}
