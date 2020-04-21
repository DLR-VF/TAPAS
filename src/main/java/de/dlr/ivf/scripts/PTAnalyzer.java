package de.dlr.ivf.scripts;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import de.dlr.ivf.scripts.SrVCalculator.TAZ;
import de.dlr.ivf.tapas.tools.TPS_VisumOeVToDB;

public class PTAnalyzer extends TPS_VisumOeVToDB {



	

	
	public void printMinMaxAvg( PTNodeField field){
        double[][] mat = this.getMatrixForField(field);
		double min= Double.MAX_VALUE, max =0, avg=0, count =0;
		//iterate over cells
		for(int i=0;i<mat.length; ++i){
			for (int j=0; j<mat[i].length;++j){
				if(mat[i][j]>=0 && i!=j){ // no connection is negative and diagonal is not testet jet
					min= Math.min(min,  mat[i][j]);
					max= Math.max(max,  mat[i][j]);
					avg +=mat[i][j];
					count ++;
				}
			}
		}
		
		System.out.println("Field "+field +": min: "+min+" max: "+max+ " avg : "+avg/count);
	}
	
	class TAZCounter implements Comparable<TAZCounter>{
		public int TAZ;
		public int val;

		public TAZCounter(int taz, int val){
			this.TAZ= taz;
			this.val = val;
		}

		public int compareTo(TAZCounter arg0) {
			
			return -(this.val-arg0.val);
		}
		
	}
	
	public void printMinMaxAvgSpeed( PTNodeField dist, PTNodeField time, double maxSpeed, double minSpeed){
        double[][] distance = this.getMatrixForField(dist);
        double[][] travelTime = this.getMatrixForField(time);
		double min= Double.MAX_VALUE, max =0, avg=0, count =0;
		Map<Integer, Integer> badTaz = new HashMap<>();
		int countBadConnections =0;
		int oldVal;
		//iterate over cells
		for(int i=0;i<distance.length; ++i){
			for (int j=0; j<distance[i].length;++j){
				if(distance[i][j]>=0 && travelTime[i][j] >0 && i!=j){ // no connection is negative and diagonal is not testet jet
					double val = 3.6*distance[i][j]/travelTime[i][j];
					
					min= Math.min(min, val);
					max= Math.max(max,  val);
					avg +=val;
					count ++;
					if( val<minSpeed && distance[i][j]>1000 || val>maxSpeed) 
					{
						countBadConnections++;
						oldVal = badTaz.get(i)==null?1:badTaz.get(i)+1; 
						badTaz.put(i,oldVal);
						oldVal = badTaz.get(j)==null?1:badTaz.get(j)+1;
						badTaz.put(j,oldVal);
						//TAZ from = this.TAZes.get(this.reverseTAZIDMap.get(i));
						//TAZ to = this.TAZes.get(this.reverseTAZIDMap.get(j));
						TAZ from = this.TAZes.get(this.reverseTAZIDMap.get(i));
						TAZ to = this.TAZes.get(this.reverseTAZIDMap.get(j));
						//if(from.taz_id == 110502911 || to.taz_id == 110502911 )
							System.out.println("Speed: "+val+"km/h from "+from.taz_id + "("+from.description+") to "+to.taz_id+"("+to.description+") tt "+travelTime[i][j]+ " dist "+distance[i][j]+ " bl "+ from.getDistance(to));
					}
				}
			}
		}

		System.out.println("Found "+countBadConnections+" bad connections between "+badTaz.size()+" tazes");

		Set<TAZCounter> tazSet = new TreeSet<>();
		for(Entry<Integer, Integer> entry: badTaz.entrySet()){
			TAZCounter e = new TAZCounter(entry.getKey(),  entry.getValue());
			tazSet.add(e); 
		}
		
		for(TAZCounter entry: tazSet.toArray(new TAZCounter[0])){
			TAZ taz = this.TAZes.get(this.reverseTAZIDMap.get(entry.TAZ));
			System.out.println("Bad Taz "+taz.taz_id + " "+taz.description+" count: "+entry.val);
		}
		System.out.println("Speed: min: "+min+" max: "+max+ " avg : "+avg/count);
	}

	
	public int numberOfWalkingConnections(){
		int count=0;
		//iterate over cells
		for(int i=0;i<this.nodes.length; ++i){
			for (int j=0; j<this.nodes[i].length;++j){
				if(this.nodes[i][j].bdh<-1 && i!=j){ // walkers are negative bdh  values
					count ++;
				}
			}
		}

		System.out.println("Number of walkers: "+count);
		return count;
	}

	public int numberOfLongDistanceWalkingConnections(double maxWalk){
		int count=0;
		//iterate over cells
		double avgLength=0;
		double max=0;
		int fromMax=0, toMax=0;
		for(int i=0;i<this.nodes.length; ++i){
			for (int j=0; j<this.nodes[i].length;++j){
				if(	i!=j &&
					this.nodes[i][j].bdh<-1 && 
					this.nodes[i][j].bld>maxWalk){ // walkers are negative bdh  values
					count ++;
					avgLength+=this.nodes[i][j].bld;
					if(this.nodes[i][j].bld>max){
						max = this.nodes[i][j].bld;
						fromMax = this.nodes[i][j].idStart;
						toMax = this.nodes[i][j].idEnd;
					}
					
				}
			}
		}

		System.out.println("Number of walkers over "+maxWalk+" km: "+count+" avg dist: "+avgLength/count);
		System.out.println("Max from "+fromMax+" to "+toMax+" : "+max);
		return count;
	}

	public void writeCSVXY( PTNodeField dist, PTNodeField time, double sample, String filename){
        double[][] x = this.getMatrixForField(dist);
        double[][] y = this.getMatrixForField(time);
		try {
			FileWriter writer = new FileWriter(filename);
			writer.append("from\tto\tx\ty\n");
			//iterate over cells
			for(int i=0;i<x.length; ++i){
				for (int j=0; j<x[i].length;++j){
					if(Math.random()<sample){
						writer.append(i +"\t"+ j +"\t"+ x[i][j] +"\t"+ y[i][j] +"\n");
						
					}
				}
			}
			
			writer.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PTAnalyzer analyzer = new PTAnalyzer();
		//String path= "Y:\\Netzgrundlagen_alle_Verkehrstraeger\\Berlin-Netz\\Berlin-Visum-OEV\\Matrizen\\Matthias Heinrichs-o\\";
		//String file= "Berlin_6-18-MinWID1G.";
		//String path= "\\\\Vf-atlas\\visum\\Master-Berlin\\OEV\\Daten\\ÖV-Kenngrößen Fahrplanfein\\Branch-and-Bound-Suche\\";
		//String file= "20150821_OEV-Kenngroessen_fahrplanfein_branch-and-bound.";
		//analyzer.loadTAZ("core.berlin_taz_multiline");
		String path= "T:\\Runs\\IHK\\ÖV-Daten\\Basis_2017\\";
		String file= "Basis-2017_M.";
		analyzer.loadTAZ("core.berlin_taz_1223_umland");
		analyzer.initTAZIDs(path+file+"JRD");
		analyzer.readValues(path+file+"JRD", PTNodeField.JRD,900000,0);
		analyzer.readValues(path+file+"NTR", PTNodeField.NTR,10,0);
		analyzer.readValues(path+file+"SFQ", PTNodeField.BDH,900000,0);
		analyzer.readValues(path+file+"OWTA", PTNodeField.SWT,900000,0);
		analyzer.readValues(path+file+"TWT", PTNodeField.TWT,12000,0);
		analyzer.readValues(path+file+"IVT", PTNodeField.IVT,12000,0);
		analyzer.readValues(path+file+"ACT", PTNodeField.ACT,900000,0);
		analyzer.readValues(path+file+"EGT", PTNodeField.EGT,900000,0);
		
		analyzer.calcBeelines();
		analyzer.transformNumOfConnectionToInitialWaiting(720, 0.5, 1);
		analyzer.transformUnits();
		//analyzer.handleMissingValues();
		analyzer.calcTop3(0.8);
		analyzer.calcSumOfTT();
//		analyzer.printMinMaxAvg(PTNodeField.JRD);
//		analyzer.printMinMaxAvg(PTNodeField.NTR);
//		analyzer.printMinMaxAvg(PTNodeField.BDH);
//		analyzer.printMinMaxAvg(PTNodeField.SWT);
//		analyzer.printMinMaxAvg(PTNodeField.TWT);
//		analyzer.printMinMaxAvg(PTNodeField.IVT);
//		analyzer.printMinMaxAvg(PTNodeField.ACT);
//		analyzer.printMinMaxAvg(PTNodeField.EGT);
//		analyzer.printMinMaxAvg(PTNodeField.BLD);
//		analyzer.numberOfWalkingConnections();
//		analyzer.numberOfLongDistanceWalkingConnections(1);
//		analyzer.calcTop3();
//		analyzer.printMinMaxAvg(PTNodeField.JRD);
//		analyzer.printMinMaxAvg(PTNodeField.NTR);
//		analyzer.printMinMaxAvg(PTNodeField.BDH);
//		analyzer.printMinMaxAvg(PTNodeField.SWT);
//		analyzer.printMinMaxAvg(PTNodeField.TWT);
//		analyzer.printMinMaxAvg(PTNodeField.IVT);
//		analyzer.printMinMaxAvg(PTNodeField.ACT);
//		analyzer.printMinMaxAvg(PTNodeField.EGT);
//		analyzer.printMinMaxAvg(PTNodeField.BLD);
		//analyzer.printMinMaxAvgSpeed(PTNodeField.JRD, PTNodeField.SUMTT, 80, 3);
		//analyzer.printMinMaxAvgSpeed(PTNodeField.JRD, PTNodeField.IVT, 60, 5);
		analyzer.writeCSVXY(PTNodeField.JRD, PTNodeField.SUMTT, 0.001,path+"jrd-summtt.csv");

		
	}

}
