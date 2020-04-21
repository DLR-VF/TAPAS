package de.dlr.ivf.tapas.person;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to store all PreferenceParameters for the special choices
 * @author hein_mh
 *
 */
public class TPS_PreferenceParameters {
	public enum ShoppingClass{
		/**
		 * I want well defined values for the coding.
		 */
		NUG(0),TEX(1),UEL(2);
		/**
		 * this is an static array of the members of this enum. The values()-function allocates EVERY TIME a new array, which makes your memory explode and slows down the code!
		 * see: https://stackoverflow.com/questions/2446135/is-there-a-performance-hit-when-using-enum-values-vs-string-arrays 
		 */
		public static ShoppingClass[] valueArray = ShoppingClass.values();
		public int code;
		ShoppingClass(int code) {
			this.code=code;
		}
	}

    public enum ShoppingPreferenceSupply{
		/**
		 * I want well defined values for the coding.
		 */
		Oeko(1),Angebot(2),Erlebnis(3),Preis(4),Sonstige(0);
		
		/**
		 * this is an static array of the members of this enum. The values()-function allocates EVERY TIME a new array, which makes your memory explode and slows down the code!
		 * see: https://stackoverflow.com/questions/2446135/is-there-a-performance-hit-when-using-enum-values-vs-string-arrays 
		 */
		public static ShoppingPreferenceSupply[] valueArray = ShoppingPreferenceSupply.values();

		public int code;
		ShoppingPreferenceSupply(int code) {
			this.code=code;
		}

	}

    public enum ShoppingPreferenceAccessibility {
		/**
		 * I want well defined values for the coding.
		 */
		Sonstige(0),Erreichbarkeit(1),Naehe(2),Kopplung(3);
		/**
		 * this is an static array of the members of this enum. The values()-function allocates EVERY TIME a new array, which makes your memory explode and slows down the code!
		 * see: https://stackoverflow.com/questions/2446135/is-there-a-performance-hit-when-using-enum-values-vs-string-arrays 
		 */
		
		public static ShoppingPreferenceAccessibility[] valueArray = ShoppingPreferenceAccessibility.values();
		
		public int code;
		ShoppingPreferenceAccessibility(int code) {
			this.code=code;
		}
	}

    public Map<ShoppingClass,Map<ShoppingPreferenceSupply,ChoiceParams>> paramsSupply = new HashMap<>();
	public Map<ShoppingClass,Map<ShoppingPreferenceAccessibility,ChoiceParams>> paramsAccess = new HashMap<>();

	public class ChoiceParams{
		double female =0;
		double age18 =0;
		double age25 =0;
		double age45 =0;
		double age65 =0;
		double abi =0;
		double retired =0;
		double working =0;
		double numAdults =0;
		double hasChildren =0;
		double hasCars =0;
		double numBikes  =0;
		double income500  =0;
		double income1100  =0;
		double income2000  =0;
		double income3200  =0;
		double income4000  =0;
		double constant =0;
		double sigma =0;
		boolean isUsed=false;
	}
	
	/**
	 * The constructor allocates all maps and fills it with default values.
	 */
	public TPS_PreferenceParameters() {
		// init the preference structures
		for(TPS_PreferenceParameters.ShoppingClass s : TPS_PreferenceParameters.ShoppingClass.valueArray) {
			Map<ShoppingPreferenceSupply,ChoiceParams> tmpPref = new HashMap<>();
			this.paramsSupply.put(s, tmpPref);
			for(TPS_PreferenceParameters.ShoppingPreferenceSupply e : TPS_PreferenceParameters.ShoppingPreferenceSupply.valueArray) {
				ChoiceParams tmpPar = new ChoiceParams();
				tmpPref.put(e, tmpPar);
			}
			Map<ShoppingPreferenceAccessibility,ChoiceParams> tmpPrefAcc = new HashMap<>();
			this.paramsAccess.put(s, tmpPrefAcc);
			for(ShoppingPreferenceAccessibility e : ShoppingPreferenceAccessibility.valueArray) {
				ChoiceParams tmpPar = new ChoiceParams();
				tmpPrefAcc.put(e, tmpPar);
			}
		}		
	}
	
	/**
	 * Method to read all the choice Parameters.
	 * TODO: from DB
	 */
	public void readParams() {
		//supply model
		Map<ShoppingPreferenceSupply,ChoiceParams> shoppingClass;
		ChoiceParams tmpPar;
		//NUG: Nahrung und Genuss
		shoppingClass = this.paramsSupply.get(ShoppingClass.NUG);
		//sonstige
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Sonstige);
		tmpPar.isUsed =true;
		tmpPar.sigma = 1.62;
		//Angebot
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Angebot);
		tmpPar.isUsed =true;
		tmpPar.age45 = -0.205;
		tmpPar.age65 =  0.354;
		tmpPar.constant = 0.167;
		tmpPar.sigma = 0.00676;

		//Erlebnis
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Erlebnis);
		tmpPar.isUsed =true;
		tmpPar.age45 = -0.383;
		tmpPar.age65 = 0.0303;
		tmpPar.abi = 0.34;
		tmpPar.constant = -1.59;
		tmpPar.sigma = -0.00869;

		//Preis
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Preis);
		tmpPar.isUsed =true;
		tmpPar.working = -0.284;
		tmpPar.hasChildren = 0.195;
		tmpPar.income3200 = -0.206;
		tmpPar.income4000 = -0.206;
		tmpPar.constant = 0.643;
		tmpPar.sigma = -0.000519;
		//Oeko
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Oeko);
		tmpPar.isUsed =true;
		tmpPar.female = 0.723;
		tmpPar.age25 = 0.606;
		tmpPar.age45 = 0.168;
		tmpPar.age65 = 0.168;
		tmpPar.abi = 0.884;
		tmpPar.hasChildren = 0.372;
		tmpPar.hasCars = -0.338;
		tmpPar.constant = -3.11;
		tmpPar.sigma = 0.000695;
		
		//TEX: Textil
		shoppingClass = this.paramsSupply.get(ShoppingClass.TEX);
				
		//sonstige
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Sonstige);
		tmpPar.isUsed =true;
		tmpPar.constant = 0;
		tmpPar.sigma = 2.83;

		//Angebot
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Angebot);
		tmpPar.isUsed =true;
		tmpPar.age25 = -0.549;
		tmpPar.age45 = -0.608;
		tmpPar.age65 = -0.00103;
		tmpPar.constant = 2.74;
		tmpPar.sigma = -0.0974;

		//Erlebnis
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Erlebnis);
		tmpPar.isUsed =true;
		tmpPar.female = 0.524;
		tmpPar.age25 = -0.801;
		tmpPar.age45 = -1.18;
		tmpPar.age65 = -0.919;
		tmpPar.numAdults = -0.173;
		tmpPar.hasChildren = -0.377;
		tmpPar.income500 = 0.934;
		tmpPar.income1100 = 0.934;
		tmpPar.income2000 = 1.19;
		tmpPar.income3200 = 0.889;
		tmpPar.income4000 = 0.889;
		tmpPar.constant = 1.04;
		tmpPar.sigma = -0.73;

		//Preis
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Preis);
		tmpPar.isUsed =true;
		tmpPar.working = -0.317;
		tmpPar.income1100 = -0.247;
		tmpPar.income2000 = -0.506;
		tmpPar.income3200 = -0.698;
		tmpPar.income4000 = -0.698;
		tmpPar.constant = 2.41;
		tmpPar.sigma = 0.204;
		
		//UEL: Unterhaltungselektronik
		shoppingClass = this.paramsSupply.get(ShoppingClass.UEL);		
		
		//sonstige
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Sonstige);
		tmpPar.isUsed =true;
		tmpPar.constant = 0;
		tmpPar.sigma = -2.96;

		//Angebot
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Angebot);
		tmpPar.isUsed =true;
		tmpPar.age65 = -0.381;
		tmpPar.abi = -0.664;
		tmpPar.working = -0.791;
		tmpPar.constant = 2.65;
		tmpPar.sigma = -0.025;

		//Erlebnis
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Erlebnis);
		tmpPar.isUsed =true;
		tmpPar.abi = -0.763; 
		tmpPar.working = -0.938;
		tmpPar.hasCars = -0.472;
		tmpPar.constant = 0.284;
		tmpPar.sigma = -0.0128;

		//Preis
		tmpPar = shoppingClass.get(ShoppingPreferenceSupply.Preis);
		tmpPar.isUsed =true;
		tmpPar.female = -0.207;
		tmpPar.age25 = 0.432;
		tmpPar.age45 = 0.386;
		tmpPar.age65 = 0.386;
		tmpPar.abi = -0.607;
		tmpPar.working = -0.911;
		tmpPar.numBikes = 0.117;
		tmpPar.income2000 = 0.27;		//was income 2000 and more
		tmpPar.income3200 = 0.27;
		tmpPar.income4000 = 0.27;
		tmpPar.constant = 1.73;
		tmpPar.sigma = 0.00462;		

		//accessibility model
		Map<ShoppingPreferenceAccessibility,ChoiceParams> shoppingClassAccess;

		//NUG: Nahrung und Genuss
		shoppingClassAccess = this.paramsAccess.get(ShoppingClass.NUG);
		//sonstige
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Sonstige);
		tmpPar.isUsed =true;
		tmpPar.sigma = 1.86;

		//Erreichbarkeit
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Erreichbarkeit);
		tmpPar.isUsed =true;
		tmpPar.female = -0.417;
		tmpPar.hasChildren = 0.311;
		tmpPar.hasCars = 1.75;
		tmpPar.constant = -0.178;
		tmpPar.sigma = 1.13;
		
		//Naehe
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Naehe);
		tmpPar.isUsed =true;
		tmpPar.female =-0.337;
		tmpPar.age45 =-0.785;
		tmpPar.age65 =-1.04;
		tmpPar.constant =2.66;
		tmpPar.sigma =0.4;

		//Kopplung
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Kopplung);
		tmpPar.isUsed =true;
		tmpPar.age25 =-0.731;
		tmpPar.age45 =-1.39;
		tmpPar.age65 =-1.55;
		tmpPar.working =1.07;
		tmpPar.numAdults =-0.357;
		tmpPar.hasCars =0.572;
		tmpPar.constant =0.842;
		tmpPar.sigma =0.88;
		
		//TEX: Textil
		shoppingClassAccess = this.paramsAccess.get(ShoppingClass.TEX);
		//sonstige
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Sonstige);
		tmpPar.isUsed =true;
		tmpPar.sigma = 3.02;

		//Erreichbarkeit
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Erreichbarkeit);
		tmpPar.isUsed =true;
		tmpPar.female =-0.478;
		tmpPar.age45 =1.27;
		tmpPar.age65 =1.27;
		tmpPar.hasChildren =0.927;
		tmpPar.hasCars =2.08;
		tmpPar.constant =-2.25;
		tmpPar.sigma =2.37;

		//Naehe
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Naehe);
		tmpPar.isUsed =true;
		tmpPar.age25 =1.18;
		tmpPar.age45 =1.09;
		tmpPar.age65 =0.607;
		tmpPar.hasChildren =0.576;
		tmpPar.constant =-2.35;
		tmpPar.sigma =0.99;
		
		//Kopplung
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Kopplung);
		tmpPar.isUsed =true;
		tmpPar.age65 =-2.41;
		tmpPar.abi =0.461;
		tmpPar.working =1.32;
		tmpPar.constant =-2.38;
		tmpPar.sigma =1.63;		

		//UEL: Unterhaltungselektronik
	
		shoppingClassAccess = this.paramsAccess.get(ShoppingClass.UEL);
		//sonstige
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Sonstige);
		tmpPar.isUsed =true;
		tmpPar.sigma = 3.07;

		//Erreichbarkeit
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Erreichbarkeit);
		tmpPar.isUsed =true;
		tmpPar.retired =0.584;
		tmpPar.hasCars =2.03;
		tmpPar.income1100  =1.14;
		tmpPar.income2000  =1.14;
		tmpPar.constant =-1.1;
		tmpPar.sigma =1.4;

		//Naehe
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Naehe);
		tmpPar.isUsed =true;
		tmpPar.hasCars =1.14;
		tmpPar.income1100  =1.2;
		tmpPar.income2000  =1.2;
		tmpPar.constant =-0.6;
		tmpPar.sigma =0.0286;
		
		//Kopplung
		tmpPar = shoppingClassAccess.get(ShoppingPreferenceAccessibility.Kopplung);
		tmpPar.isUsed =true;
		tmpPar.age25 =-0.759;
		tmpPar.age45 =-0.965;
		tmpPar.age65 =-0.924;
		tmpPar.working =0.648;
		tmpPar.hasCars =1.01;
		tmpPar.income1100  =1.1;
		tmpPar.income2000  =1.1;
		tmpPar.constant =-1.22;
		tmpPar.sigma =-1.08;	
	}
}
