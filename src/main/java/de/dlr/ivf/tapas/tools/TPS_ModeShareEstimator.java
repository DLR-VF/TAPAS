package de.dlr.ivf.tapas.tools;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TPS_ModeShareEstimator extends ApplicationFrame {	
	
	String defaultPersons="3450000";
	String defaultTrips = "3.0";
	double refCarLength=8.2;

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3502972524853251146L;
	public static GridBagConstraints newGridBag(int gridX, int padX, int gridY, int padY){
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = gridX;
		gridBagConstraints.gridy = gridY;
		gridBagConstraints.ipadx = padX;
		gridBagConstraints.ipady = padY;
		return gridBagConstraints;
	}
	
	enum ValueType{TIME,COST,LENGTH}

	class SliderListener implements ChangeListener {
		String label;
		ValueType type;
		JLabel jLab;
		public SliderListener(String label, ValueType type, JLabel jLab){
			this.label=label;
			this.type=type;
			this.jLab=jLab;
		}
	    public void stateChanged(ChangeEvent e) {
	        JSlider source = (JSlider)e.getSource();
            double val = source.getValue();
            String text ="";
            switch(this.type){
            case TIME:
            	val*=60; //minute to seconds
            	deltaTime.put(label, val);
            	text= String.format("%+5d min", source.getValue());
            	break;
            case COST:
            	val*=0.01; //cent to euro
            	deltaCost.put(label, val);
            	text= String.format("%3d ct", source.getValue());
            	break;
            case LENGTH:
            	val*=0.001; //m to km
            	averageLength.put(label, val);
            	text= String.format("%5d m", source.getValue());
            	break;
            }

            jLab.setText(text);
            updateValue();
	    }
	}
	
	 /**
	 * A custom renderer that returns a different color for each item in a single series.
	 */
	class CustomRenderer extends BarRenderer {

	    /**
		 * 
		 */
		private static final long serialVersionUID = -3727600228974279452L;

		/**
	     * Returns the paint for an item.  Overrides the default behaviour inherited from
	     * AbstractSeriesRenderer.
	     *
	     * @param row  the series.
	     * @param column  the category.
	     *
	     * @return The item color.
	     */
	    public Paint getItemPaint(final int row, final int column) {
	        return colors[column % colors.length];
	    }
	}
	
	public void updateValue(){
		//refCost enthält die Referenzkosten des Basisfalls
		//deltaTime enthält den aktuellen "Slider Wert" für Zeiten
		//deltaCost enthält den aktuellen "Slider Wert" für Kosten
		//timeFactors enthält den aktuellen Faktoren für Zeiten
		//costFactors enthält den aktuellen Faktoren für Kosten
		double[] newShare = new double[datasetReference.getItemCount()];
		double sum =0;
		double timeD;
		double costD;
		double factT;
		double factC;
		//calc the new shares
		for(int i= 0; i<newShare.length; ++i){
			String key  =  (String)datasetReference.getKey(i);
			timeD = deltaTime.get(key);
			factT = timeFactors.get(key);
			factC = costFactors.get(key);
			if(key.equalsIgnoreCase("Car")){
				costD = deltaCost.get(key)*averageLength.get(key)-refCost.get(key)*refCarLength;
			}
			else
				costD = deltaCost.get(key)-refCost.get(key);

			newShare[i] = (double)datasetReference.getValue(i)*
							Math.exp(
								timeD*factT+
								+costD*factC);
			//sum up the norming factor
			sum += newShare[i];
		}
		double tmp;
		//set the new values
		for(int i= 0; i<newShare.length; ++i){
			String key  =  (String)datasetReference.getKey(i);
			tmp = 100*newShare[i]/sum;
			datasetEstimation.setValue(key, tmp);
			int persons = Integer.parseInt(this.noPersons.getText());
			double trips = Double.parseDouble(this.noTrips.getText());
			tmp = newShare[i]*averageLength.get(key)*persons*trips/(sum*1000000);
			trafficVolume.setValue(tmp, "km",key);
		}		
	}
	
		public TPS_ModeShareEstimator(String title) {
		super(title);
	      setContentPane(createDemoPanel( ));
	      this.setSize(1500,700);
		
		}
		//private TPS_BasicConnectionClass connector = new TPS_BasicConnectionClass();
  
		DefaultPieDataset datasetReference = new DefaultPieDataset( );
		DefaultPieDataset datasetEstimation = new DefaultPieDataset( );
		DefaultCategoryDataset  trafficVolume = new DefaultCategoryDataset( );
		JFreeChart chartReference =null;
		JFreeChart chartEstimation =null;
		JFreeChart chartVolume = null;
		Map<String,Double> refCost = new HashMap<>( );
		Map<String,Double> deltaTime = new HashMap<>( );
		Map<String,Double> deltaCost = new HashMap<>( );		
		Map<String,Double> costFactors = new HashMap<>( );
		Map<String,Double> timeFactors = new HashMap<>( );
		Map<String,Double> averageLength = new HashMap<>( );
		Map<String,Integer> defaultTime = new HashMap<>( );
		Map<String,Integer> defaultCost = new HashMap<>( );
		Map<String,Integer> defaultLength = new HashMap<>( );
		
		JSlider walkTime =null;
		JLabel  walkTimeLabel =new JLabel();
		JSlider bikeTime =null;
		JLabel  bikeTimeLabel =new JLabel();
		JSlider ptTime =null;
		JLabel  ptTimeLabel =new JLabel();
		JSlider carTime =null;
		JLabel  carTimeLabel =new JLabel();
		JSlider ptCost =null;
		JLabel  ptCostLabel =new JLabel();
		JSlider carCost =null;
		JLabel  carCostLabel =new JLabel();
		JSlider walkLength =null;
		JLabel  walkLengthLabel =new JLabel();
		JSlider bikeLength =null;
		JLabel  bikeLengthLabel =new JLabel();
		JSlider ptLength =null;
		JLabel  ptLengthLabel =new JLabel();
		JSlider carLength =null;
		JLabel  carLengthLabel =new JLabel();
		JTextField  noPersons =  new JTextField(defaultPersons);
		JTextField  noTrips =  new JTextField(defaultTrips);
		JButton resetButton =  null;

		   //set some colors
        Color[] colors = {	new Color(62,130,196,255), //walk
        					new Color(13,76,137,255), //bike
        					new Color(61,141,7,255), //pt
        					new Color(255,0,0,255) //car
        					}; 

		
		private void createDataset( ) 
	   {
			
			datasetReference.setValue( "Walk" , Double.valueOf( 28.1 ) );  
			datasetReference.setValue( "Bike" , Double.valueOf( 12.5 ) );   
			datasetReference.setValue( "PT" , Double.valueOf( 27.9 ) );    
			datasetReference.setValue( "Car" , Double.valueOf( 31.5 ) );  
			refCost.put("Walk", 0.0);
			refCost.put("Bike", 0.0);
			refCost.put("PT", 2.1);
			refCost.put("Car", 0.10);
			deltaTime.put("Walk", 0.0);
			deltaTime.put("Bike", 0.0);
			deltaTime.put("PT", 0.0);
			deltaTime.put("Car", 0.0);
			deltaCost.put("Walk", 0.0);
			deltaCost.put("Bike", 0.0);
			deltaCost.put("PT", 2.1);
			deltaCost.put("Car", 0.10);
			timeFactors.put("Walk", -.00025054);
			timeFactors.put("Bike", -.0012931);
			timeFactors.put("PT", -.0002906);
			timeFactors.put("Car", -.0008644);
			costFactors.put("Walk", 0.0);
			costFactors.put("Bike", 0.0);
			costFactors.put("PT", -.7701009);
			costFactors.put("Car", -0.5653694);
			
			averageLength.put("Walk", 1.0);
			averageLength.put("Bike", 3.5);
			averageLength.put("PT", 10.4);
			averageLength.put("Car", 8.2);
			
			defaultTime.put("Walk", 0);
			defaultTime.put("Bike", 0);
			defaultTime.put("PT", 0);
			defaultTime.put("Car", 0);
			
			defaultCost.put("Walk", 0);
			defaultCost.put("Bike", 0);
			defaultCost.put("PT", 210);
			defaultCost.put("Car", 10);
			
			defaultLength.put("Walk", 1000);
			defaultLength.put("Bike", 3500);
			defaultLength.put("PT", 10400);
			defaultLength.put("Car", 8200);
			
			
			updateValue();			
			
	   }
	   private void createChart( )
	   {
		   chartReference = ChartFactory.createPieChart(      
	         "Reference",  // chart title 
	         datasetReference,        // data    
	         false,           // include legend   
	         true, 
	         false);
		   chartEstimation = ChartFactory.createPieChart(      
			         "Estimation",  // chart title 
			         datasetEstimation,        // data    
			         false,           // include legend   
			         true, 
			         false);
		   chartVolume = ChartFactory.createStackedBarChart(
		            "Traffic volume",  // chart title
		            "Mode",                  // domain axis label
		            "Volume (million km)",                     // range axis label
		            trafficVolume,                     // data
		            PlotOrientation.VERTICAL,    // the plot orientation
		            false,                        // legend
		            true,                        // tooltips
		            false                        // urls
		        );

	        PieRenderer renderer = new PieRenderer(colors); 
	        renderer.setColor(((PiePlot) chartReference.getPlot()), datasetReference);
	        PieRenderer renderer2 = new PieRenderer(colors); 
	        renderer2.setColor(((PiePlot) chartEstimation.getPlot()), datasetEstimation);
	        chartReference.getPlot().setBackgroundPaint(Color.WHITE);
	        chartReference.setBackgroundPaint(new Color(0,0,0,0));
	        chartEstimation.getPlot().setBackgroundPaint(Color.WHITE);
	        chartEstimation.setBackgroundPaint(new Color(0,0,0,0));
	        chartVolume.getPlot().setBackgroundPaint(Color.WHITE);
	        chartVolume.setBackgroundPaint(new Color(0,0,0,0));
	        
	        BarRenderer  renderer3 = new CustomRenderer ();
	        renderer3.setBarPainter(new StandardBarPainter());
	        renderer3.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator(
	                "Mode {1}, Volume: {2} million km", NumberFormat.getInstance()));	        	        
	        ((CategoryPlot) chartVolume.getPlot()).setRenderer(renderer3);
	        NumberAxis rangeAxis = (NumberAxis) renderer3.getPlot().getRangeAxis();
	        rangeAxis.setAutoRange(false);
	        rangeAxis.setRange(0, 40.0);

	        
	        //turn on values
		   ((PiePlot) chartReference.getPlot()).setLabelGenerator(new
				    StandardPieSectionLabelGenerator("{0}: {2}"));
		   ((PiePlot) chartEstimation.getPlot()).setLabelGenerator(new
				    StandardPieSectionLabelGenerator("{0}: {2}"));
	        renderer3.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
	        renderer3.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.TOP_CENTER));
	        renderer3.setDefaultItemLabelsVisible(true);

	        //shadows off
		   ((PiePlot) chartReference.getPlot()).setShadowPaint(null);
		   ((PiePlot) chartEstimation.getPlot()).setShadowPaint(null);
	        renderer3.setShadowVisible(false);

		   //now we a ready to rock! perhaps this can be exchaged to the standard initializer during start up
	   }
	   
	   public void setDefaultSliderValues(){
		   walkTime.setValue(defaultTime.get("Walk"));
		   bikeTime.setValue(defaultTime.get("Bike"));
		   ptTime.setValue(defaultTime.get("PT"));
		   carTime.setValue(defaultTime.get("Car"));
		   ptCost.setValue(defaultCost.get("PT"));
		   carCost.setValue(defaultCost.get("Car"));
		   walkLength.setValue(defaultLength.get("Walk"));
		   bikeLength.setValue(defaultLength.get("Bike"));
		   ptLength.setValue(defaultLength.get("PT"));
		   carLength.setValue(defaultLength.get("Car"));
		   noTrips.setText(defaultTrips);
		   noPersons.setText(defaultPersons);
		   this.updateValue();
	   }
	   
	   public JPanel createDemoPanel( )
	   {
		   this.createDataset();
		   this.createChart();
		   
		   JPanel	mainPanel = new JPanel();
		   mainPanel.setLayout(new GridBagLayout());
		   JPanel   chartPanel = new JPanel();
		   chartPanel.setLayout(new GridBagLayout());
		   chartPanel.setSize( 560 , 367 );

		   JPanel	modifierPanel = new JPanel();
		   modifierPanel.setLayout(new GridBagLayout());
		   modifierPanel.setSize( 560 , 200 );
		   
		   JPanel  valuePanel = new JPanel();
		   valuePanel.setLayout(new GridBagLayout());
		   
		   ChartPanel ref = new ChartPanel(this.chartReference);		   
		   ref.setPreferredSize(new Dimension(460,367));
		   chartPanel.add(ref , newGridBag(0,0,0,0));
		   ChartPanel est = new ChartPanel(this.chartEstimation);
		   est.setPreferredSize(new Dimension(460,367));
		   chartPanel.add(est, newGridBag(1,0,0,0));
		   ChartPanel vol = new ChartPanel(this.chartVolume);
		   vol.setPreferredSize(new Dimension(460,367));
		   chartPanel.add(vol, newGridBag(2,0,0,0));
		   
		   //create labels and sliders

		   JLabel walk = new JLabel("Walk");
		   walkTime = new JSlider(JSlider.HORIZONTAL,-10,10,0);
		   walkTime.setMajorTickSpacing(5);
		   walkTime.setMinorTickSpacing(1);
		   walkTime.createStandardLabels(5);
		   walkTime.setPaintTicks(true);
		   walkTime.setPaintLabels(true);
		   walkTime.setSize(new Dimension(150,20));
		   walkTime.addChangeListener(new SliderListener("Walk",ValueType.TIME,walkTimeLabel));
		   walkLength = new JSlider(JSlider.HORIZONTAL,0,15000,1000);
		   walkLength.setMajorTickSpacing(5000);
		   walkLength.setMinorTickSpacing(500);
		   walkLength.createStandardLabels(1000);
		   walkLength.setPaintTicks(true);
		   walkLength.setPaintLabels(true);
		   walkLength.setSize(new Dimension(150,20));
		   walkLength.addChangeListener(new SliderListener("Walk",ValueType.LENGTH,walkLengthLabel));

		   JLabel bike = new JLabel("Bike");
		   bikeTime = new JSlider(JSlider.HORIZONTAL,-10,10,0);
		   bikeTime.setMajorTickSpacing(5);
		   bikeTime.setMinorTickSpacing(1);
		   bikeTime.createStandardLabels(5);
		   bikeTime.setPaintTicks(true);
		   bikeTime.setPaintLabels(true);
		   bikeTime.setSize(new Dimension(150,20));
		   bikeTime.addChangeListener(new SliderListener("Bike",ValueType.TIME,bikeTimeLabel));
		   bikeLength = new JSlider(JSlider.HORIZONTAL,0,15000,3500);
		   bikeLength.setMajorTickSpacing(5000);
		   bikeLength.setMinorTickSpacing(500);
		   bikeLength.createStandardLabels(1000);
		   bikeLength.setPaintTicks(true);
		   bikeLength.setPaintLabels(true);
		   bikeLength.setSize(new Dimension(150,20));
		   bikeLength.addChangeListener(new SliderListener("Bike",ValueType.LENGTH,bikeLengthLabel));

		   JLabel pt = new JLabel("Public transport");
		   ptTime = new JSlider(JSlider.HORIZONTAL,-10,10,0);
		   ptTime.setMajorTickSpacing(5);
		   ptTime.setMinorTickSpacing(1);
		   ptTime.createStandardLabels(5);
		   ptTime.setPaintTicks(true);
		   ptTime.setPaintLabels(true);
		   ptTime.setSize(new Dimension(150,20));
		   ptTime.addChangeListener(new SliderListener("PT",ValueType.TIME,ptTimeLabel));
		   ptCost = new JSlider(JSlider.HORIZONTAL,100,420,210);
		   ptCost.setMajorTickSpacing(50);
		   ptCost.setMinorTickSpacing(5);
		   ptCost.createStandardLabels(10);
		   ptCost.setPaintTicks(true);
		   ptCost.setPaintLabels(true);
		   ptCost.setSize(new Dimension(150,20));
		   ptCost.addChangeListener(new SliderListener("PT",ValueType.COST,ptCostLabel));
		   ptLength = new JSlider(JSlider.HORIZONTAL,0,15000,10400);
		   ptLength.setMajorTickSpacing(5000);
		   ptLength.setMinorTickSpacing(500);
		   ptLength.createStandardLabels(1000);
		   ptLength.setPaintTicks(true);
		   ptLength.setPaintLabels(true);
		   ptLength.setSize(new Dimension(150,20));
		   ptLength.addChangeListener(new SliderListener("PT",ValueType.LENGTH,ptLengthLabel));

		   JLabel car = new JLabel("Car");
		   carTime = new JSlider(JSlider.HORIZONTAL,-10,10,0);
		   carTime.setMajorTickSpacing(5);
		   carTime.setMinorTickSpacing(1);
		   carTime.createStandardLabels(5);
		   carTime.setPaintTicks(true);
		   carTime.setPaintLabels(true);
		   carTime.setSize(new Dimension(150,20));
		   carTime.addChangeListener(new SliderListener("Car",ValueType.TIME,carTimeLabel));
		   carCost = new JSlider(JSlider.HORIZONTAL,5,30,10);
		   carCost.setMajorTickSpacing(5);
		   carCost.setMinorTickSpacing(1);
		   carCost.createStandardLabels(5);
		   carCost.setPaintTicks(true);
		   carCost.setPaintLabels(true);
		   carCost.setSize(new Dimension(150,20));
		   carCost.addChangeListener(new SliderListener("Car",ValueType.COST,carCostLabel));
		   carLength = new JSlider(JSlider.HORIZONTAL,0,15000,8200);
		   carLength.setMajorTickSpacing(5000);
		   carLength.setMinorTickSpacing(500);
		   carLength.createStandardLabels(1000);
		   carLength.setPaintTicks(true);
		   carLength.setPaintLabels(true);
		   carLength.setSize(new Dimension(150,20));
		   carLength.addChangeListener(new SliderListener("Car",ValueType.LENGTH,carLengthLabel));
		   
			walkTimeLabel.setText("   +0 min");
			walkTimeLabel.setMinimumSize(new Dimension(100,20));
			bikeTimeLabel.setText("   +0 min");
			bikeTimeLabel.setMinimumSize(new Dimension(100,20));
			ptTimeLabel.setText("   +0 min");
			ptTimeLabel.setMinimumSize(new Dimension(100,20));
			carTimeLabel.setText("   +0 min");
			carTimeLabel.setMinimumSize(new Dimension(100,20));
			ptCostLabel.setText("210 ct");
			ptCostLabel.setMinimumSize(new Dimension(100,20));
			carCostLabel.setText(" 10 ct");
			carCostLabel.setMinimumSize(new Dimension(100,20));
			walkLengthLabel.setText(" 1000 m");
			bikeLengthLabel.setText(" 3500 m");
			ptLengthLabel.setText("10400 m");
			carLengthLabel.setText(" 8200 m");
			walkLengthLabel.setMinimumSize(new Dimension(100,20));
			bikeLengthLabel.setMinimumSize(new Dimension(100,20));
			ptLengthLabel.setMinimumSize(new Dimension(100,20));
			carLengthLabel.setMinimumSize(new Dimension(100,20));
			
			setDefaultSliderValues();
			//some labels
			JLabel mode =new JLabel("Mode");
			JLabel time =new JLabel("Time in minutes");
			JLabel timeVal =new JLabel("Time");
			timeVal.setMinimumSize(new Dimension(100,20));
			JLabel cost =new JLabel("Cost in cents");			
			JLabel costVal =new JLabel("Cost");
			costVal.setMinimumSize(new Dimension(100,20));
			JLabel length =new JLabel("Average length in meter");			
			JLabel lengthVal =new JLabel("Length");
			costVal.setMinimumSize(new Dimension(100,20));
			JLabel  noPersonsLabel =new JLabel("Number of persons:");
			JLabel  noTripslabel =new JLabel("Average no. of trips");
		
		   //put them together
		   int y=0;
		   int x=0;
		   final int timeSliderSpacing = 150;
		   final int timeValSpacing = 20;
		   final int costSliderSpacing = 250;
		   final int costValSpacing = 0;
		   modifierPanel.add(mode,  newGridBag(x++,0,y,0));
		   modifierPanel.add(time,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(timeVal,  newGridBag(x++,timeValSpacing,y,0));
		   modifierPanel.add(cost,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(costVal,  newGridBag(x++,costValSpacing,y,0));
		   modifierPanel.add(length,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(lengthVal,  newGridBag(x++,costValSpacing,y,0));
		   y++; x=0;
		   modifierPanel.add(walk,  newGridBag(x++,0,y,0));
		   modifierPanel.add(walkTime,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(walkTimeLabel,  newGridBag(x++,timeValSpacing,y,0));
		   modifierPanel.add(walkLength,  newGridBag(2+x++,timeSliderSpacing,y,0));
		   modifierPanel.add(walkLengthLabel,  newGridBag(2+x++,timeValSpacing,y,0));
		   y++; x=0;
		   modifierPanel.add(bike,  newGridBag(x++,0,y,0));
		   modifierPanel.add(bikeTime,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(bikeTimeLabel,  newGridBag(x++,timeValSpacing,y,0));
		   modifierPanel.add(bikeLength,  newGridBag(2+x++,timeSliderSpacing,y,0));
		   modifierPanel.add(bikeLengthLabel,  newGridBag(2+x++,timeValSpacing,y,0));
		   y++; x=0;
		   modifierPanel.add(pt,  newGridBag(x++,0,y,0));
		   modifierPanel.add(ptTime,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(ptTimeLabel,  newGridBag(x++,timeValSpacing,y,0));
		   modifierPanel.add(ptCost,  newGridBag(x++,costSliderSpacing,y,20));
		   modifierPanel.add(ptCostLabel,  newGridBag(x++,costValSpacing,y,0));
		   modifierPanel.add(ptLength,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(ptLengthLabel,  newGridBag(x++,timeValSpacing,y,0));
		   y++; x=0;
		   modifierPanel.add(car,  newGridBag(x++,0,y,0));
		   modifierPanel.add(carTime,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(carTimeLabel,  newGridBag(x++,timeValSpacing,y,0));
		   modifierPanel.add(carCost,  newGridBag(x++,costSliderSpacing,y,0));
		   modifierPanel.add(carCostLabel,  newGridBag(x++,costValSpacing,y,0));
		   modifierPanel.add(carLength,  newGridBag(x++,timeSliderSpacing,y,0));
		   modifierPanel.add(carLengthLabel,  newGridBag(x++,timeValSpacing,y,0));

		   
		   resetButton = new JButton("Reset values");
		   resetButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					setDefaultSliderValues();
				}
			});
		   
		   noPersons.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					updateValue();
				}
			});
		   noPersons.addKeyListener(new KeyAdapter() {
               public void keyTyped(KeyEvent e) {
                   char vChar = e.getKeyChar();
                   if (!(Character.isDigit(vChar)
                           || (vChar == KeyEvent.VK_BACK_SPACE)
                           || (vChar == KeyEvent.VK_DELETE))) {
                       e.consume();
                   }
               }
           }); //allow only numerics
		   
		   noTrips.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					updateValue();
				}
			});
		   noTrips.addKeyListener(new KeyAdapter() {
               public void keyTyped(KeyEvent e) {
                   char vChar = e.getKeyChar();
                   if (!(Character.isDigit(vChar)
                           || (vChar == KeyEvent.VK_BACK_SPACE)
                           || (vChar == KeyEvent.VK_DELETE)
                           || (vChar == KeyEvent.VK_PERIOD))) {
                       e.consume();
                   }
               }
           }); //allow only numerics and decimal
		   
		   x=0;
		   valuePanel.add(noPersonsLabel,  newGridBag(x++,0,0,0) );
		   valuePanel.add(Box.createHorizontalStrut( 20 ),  newGridBag(x++,0,0,0) );
		   valuePanel.add(this.noPersons,  newGridBag(x++,70,0,0) );
		   valuePanel.add(Box.createHorizontalStrut( 20 ),  newGridBag(x++,0,0,0) );
		   valuePanel.add(noTripslabel,  newGridBag(x++,0,0,0) );
		   valuePanel.add(Box.createHorizontalStrut( 20 ),  newGridBag(x++,0,0,0) );
		   valuePanel.add(this.noTrips,  newGridBag(x++,20,0,0) );
		   valuePanel.add(Box.createHorizontalStrut( 40 ),  newGridBag(x++,0,0,0) );
		   valuePanel.add(resetButton,  newGridBag(x++,0,0,0) );
		   
		   mainPanel.add(chartPanel,  newGridBag(0,0,0,0));
		   mainPanel.add(modifierPanel, newGridBag(0,0,1,0));
		   mainPanel.add(valuePanel, newGridBag(0,0,3,0));
		   //mainPanel.setSize(mainPanel.getPreferredSize());
		   
	      return mainPanel; 
	   }
	   
	   /* 
	     * A simple renderer for setting custom colors 
	     * for a pie chart. 
	     */ 
	    
	    public static class PieRenderer 
	    { 
	        private Color[] color; 
	        
	        public PieRenderer(Color[] color) 
	        { 
	            this.color = color; 
	        }        
	        
	        @SuppressWarnings("rawtypes")
			public void setColor(PiePlot plot, DefaultPieDataset dataset) 
	        { 
	            @SuppressWarnings("unchecked")
				List <Comparable> keys = dataset.getKeys(); 
	            int aInt; 
	            
	            for (int i = 0; i < keys.size(); i++) 
	            { 
	                aInt = i % this.color.length; 
	                plot.setSectionPaint(keys.get(i), this.color[aInt]); 
	            } 
	        } 
	    } 
	   
	   public static void main( String[ ] args )
	   {
		   TPS_ModeShareEstimator demo = new TPS_ModeShareEstimator( "Mode Shares 2008" );  
	      UIUtils.centerFrameOnScreen( demo );
	      demo.setVisible( true ); 
	   }
}
