package de.dlr.ivf.tapas.tools.fileModifier;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 
 * Helper routine to convert a location file from the old Text-Tapas to a versio for the new Tapas. 
 * 
 * Seems to be obsolete!
 * 
 */
public class TPS_LocationFileUpdater {

	/**
	 * Main method
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		JFileChooser fd = new JFileChooser();
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd.setVisible(true);

		Frame f = new Frame();

		File selFile = new File("G:/VF_Server_neu/Projekte/PJ_laufend/TAPAS/Input");
		if (selFile.exists())
			fd.setSelectedFile(selFile);

		fd.setDialogTitle("Choose location file for load");
		fd.showOpenDialog(f);
		File oPath = fd.getSelectedFile();

		fd.setDialogTitle("Choose location file for save");
		fd.showOpenDialog(f);
		File nPath = fd.getSelectedFile();

		f.dispose();

		CsvReader reader = new CsvReader(new FileReader(oPath));
		CsvWriter writer = new CsvWriter(new FileWriter(nPath), ',');

		reader.readHeaders();
		writer.writeRecord(reader.getHeaders());

		String[] record;
		int block;

		while (reader.readRecord()) {
			record = reader.getValues();
			block = -1;
			if (!record[2].startsWith("misc")) {
				for (int i = 0; i < record[2].length(); i++) {
					if (Character.isDigit(record[2].charAt(i))) {
						block = Integer.parseInt(record[2].substring(i));
						break;
					}
				}
			}
			record[7] = Integer.toString(block);

			writer.writeRecord(record);
		}

		writer.flush();
		writer.close();
		reader.close();
	}
}
