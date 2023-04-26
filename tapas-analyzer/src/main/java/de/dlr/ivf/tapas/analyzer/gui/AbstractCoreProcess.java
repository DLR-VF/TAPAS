/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.gui;

import de.dlr.ivf.tapas.analyzer.core.CoreProcessInterface;
import de.dlr.ivf.tapas.tools.TUM.IntegratedTUM;

import javax.swing.text.BadLocationException;
import java.io.File;


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

    public void doCancel() {
        isCancelled = true;
        this.isProcessing = false;
    }

    public abstract boolean finish(File exportfile) throws BadLocationException;

    /**
     * Must be called once before executing the {@link IntegratedTUM}
     *
     * @param clearSources Flag to omit previous results
     * @return
     */
    public abstract boolean init(boolean clearSources);

    public boolean isCancelled() {
        return isCancelled;
    }

    public final void run() {
        try {
            this.finish();
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
