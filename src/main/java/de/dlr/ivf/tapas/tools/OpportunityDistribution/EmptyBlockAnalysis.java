package de.dlr.ivf.tapas.tools.OpportunityDistribution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class EmptyBlockAnalysis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File file_new = new File(
				"C:\\Users\\boec_pa\\Documents\\HouseholdDistribution\\emptyBlocks_neu.dat");
		File file_old = new File(
				"C:\\Users\\boec_pa\\Documents\\HouseholdDistribution\\emptyBlocks_old.dat");

		ArrayList<Integer> oldBlocks = new ArrayList<>();
		ArrayList<Integer> newBlocks = new ArrayList<>();

		try {

			BufferedReader br = new BufferedReader(new FileReader(file_new));

			String line;
			while ((line = br.readLine()) != null) {
				newBlocks.add(Integer.valueOf(line));
			}

			br.close();

			br = new BufferedReader(new FileReader(file_old));

			while ((line = br.readLine()) != null) {
				oldBlocks.add(Integer.valueOf(line));
			}

			br.close();
			
			HashSet<Integer> oldB = new HashSet<>(oldBlocks);
			HashSet<Integer> newB = new HashSet<>(newBlocks);
			
			newB.removeAll(oldB);
			System.out.println("All blocks in new but not in old ("+newB.size() + "):");
			for (int i : newB){
				System.out.println(i + ",");
			}
			

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
