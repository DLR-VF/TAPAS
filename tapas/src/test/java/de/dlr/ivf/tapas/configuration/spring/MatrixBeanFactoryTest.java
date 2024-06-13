package de.dlr.ivf.tapas.configuration.spring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatrixBeanFactoryTest {

    @Test
    @DisplayName("Should return a squared matrix")
    void shouldReturnASquaredMatrix() {

        int[] inputMatrix = new int[]{1,2,3,4};

        int[][] expectedMatrix = new int[][]{{1,2,},{3,4}};

        MatrixBeanFactory mf = new MatrixBeanFactory();

        assertArrayEquals(expectedMatrix, mf.buildSquareMatrix(inputMatrix).getMatrix());
    }

}