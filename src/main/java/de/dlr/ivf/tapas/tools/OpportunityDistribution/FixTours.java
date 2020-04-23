package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import java.io.*;

public class FixTours {
	public static void main(String[] args) {

		String input = "C:\\Users\\boec_pa\\Documents\\Touren\\MiD2008_Wege_Rita-Alex_Paul_reduziert.csv";
		String output = "C:\\Users\\boec_pa\\Documents\\Touren\\out.csv";

		try {

			BufferedWriter bw = new BufferedWriter(new FileWriter(output));

			BufferedReader br = new BufferedReader(new FileReader(input));

			String line;
			bw.write(br.readLine() + ";tour_nr\n");// Header

			int tour_nr = 1;

			while ((line = br.readLine()) != null) {
				String[] s = line.split(";");
				if (Integer.parseInt(s[3]) == 1) {// next
					tour_nr = 1;
				}

				bw.write(line + ";" + tour_nr + "\n");// Header

				if (Integer.parseInt(s[7]) == 101) {// ziel
					tour_nr++;
				}

			}

			br.close();
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
