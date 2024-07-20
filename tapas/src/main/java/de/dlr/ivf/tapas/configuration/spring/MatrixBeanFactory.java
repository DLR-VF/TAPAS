package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.api.io.configuration.DataSource;
import de.dlr.ivf.api.io.configuration.Filter;
import de.dlr.ivf.api.io.configuration.FilterableDataSource;
import de.dlr.ivf.tapas.configuration.json.region.matrix.MatrixConfiguration;
import de.dlr.ivf.tapas.configuration.json.region.matrix.MatrixMapConfiguration;
import de.dlr.ivf.tapas.configuration.json.region.matrix.MatrixMapEntry;
import de.dlr.ivf.tapas.dto.IntMatrixDto;
import de.dlr.ivf.tapas.model.IntMatrix;
import de.dlr.ivf.tapas.model.MatrixMap;
import de.dlr.ivf.tapas.model.mode.Modes;
import de.dlr.ivf.tapas.model.mode.TPS_Mode;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The MatrixBeanFactory class is responsible for creating and configuring various matrix-related beans used in the application.
 */
@Lazy
@Configuration
public class MatrixBeanFactory {

    /**
     * Creates and returns a map of MatrixMap objects based on the provided MatrixConfiguration and matrices.
     *
     * @param matrixConfiguration The MatrixConfiguration object that contains the matrix map configurations.
     * @param matrices            A map of matrix names to IntMatrix objects.
     * @return A map of matrix map names to MatrixMap objects.
     */
    @Bean(name = "matrixMaps")
    public Map<String, MatrixMap> matrixMaps(MatrixConfiguration matrixConfiguration,
                                             @Qualifier("matrices") Map<String, IntMatrix> matrices) {

        Map<String, MatrixMap> matrixMaps = new HashMap<>();

        Collection<MatrixMapConfiguration> mapConfigurations = matrixConfiguration.matrixMaps();

        for (MatrixMapConfiguration mapConfiguration : mapConfigurations) {

            MatrixMap matrixMap = new MatrixMap(mapConfiguration.name(), 1440, TreeMap::new);

            mapConfiguration.matrices()
                    .forEach(matrixMapEntry -> matrixMap.addMatrix(matrixMapEntry.fromTime(), matrices.get(matrixMapEntry.matrixName())));

            matrixMaps.put(mapConfiguration.name(), matrixMap);
        }

        return matrixMaps;
    }

    /**
     * Populates a map of matrix names to IntMatrix objects based on the provided MatrixConfiguration.
     *
     * @param dbIo           The TPS_DB_IO object used to read from the database.
     * @param configuration The MatrixConfiguration object that contains the matrices source and matrix map configurations.
     * @return A map of matrix names to IntMatrix objects.
     */
    @Bean("matrices")
    public Map<String, IntMatrix> matrices(TPS_DB_IO dbIo, MatrixConfiguration configuration){

        Map<String, IntMatrix> matrices = new HashMap<>();

        DataSource matricesDataSource = configuration.matricesSource();

        Stream<String> beelineDistanceMatrixNames = configuration.beelineModeDistanceMatrices().values().stream();

        Stream<String> netDistanceMatrixNames = configuration.modeDistanceMatrices().values().stream();

        Stream<String> travelTimeMatrixNames = configuration.matrixMaps()
                .stream()
                .map(MatrixMapConfiguration::matrices)
                .flatMap(Collection::stream)
                .map(MatrixMapEntry::matrixName);


        Set<String> matrixNames = Stream.of(beelineDistanceMatrixNames, netDistanceMatrixNames, travelTimeMatrixNames)
                .flatMap(matrixName -> matrixName)
                .collect(Collectors.toSet());

        for (String matrixName : matrixNames) {
            FilterableDataSource matrixDataSource = new FilterableDataSource(matricesDataSource.getUri(), new Filter("matrix_name", matrixName));
            Collection<IntMatrixDto> matrixDtos = dbIo.readFromDb(matrixDataSource, IntMatrixDto.class, IntMatrixDto::new);
            IntMatrixDto matrixDto = dbIo.validateSingleResultAndGetOrThrow(matrixDtos);
            IntMatrix matrix = buildSquareMatrix(matrixDto.getMatrix());
            matrices.put(matrixName, matrix);
        }

        return matrices;
    }

    /**
     * Builds a square matrix from the given input matrix.
     *
     * @param inputMatrix The input matrix represented as a one-dimensional array of integers. It is expected that the
     *                    output matrix is stored row by row in the input array.
     * @return The square matrix created from the input matrix.
     * @throws IllegalArgumentException if the input matrix cannot be converted into a squared matrix.
     */
    public IntMatrix buildSquareMatrix(int[] inputMatrix) {

        int matrixLength =  (int) Math.sqrt(inputMatrix.length);

        if(matrixLength * matrixLength != inputMatrix.length){
            throw new IllegalArgumentException("The provided input matrix cannot be converted into a squared matrix.");
        }

        IntMatrix matrix = new IntMatrix(matrixLength, matrixLength);
        for (int i = 0; i < inputMatrix.length; i++) {
            matrix.put(i / matrixLength, i % matrixLength, inputMatrix[i]);
        }
        return matrix;
    }

    /**
     * Sets up a map of beeline distance matrices for each mode.
     *
     * @param modes               The Modes object that contains the mode information.
     * @param matrices            A map of matrix names to IntMatrix objects.
     * @param blMatrixMappings    A map of mode names to beeline distance matrix names.
     * @return A map of TPS_Mode objects to beeline distance IntMatrix objects.
     * @throws IllegalArgumentException If the provided matrix refers to a mode that has not been defined or if no matrix
     *                                  with the specified name has been configured for a specific mode.
     */
    @Bean("modeBeelineDistanceMatrices")
    public Map<TPS_Mode, IntMatrix> beelineDistanceMatrices(Modes modes,
                                                            Map<String, IntMatrix> matrices,
                                                            @Qualifier("beelineModeDistanceMatrixMappings") Map<String, String> blMatrixMappings) {

        Map<TPS_Mode,  IntMatrix> beelineDistanceMatrices = buildModeMatrices(modes, matrices, blMatrixMappings);
        validateAllModesConfiguredOrThrow(modes, beelineDistanceMatrices.keySet());

        return beelineDistanceMatrices;
    }

    @Bean("modeDistanceMatrices")
    public Map<TPS_Mode, IntMatrix> modeDistanceMatrices(Modes modes,
                                                         Map<String, IntMatrix> matrices,
                                                         @Qualifier("modeDistanceMatrixMappings") Map<String, String> distanceMatrixMappings){

        Map<TPS_Mode, IntMatrix> modeDistanceMatrices = buildModeMatrices(modes, matrices, distanceMatrixMappings);
        validateAllModesConfiguredOrThrow(modes, modeDistanceMatrices.keySet());

        return modeDistanceMatrices;
    }

    public Map<TPS_Mode, IntMatrix> buildModeMatrices(Modes modes, Map<String, IntMatrix> matrices,
                                                      Map<String, String> modeNameToMatrixNameMapping) {

        Map<TPS_Mode, IntMatrix> modeMatrices = new HashMap<>();

        for(Map.Entry<String,String> blMatrixMapping : modeNameToMatrixNameMapping.entrySet()){
            String modeName = blMatrixMapping.getKey();
            String matrixName = blMatrixMapping.getValue();

            TPS_Mode mode = modes.getModeByName(modeName);
            if (mode == null) {
                throw new IllegalArgumentException("The provided matrix refers to a mode [" + modeName + "] that has not been defined.");
            }

            IntMatrix matrix = matrices.get(matrixName);
            if (matrix == null) {
                throw new IllegalArgumentException("No matrix with name " + matrixName + " has been configured for mode: " + modeName + ".");
            }
            modeMatrices.put(mode, matrix);
        }

        return modeMatrices;
    }

    /**
     * Returns a map of mode to matrix map mappings based on the provided MatrixConfiguration.
     *
     * @param matrixConfiguration The MatrixConfiguration object that contains the mode to matrix map configurations.
     * @return A map of mode names to a map of travel stage names like (ACCESS, EGRESS, TT,...) and their corresponding
     * matrix map name.
     */
    @Bean(name = "modeMatrixMapMappings")
    public Map<String, Map<String,String>> modeToMatrixMapMappings(MatrixConfiguration matrixConfiguration) {
        return matrixConfiguration.modeMatrixMapMappings();
    }

    /**
     * Returns a map of mode names to beeline distance matrix names based on the provided MatrixConfiguration.
     *
     * @param matrixConfiguration The MatrixConfiguration object that contains the matrices source and matrix map configurations.
     * @return A map of mode names to beeline distance matrix names.
     */
    @Bean("beelineModeDistanceMatrixMappings")
    public Map<String, String> modeBeelineDistanceMatrixMappings(MatrixConfiguration matrixConfiguration){
        return matrixConfiguration.beelineModeDistanceMatrices();
    }

    /**
     * Retrieves the mode distance matrix mappings based on the provided MatrixConfiguration.
     *
     * @param matrixConfiguration The MatrixConfiguration object that contains the mode distance matrix configurations.
     * @return A map of mode names to their corresponding distance matrix names.
     */
    @Bean("modeDistanceMatrixMappings")
    public Map<String, String> modeDistanceMatrixMappings(MatrixConfiguration matrixConfiguration){
        return matrixConfiguration.modeDistanceMatrices();
    }

    /**
     * Creates and returns a map of TPS_Mode objects to a map of travel stage names and their corresponding MatrixMap objects.
     *
     * @param modeToMatrixMapMappings A map of mode names to a map of travel stage names and their corresponding matrix map name.
     * @param matrixMaps              A map of matrix map names to MatrixMap objects.
     * @param modes                   The Modes object that contains the mode information.
     * @return A map of TPS_Mode objects to a map of travel stage names and their corresponding MatrixMap objects.
     * @throws IllegalArgumentException if the provided matrix map refers to a mode that has not been defined or if a matrix map with the specified name has not been configured for
     *  a specific mode and stage.
     */
    @Bean
    public Map<TPS_Mode, Map<String, MatrixMap>> modeMatrixMaps(@Qualifier("modeMatrixMapMappings") Map<String, Map<String,String>> modeToMatrixMapMappings,
                                                    @Qualifier("matrixMaps") Map<String, MatrixMap> matrixMaps,
                                                    Modes modes) {
        Map<TPS_Mode, Map<String, MatrixMap>> modeMatrixMaps = new HashMap<>();

        for(Map.Entry<String, Map<String, String>> modeMatrixMapEntry : modeToMatrixMapMappings.entrySet()) {

            String modeName = modeMatrixMapEntry.getKey();
            TPS_Mode mode = modes.getModeByName(modeName);
            if(mode == null){
                throw new IllegalArgumentException("The provided matrix map refers to a mode ["+modeName+"] that has not been defined.");
            }

            Map<String, MatrixMap> modeSpecificMatrixMaps = new HashMap<>();

            for(Map.Entry<String, String> matrixMapEntries : modeMatrixMapEntry.getValue().entrySet()) {

                String stageName = matrixMapEntries.getKey();
                String matrixMapName = matrixMapEntries.getValue();
                MatrixMap matrixMap = matrixMaps.get(matrixMapName);

                if(matrixMap == null){
                    throw new IllegalArgumentException("No MatrixMap with name " + matrixMapName + " has been configured for mode: "+modeName+" and stage: "+stageName+".");
                }

                modeSpecificMatrixMaps.put(stageName, matrixMap);
            }

            modeMatrixMaps.put(mode, modeSpecificMatrixMaps);
        }
        validateAllModesConfiguredOrThrow(modes, modeMatrixMaps.keySet());

        return modeMatrixMaps;
    }

    /**
     * Validates whether all modes have been configured based on the given Modes object and the set of configured modes.
     *
     * @param modes             The Modes object that contains the mode information.
     * @param configuredModes   A set of configured modes.
     * @throws IllegalArgumentException If not all modes have been configured.
     */
    private void validateAllModesConfiguredOrThrow(Modes modes, Set<TPS_Mode> configuredModes){

        Set<TPS_Mode> allModes = new HashSet<>(modes.getModes());
        allModes.removeAll(configuredModes);
        if (!allModes.isEmpty()) {
            throw new IllegalArgumentException("Not all modes have been configured: " + allModes);
        }
    }
}
