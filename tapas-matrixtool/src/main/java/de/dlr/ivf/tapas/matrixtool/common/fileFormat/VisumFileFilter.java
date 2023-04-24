package de.dlr.ivf.tapas.matrixtool.common.fileFormat;

import java.io.File;

public class VisumFileFilter extends AbstractFileFilter {

	public boolean accept(File pathname) {
		return true;
	}

	public String getDescription() {
        return Format.VISUM.getDesc();
    }
	
	public Format getFormat(){
		return Format.VISUM;
	}
}
