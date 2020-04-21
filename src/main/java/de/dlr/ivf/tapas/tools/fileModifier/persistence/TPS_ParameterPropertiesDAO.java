package de.dlr.ivf.tapas.tools.fileModifier.persistence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

public class TPS_ParameterPropertiesDAO extends AbstractTPS_ParameterDAO {
	
	public void readParameter(TPS_ParameterClass parameterClass) {
		try {
			BufferedReader breader = new BufferedReader(new FileReader(mFile));
			String value, key;
			Enum<?> e;
			
			for (String line = breader.readLine(); line != null; line = breader.readLine()) {
				if (!line.startsWith("#") && line.length() > 2) {
					StringTokenizer st = new StringTokenizer(line, " =,");
					if (st.countTokens() < 2) {
						System.err.println("Skipped [no value] in " + mFile.getName() + ": " + line);
						continue;
					}
					key = st.nextToken();
					value = st.nextToken();
					e = this.mMap.get(key);
					
					if (e == null) {
						System.err.println("Skipped [deprecated] in " + mFile.getName() + ": " + line);
						continue;
					}
					
					if (e instanceof ParamFlag) {
						ParamFlag pf = (ParamFlag) e;
						parameterClass.setFlag(pf, Boolean.parseBoolean(value));
					} else if (e instanceof ParamString) {
						ParamString ps = (ParamString) e;
						parameterClass.setString(ps, value);
					} else if (e instanceof ParamValue) {
						ParamValue pv = (ParamValue) e;
						parameterClass.setValue(pv, Double.parseDouble(value));
					} else {
						System.err.println("Skipped [???] in " + mFile.getName() + ": " + line);
					}
				}
			}
			
			breader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
	}
	
}
