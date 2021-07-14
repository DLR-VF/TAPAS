/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.TUM;

import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general.AnalyzerCollection;

import javax.swing.tree.DefaultMutableTreeNode;

public class StandardTUM extends IntegratedTUM {

    public void buildAnalyzerList() {

        DefaultMutableTreeNode file = new DefaultMutableTreeNode(new AnalyzerCollection("TUMExport"));

        // wegelängen sheet
        DefaultMutableTreeNode tab = new DefaultMutableTreeNode(
                new AnalyzerCollection("Wegelängen", getAnalyzer(Categories.RegionCode)));
        file.add(tab);

        DefaultMutableTreeNode analyzers = new DefaultMutableTreeNode(
                new AnalyzerCollection(getAnalyzer(Categories.TripIntention)));
        tab.add(analyzers);

        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(getAnalyzer(Categories.TripIntention),
                getAnalyzer(Categories.DistanceCategoryDefault)));
        tab.add(analyzers);

        // modalsplit
        tab = new DefaultMutableTreeNode(new AnalyzerCollection("Modalsplit", getAnalyzer(Categories.RegionCode)));
        file.add(tab);
        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(getAnalyzer(Categories.Mode)));
        tab.add(analyzers);
        analyzers = new DefaultMutableTreeNode(
                new AnalyzerCollection(getAnalyzer(Categories.Mode), getAnalyzer(Categories.DistanceCategoryDefault)));
        tab.add(analyzers);
        analyzers = new DefaultMutableTreeNode(
                new AnalyzerCollection(getAnalyzer(Categories.Mode), getAnalyzer(Categories.DistanceCategoryDefault),
                        getAnalyzer(Categories.TripIntention)));
        tab.add(analyzers);

        // personengruppen
        tab = new DefaultMutableTreeNode(new AnalyzerCollection("Personengruppen", getAnalyzer(Categories.RegionCode)));
        file.add(tab);
        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(getAnalyzer(Categories.PersonGroup)));
        tab.add(analyzers);

        this.root.insert(file, 0);
    }


}
