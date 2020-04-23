package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper to make {@link TapasTripConverter} {@link Iterable} without changing
 * it.
 *
 * @author boec_pa
 */
public class FileTripReader implements TapasTripReader {

    private final TripFileIterator tripFileIterator;

    public FileTripReader(List<File> tripFiles, StyledDocument console) {
        tripFileIterator = new TripFileIterator(tripFiles, console);
    }

    @Override
    public void close() {
        // TODO clean up
    }

    public Iterator<TapasTrip> getIterator() {
        return tripFileIterator;
    }

    @Override
    public int getProgress() {
        return 0; // TODO implement getProcess();
    }

    public String getSource() {
        return tripFileIterator.tapasTripConverter.getTripFile();
    }

    @Override
    public long getTotal() {
        return -1; // TODO implement getTotal();
    }

    private class TripFileIterator implements Iterator<TapasTrip> {

        private final TapasTripConverter tapasTripConverter;
        private final StyledDocument console;
        private boolean tripCollected = true;

        public TripFileIterator(List<File> tripFiles, StyledDocument console) {
            tapasTripConverter = new TapasTripConverter(tripFiles);
            this.console = console;
        }

        public boolean hasNext() {
            if (tripCollected) {
                try {
                    if (tapasTripConverter.convertNextTrip(console)) {
                        tripCollected = false;
                        return true;
                    } else {
                        return false;
                    }
                } catch (BadLocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return false;
                }
            } else {
                return true;
            }
        }

        public TapasTrip next() {
            tripCollected = true;
            return tapasTripConverter.getTrip();
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

}
