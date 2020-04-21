package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

public class TapasTripConverter {

	private File currentTripFile = null;
	private TPS_FileReader reader = null;
	private TapasTrip trip = null;
	private int tripFileCounter;
	private List<File> tripFiles;

	public TapasTripConverter(List<File> tripFiles) {
		this.tripFiles = tripFiles;
		if (tripFiles.size() > 0) {
			currentTripFile = tripFiles.get(0);
			tripFileCounter = 0;
			try {
				reader = new TPS_FileReader(currentTripFile.getAbsolutePath(),true);
				trip = new TapasTrip();
				TapasTrip.generateKeyMap(reader.getHeaders());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out.println("File not found constructor");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("IOException constructor");
			}
		} else {
			System.out.println("Kein Tripfile gefunden");
		}
	}

	/**
	 * Wird vom core aufgerufen. bekommt die tripFiles Liste übergeben und holt
	 * sich daraus die Zeilen der Tripfiles gibt diese dann an TapasTrip weiter
	 * 
	 * @param console
	 * @throws BadLocationException 
	 */
	public boolean convertNextTrip(StyledDocument console) throws BadLocationException {
		if (reader != null) {
			String[] record;
			try {
				record = reader.getRecord();
				// System.out.println("A..."+record[0].toString());
			} catch (IOException e1) {
				System.out.println("IOException convertNextTrip (IOException)");
				return false;
			}
			if (record != null) {
				trip.setValues(record);
				return true;
			} else {
				// if end of file reached...
				// e.printStackTrace();
				// System.out.println("End of File reached (NullPointer) Tripfile parsed");
				tripFileCounter++;
				console.insertString(console.getLength(),"TripConv gelesen "
						+ (int) (100.0 * tripFileCounter / tripFiles.size())
						+ "%: " + reader.getFileName() + "\n",null);
				if (tripFileCounter >= tripFiles.size()) {
					// System.out.println("Index out of bounds, All tripfiles parsed");
					console.insertString(console.getLength(),"TripConv.: Alle Tripdateien eingelesen\n",null);
					return false;
				}

				try {// set new trip file
					currentTripFile = tripFiles.get(tripFileCounter);
					reader = new TPS_FileReader(
							currentTripFile.getAbsolutePath(), true);
					return this.convertNextTrip(console);
					// System.out.println("B..."+record[0].toString());
				} catch (FileNotFoundException e1) {
					// If no further file available
					// e1.printStackTrace();
					System.out
							.println("File not found convertNextTrip (FileIteration)");
					return false;
				} catch (IOException e1) {
					// e1.printStackTrace();
					System.out
							.println("IOException convertNextTrip (FileIteration)");
					return false;
				}
			}
		} else {
			System.err.println("FileReader not set.");
			return false;
		}
	}

	public TapasTrip getTrip() {
		return trip;
	}

	public void setTrip(TapasTrip trip) {
		this.trip = trip;
	}

	public String getTripFile() {
		if (currentTripFile != null)
			return currentTripFile.getAbsolutePath();
		else
			return null;
	}

}