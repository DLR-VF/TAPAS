/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TPS_CSV2SQLMatrix {


    double[][] matrix = null;

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: OD2SQLMatrix <inputfile> <outputfile> <matrixName>");
            return;
        }
        String input = args[0];
        String output = args[1];
        String name = args[2];
        TPS_CSV2SQLMatrix worker = new TPS_CSV2SQLMatrix();
        worker.readCSVFile(input);
        worker.writeSQLScript(output, name);

    }

    public void readCSVFile(String fileName) {
        String line;
        try {
            int minIndex = 0x0fffffff;
            int maxIndex = 0;
            int from = 0, to = 0;
            double time;
            SortedSet<Integer> TAZes = new TreeSet<>();
            // read the file for min/max-values
            FileReader in = new FileReader(fileName);
            BufferedReader input = new BufferedReader(in);
            input.readLine();//header
            while ((line = input.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line);
                if (tok.countTokens() == 3 && line.startsWith(" ")) {
                    from = Integer.parseInt(tok.nextToken());
                    to = Integer.parseInt(tok.nextToken());
                    time = Double.parseDouble(tok.nextToken());
                    TAZes.add(from);
                    TAZes.add(to);
                }
                minIndex = Math.min(minIndex, Math.min(from, to));
                maxIndex = Math.max(maxIndex, Math.max(from, to));
            }
            input.close();
            in.close();

            //stupid copy but Im to lazy to find an elegant method to convert a SortetMap-Array to int[]
            Map<Integer, Integer> indexes = new HashMap<>();
            int i = 0;
            for (Integer taZe : TAZes) {
                // Get element
                indexes.put(taZe, i);
                i++;
            }

            this.matrix = new double[indexes.size()][indexes.size()];

            // read the file for time-values
            in = new FileReader(fileName);
            input = new BufferedReader(in);
            input.readLine();//header
            while ((line = input.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(line);
                if (tok.countTokens() == 3 && line.startsWith(" ")) {
                    from = Integer.parseInt(tok.nextToken());
                    from = indexes.get(from);
                    to = Integer.parseInt(tok.nextToken());
                    to = indexes.get(to);
                    time = Double.parseDouble(tok.nextToken());
                    if (time > 777776) {
                        time = 0;
                    }

                    this.matrix[from][to] = time;
                }
            }
            input.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeSQLScript(String output, String name) {

        try {
            FileWriter writer = new FileWriter(output);
            writer.append("INSERT INTO core.berlin_matrices VALUES ('" + name + "', \n");
            //todo revise this
            //writer.append(TPS_VisumConverter.matrixToSQLArray(matrix, 0) + ")\n");

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
