package de.dlr.ivf.tapas.tools.riesel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * This class handles the output of {@link SimpleCollector} into a file. <br>
 * It gives a progress report every {@link SimpleCollectorWriter#REPORT_SIZE} entries and reports the final number.
 * 
 */
public class SimpleCollectorWriter implements Runnable {

	private static final String DELIMITER = ";";

	private BlockingQueue<AddressPojo> queue;
	private BufferedWriter writer;
	private long counter = 0;
	private long inhabitants = 0;
	private static final long REPORT_SIZE = 1000;

	/**
	 * 
	 * @param outputFilePath
	 *            Path to the file to written. This file will be overwritten.
	 * @param queue
	 *            A queue of all entries to be written. This queue is supposed to be filled by another thread and is
	 *            expected to end with {@link AddressPojo#POISON_ELEMENT}.
	 * @throws IOException
	 */
	public SimpleCollectorWriter(String outputFilePath,
			BlockingQueue<AddressPojo> queue) throws IOException {
		super();
		this.queue = queue;
		writer = new BufferedWriter(new FileWriter(outputFilePath));
		System.out.println("Writer ready to take elements");
	}

	@Override
	public void run() {

		AddressPojo ap;
		try {
			while ((ap = queue.take()) != AddressPojo.POISON_ELEMENT) {
				try {
					writer.write(ap.getKey() + DELIMITER
							+ ap.getInhabitants()
							);
					writer.newLine();
					inhabitants += ap.getInhabitants();
					if (++counter % REPORT_SIZE == 0) {
						System.out.println("Written " + counter + " lines.");
						writer.flush();
					}
				} catch (IOException e) {
					System.err.println("Couldn't write " + ap);
					e.printStackTrace();
				}

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("I distributed " + inhabitants + " inhabitants to "
				+ counter + " addresses.");

	}
}
