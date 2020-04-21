package de.dlr.ivf.tapas.tools.matrixMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import de.dlr.ivf.tapas.tools.persitence.db.TPS_BasicConnectionClass;
import de.dlr.ivf.tapas.util.TPS_Geometrics;

public class CruisingSpeedCalculate extends TPS_BasicConnectionClass
{
    private final Map<String, Double> correctionFactorModus = new HashMap<>();
    private final Map<String, Double> correctionFactorTerrain  = new HashMap<>();
    
    private CruisingSpeedCalcData calcData;
    private double[][] travelTime = null;
    private double[][] beeLine = null;
    private double[][] indirectWayFactor = null;

	/**
     *  (factors[0]/log10(factors[1]*(x^factors[2])+factors[3]))+factors[4]
     *  (1         /log10(200       *(x^4)         +1         ))+1,1
     */
    //private double[] factors = {1.0, 200.0, 4.0, 1.0, -0.1};
    //private double[] factors = {1.0, 200.0, 2.0, 1.0, 0.4};
    //private double[] factors = {1.0, 20.0, 1.0, 1.0, 0.5};
    private double[] factors = {1.0, 50.0, 2.0, 1.0, 0.60};
    
    public CruisingSpeedCalculate(CruisingSpeedCalcData calcData)
    {
        this.calcData = calcData;
        init();
    }
    
    private void init()
    {
      //correction factor for modi
        correctionFactorModus.put(Modus.PT.toString(), Modus.PT.getCorrectionFactor());
        correctionFactorModus.put(Modus.MIV.toString(), Modus.MIV.getCorrectionFactor());
        correctionFactorModus.put(Modus.BIKE.toString(), Modus.BIKE.getCorrectionFactor());
        correctionFactorModus.put(Modus.WALK.toString(), Modus.WALK.getCorrectionFactor());
        
        //correction factor for terrain types
        correctionFactorTerrain.put(Terrain.PLAIN.toString(), Terrain.PLAIN.getValue());
        correctionFactorTerrain.put(Terrain.DOWNS.toString(), Terrain.DOWNS.getValue());
        correctionFactorTerrain.put(Terrain.MOUNTAINOUS.toString(), Terrain.MOUNTAINOUS.getValue());
        correctionFactorTerrain.put(Terrain.CITY.toString(), Terrain.CITY.getValue());
        correctionFactorTerrain.put(Terrain.STOPS.toString(), Terrain.STOPS.getValue());
    }
    
    /**
     * needed for validate
     * @param tazTable
     * @param matricesTable
     * @param modus
     * @param terrain
     * @param indirectWayFactor
     * @return
     */
    public double[][] calcDistance(String tazTable, String matricesTable, String modus, String terrain, boolean indirectWayFactor)
    {
        this.calcData.setTazTable(tazTable);
        this.calcData.setMatricesTable(matricesTable);  
        this.calcData.setTerrain(terrain);
        this.calcData.setIndirectWayFactor(indirectWayFactor);
        
        return calcDistance(modus);
    }
    
    
    public double[][] calcDistance(String modus)
    {
        int countTVZ = -1;
        String query = "select count(taz_id) from "+this.calcData.getTazTable()+";";
        double[][] coord = null;
        double[][] dist = null; 
        
        try
        {
            ResultSet rs = dbCon.executeQuery(query, this);
            if(rs.next())
                countTVZ = (int)rs.getLong(1);
            
            System.out.println("countTVZ = " + countTVZ);         
            
            if(countTVZ <= 0)
                return null;

            coord = new double[countTVZ][2];
            dist = new double[countTVZ][countTVZ];    
            travelTime = new double[countTVZ][countTVZ];
            indirectWayFactor = new double [countTVZ][countTVZ];
            
            query = "select taz_id, taz_bbr_type, st_x(taz_coordinate) as x, st_y(taz_coordinate) as y from "+this.calcData.getTazTable() +" order by taz_id;"; 
            rs = dbCon.executeQuery(query, this);  
            int i = 0;
            while(rs.next())
            {
                coord[i][0] = rs.getDouble(3);
                coord[i][1] = rs.getDouble(4);
                i++;
            } 
            
            rs.close();
                      
        }
        catch(SQLException ex)
        {
            System.err.println(this.getClass().getCanonicalName() 
                     + " executeParameters: SQL-Error during statement: "
                     + query);
            ex.printStackTrace();
        }
         
        if(dist != null && coord != null)
        {           	
            for(int i = 0; i < countTVZ; i++)
            {
                for(int k = 0; k < countTVZ; k++)
                {
                    if(i==k)
                    {
                        dist[i][k] = 0;
                    }
                    else
                    {      
                        dist[i][k] = TPS_Geometrics.getDistance(coord[i][0], coord[i][1], coord[k][0], coord[k][1]);
                        indirectWayFactor[i][k] = getIndirectWayFactor(modus, this.calcData.getTerrain(), dist[i][k]);
                        
                        if(this.calcData.isIndirectWayFactor())
                            dist[i][k] *= indirectWayFactor[i][k];
                    }
                }
            }
        }
        beeLine = dist.clone();
        return dist;  
    }    
   
    /**
     * 
     * @param modus PT, MIV, BIKE, WALK
     * @return 
     */
    public double getIndirectWayFactor(String modus, String terrain, double distance)
    {
        double iwf;
        iwf = factors[0]/Math.log10((factors[1]*Math.pow(distance/1000.0, factors[2]))+factors[3]);
        iwf += factors[4]+this.correctionFactorModus.get(modus) + this.correctionFactorTerrain.get(terrain);
             
        return iwf;
    }
    
    public double[] getFactors()
    {
        return factors;
    }

    public void setFactors(double[] factors)
    {
        this.factors = factors;
    }
    
    public void setFactors(double factor0, double factor1, double factor2, double factor3, double factor4)
    {
        this.factors[0] = factor0;
        this.factors[1] = factor1;
        this.factors[2] = factor2;
        this.factors[3] = factor3;
        this.factors[4] = factor4;
    }
    
    public double[][] getTravelTime(String modus)
    {
    	 
    	double Vm = this.calcData.getTravelTimeParameter()[Modus.valueOf(modus).getValue()].getSpeed()*1000.0/60.0;
    	
         	// Tm = L *Um / Vm + Zm 
         	// tm:  traveltime by modus
         	// L:   bee-line
         	// Um:  indirect way factor by modus
         	// Vm   speed by modus
         	// Zm:  additional travel time by modus
         	
         	for(int i = 0; i< beeLine.length; i++)
         	{
         		for( int k = 0; k < beeLine[0].length; k++)
         		{
         			travelTime[i][k] = beeLine[i][k];
         			travelTime[i][k] /=	Vm;
                 	travelTime[i][k] += this.calcData.getTravelTimeParameter()[Modus.valueOf(modus).getValue()].getTimeDeparture();
                 	travelTime[i][k] *= 60.0;
         		}
         	}
         	                     	
    	
		return travelTime;
	}
}
