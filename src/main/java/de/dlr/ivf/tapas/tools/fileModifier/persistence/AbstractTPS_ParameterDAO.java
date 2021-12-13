/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.fileModifier.persistence;

import de.dlr.ivf.tapas.util.parameters.ParamFlag;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTPS_ParameterDAO implements ITPS_ParameterDAO {
    /**
     * local variable for mapping the parameter key to an enum type
     */
    final Map<String, Enum<?>> mMap;
    /**
     * local variable for the input file
     */
    File mFile;
    /**
     * local variable for a list of parameters to convert
     */
    List<String> mParameters = new ArrayList<>();

    public AbstractTPS_ParameterDAO() {
        Map<Enum<?>, String> map0 = new HashMap<>();

        map0.put(ParamFlag.FLAG_USE_SCHOOLBUS, "main.UseSchoolbus");
        map0.put(ParamFlag.FLAG_USE_BLOCK_LEVEL, "tpsMode.UseBlockLevel");
        map0.put(ParamFlag.FLAG_USE_EXIT_MAUT, "tpsMode.UseExitMaut");
        map0.put(ParamFlag.FLAG_INTRA_INFOS_MATRIX, "tpsMode.IntraTVZMatrix");
        map0.put(ParamFlag.FLAG_USE_DRIVING_LICENCE, "tpsPerson.useDrivingLicence");
        map0.put(ParamFlag.FLAG_USE_FIXED_LOCS_ON_BASE, "tpsScheme.useFixedLocsOnBasis");
        map0.put(ParamFlag.FLAG_REJUVENATE_RETIREE, "tpsPersons.rejuventateRetiree");
        map0.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_SELECTION_PROBS, "tpsSelectSchemes.manipulateSelectionProbs");
        map0.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_BY_WORKINGCHAINS, "tpsPersons.manipulateByWorkingChains");
        map0.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_BY_WORK_AT_HOME, "tpsPersons.manipulateByWorkAtHome");
        map0.put(ParamFlag.FLAG_CHECK_BUDGET_CONSTRAINTS, "tpsPersons.checkBudgetConstraint");
        map0.put(ParamFlag.FLAG_SELECT_LOCATIONS_DIFF_PERSON_GROUP, "tpsSelectLocations.diffPersonGroup");
        map0.put(ParamFlag.FLAG_RUN_SZENARIO, "tpsScheme.RunSzenario");
        map0.put(ParamFlag.FLAG_LOCATION_POCKET_COSTS, "tpsSelectLocations.pocketCosts");
        map0.put(ParamFlag.FLAG_INFLUENCE_RANDOM_NUMBER, "tpsMain.influenceRandomNumber");
        map0.put(ParamString.DB_DBNAME, "main.databaseTapas");
        map0.put(ParamString.PATH_ABS_INPUT, "main.pathDataInput");
        map0.put(ParamString.PATH_ABS_OUTPUT, "main.pathDataOutput");
        map0.put(ParamString.UTILITY_FUNCTION_CLASS, "tpsModeDistribution.useBetaBudgets");
        map0.put(ParamValue.AVERAGE_DISTANCE_PT_STOP, "main.tvzInformation");
        map0.put(ParamValue.VELOCITY_BIKE, "tpsMode.VelocityBike");
        map0.put(ParamValue.VELOCITY_FOOT, "tpsMode.VelocityFoot");
        map0.put(ParamValue.REJUVENATE_BY_NB_YEARS, "tpsPersons.rejuventateBy");
        map0.put(ParamValue.MAX_TRIES_PERSON, "tpsPersons.maxTrials");
        map0.put(ParamValue.MAX_TIME_DIFFERENCE, "tpsPersons.maxTimeDifference");
        map0.put(ParamValue.WEIGHT_WORKING_CHAINS, "tpsPersons.weightWorkingChains");
        map0.put(ParamValue.WEIGHT_WORKING_AT_HOME, "tpsPersons.weightWorkAtHome");
        map0.put(ParamValue.TIME_BUDGET_E, "tpsPersons.timeBudgetE");
        map0.put(ParamValue.TIME_BUDGET_F, "tpsPersons.timeBudgetF");
        map0.put(ParamValue.TIME_BUDGET_WP, "tpsPersons.timeBudgetWP");
        map0.put(ParamValue.FINANCE_BUDGET_E, "tpsPersons.financeBudgetE");
        map0.put(ParamValue.FINANCE_BUDGET_F, "tpsPersons.financeBudgetF");
        map0.put(ParamValue.FINANCE_BUDGET_WP, "tpsPersons.financeBudgetWP");
        map0.put(ParamValue.GAMMA_LOCATION_WEIGHT, "tpsSelectLocations.gammaLocationWeight");
        map0.put(ParamValue.LOC_CHOICE_MOD_CFN4, "tpsSelectLocations.ModCfn4");
        map0.put(ParamValue.TOLL_CAT_1_BASE, "tpsTVZElement.BasisTollCat1");
        map0.put(ParamValue.TOLL_CAT_1, "tpsTVZElement.SzenTollCat1");
        map0.put(ParamValue.TOLL_CAT_2_BASE, "tpsTVZElement.BasisTollCat2");
        map0.put(ParamValue.TOLL_CAT_2, "tpsTVZElement.SzenTollCat2");
        map0.put(ParamValue.TOLL_CAT_3_BASE, "tpsTVZElement.BasisTollCat3");
        map0.put(ParamValue.TOLL_CAT_3, "tpsTVZElement.SzenTollCat3");
        map0.put(ParamValue.PARKING_FEE_CAT_1_BASE, "tpsTVZElement.BasisParkingCat1");
        map0.put(ParamValue.PARKING_FEE_CAT_1, "tpsTVZElement.SzenParkingCat1");
        map0.put(ParamValue.PARKING_FEE_CAT_2_BASE, "tpsTVZElement.BasisParkingCat2");
        map0.put(ParamValue.PARKING_FEE_CAT_2, "tpsTVZElement.SzenParkingCat2");
        map0.put(ParamValue.PARKING_FEE_CAT_3_BASE, "tpsTVZElement.BasisParkingCat3");
        map0.put(ParamValue.PARKING_FEE_CAT_3, "tpsTVZElement.SzenParkingCat3");
        map0.put(ParamValue.MIT_GASOLINE_COST_PER_KM_BASE, "tpsModeMIV.BasisFuelCostPerKilometer");
        map0.put(ParamValue.MIT_FUEL_COST_PER_KM_COMMUTE_BASE, "tpsModeMIV.BasisFuelCostPendlerPerKilometer");
        map0.put(ParamValue.MIT_GASOLINE_COST_PER_KM, "tpsModeMIV.SzenFuelCostPerKilometer");
        map0.put(ParamValue.MIT_FUEL_COST_PER_KM_COMMUTE, "tpsModeMIV.SzenFuelCostPendlerPerKilometer");
        map0.put(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE, "tpsModeMIV.BasisVariableCostPerKilometer");
        map0.put(ParamValue.MIT_VARIABLE_COST_PER_KM, "tpsModeMIV.SzenVariableCostPerKilometer");
        map0.put(ParamValue.PT_COST_PER_KM_BASE, "tpsModeOEV.BasisCostPerKilometer");
        map0.put(ParamValue.PT_COST_PER_KM, "tpsModeOEV.SzenCostPerKilometer");
        map0.put(ParamValue.CAR_SHARING_COST_PER_KM_BASE, "tpsModeTrain.BasisCostPerKilometer");
        map0.put(ParamValue.CAR_SHARING_COST_PER_KM, "tpsModeTrain.SzenCostPerKilometer");
        map0.put(ParamValue.TAXI_COST_PER_KM_BASE, "tpsModeTAXI.BasisCostPerKilometer");
        map0.put(ParamValue.TAXI_COST_PER_KM, "tpsModeTAXI.SzenCostPerKilometer");
        map0.put(ParamValue.DEFAULT_VOT, "tpsVOT.defaultVOT");
        map0.put(ParamValue.RANDOM_SEED_NUMBER, "tpsMain.randomSeed");

        this.mMap = new HashMap<>();
        for (Enum<?> e : map0.keySet()) {
            this.mMap.put(map0.get(e), e);
        }

    }

    public void addAdditionalParameter(String name, String value, String comment) {
        mParameters.add(name);
        mParameters.add(value);
        mParameters.add(comment);
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        this.mFile = file;
    }

    public void readParameter() {
        throw new RuntimeException("not yet implemented");
    }

    public void writeParameter() {
        throw new RuntimeException("not yet implemented");
    }

}
