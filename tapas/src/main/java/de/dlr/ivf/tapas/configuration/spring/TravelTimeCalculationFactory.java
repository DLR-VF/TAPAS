package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.util.traveltime.TravelTimeFunction;
import de.dlr.ivf.tapas.util.traveltime.functions.SimpleMatrixMapTravelTimeFunction;
import de.dlr.ivf.tapas.configuration.json.util.TravelTimeConfiguration;
import de.dlr.ivf.tapas.model.IntMatrix;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.Map;

@Lazy
@Configuration
public class TravelTimeCalculationFactory {

    /**
     * Initializes the TravelTimeFunctions for each mode that will be used when calculating the travel times between two
     * distinct locations.
     *
     * @param modeMatrixMaps        The matrix maps for each mode of transportation.
     * @param modes                 The available modes of transportation.
     * @param configuration         The travel time configuration.
     * @param modeBlDistanceMatrices The beeline distance matrix.
     * @return A map of travel time functions for each mode of transportation.
     * @throws IllegalArgumentException if there are no matrix maps for a mode.
     */
    @Bean("interTazTravelTimeFunctions")
    public Map<TPS_Mode, TravelTimeFunction> interTazTravelTimeFunctions(Map<TPS_Mode, Map<String, MatrixMap>> modeMatrixMaps,
                                                                         Modes modes, TravelTimeConfiguration configuration,
                                                                         @Qualifier("modeBeelineDistanceMatrices") Map<TPS_Mode, IntMatrix> modeBlDistanceMatrices){

        double minDist = configuration.minDist();

        Map<TPS_Mode, TravelTimeFunction> modeTravelTimeFunctions = new HashMap<>();

        for(TPS_Mode mode: modes.getModes()){

            Map<String, MatrixMap> matrixMaps = modeMatrixMaps.get(mode);
            if(matrixMaps == null){
                throw new IllegalArgumentException("No MatrixMaps for mode " + mode.getName());
            }

            IntMatrix blMatrix = modeBlDistanceMatrices.get(mode);
            if(blMatrix == null){
                throw new IllegalArgumentException("No beeline distance matrix for mode " + mode.getName());
            }

            TravelTimeFunction travelTimeFunction = new SimpleMatrixMapTravelTimeFunction(minDist, blMatrix, matrixMaps, mode);
            modeTravelTimeFunctions.put(mode, travelTimeFunction);
        }

        return modeTravelTimeFunctions;
    }

    /**
     * Initializes the TravelTimeFunctions for each mode that will be used when calculating the travel times between two
     * locations that are the same.
     *
     * @param configuration           The travel time configuration.
     * @param interTravelTimeFunctions The map of travel time functions for inter-taz trips.
     * @return The map of travel time functions for intra-taz trips.
     * @throws IllegalArgumentException if the configuration does not use matrix values for intra-taz trips.
     */
    @Bean("intraTazTravelTimeFunctions")
    public Map<TPS_Mode, TravelTimeFunction> intraTazTravelTimeFunctions(TravelTimeConfiguration configuration,
            @Qualifier("interTazTravelTimeFunctions") Map<TPS_Mode, TravelTimeFunction> interTravelTimeFunctions){

        if(!configuration.useTazIntraInfoMatrix()){
            throw new IllegalArgumentException("TAPAS currently only supports matrix values for intra taz trips.");
        }

        return interTravelTimeFunctions;
    }

    @Bean("minDist")
    public double minDist(TravelTimeConfiguration configuration){
        return configuration.minDist();
    }

    @Bean("useTazIntraInfoMatrix")
    public boolean useTazIntraInfoMatrix(TravelTimeConfiguration configuration){
        return configuration.useTazIntraInfoMatrix();
    }

    @Bean("ptTravelTimeFactor")
    public double ptTravelTimeFactor(TravelTimeConfiguration configuration){
        return configuration.ptTravelTimeFactor();
    }

    @Bean("ptAccessFactor")
    public double ptAccessFactor(TravelTimeConfiguration configuration){
        return configuration.ptAccessFactor();
    }

    @Bean("ptEgressFactor")
    public double ptEgressFactor(TravelTimeConfiguration configuration){
        return configuration.ptEgressFactor();
    }

    @Bean("defaultBlockScore")
    public double defaultBlockScore(TravelTimeConfiguration configuration){
        return configuration.defaultBlockScore();
    }

    @Bean("averageDistancePtStop")
    public double averageDistancePtStop(TravelTimeConfiguration configuration){
        return configuration.averageDistancePtStop();
    }

    @Bean("beelineFactorPt")
    public double beelineFactorPt(TravelTimeConfiguration configuration){
        return configuration.beelineFactorPt();
    }

    @Bean("beelineFactorBike")
    public double beelineFactorBike(TravelTimeConfiguration configuration){
        return configuration.beelineFactorBike();
    }
    @Bean("beelineFactorFoot")
    public double beelineFactorFoot(TravelTimeConfiguration configuration){
        return configuration.beelineFactorFoot();
    }
    @Bean("beelineFactorMit")
    public double beelineFactorMit(TravelTimeConfiguration configuration){
        return configuration.beelineFactorMit();
    }
}
