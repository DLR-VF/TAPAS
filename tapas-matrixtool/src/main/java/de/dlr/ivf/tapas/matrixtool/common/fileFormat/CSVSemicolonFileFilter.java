package de.dlr.ivf.tapas.matrixtool.common.fileFormat;

public class CSVSemicolonFileFilter extends AbstractCSVFileFilter {

	@Override
	public String getDescription() {
        return Format.CSV_SEMC.getDesc();
    }

	public Format getFormat(){
		return Format.CSV_SEMC;
	}
}
