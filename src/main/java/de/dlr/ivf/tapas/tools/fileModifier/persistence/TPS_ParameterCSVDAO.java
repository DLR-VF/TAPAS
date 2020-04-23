package de.dlr.ivf.tapas.tools.fileModifier.persistence;

import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.io.PrintWriter;

public class TPS_ParameterCSVDAO extends AbstractTPS_ParameterDAO {

    public void readParameter() {
        throw new RuntimeException("not yet implemented");
    }

    public void writeParameter(TPS_ParameterClass parameterClass) {
        PrintWriter pw = null;
        try {
            if (!mFile.exists()) mFile.createNewFile();

            pw = new PrintWriter(mFile);
            // Print header
            for (String headerElement : header) {
                pw.print(headerElement + ",");
            }
            pw.println();

            String name = mFile.getName();
            int start = name.startsWith("run_") ? 4 : 0;
            name = name.substring(start, name.lastIndexOf('.'));

            // Print additional params
            for (int i = 0; i < mParameters.size() / 3; i++) {
                for (int j = 0; j < 3; j++) {
                    String paramElement = mParameters.get(i);
                    pw.print(paramElement + ",");
                }
            }
            pw.println();

            for (ParamFlag pf : ParamFlag.values()) {
                if (parameterClass.isDefined(pf)) pw.println(pf.name() + "," + parameterClass.isTrue(pf) + ",");
            }
            for (ParamString ps : ParamString.values()) {
                if (parameterClass.isDefined(ps)) pw.println(ps.name() + "," + parameterClass.getString(ps) + ",");
            }
            for (ParamValue pv : ParamValue.values()) {
                if (parameterClass.isDefined(pv)) pw.println(pv.name() + "," + parameterClass.getDoubleValue(pv) + ",");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pw != null) pw.close();
        }

    }
}
