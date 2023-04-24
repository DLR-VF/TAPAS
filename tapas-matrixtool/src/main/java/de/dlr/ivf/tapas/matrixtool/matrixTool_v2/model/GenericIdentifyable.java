package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model;

import de.dlr.ivf.tapas.matrixtool.erzeugung.model.AbstractIdentifyable;
import de.dlr.ivf.tapas.matrixtool.erzeugung.model.Identificator;

public class GenericIdentifyable extends AbstractIdentifyable {

	public GenericIdentifyable(String... id){
		this.id = new Identificator(id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericIdentifyable other = (GenericIdentifyable) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
