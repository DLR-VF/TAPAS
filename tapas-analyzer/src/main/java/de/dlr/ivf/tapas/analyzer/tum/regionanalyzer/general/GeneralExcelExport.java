/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general;

import de.dlr.ivf.tapas.analyzer.tum.constants.CategoryCombination;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.Categories;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.RegionCode;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.Analyzer;
import jxl.Workbook;
import jxl.write.*;
import jxl.write.biff.RowsExceededException;


import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;

import jxl.write.Number;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

/**
 * This class uses a {@link GeneralAnalyzer} and exports it to (multiple) excel
 * files using its tree structure.
 *
 * @author boec_pa
 */
@SuppressWarnings("rawtypes")
public class GeneralExcelExport {

    private static int HEADER_HEIGHT = 0;
    private WritableCellFormat percentageFormat;
    private WritableCellFormat floatFormat;
    private WritableCellFormat intFormat;
    private final GeneralAnalyzer analyzer;

    public GeneralExcelExport(GeneralAnalyzer analyzer) {
        this.analyzer = analyzer;
        System.out.println(this.analyzer.getNumberTrips());
    }

    private void createFormats() {
        percentageFormat = new WritableCellFormat(NumberFormats.PERCENT_FLOAT);
        floatFormat = new WritableCellFormat(NumberFormats.FLOAT);
        intFormat = new WritableCellFormat(NumberFormats.INTEGER);

    }

    /**

     *
     * @param file
     */
    public boolean writeAnalysis(File file) {

        DefaultMutableTreeNode root = analyzer.getRoot();
        boolean result = false;
        for (int i = 0; i < root.getChildCount(); ++i) {

            try {
                DefaultMutableTreeNode fileNode = (DefaultMutableTreeNode) root.getChildAt(i);

                WritableWorkbook workbook = Workbook.createWorkbook(file);

                writeFile(workbook, fileNode, new Enum[0]);
                workbook.write();
                workbook.close();
                result = true;

            } catch (Exception e) {
                result = false;
            }
        }

        return result;
    }

    public void writeAnalysis(String baseName) throws IOException, WriteException {

        DefaultMutableTreeNode root = analyzer.getRoot();

        for (int i = 0; i < root.getChildCount(); ++i) {
            System.out.println("----childcount: " + root.getChildCount());

            DefaultMutableTreeNode fileNode = (DefaultMutableTreeNode) root.getChildAt(i);

            AnalyzerCollection fileAnalyzer = (AnalyzerCollection) fileNode.getUserObject();
            System.out.println("---anzahl kategorien des filenalyzer" + fileAnalyzer.getCategories().length);
            if (fileAnalyzer.getCategories().length > 0) {

                CategoryCombination.listAllCombinations(fileAnalyzer.getCategories());

                ArrayList<CategoryCombination> fileCombinations = CategoryCombination.listAllCombinations(
                        fileAnalyzer.getCategories());

                for (CategoryCombination cc : fileCombinations) {
                    File file = new File(baseName + fileAnalyzer.getName() + cc.toString() + ".xls");
                    WritableWorkbook workbook = Workbook.createWorkbook(file);

                    if (!writeFile(workbook, fileNode, cc.getCategories())) {
                        workbook.write();
                        workbook.close();
                    } else {// delete file if empty
                        workbook.close();
                        file.delete();
                    }

                }
            } else { // one file only
                System.out.println("ONE FILE ONLY");
                Date now = new Date();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                WritableWorkbook workbook = Workbook.createWorkbook(
                        new File(baseName + " exported on " + simpleDateFormat.format(now) + ".xls"));

                writeFile(workbook, fileNode, new Enum[0]);
                workbook.write();
                workbook.close();
            }
        }
    }

    /**
     * @param workbook
     * @param fileNode
     * @param fixedSheetValues are the categories that are fixed for the whole file (usually,
     *                         this would be a zero-length-array.
     * @return <code>true</code> if empty.
     * @throws RowsExceededException
     * @throws WriteException
     */
    private boolean writeFile(WritableWorkbook workbook, DefaultMutableTreeNode fileNode, Enum[] fixedSheetValues) throws RowsExceededException, WriteException {

        // has to be called for each workbook as formats rely on them
        // implicitly. :(
        createFormats();
        boolean empty = true;
        int numberOfTabs = fileNode.getChildCount();
        int tabIndex = 0;
        for (int i = 0; i < numberOfTabs; ++i) {// tab classes
            AnalyzerCollection tabAnalyzer = (AnalyzerCollection) ((DefaultMutableTreeNode) fileNode.getChildAt(i))
                    .getUserObject();
            if (tabAnalyzer.getCategories().length > 0) {
                ArrayList<CategoryCombination> tabCombinations = CategoryCombination.listAllCombinations(
                        tabAnalyzer.getCategories());

                for (CategoryCombination cc : tabCombinations) {
                    WritableSheet sheet = workbook.createSheet(tabAnalyzer.getName() + cc.toString(), tabIndex++);

                    // build new fixed values
                    Enum[] fixedValues = new Enum[fixedSheetValues.length + cc.getCategories().length];
                    System.arraycopy(fixedSheetValues, 0, fixedValues, 0, fixedSheetValues.length);
                    System.arraycopy(cc.getCategories(), 0, fixedValues, 0, cc.getCategories().length);
                    if (!writeTab(sheet, (DefaultMutableTreeNode) fileNode.getChildAt(i), fixedValues)) {
                        empty = false;
                    } else {
                        workbook.removeSheet(--tabIndex);
                    }
                }

            } else {// one tab only
                WritableSheet sheet = workbook.createSheet(tabAnalyzer.toString(), tabIndex++);
                if (!writeTab(sheet, (DefaultMutableTreeNode) fileNode.getChildAt(i), fixedSheetValues)) {
                    empty = false;
                } else {
                    workbook.removeSheet(--tabIndex);
                }
            }
        }
        return empty;
    }

    /**
     * @return new y
     */
    private int writeSplit(WritableSheet sheet, Analyzer a, Enum[] fixedSplitValues, Enum[] splitValues, int x, int y) throws WriteException {

        ArrayList<TASplitData> split = a.getSplit(fixedSplitValues);
        if (split == null) {
            return y;
        }

        for (int i = 0; i < split.size(); ++i) {
            sheet.addCell(new Label(x, y, splitValues[i].toString()));
            sheet.addCell(new Number(x + 1, y, split.get(i).getSplit(), percentageFormat));
            sheet.addCell(new Number(x + 2, y, split.get(i).getCntTrips()));
            sheet.addCell(new Number(x + 3, y, split.get(i).getAvgLength(), floatFormat));

            y++;
        }
        return y;
    }

    /**
     * @param sheet
     * @param tabNode
     * @param fixedSheetValues are the categories fixed in the sheet to write (in the default
     *                         version, that is {@link RegionCode}).
     * @throws WriteException
     * @throws RowsExceededException
     */
    private boolean writeTab(WritableSheet sheet, DefaultMutableTreeNode tabNode, Enum[] fixedSheetValues) throws RowsExceededException, WriteException {

        boolean empty = true;

        writeTabHeader(sheet);

        Enumeration en = tabNode.depthFirstEnumeration();

        int x = 0;
        int y = HEADER_HEIGHT;
        int ty;// temp variable

        while (en.hasMoreElements()) {
            DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) en.nextElement();
            if (leaf.equals(tabNode)) continue;
            Analyzer a = analyzer.getAnalyzers().get(leaf);

            // all values that are fixed in a split
            Enum[] fixedSplitValues = new Enum[a.getNumberOfAnalyzers() - 1];

            // filed with the fixed values for this sheet
            System.arraycopy(fixedSheetValues, 0, fixedSplitValues, 0, fixedSheetValues.length);

            // all values in the category of the split
            Enum[] splitValues = (Enum[]) a.getCategories()[a.getNumberOfAnalyzers() - 1].getEnumeration()
                                                                                         .getEnumConstants();

            // get all category combinations, we want a split for:
            Categories[] cats = new Categories[a.getNumberOfAnalyzers() - 1 - fixedSheetValues.length];
            System.arraycopy(a.getCategories(), fixedSheetValues.length, cats, 0, cats.length);
            ArrayList<CategoryCombination> splitCats = CategoryCombination.listAllCombinations(cats);

            y += 4; // start space

            sheet.addCell(new Label(x, y++, a.toString()));

            x = fixedSplitValues.length;
            sheet.addCell(new Label(x++, y, "Prozent"));
            sheet.addCell(new Label(x++, y, "Anzahl Wege"));
            sheet.addCell(new Label(x++, y, "Durchschnittliche Länge"));

            y += 1;

            for (CategoryCombination cc : splitCats) {
                x = 0;
                for (int i = 0; i < cc.getCategories().length; ++i) {
                    Enum c = cc.getCategories()[i];
                    fixedSplitValues[i + fixedSheetValues.length] = c;
                    sheet.addCell(new Label(x++, y, c.toString()));
                }

                ty = writeSplit(sheet, a, fixedSplitValues, splitValues, x, y);
                if (ty != y) {
                    empty = false;
                }
                y = ty;
            }

            if (splitCats.size() == 0) {
                x = 0;
                ty = writeSplit(sheet, a, fixedSplitValues, splitValues, x, y);
                if (ty != y) {
                    empty = false;
                }
                y = ty;
            }

        }
        return empty;
    }

    private void writeTabHeader(WritableSheet sheet) throws WriteException {
        int y = 0;

        sheet.addCell(new Label(0, y, "Source"));
        sheet.addCell(new Label(1, y++, analyzer.getSource()));
        sheet.addCell(new Label(0, y, "Region"));
        sheet.addCell(new Label(1, y++, analyzer.getRegion()));
        sheet.addCell(new Label(0, y, "Number of trips"));
        sheet.addCell(new Number(1, y++, analyzer.getNumberTrips(), intFormat));

        HEADER_HEIGHT = y;

    }
}
