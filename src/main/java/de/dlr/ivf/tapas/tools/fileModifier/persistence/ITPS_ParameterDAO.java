package de.dlr.ivf.tapas.tools.fileModifier.persistence;


public interface ITPS_ParameterDAO {
	String[]	header	= new String[] { "name", "value", "comment" };
	
	void addAdditionalParameter(String name, String value, String comment);
	
	void readParameter();
	
	void writeParameter();
}
