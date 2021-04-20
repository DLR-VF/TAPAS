/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general;


import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.AnalyzerBase;

/**
 * This class groups {@link AnalyzerBase} objects and works as a wrapper for
 * {@link javax.swing.tree.DefaultMutableTreeNode} that form the tree in
 * {@link TUMControlGeneral}.
 *
 * @author boec_pa
 */
@SuppressWarnings("rawtypes")
public class AnalyzerCollection {

    static private final String SEPARATOR = " - ";

    private AnalyzerBase[] analyzers;
    private String name;

    public AnalyzerCollection(String name, AnalyzerBase... analyzers) {
        this.analyzers = analyzers;
        this.name = name;
    }

    public AnalyzerCollection(AnalyzerBase... analyzers) {
        this("", analyzers);
    }

    public AnalyzerCollection(String name) {
        this(name, new AnalyzerBase[0]);
    }

    /**
     * adds an {@link AnalyzerBase} to the collection and returns the index
     * where it is inserted.
     */
    public int addAnalyzer(AnalyzerBase analyzer) {
        AnalyzerBase[] newAnalyzers = new AnalyzerBase[size() + 1];
        System.arraycopy(analyzers, 0, newAnalyzers, 0, size());
        newAnalyzers[size()] = analyzer;
        analyzers = newAnalyzers;

        return size() - 1;
    }

    public AnalyzerBase[] getAnalyzers() {
        return analyzers;
    }

    public Categories[] getCategories() {
        Categories[] result = new Categories[analyzers.length];
        for (int i = 0; i < analyzers.length; ++i) {
            result[i] = analyzers[i].getCategories();
        }

        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int size() {
        return analyzers.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (AnalyzerBase a : analyzers) {
            sb.append(SEPARATOR).append(a.toString());
        }
        String s = sb.toString().replaceFirst(SEPARATOR, "");
        if (s.length() > 0 && name.length() > 0) {
            return name + ": " + s;
        } else if (s.length() > 0) {
            return s;
        } else {
            return name;
        }
    }

}
