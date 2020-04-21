package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

/**
 * Wrapper to make {@link TapasTripConverter} {@link Iterable} without changing
 * it.
 * 
 * @author boec_pa
 * 
 */
public class FileTripReader implements TapasTripReader {

	private class TripFileIterator implements Iterator<TapasTrip>{

		private TapasTripConverter tapasTripConverter;
		private StyledDocument console;
		private boolean tripCollected = true;

		public TripFileIterator(List<File> tripFiles, StyledDocument console) {
			tapasTripConverter = new TapasTripConverter(tripFiles);
			this.console = console;
		}

		public boolean hasNext(){
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

	public Iterator<TapasTrip> getIterator() {
		return tripFileIterator;
	}

	private TripFileIterator tripFileIterator;

	public FileTripReader(List<File> tripFiles, StyledDocument console) {
		tripFileIterator = new TripFileIterator(tripFiles, console);
	}

	public String getSource() {
		return tripFileIterator.tapasTripConverter.getTripFile();
	}

	@Override
	public void close() {
		// TODO clean up
	}

	@Override
	public long getTotal() {
		return -1; // TODO implement getTotal();
	}

	@Override
	public int getProgress() {
		return 0; // TODO implement getProcess();
	}

}
