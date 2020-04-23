package de.dlr.ivf.tapas.analyzer.core;

import de.dlr.ivf.tapas.analyzer.gui.ControlInputInterface;
import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTripReader;

import javax.swing.text.StyledDocument;
import java.io.File;
import java.util.List;

/**
 * Interface for access of selected modules and other configuration parameters
 *
 * @author Marco
 */
public interface CoreInputInterface {
	
	/**
	 * all implemented modules
	 */
	enum Module {
		TUM
	}
	
	
	/**
	 * @param module
	 * @return corresponding {@link ControlInputInterface} or null if not existent
	 */
    ControlInputInterface getInterface(Module module);
	
	/**
	 * @return path of output files
	 */
    String getOutputPath();
	
	/**
	 * @return paths of selected trip files
	 */
    List<File> getTripFiles();
	
	
	TapasTripReader getTripReader();
	
	/**
	 * @param module
	 * @return returns true if the corresponding {@link ControlInputInterface} is currently in process
	 */
    boolean isActive(Module module);
	
	/**
	 * @return UI component of status bar
	 */
    StyledDocument getConsole();
	
}
