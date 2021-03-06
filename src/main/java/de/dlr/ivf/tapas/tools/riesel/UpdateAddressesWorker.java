/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.riesel;

import de.dlr.ivf.tapas.TPS_Main;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

/**
 * <p>
 * This class updates the number of inhabitants of the given list of addresses.
 * The input of the data is handled by the {@link BlockingQueue} given and
 * construction time.
 * </p>
 * <p>
 * The update is done in batches of size
 * {@link UpdateAddressesWorker#BATCH_MAX_SIZE}. The last batch can be emptied
 * by notifying this object via {@link TPS_Main.TPS_State#setFinished()}.
 * </p>
 */
public class UpdateAddressesWorker implements Runnable {

    private final BlockingQueue<AddressPojo> queue;
    private final int BATCH_MAX_SIZE = 10000;
    private final PreparedStatement updateBatch;
    private final Connection connection;
    private int BATCH_SIZE = 0;

    public UpdateAddressesWorker(BlockingQueue<AddressPojo> queue, TPS_DB_Connector dbCon) throws SQLException {
        super();
        this.queue = queue;

        connection = dbCon.getConnection(this);
        connection.setAutoCommit(false);
        updateBatch = connection.prepareStatement("UPDATE address_bkg SET inhabitants = ? WHERE id = ?");
    }

    private void addToBatch(AddressPojo address) {

        try {
            updateBatch.setInt(1, address.getInhabitants());
            updateBatch.setLong(2, address.getKey());
            updateBatch.addBatch();
            BATCH_SIZE++;
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("An error occured while trying to add address " + address.getKey() + ".");
        }

        if (BATCH_SIZE >= BATCH_MAX_SIZE) {
            commitBatch();
            BATCH_SIZE = 0;
        }
    }

    public void commitBatch() {

        try {
            System.out.println("commiting batch\t queue size=" + queue.size());
            updateBatch.executeBatch();
            connection.commit();
            updateBatch.clearBatch();

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println(
                    "An error occured while trying to execute a batch update. This thread will be terminated.");
            Thread.currentThread().interrupt();
        }

    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AddressPojo address = queue.take();
                if (address != AddressPojo.POISON_ELEMENT) addToBatch(address);
                else {
                    commitBatch();
                    System.out.println("All updates have been performed.");
                    Thread.currentThread().interrupt();
                }

            } catch (InterruptedException ex) {
                ex.printStackTrace();
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
