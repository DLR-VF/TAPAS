package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.api.io.connection.ConnectionPool;
import de.dlr.ivf.tapas.configuration.json.SimulationRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.TapasConfig;
import de.dlr.ivf.tapas.configuration.json.acceptance.PlanEVA1AcceptanceConfig;
import de.dlr.ivf.tapas.configuration.json.agent.HouseholdConfiguration;
import de.dlr.ivf.tapas.configuration.json.region.RegionConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.ChronologicalRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.runner.TripPriorityRunnerConfiguration;
import de.dlr.ivf.tapas.configuration.json.trafficgeneration.TrafficGenerationConfiguration;
import de.dlr.ivf.tapas.configuration.json.util.TravelTimeConfiguration;
import de.dlr.ivf.tapas.model.plan.acceptance.TPS_PlanEVA1Acceptance;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.simulation.runner.ChronologicalRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * The `TapasFactory` class is responsible for creating and configuring various beans used in the TAPAS application.
 * It is a Spring configuration class that defines the bean creation methods and their dependencies.
 */
@Lazy
@Configuration
@ComponentScan(basePackages = {"de.dlr.ivf.tapas"})
public class TapasFactory {

    private final TapasConfig tapasConfig;

    @Autowired
    public TapasFactory(TapasConfig tapasConfig) {
        this.tapasConfig = tapasConfig;
    }

    @Bean
    public TPS_DB_IO setUpIo(){
        return new TPS_DB_IO(new ConnectionPool(tapasConfig.getConnectionDetails()), null);
    }

    @Bean("workerCount")
    public int workerCount(TripPriorityRunnerConfiguration configuration){
        return configuration.workerCount();
    }

    @Bean("maxSystemSpeed")
    public int maxSystemSpeed(){
        return tapasConfig.getMaxSystemSpeed();
    }

    @Bean(name = "chronologicalRunner")
    public ChronologicalRunner chronologicalRunner(ChronologicalRunnerConfiguration configuration){
        return new ChronologicalRunner();
    }

    @Bean(name = "trafficGenerationConfiguration")
    public TrafficGenerationConfiguration trafficGenerationConfiguration() {
        SimulationRunnerConfiguration runnerConfiguration = tapasConfig.getSimulationRunnerConfiguration();

        return runnerConfiguration.getTrafficGenerationConfiguration();
    }

    @Bean(name = "abstractSimulationRunner")
    public SimulationRunnerConfiguration abstractSimulationRunnerConfiguration() {
        return tapasConfig.getSimulationRunnerConfiguration();
    }

    @Bean(name = "tripPriorityRunnerConfiguration")
    public TripPriorityRunnerConfiguration tripPriorityRunnerConfiguration(
            @Qualifier("abstractSimulationRunner") SimulationRunnerConfiguration configuration) {

        if(configuration instanceof TripPriorityRunnerConfiguration config){
            return config;
        }else{
            throw new IllegalArgumentException("The requested simulationRunnerConfiguration is not a TripPriorityRunnerConfiguration");
        }
    }

    @Bean(name = "chronologicalRunnerConfiguration")
    public ChronologicalRunnerConfiguration chronologicalRunnerConfiguration(
            @Qualifier("abstractSimulationRunner") SimulationRunnerConfiguration configuration){

        if(configuration instanceof ChronologicalRunnerConfiguration config){
            return config;
        }else{
            throw new IllegalArgumentException("The requested simulationRunnerConfiguration is not a ChronologicalRunnerConfiguration");
        }
    }

    @Bean
    public TravelTimeConfiguration travelTimeConfiguration(){
        return tapasConfig.getTravelTimeCalculator();
    }

    @Bean
    public TPS_PlanEVA1Acceptance acceptance(PlanEVA1AcceptanceConfig config){
        return new TPS_PlanEVA1Acceptance(config.eBottomFinance(),config.fTopFinance(),config.turningPointFinance(),
                config.eBottomTime(), config.fTopTime(), config.turningPointMaxTime(), config.eBottomOverallTime(),
                config.fTopOverallTime(), config.checkBudgetConstraints());
    }

    @Bean
    public HouseholdConfiguration householdConfiguration(){
        return tapasConfig.getHouseholdConfiguration();
    }

    @Bean
    public RegionConfiguration regionConfiguration(){
        return tapasConfig.getRegionConfiguration();
    }

    @Bean
    public PlanEVA1AcceptanceConfig acceptanceConfig(){
        return tapasConfig.getPlanAcceptance();
    }
}
