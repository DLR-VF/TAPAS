package de.dlr.ivf.tapas.runtime.util;

import de.dlr.ivf.tapas.log.TPS_Logger;
import de.dlr.ivf.tapas.log.TPS_LoggingInterface.SeverenceLogLevel;

import java.io.*;

/**
 * This class reads all output from a foreign process and prints it to the System default output and error stream.
 * 
 * @author mark_ma
 * 
 */
public class ProcessHandler extends Thread {

	/**
	 * Thread to read from a specific input stream and write into a specific in a specific output stream
	 * 
	 * @author mark_ma
	 * 
	 */
	private class ProcesshandlerThread extends Thread {

		/**
		 * Reader
		 */
		private BufferedReader reader;

		/**
		 * Writer
		 */
		private BufferedWriter writer;

		/**
		 * This constructor builds a newThread which reads from the input stream and writes everything in the output stream.
		 * To identify the thread in the debug mode the first two parameters are used to generate a name.
		 * 
		 * @param ph
		 *          current process handler for identifying the thread
		 * @param type
		 *          type of the thread for identifying
		 * @param is
		 *          input stream to read from
		 * @param os
		 *          output stream to write in
		 */
		public ProcesshandlerThread(ProcessHandler ph, String type, InputStream is, OutputStream os) {
			super("ProcessHandlerThread [" + type + "] of " + ph.getName());
			this.reader = new BufferedReader(new InputStreamReader(is));
			this.writer = new BufferedWriter(new OutputStreamWriter(os));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try {
				String line;
				while ((line = this.reader.readLine()) != null) {
					this.writer.write("pht: " + line + "\n");
					this.writer.flush();
				}
			} catch (IOException e) {
				TPS_Logger.log(SeverenceLogLevel.ERROR, e);
			}
		}
	}

	/**
	 * Reference to the linked process
	 */
	private Process process;

	/**
	 * Builds a new Thread linked to the given process' output streams.
	 * 
	 * @param identifier
	 *          identifier for this thread
	 * @param process
	 *          corresponding process of this handler
	 */
	public ProcessHandler(String identifier, Process process) {
		super("ProcessHandler of " + identifier);
		this.process = process;
	}

	public void kill() {
		this.process.destroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		new ProcesshandlerThread(this, "err", this.process.getErrorStream(), System.err).start();
		new ProcesshandlerThread(this, "out", this.process.getInputStream(), System.out).start();

		try {
			this.process.waitFor();
		} catch (InterruptedException e) {
			TPS_Logger.log(SeverenceLogLevel.ERROR, e);
		}

	}
}
