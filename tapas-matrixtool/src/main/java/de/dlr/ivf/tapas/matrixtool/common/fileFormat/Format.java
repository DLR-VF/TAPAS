package de.dlr.ivf.tapas.matrixtool.common.fileFormat;

public enum Format {

	CSV_SEMC ("CSV-Dateien (';') (*.*)", ";"),
	CSV_CMA ("CSV-Dateien (',') (*.*)", ","),
	CSV_BLNK ("CSV-Dateien (' ') (*.*)", " "),
	VISUM ("Visum-Dateien ($V) (*.*)", "\\s+");
	
	private String desc;
	private String delim;	
	//this is the delimiter in any case for parsing
	//maybe the delimiter for writing is another one, but that is only in the 
	//Visum-format the case
	
	Format(String desc, String delim){
		this.desc = desc;
		this.delim = delim;
	}
	
	public String getDesc(){
		return desc;
	}
	
	public String getDelim(){
		return delim;
	}
}
