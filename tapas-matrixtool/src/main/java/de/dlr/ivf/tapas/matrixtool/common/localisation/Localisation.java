package de.dlr.ivf.tapas.matrixtool.common.localisation;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;


public class Localisation {
	
	private static Locale[] supportedLocales = {
		    Locale.GERMANY  // "..._de.properties"
//		    ,
//		    Locale.US,		// "..._en.properties"
//		    Locale.UK,		// "..._en.properties"
//		    Locale.FRANCE	// "..._fr.properties"
		};
	private static Locale currLocale = 
		(!Arrays.asList(supportedLocales).contains(Locale.getDefault())) ? 
				Locale.GERMANY : Locale.getDefault();	
	private static ResourceBundle guiTerms = 
		ResourceBundle.getBundle("gui",currLocale);
	private static ResourceBundle messageTerms = 
		ResourceBundle.getBundle("messages",currLocale);
	private static ResourceBundle skimTerms = 
		ResourceBundle.getBundle("skim",currLocale);
	
	private static DecimalFormat decFormat = (DecimalFormat)DecimalFormat.getInstance();
	
    public static String getLocaleGuiTerm(String s){
    	return guiTerms.getString(s);
    }
    
    public static String getLocaleMessageTerm(String s){
    	return messageTerms.getString(s);
    }
    
    public static String getLocaleSkimTerm(String s){
    	return skimTerms.getString(s);
    }

	public static String doubleToString(Double d, int decs){
		
		if (decs >= 0){
			decFormat.setMaximumFractionDigits(decs);
		} else {
			decFormat.setMaximumFractionDigits(100);
		}
		if (d == null)
			return "";
		
		return decFormat.format(d);
	}
	
	public static Double checkForDouble(String s) throws ParseException, NumberFormatException{

		return DecimalFormat.getInstance().parse(s).doubleValue();
	}

	public static Double stringToDouble(String s) throws ParseException, NumberFormatException{
//		Double d = null;
//		try {
//			d = checkForDouble(s);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		} catch (NumberFormatException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}

		Double d = checkForDouble(s);		
		return d;
	}
	
	public static String integerToString(Integer d){
				
		decFormat.setMaximumFractionDigits(0);
		return decFormat.format(d);
	}
	
	public static Integer checkForInteger(String s) 
		throws ParseException, NumberFormatException{

		return DecimalFormat.getInstance().parse(s).intValue();
	}

	public static Integer stringToInteger(String s)  throws ParseException, NumberFormatException{
		Integer d = null;
//		try {
//			d = checkForInteger(s);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		} catch (NumberFormatException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
		d = checkForInteger(s);
		return d;
	}
}
