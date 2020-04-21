package de.dlr.ivf.tapas.tools.locationallocation;

public enum State {
	SCHLESWIG_HOLSTEIN(1), HAMBURG(2), NIEDERSACHSEN(3), BREMEN(4), NORDRHEIN_WESTFALEN(5), HESSEN(6), RHEINLAND_PFALZ(7), BADEN_WÜRTTEMBERG(8), FREISTAAT_BAYERN(9), SAARLAND(10), BERLIN(11), BRANDENBURG(
			12), MECKLENBURG_VORPOMMERN(13), SACHSEN(14), SACHSEN_ANHALT(15), THÜRINGEN(16);

	private final int gemeindeSchluessel;

	State(int gemeindeSchluessel) {
		this.gemeindeSchluessel = gemeindeSchluessel;
	}

	public static State getByGemeindeSchluessel(int gemeindeSchluessel) {
		State[] values = State.values();
		for (State bundesland : values) {
			if (bundesland.gemeindeSchluessel == gemeindeSchluessel)
				return bundesland;
		}
		return null;
	}

	public int getGemeindeSchluessel() {
		return gemeindeSchluessel;
	}

	public static State getByName(String stateName) {
		if (stateName != null) {
			State[] values = State.values();
			for (State bundesland : values) {
				if (bundesland.name().toLowerCase().equals(stateName.toLowerCase()))
					return bundesland;
			}
		}
		return null;
	}
}
