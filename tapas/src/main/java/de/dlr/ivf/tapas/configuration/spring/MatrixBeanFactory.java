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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Configuration
public class MatrixBeanFactory {

    @Lazy
    @Bean(name = "matrixMaps")
    public Map<String, MatrixMap> matrixMaps(MatrixConfiguration matrixConfiguration, TPS_DB_IO dbIo) {

        Map<String, MatrixMap> matrixMaps = new HashMap<>();
        DataSource matricesDataSource = matrixConfiguration.matricesSource();

        Collection<MatrixMapConfiguration> mapConfigurations = matrixConfiguration.matrixMaps();

        for (MatrixMapConfiguration mapConfiguration : mapConfigurations) {

            MatrixMap matrixMap = buildMatrixMap(mapConfiguration, matricesDataSource.getUri(), dbIo);

            matrixMaps.put(mapConfiguration.name(), matrixMap);
        }

        return matrixMaps;
    }

    public MatrixMap buildMatrixMap(MatrixMapConfiguration mapConfiguration, String matrixUri, TPS_DB_IO dbIo) {

        Collection<MatrixMapEntry> mapMatrices = mapConfiguration.matrices();

        MatrixMap matrixMap = new MatrixMap(mapConfiguration.name(), 1440, TreeMap::new);

        for (MatrixMapEntry matrixMapEntry : mapMatrices) {

            FilterableDataSource matrixDatasource = new FilterableDataSource(matrixUri, new Filter("matrix_name", matrixMapEntry.matrixName()));

            Collection<IntMatrixDto> matrices = dbIo.readFromDb(matrixDatasource, IntMatrixDto.class, IntMatrixDto::new);

            IntMatrixDto matrixDto = dbIo.validateSingleResultAndGetOrThrow(matrices);

            IntMatrix matrix = buildSquareMatrix(matrixDto.getMatrix());

            matrixMap.addMatrix(matrixMapEntry.fromTime(), matrix);
        }

        return matrixMap;
    }

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

    @Bean("beelineDistanceMatrix")
    public IntMatrix beelineDistanceMatrix(MatrixConfiguration matrixConfiguration, TPS_DB_IO dbIo) {
        DataSource matricesSource = matrixConfiguration.matricesSource();
        FilterableDataSource blMatrixDataSource = new FilterableDataSource(matricesSource.getUri(),
                new Filter("matrix_name", matrixConfiguration.beelineDistanceMatrixName()));
        Collection<IntMatrixDto> matrices = dbIo.readFromDb(blMatrixDataSource, IntMatrixDto.class, IntMatrixDto::new);

        IntMatrixDto matrixDto = dbIo.validateSingleResultAndGetOrThrow(matrices);

        return buildSquareMatrix(matrixDto.getMatrix());
    }

    @Lazy
    @Bean(name = "modeMatrixMapMappings")
    public Map<String, Map<String,String>> modeToMatrixMapMappings(MatrixConfiguration matrixConfiguration) {
        return matrixConfiguration.modeMatrixMapMappings();
    }

    @Lazy
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
