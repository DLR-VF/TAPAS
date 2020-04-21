package de.dlr.ivf.tapas.util;

import java.util.Vector;

/**
 * Class provides helper function for random selection
 * 
 */
public class TPS_GX {

	/**
	 * Calculates the acceptance probability of the value provided (resistance term) according to a EVA1-Curve (Lohse),
	 * who's calibration parameter have to be provided
	 * I
	 * I------  F
	 * I       \
	 * I        -\          
	 * I          \  G           
	 * I           \          
	 * I            -\ ___________    E   
	 * ----------------------------------
	 * 
	 * @param value
	 *            value to determine the acceptance for
	 * @param EBottom
	 *            parameter determining the curve in the lower part (snuggle at the bottom)
	 * @param FTop
	 *            parameter determining the curve in the upper part (acceptance drop for small discrepancies)
	 * @param TurningPoint
	 *            turning point of the function
	 * @return acceptance probability between 0.0 and 1.0
	 */
	public static double calculateEVA1Acceptance(double value, double EBottom, double FTop, double TurningPoint) {
        return 1 / Math.pow((1 + value), (EBottom / (1 + TPS_FastMath.exp(FTop * (1 - value / TurningPoint)))));
	}

	/* (C) Copr. 1986-92 Numerical Recipes Software 0NLINE. */
	// http://lib-www.lanl.gov/numerical/bookcpdf.html
	/**
	 * Method to identify the sign of the given double with a epsilon around zero
	 * 
	 * @param delta
	 * @return -1 if delta is below -eps, 0 if delta is between -eps and eps and 1 if delta is greater than eps
	 */
	public static int deltaSign(double delta) {
		double eps = 1e-10;

		if (delta > eps) {
			return 1;
		} else if (delta < -eps) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * tridiag solves the system of linear equations, legacy import from old c files, not used for a long time!
	 * 
	 * Attention: input vectors are references and thus eventually changed!
	 * 
	 * @param a Vector
	 * @param b Vector
	 * @param c Vector
	 * @param rhs Vector
	 * @param sol Vector
	 * @return sol Vector<Double>
	 */
	public static Vector<Double> tridiag(Vector<Double> a, Vector<Double> b, Vector<Double> c, Vector<Double> rhs,
			Vector<Double> sol) {
		double bet;
		double solC;

		int n = b.size();
		assert (a.size() == n);
		assert (c.size() == n);

		if (n == 0) {
			return null;
		}

		sol.clear();

		Vector<Double> gam = new Vector<>();
		gam.setSize(n);

		if (b.get(0) == 0.0) {
//			LOG.error("\t\t\t '--> gx.tridiag: b[0] = 0: reorder the system.");
			return null;
		}
		solC = rhs.get(0) / (bet = b.get(0));
		sol.add(solC);
		for (int j = 1; j < n; ++j) {
			gam.setElementAt(c.get(j - 1) / bet, j);
			bet = b.get(j) - a.get(j) * gam.get(j);
			if (bet == 0.0) {
//				LOG.error("\t\t\t '--> GX:Tridiag: bet = 0; algorithm fails.");
				return null;
			}
			solC = (rhs.get(j) - a.get(j) * solC) / bet;
			sol.add(solC);
		}

		for (int j = n - 2; j >= 0; --j) {
			double buff = sol.get(j);
			buff -= gam.get(j + 1) * sol.get(j + 1);
			sol.setElementAt(buff, j);
		}

		return (sol);

	} // Tridiag
}
