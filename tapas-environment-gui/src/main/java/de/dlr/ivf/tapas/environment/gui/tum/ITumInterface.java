/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.environment.gui.tum;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;

import javax.swing.text.StyledDocument;
import java.io.File;

/**
 * @author sche_ai
 */
public interface ITumInterface {

    TPS_DB_Connector getConnection();

    StyledDocument getConsole();

    File[] getExportFiles();

    String[] getSimKeys();


}
