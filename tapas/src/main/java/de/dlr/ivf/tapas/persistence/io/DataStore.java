package de.dlr.ivf.tapas.persistence.io;

import de.dlr.ivf.tapas.dto.UtilityFunctionDto;
import de.dlr.ivf.tapas.legacy.TPS_ModeSet;
import de.dlr.ivf.tapas.mode.Modes;
import de.dlr.ivf.tapas.model.ActivityAndLocationCodeMapping;
import de.dlr.ivf.tapas.model.Incomes;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.scheme.TPS_SchemeSet;
import lombok.Builder;

import java.util.Collection;

@Builder
public class DataStore {

    Collection<TPS_ActivityConstant> activityConstants;

    AgeClasses ageClasses;

    Collection<TPS_Distance> distanceClasses;

    Incomes incomes;

    Collection<TPS_LocationConstant> locationConstants;

    Modes modes;

    Collection<TPS_PersonGroup> personGroups;

    Collection<TPS_SettlementSystem> settlementSystems;

    ActivityAndLocationCodeMapping activityAndLocationCodeMapping;

    Collection<UtilityFunctionDto> utilityFunctionData;

    TPS_SchemeSet schemeSet;

    TPS_ModeSet modeSet;
}
