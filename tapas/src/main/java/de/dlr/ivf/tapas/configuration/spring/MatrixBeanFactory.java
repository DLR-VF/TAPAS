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
                                             @Qualifier("matrixMapsMatrices") Map<String, IntMatrix> matrices) {

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
    @Bean("matrixMapsMatrices")
    public Map<String, IntMatrix> matrices(TPS_DB_IO dbIo, MatrixConfiguration configuration){

        Map<String, IntMatrix> matrices = new HashMap<>();

        DataSource matricesDataSource = configuration.matricesSource();

        Set<String> matrixNames = configuration.matrixMaps()
                .stream()
                .map(MatrixMapConfiguration::matrices)
                .flatMap(Collection::stream)
                .map(MatrixMapEntry::matrixName)
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
     * The beeline distance matrix is obtained by retrieving the beeline distance matrix from the database,
     * building a square matrix from it, and returning the result as an IntMatrix object.
     *
     * @param matrixConfiguration The MatrixConfiguration object that contains the matrices source and matrix map configurations.
     * @param dbIo                The TPS_DB_IO object used to read from the database.
     * @return The beeline distance matrix as an IntMatrix object.
     */
    @Bean("beelineDistanceMatrix")
    public IntMatrix beelineDistanceMatrix(MatrixConfiguration matrixConfiguration, TPS_DB_IO dbIo) {
        DataSource matricesSource = matrixConfiguration.matricesSource();
        FilterableDataSource blMatrixDataSource = new FilterableDataSource(matricesSource.getUri(),
                new Filter("matrix_name", matrixConfiguration.beelineDistanceMatrixName()));
        Collection<IntMatrixDto> matrices = dbIo.readFromDb(blMatrixDataSource, IntMatrixDto.class, IntMatrixDto::new);

        IntMatrixDto matrixDto = dbIo.validateSingleResultAndGetOrThrow(matrices);

        return buildSquareMatrix(matrixDto.getMatrix());
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

        return modeMatrixMaps;
    }
}
