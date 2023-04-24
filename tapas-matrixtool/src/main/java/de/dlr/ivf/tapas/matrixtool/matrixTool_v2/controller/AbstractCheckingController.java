package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller;

import java.text.ParseException;
import java.util.Observable;

import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.events.UserInputEvent;
import de.dlr.ivf.tapas.matrixtool.common.compatibility.Compatibility;
import de.dlr.ivf.tapas.matrixtool.common.compatibility.CompatibilityException;
import de.dlr.ivf.tapas.matrixtool.common.compatibility.CompatibilityFactory;
import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;

public abstract class AbstractCheckingController extends Observable {
	
	public int getDisplayDecimalPlaces(){
		return 10;
	}

	public void checkDoubleValue(String value) throws ParseException,NumberFormatException {
		
		try {
			
			Localisation.checkForDouble(value);
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.OK, 
					Localisation.doubleToString(Localisation.stringToDouble(value), 
							getDisplayDecimalPlaces()),null));
			
		} catch (ParseException e) {
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.WRONG, value, 
					Localisation.getLocaleMessageTerm("ERROR_NO_DOUBLE")));

			throw e;
		} catch (NumberFormatException e) {
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.WRONG, value, 
					Localisation.getLocaleMessageTerm("ERROR_NO_DOUBLE")));

			throw e;
		}
	}
	
	public void checkIntegerValue(String value) 
		throws ParseException,NumberFormatException {
		
		try {
			
			Localisation.checkForInteger(value);
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.OK, value,null));
			
		} catch (ParseException e) {
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.WRONG, value, 
					Localisation.getLocaleMessageTerm("ERROR_NO_INT")));

			throw e;
		} catch (NumberFormatException e) {
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.WRONG, value, 
					Localisation.getLocaleMessageTerm("ERROR_NO_INT")));

			throw e;
		}
	}
	
	public void checkValueCompatible(String text) throws CompatibilityException{
		
		Compatibility[] comps = CompatibilityFactory.getCompatibilities();
		
		try {
			
			for (Compatibility c : comps){
				c.checkForInvalidIDChars(text);
				c.checkForMaxIDLength(text);
			}
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.OK, text,null));
			
		} catch (CompatibilityException e) {
			setChanged();
			notifyObservers(new UserInputEvent(this, UserInputEvent.Type.PROBLEM, text,e.getMessage()));

			throw e;
		}
	}
}
