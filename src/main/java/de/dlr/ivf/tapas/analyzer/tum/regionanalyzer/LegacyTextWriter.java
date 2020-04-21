/*
 * $Id$
 *
 * (c) Copyright 2009 DLR-VFBA
 * Erstellt: 27.01.2009
 */
package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.DistanceCategory;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Job;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Mode;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.PersonGroup;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.RegionCode;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.TripIntention;

public class LegacyTextWriter {

	private final String file;

	public LegacyTextWriter(String file) {
		this.file = file + "\\legacyText.txt";
	}

	public void process(RegionAnalyzer ra) {

		// String path = Activator.getDefault().getPath();
		try {
			File f = new File(file);
			PrintStream ps = new PrintStream(f);

			// for (RegionPOJO regionPOJO : analysen) {
			processTUM(ra, ps);
			// }

			ps.close();
		} catch (FileNotFoundException e) {
			//
		}
	}

	@SuppressWarnings("rawtypes")
	private void processTUM(RegionAnalyzer results, PrintStream ps) {

		RegionCode region = RegionCode.REGION_0;// TODO add loop over regions

		/*
		  Berechnung der Weglängen nach Distanzklassen sowie Durchschnittswerte
		  für Länge und Dauer
		 */
		ps.println("-----------------------------------------------------------------------------------------\n"
				+ "Ergebnisse Raumtyp "
				+ region
				+ " bzw. aller Trips, so keine Differenzierung vorgesehen:\n");

		long cntTripsRc = results.getCntTrips(region);
		long cntPersonsRc = results.getCntPersons(region);
		ps.println("\n\t Durchs. Anzahl der trips pro Person im Raumtyp: "
				+ region + ": " + saveDiv(cntTripsRc, cntPersonsRc));
		ps.println("Anzahl der Trips im Raumtyp "
				+ region
				+ ": "
				+ results.getCntTrips(region)
				+ "\n -----------------------------------------------------------------");

		ps.println("\n\t Min/Max distMin:= " + results.getMinTripLength(region)
				+ " distMax: " + results.getMaxTripLength(region) + " durMin: "
				+ results.getMinDuration(region) + " durMax: "
				+ results.getMaxDuration(region));

		Categories[] catRcPg = { Categories.RegionCode, Categories.PersonGroup };
		Enum[] instRcPg = { region, PersonGroup.PG_1 };

		for (PersonGroup pg : PersonGroup.values()) {
			if (pg == PersonGroup.PG_12) {
				continue;
			}
			instRcPg[1] = pg;
			long cntTrips = results.getCntTrips(catRcPg, instRcPg);
			int cntPersons = results.getCntPersons(pg, region);
			double dist = results.getDist(catRcPg, instRcPg);
			double dur = results.getDur(catRcPg, instRcPg);
			if (cntTrips > 0 && cntPersons > 0) {
				ps.println("\n\t MID-Gruppe " + (pg.getId() + 1)//
						+ ": N = " + cntPersons //
						+ " trips: " + cntTrips);
				ps.println("\t\t Durchs. Anzahl der trips: "
						+ saveDiv(cntTrips, cntPersons));
				ps.println("\t\t Durchs. Wegelaenge [m] der trips: "
						+ saveDiv(dist, cntTrips));
				ps.println("\t\t Durchs. Dauer [sek] der trips: "
						+ Math.round(saveDiv(dur, cntTrips)));
			}
		}

		double distRc = results.getDist(Categories.RegionCode, region);
		double durRc = results.getDur(Categories.RegionCode, region);

		ps.println("\n\t Anzahl Wege; alle Wegezwecke: trips: " + cntTripsRc);
		ps.println("\t\t Durchs. Wegelaenge [m] der trips: "
				+ (int) (saveDiv(distRc, cntTripsRc)));
		ps.println("\t\t Durchs. Dauer [sek] der trips: "
				+ (int) saveDiv(durRc, cntTripsRc));
		ps.println("\t\t Prozentuale Weglängenverteilung nach Kategorien:");

		Categories[] catRcDc = { Categories.RegionCode,
				Categories.DistanceCategory };
		Enum[] instRcDc = { region, DistanceCategory.CAT_1 };

		ArrayList<DistanceCategory> distanceCategories = results
				.getDistanceCategories();

		for (DistanceCategory dc : distanceCategories) {
			instRcDc[1] = dc;
			double distDc = results.getDist(catRcDc, instRcDc);

			ps.println("\t\t    " + results.getDistanceCategoryDescription(dc)
					+ ": " + perc(distDc, distRc));
		}

		ps.println("\n\t Prozentuale Tripverteilung nach Viseva-Personengruppen: N = "
				+ cntTripsRc);

		for (PersonGroup pg : PersonGroup.values()) {
			instRcPg[1] = pg;
			long cntTripRcPg = results.getCntTrips(catRcPg, instRcPg);

			ps.println("\t\t " + cntTripRcPg + "x\tPG_" + (pg.getId() + 1)
					+ " = " + perc(cntTripRcPg, cntTripsRc));
		}

		ps.println("\n\t Prozentuale Tripverteilung nach Tapas-Personengruppen: N = "
				+ cntTripsRc);
		Categories[] catRcTa = { Categories.RegionCode,
				Categories.Job };
		Enum[] instRcTa = { region, Job.JOB_1 };
		for (Job pg : Job.values()) {
			instRcTa[1] = pg;
			long cntPg = results.getCntTrips(catRcTa, instRcTa);
			ps.println("\t\t " + cntPg + "x\tPG_" + (pg.getId()) + " = "
					+ perc(cntPg, cntTripsRc));
		}

		ps.println("\n\t Prozentuale Tripverteilung nach Wegezwecken: N = "
				+ cntTripsRc);
		Categories[] catRcTi = { Categories.RegionCode,
				Categories.TripIntention };
		Enum[] instRcTi = { region, TripIntention.TRIP_31 };
		for (TripIntention ti : TripIntention.values()) {
			instRcTi[1] = ti;
			long cntTripRcTi = results.getCntTrips(catRcTi, instRcTi);

			ps.println("\t\t " + cntTripRcTi + "x\t" + ti.getCaption() + " = "
					+ perc(cntTripRcTi, cntTripsRc));
		}

		ps.println("\n\t Prozentuale Tripverteilung nach Viseva-Personengruppen und Wegezwecken: N = "
				+ cntTripsRc);
		Categories[] catRcTiPg = { Categories.RegionCode,
				Categories.TripIntention, Categories.PersonGroup };
		Enum[] instRcTiPg = { region, TripIntention.TRIP_31, PersonGroup.PG_1 };

		for (TripIntention ti : TripIntention.values()) {
			instRcTi[1] = ti;
			instRcTiPg[1] = ti;

			long cntRcTi = results.getCntTrips(catRcTi, instRcTi);

			for (PersonGroup pg : PersonGroup.values()) {
				instRcTiPg[2] = pg;
				long cntTripsRcTiPg = results
						.getCntTrips(catRcTiPg, instRcTiPg);

				ps.println("\t\t " + cntTripsRcTiPg + "x\t(" + ti.getCaption()
						+ " & PG_" + (pg.getId() + 1) + ") = "
						+ perc(cntTripsRcTiPg, cntRcTi));
			}
		}

		ps.println("\n\t Prozentuale Tripverteilung nach Tapas-Personengruppen und Wegezwecken: N = "
				+ cntTripsRc);
		Categories[] catRcTiTa = { Categories.RegionCode,
				Categories.TripIntention, Categories.Job };
		Enum[] instRcTiTa = { region, TripIntention.TRIP_31, Job.JOB_1 };
		for (TripIntention ti : TripIntention.values()) {
			for (Job pg : Job.values()) {
				instRcTiTa[1] = ti;
				instRcTiTa[2] = pg;
				long cntRcTiTa = results.getCntTrips(catRcTiTa, instRcTiTa);
				ps.println("\t\t " + cntRcTiTa + "x\t(" + ti.getCaption()
						+ " & PG_" + (pg.getId()) + ") =\t"
						+ perc(cntRcTiTa, cntTripsRc));
			}
		}

		ps.println("\n\t Modal Split: N = " + cntTripsRc);
		Categories[] catRcMo = { Categories.RegionCode, Categories.Mode };
		Enum[] instRcMo = { region, Mode.BIKE };
		for (Mode m : Mode.values()) {
			instRcMo[1] = m;
			long cntTripsRcMo = results.getCntTrips(catRcMo, instRcMo);
			ps.println("\t\t " + cntTripsRcMo + "x\t" + m.getDescription()
					+ " = " + perc(cntTripsRcMo, cntTripsRc));
		}

		ps.println("\n\t Modal Split pro Wegezweck und Viseva-Personengruppe: N = "
				+ cntTripsRc);
		Categories[] catRcMoTiPg = { Categories.RegionCode, Categories.Mode,
				Categories.TripIntention, Categories.PersonGroup };
		Enum[] instRcMoTiPg = { region, Mode.BIKE, TripIntention.TRIP_31,
				PersonGroup.PG_1 };
		for (TripIntention ti : TripIntention.values()) {
			for (PersonGroup pg : PersonGroup.values()) {
				instRcTiPg[1] = ti;
				instRcMoTiPg[2] = ti;
				instRcTiPg[2] = pg;
				instRcMoTiPg[3] = pg;
				long cntTripsRcTiPg = results
						.getCntTrips(catRcTiPg, instRcTiPg);
				ps.println("\t" + ti.getCaption() + "(" + pg.getName()
						+ ")\t\tn = " + cntTripsRcTiPg);
				if (cntTripsRcTiPg > 0) {
					for (Mode m : Mode.values()) {
						instRcMoTiPg[1] = m;
						long cntTripsRcMoTiPg = results.getCntTrips(
								catRcMoTiPg, instRcMoTiPg);
						ps.println("\t\t " + cntTripsRcMoTiPg + "x\t"
								+ m.getDescription() + " = "
								+ perc(cntTripsRcMoTiPg, cntTripsRcTiPg));
					}
				}
			}
		}

		ps.println("\n\t Modal Split pro Wegezweck und Tapas-Personengruppe: N = "
				+ cntTripsRc);
		Categories[] catRcMoTiTa = { Categories.RegionCode, Categories.Mode,
				Categories.TripIntention, Categories.Job };
		Enum[] instRcMoTiTa = { region, Mode.BIKE, TripIntention.TRIP_31,
				Job.JOB_1 };
		for (TripIntention ti : TripIntention.values()) {
			for (Job job : Job.values()) {
				instRcTiTa[1] = ti;
				instRcMoTiTa[2] = ti;
				instRcTiTa[2] = job;
				instRcMoTiTa[3] = job;
				long cntTripsRcTiTa = results
						.getCntTrips(catRcTiTa, instRcTiTa);
				ps.println("\t" + ti.getCaption() + "(PG_" + job.getId()
						+ ")\t\tn = " + cntTripsRcTiTa);
				if (cntTripsRcTiTa > 0) {
					for (Mode m : Mode.values()) {
						instRcMoTiTa[1] = m;
						long cntTripsRcMoTiTa = results.getCntTrips(
								catRcMoTiTa, instRcMoTiTa);
						ps.println("\t\t " + cntTripsRcMoTiTa + "x\t"
								+ m.getDescription() + " = "
								+ perc(cntTripsRcMoTiTa, cntTripsRcTiTa));
					}
				}
			}
		}

		ps.println("\n\t Modal Split pro Wegezweck: N = " + cntTripsRc);
		Categories[] catRcMoTi = { Categories.RegionCode, Categories.Mode,
				Categories.TripIntention };
		Enum[] instRcMoTi = { region, Mode.BIKE, TripIntention.TRIP_31 };
		for (Mode m : Mode.values()) {
			instRcMoTi[1] = m;
			for (TripIntention ti : TripIntention.values()) {

				instRcMoTi[2] = ti;
				instRcTi[1] = ti;

				long cntTripsRcMoTi = results
						.getCntTrips(catRcMoTi, instRcMoTi);
				long cntTripsRcTi = results.getCntTrips(catRcTi, instRcTi);

				ps.println("\t\t " + cntTripsRcMoTi + "x\t"
						+ m.getDescription() + "(" + ti.getCaption() + ")"
						+ " = " + perc(cntTripsRcMoTi, cntTripsRcTi));
			}
		}

		ps.println("\n\t Modal Split pro Wegelänge: N = " + cntTripsRc);
		Categories[] catRcDcMo = { Categories.RegionCode,
				Categories.DistanceCategory, Categories.Mode };
		Enum[] instRcDcMo = { region, distanceCategories.get(0), Mode.BIKE };
		for (DistanceCategory dc : distanceCategories) {
			instRcDc[1] = dc;
			instRcDcMo[1] = dc;
			long cntRcDc = results.getCntTrips(catRcDc, instRcDc);
			ps.println("\t" + results.getDistanceCategoryDescription(dc)
					+ "\t\tn = " + cntRcDc);

			if (cntRcDc > 0) {
				for (Mode mo : Mode.values()) {
					instRcDcMo[2] = mo;
					long cntRcDcMo = results.getCntTrips(catRcDcMo, instRcDcMo);
					ps.println("\t\t" + cntRcDcMo + "x\t" + mo.getDescription()
							+ " = " + perc(cntRcDcMo, cntRcDc));
				}
			}
		}

		ps.println("\n\t Modal Split pro Wegelänge und Wegezweck: N = "
				+ cntTripsRc);
		Categories[] catRcDcTiMo = { Categories.RegionCode,
				Categories.DistanceCategory, Categories.TripIntention,
				Categories.Mode };
		Enum[] instRcDcTiMo = { region, distanceCategories.get(0),
				TripIntention.TRIP_31, Mode.BIKE };
		Categories[] catRcDcTi = { Categories.RegionCode,
				Categories.DistanceCategory, Categories.TripIntention };
		Enum[] instRcDcTi = { region, distanceCategories.get(0),
				TripIntention.TRIP_31 };
		for (DistanceCategory dc : distanceCategories) {
			for (TripIntention ti : TripIntention.values()) {
				instRcDcTi[1] = dc;
				instRcDcTiMo[1] = dc;
				instRcDcTi[2] = ti;
				instRcDcTiMo[2] = ti;
				long cntRcDcTi = results.getCntTrips(catRcDcTi, instRcDcTi);
				ps.println("\t" + results.getDistanceCategoryDescription(dc)
						+ "(" + ti.getCaption() + ")\t\tn = " + cntRcDcTi);
				if (cntRcDcTi > 0) {
					for (Mode mo : Mode.values()) {
						instRcDcTiMo[3] = mo;
						long cntRcDcTiMo = results.getCntTrips(catRcDcTiMo,
								instRcDcTiMo);
						ps.println("\t\t" + cntRcDcTiMo + "x\t"
								+ mo.getDescription() + " = "
								+ perc(cntRcDcTiMo, cntRcDcTi));
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param n
	 * @param d
	 * @return 0.0 if <code>d == 0</code> and <code>n/d</code> else
	 */
	private static double saveDiv(double n, double d) {
		return d != 0 ? n / d : 0.0;
	}

	/**
	 * @return p/n*100 as a {@link String} with 4 decimal places
	 */
	private static String perc(double p, double n) {
		return String.format("%.4f", saveDiv(p, n) * 100)+ "%";
	}

}
