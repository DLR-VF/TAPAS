package de.dlr.ivf.tapas.matrixtool.common.fileFormat;

import java.io.File;

public abstract class AbstractCSVFileFilter extends AbstractFileFilter{

	public boolean accept(File pathname) {
		return true;
	}
}
