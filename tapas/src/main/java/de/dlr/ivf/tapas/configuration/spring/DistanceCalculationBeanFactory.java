package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.util.distance.DistanceFunction;
import de.dlr.ivf.tapas.util.distance.functions.MatrixDistanceFunction;
import de.dlr.ivf.tapas.model.IntMatrix;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.Map;

@Lazy
@Configuration
public class DistanceCalculationBeanFactory {

    @Bean
    public Map<TPS_Mode, DistanceFunction> travelDistanceFunctions(@Qualifier("modeDistanceMatrices") Map<TPS_Mode, IntMatrix> modeDistanceMatrices){

        Map<TPS_Mode, DistanceFunction> modeDistanceFunctions = new HashMap<>();

        for(Map.Entry<TPS_Mode, IntMatrix> entry : modeDistanceMatrices.entrySet()){
            modeDistanceFunctions.put(entry.getKey(), new MatrixDistanceFunction(entry.getValue()));
        }

        return modeDistanceFunctions;
    }

}
