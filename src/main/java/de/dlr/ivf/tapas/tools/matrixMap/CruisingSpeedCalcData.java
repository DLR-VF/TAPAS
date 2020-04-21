package de.dlr.ivf.tapas.tools.matrixMap;

public class CruisingSpeedCalcData
{
    private String terrain = null;
    private boolean top3=true;
    private String tazTable = null;
    private String matricesTable = null;
    private boolean indirectWayFactor = true;
    private boolean zValues = false;

    private boolean[] calculation = null;
	private TravelTimeParameter[] travelTimeParameter = null;
    
    public CruisingSpeedCalcData()
    {
        // 0=WALK, 1=BIKE. 2=PT, 3=MIV
        setTravelTimeParameter(new TravelTimeParameter[4]);
        for(int i = 0; i < 4; i++)
            travelTimeParameter[i] = new TravelTimeParameter();
      
        setCalculation(new boolean[8]);
        for(int i = 0; i < 8; i++)
        	calculation[i] = false;
    }

    public String getTerrain()
    {
        return terrain;
    }

    public void setTerrain(String terrain)
    {
        this.terrain = terrain;
    }

    public boolean isTop3()
    {
        return top3;
    }

    public void setTop3(boolean top3)
    {
        this.top3 = top3;
    }
  
    public String getMatricesTable()
    {
        return matricesTable;
    }

    public void setMatricesTable(String matricesTable)
    {
        this.matricesTable = matricesTable;
    }

    public boolean isIndirectWayFactor()
    {
        return indirectWayFactor;
    }

    public void setIndirectWayFactor(boolean indirectWayFactor)
    {
        this.indirectWayFactor = indirectWayFactor;
    }

    public String getTazTable()
    {
        return tazTable;
    }

    public void setTazTable(String tazTable)
    {
        this.tazTable = tazTable;
    }

    public boolean iszValues()
    {
        return zValues;
    }

    public void setzValues(boolean zValues)
    {
        this.zValues = zValues;
    }
   
    
    
    public boolean[] getCalculation()
    {
		return calculation;
	}

	public void setCalculation(boolean[] calculation)
	{
		this.calculation = calculation;
	}

	public TravelTimeParameter[] getTravelTimeParameter()
    {
        return travelTimeParameter;
    }

    public void setTravelTimeParameter(TravelTimeParameter[] travelTimeParameter)
    {
        this.travelTimeParameter = travelTimeParameter;
    }
   
	class TravelTimeParameter
    {
        private double speed;
        private int timeApproach;
        private int timeDeparture;
        
        public double getSpeed()
        {
            return speed;
        }
        public void setSpeed(double speed)
        {
            this.speed = speed;
        }
        public int getTimeApproach()
        {
            return timeApproach;
        }
        public void setTimeApproach(int timeApproach)
        {
            this.timeApproach = timeApproach;
        }
        public int getTimeDeparture()
        {
            return timeDeparture;
        }
        public void setTimeDeparture(int timeDeparture)
        {
            this.timeDeparture = timeDeparture;
        }
    }
}
