package de.dlr.ivf.tapas.tools.TUM;

import java.io.File;

import javax.swing.text.StyledDocument;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;

/**
 * @author sche_ai
 *
 */
public interface ITumInterface {
	
	File[] getExportFiles();
	
	StyledDocument getConsole();
	
	TPS_DB_Connector getConnection();
	
	String[] getSimKeys();
	
	
	
	

}
