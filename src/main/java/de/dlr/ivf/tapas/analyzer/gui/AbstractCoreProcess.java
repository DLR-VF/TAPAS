package de.dlr.ivf.tapas.analyzer.gui;

import java.io.File;

import javax.swing.text.BadLocationException;

import de.dlr.ivf.tapas.analyzer.core.CoreProcessInterface;


public abstract class AbstractCoreProcess implements CoreProcessInterface {

	protected boolean isProcessing;
	private boolean isCancelled;

	public AbstractCoreProcess() {
		this.isCancelled = false;
		this.isProcessing = false;
	}

	public final boolean cancelFinish() {
		this.isCancelled = true;
		this.isProcessing = false;
		return true;
	}

	public boolean isCancelled() {
		return isCancelled;
	}

	public void doCancel() {
		isCancelled = true;
		this.isProcessing = false;
	}

	public final void run() {
		try {
			this.finish();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Must be called once before executing the {@link de.dlr.ivf.tapas.tools.TUM.IntegratedTUM}
	 * 
	 * @param clearSources
	 * 				Flag to omit previous results
	 * @return
	 */
	public abstract boolean init(boolean clearSources);

	public abstract boolean finish(File exportfile) throws BadLocationException;
}
