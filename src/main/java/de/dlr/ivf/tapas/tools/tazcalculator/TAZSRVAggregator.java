
package de.dlr.ivf.tapas.tools.tazcalculator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import de.dlr.ivf.tapas.util.TPS_Geometrics;

/**
 * This class reads a mapping file for tvz to a more aggregate level. 
 * Then it loads some source/dest-matrices and calculates the mean or median of the aggregated values.
 * Please see main routine for filepaths (hard coded fro simplicity...)
 * @author hein_mh
 *
 */

public class TAZSRVAggregator {
	
	
	double[][] tt=null, ttStatArea=null;
	double[][] dist=null, distStatArea=null;
	HashMap<Integer, ArrayList<Integer>> statisticalAreas = new HashMap<>();
	HashMap<Integer, Point> tazCentroids = new HashMap<>();

	class Point {

		double x,y;

		public Point(double x, double y){
			this.x=x;
			this.y=y;
		}
		
		public double distanceTo(Point ref){
			return TPS_Geometrics.getDistance(this.x, this.y, ref.x, ref.y);
		}

	}

	/**
	 * Function to read taz-coordinates from a csv-file tab-separated without header: id x y
	 * @param string input file
	 */
	private void readTAZCentroids(String string) {

		try {
			FileReader in = new FileReader(string);
			String line;
			StringTokenizer tokens;
			BufferedReader input = new BufferedReader (in);
			int id;
			double x,y;
			do{
				line = input.readLine();
				if(line!=null){
					tokens = new StringTokenizer(line, "\t ");
					if(tokens!=null){
						//check tokens
						if(tokens.countTokens()!= 3){
							System.err.println("Input error! File "+string+" size tokens: "+tokens.countTokens()+ " ecpected tokens: 3");
						}
						else{ //parse data
							id= Integer.parseInt(tokens.nextToken().trim());
							x= Double.parseDouble(tokens.nextToken().trim());
							y= Double.parseDouble(tokens.nextToken().trim());
							this.tazCentroids.put(id, new Point(x,y));
						}
					}					
				}
			}while (line != null);
			input.close();
			in.close();
			
			System.out.println("Reading centroids done. File "+string);
			

		}catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void writeODValueFile(double[][] matrix, String file){
		try {
			if (matrix != null) {
				FileWriter outTrip = new FileWriter(file);
				for (int i = 0; i < matrix.length; ++i) {
					for (int j = 0; j < matrix[i].length; ++j) {
						outTrip.write((i + 1) +"\t"+ (j + 1) +"\t"+ matrix[i][j] +"\n");
					}
				}
				outTrip.close();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Writing file done:" +file);
		
	}
	
	
	/**
	 * Function to read a specified o/d-matrix. Tab or space delimited row-wise matrix
	 * @param name Input
	 * @return matrix or null
	 */
	private double[][] readMatrices(String name){
		double[][] destination=null;
		try {
			FileReader in = new FileReader(name);
			String line;
			StringTokenizer tokens;
			BufferedReader input = new BufferedReader (in);
			int i, j=0;
			do{
				line = input.readLine();
				if(line!=null){
					tokens = new StringTokenizer(line, "\t ");
					if(tokens!=null){
						if(destination==null){
							destination=new double[tokens.countTokens()][tokens.countTokens()];
							System.out.println("Found "+destination.length+" TAZ");
						}
						if(tokens.countTokens()!= destination.length){
							System.err.println("Input error! File "+name+" size tokens: "+tokens.countTokens()+ " size array "+destination.length);
						}
						i=0;
						while(tokens.hasMoreTokens()){
							String tok = tokens.nextToken();
							destination[j][i]= Integer.parseInt(tok);
							i++;
						}
					}					
				}
				j++;
			}while (line != null);
			input.close();
			in.close();
			
			System.out.println("Reading done. File "+name);
			

		}catch (IOException e1) {
			e1.printStackTrace();
		}
		return destination;
		
	}

	/**
	 * Function to read a mapping file (semicolon separated with header): source id;destinantion id
	 * @param name The filename
	 */
	
	private void readStatisticalAreasMappingf(String name){
		try {
			FileReader in = new FileReader(name);
			String line;
			StringTokenizer tokens;
			BufferedReader input = new BufferedReader (in);
			line = input.readLine(); //header
			int statArea, tazIndex;
			ArrayList<Integer> mapping;
			while (line != null){
				line = input.readLine();
				if(line!=null){
					tokens = new StringTokenizer(line, ";");
					
					if(tokens!=null && tokens.countTokens()>=2){
						statArea = Integer.parseInt(tokens.nextToken().trim());
						tazIndex = Integer.parseInt(tokens.nextToken().trim());
						if(this.statisticalAreas.containsKey(statArea)){ //known element
							mapping = this.statisticalAreas.get(statArea);
							mapping.add(tazIndex);
						}
						else{ //new element
							mapping = new ArrayList<>();
							mapping.add(tazIndex);
							this.statisticalAreas.put(statArea, mapping);
						}
					}															
				}
			}
			input.close();
			in.close();
			
			
			
			System.out.println("Reading mapping done. File:" +name +" Found areas: "+this.statisticalAreas.size());
			

		}catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	}
	
	/**
	 * Calculates the aggregated s/d-matrix. The flag useMedian determines, 
	 * if the resulting correspondences should be outlierfiltered: 
	 * Largest and smallest values are removed from the aggregation set.
	 * @param source source matrix
	 * @param useMedian flag for median filtering
	 * @return aggregated matrix
	 */
	
	private double[][] calculateAggegation( double [][] source, boolean useMedian){
		
		final int sizeStatArea = this.statisticalAreas.size();
		
		double[][] destination = new double[sizeStatArea][sizeStatArea];
		
		
		List<Double> values;
		double average;
		
		//wow! four nested loops!
		
		//over all areas
		for(Integer fromS : this.statisticalAreas.keySet()){
			for(Integer toS : this.statisticalAreas.keySet()){
				values = new ArrayList<>();
				//collect data
				for(Integer fromT : this.statisticalAreas.get(fromS)){
					for(Integer toT : this.statisticalAreas.get(toS)){
						if(!fromT.equals(toT) || source[fromT-1][toT-1]>0){
							values.add(source[fromT-1][toT-1]);
						}
					}
				}			
				if(values.size()>0){
					//build average
					average=0;
					if(useMedian){ //mean of median center
						Collections.sort(values);
						//enough data: forget closest and farest cell 
						if(values.size()>=3){
							for(int i=1; i<values.size()-1 ;++i){
								average+=values.get(i);
							}
							average /= values.size()-2;
						}
						else{ //use all
							for (Double value : values) {
								average += value;
							}
							average /= values.size();						
						}					
					}
					else{ //mean
						for (Double value : values) {
							average += value;
						}
						average /= values.size();											
					}
					destination[fromS-1][toS-1]=average;
				}
			}			
		}
		return destination;
	}
	
	/**
	 * Internal method to calculate the beeline matrix of the tvz-centroids
	 * @return
	 */
	private double[][] calculateBeeline(){
		
		int sizeTAZ = this.tazCentroids.size();
		double[][] destination = new double[sizeTAZ][sizeTAZ];
		
			
		//collect data
		for(int fromT =0; fromT<sizeTAZ;++fromT){
			for(int toT =0; toT<sizeTAZ;++toT){
				if(fromT!=toT  || destination[fromT][toT]>0){
					destination[fromT][toT]= this.tazCentroids.get(fromT+1).distanceTo(this.tazCentroids.get(toT+1));
				}
				else{
					destination[fromT][toT]=0;
				}
			}
		}
		return destination;
	}
	
	
	/**
	 * Writes a tab separated matrix
	 * @param matrix input
	 * @param file outpu
	 */
	private void writeMatrix(double[][] matrix, String file){
		try {
			if (matrix != null) {
				FileWriter outTrip = new FileWriter(file);
				for (double[] doubles : matrix) {
					for (double aDouble : doubles) {
						outTrip.write(Double.toString(aDouble));
						outTrip.write("\t");
					}
					outTrip.write("\n");
				}
				outTrip.close();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Writing file done:" +file);
	}
		
	/**
	 * @param args
	 */
	public static void main(String[] args){
		TAZSRVAggregator worker = new TAZSRVAggregator();

		worker.readStatisticalAreasMappingf("F:\\Daten\\srv\\TVZ_879_Altbezirke_IDs.csv");
		worker.readTAZCentroids("F:\\Daten\\srv\\tvzIDBerlinGK.txt");
		
		//MIV tt
		worker.tt = worker.readMatrices("F:\\Daten\\srv\\ttBerlinMIVFinal.txt");
		TPS_Geometrics.calcTop3(worker.tt);
		worker.ttStatArea = worker.calculateAggegation(worker.tt, true);
		worker.writeMatrix(worker.ttStatArea, "F:\\Daten\\srv\\rita_out_TT_IV_median.csv");
		worker.ttStatArea = worker.calculateAggegation(worker.tt, false);
		worker.writeMatrix(worker.ttStatArea, "F:\\Daten\\srv\\rita_out_TT_IV_mean.csv");
		worker.writeODValueFile(worker.tt, "F:\\Daten\\srv\\rita_out_TT_IV_od_list.csv");
		worker.writeODValueFile(worker.ttStatArea, "F:\\Daten\\srv\\rita_out_TT_IV_od_list_23_bez.csv");
		
		
		//MIT dist
		worker.dist = worker.readMatrices("F:\\Daten\\srv\\distBerlin.txt");
		TPS_Geometrics.calcTop3(worker.dist);
		worker.distStatArea = worker.calculateAggegation(worker.dist, true);
		worker.writeMatrix(worker.distStatArea, "F:\\Daten\\srv\\rita_out_dist_IV_median.csv");
		worker.distStatArea = worker.calculateAggegation(worker.dist, false);
		worker.writeMatrix(worker.distStatArea, "F:\\Daten\\srv\\rita_out_dist_IV_mean.csv");
		worker.writeODValueFile(worker.dist, "F:\\Daten\\srv\\rita_out_dist_IV_od_list.csv");
		worker.writeODValueFile(worker.distStatArea, "F:\\Daten\\srv\\rita_out_dist_IV_od_list_23_bez.csv");

		//PT tt
		worker.tt = worker.readMatrices("F:\\Daten\\srv\\OEV_TT_B_2005_HDB.txt");
		TPS_Geometrics.calcTop3(worker.tt);
		worker.ttStatArea = worker.calculateAggegation(worker.tt, true);
		worker.writeMatrix(worker.ttStatArea, "F:\\Daten\\srv\\rita_out_TT_PT_median.csv");
		worker.ttStatArea = worker.calculateAggegation(worker.tt, false);
		worker.writeMatrix(worker.ttStatArea, "F:\\Daten\\srv\\rita_out_TT_PT_mean.csv");
		worker.writeODValueFile(worker.tt, "F:\\Daten\\srv\\rita_out_TT_PT_od_list.csv");
		worker.writeODValueFile(worker.ttStatArea, "F:\\Daten\\srv\\rita_out_TT_PT_od_list_23_bez.csv");

		//beeline
		worker.dist=worker.calculateBeeline();
		TPS_Geometrics.calcTop3(worker.dist);
		worker.distStatArea = worker.calculateAggegation(worker.dist, true);
		worker.writeMatrix(worker.distStatArea, "F:\\Daten\\srv\\rita_out_dist_Beeline_median.csv");
		worker.distStatArea = worker.calculateAggegation(worker.dist, false);
		worker.writeMatrix(worker.distStatArea, "F:\\Daten\\srv\\rita_out_dist_Beeline_mean.csv");
		worker.writeODValueFile(worker.dist, "F:\\Daten\\srv\\rita_out_dist_BL_od_list.csv");
		worker.writeODValueFile(worker.distStatArea, "F:\\Daten\\srv\\rita_out_dist_BL_od_list_23_bez.csv");
	}


}
