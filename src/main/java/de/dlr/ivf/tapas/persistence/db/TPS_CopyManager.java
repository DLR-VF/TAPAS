package de.dlr.ivf.tapas.persistence.db;

import com.lmax.disruptor.*;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TPS_CopyManager extends CopyManager {
    public TPS_CopyManager(BaseConnection connection) throws SQLException {
        super(connection);
    }

    /**
     * Use COPY FROM STDIN for very fast copying from an InputStream into a database table.
     *
     * @param sql COPY FROM STDIN statement

     * @return number of rows updated for server 8.2 or newer; -1 for older
     * @throws SQLException on database usage issues
     */
    public long copyIn(final String sql, RingBuffer<TPS_WritableTripEvent> ring_buffer, AtomicInteger counter)
            throws SQLException {

        AtomicBoolean keep_running = new AtomicBoolean(true);
        CopyIn cp = copyIn(sql);

        final EventPoller<TPS_WritableTripEvent> poller = ring_buffer.newPoller();
        ring_buffer.addGatingSequences(poller.getSequence());

        final EventPoller.Handler<TPS_WritableTripEvent> handler = (event, sequence, endOfBatch) -> {

            byte[] writable_trip = event.getTripAsByteArray();

            if (writable_trip.length == 0){
                keep_running.set(false);
            }else{
                counter.getAndIncrement();
            }
            cp.writeToCopy(writable_trip, 0, writable_trip.length);
            event.clear();
            return true;
        };

        try {
            while (keep_running.get()) {
                poller.poll(handler);
            }
            return cp.endCopy();
        } catch (Exception e) {
            e.printStackTrace();
        } finally { // see to it that we do not leave the connection locked
            if (cp.isActive()) {
                cp.cancelCopy();
            }

        }
        return cp.endCopy();
    }
}
