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
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
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

    @Lazy
    @Bean(name = "modeMatrixMapMappings")
    public Map<String, Map<String,String>> modeToMatrixMapMappings(MatrixConfiguration matrixConfiguration) {
        return matrixConfiguration.modeMatrixMapMappings();
    }
}
