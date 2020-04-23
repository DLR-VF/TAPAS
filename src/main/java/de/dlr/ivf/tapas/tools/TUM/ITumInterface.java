package de.dlr.ivf.tapas.tools.TUM;

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
