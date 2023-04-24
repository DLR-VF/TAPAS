package de.dlr.ivf.tapas.matrixtool.common.fileFormat;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public abstract class AbstractFileFilter extends FileFilter {

	public abstract boolean accept(File pathname);

	public abstract String getDescription();

	public abstract Format getFormat(); 
}
