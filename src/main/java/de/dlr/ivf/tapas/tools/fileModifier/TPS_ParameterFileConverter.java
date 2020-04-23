package de.dlr.ivf.tapas.tools.fileModifier;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.tools.fileModifier.filefilter.ExtensionFilter;
import de.dlr.ivf.tapas.util.parameters.*;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * This method was designed to convert old Text-Tapas configs to the new config format.
 * Since a lot of new parameters are introduces this method is obsolete.
 */
public class TPS_ParameterFileConverter {

    /**
     * Mapping for String to enums
     */
    private final Map<String, Enum<?>> map;
    String[] prefixes = new String[]{"run_", "file_", "db_", "par_", "log_"};
    String[] header = new String[]{"name", "value", "comment"};
    IndexedColors[] indexedColors = new IndexedColors[]{IndexedColors.LIGHT_CORNFLOWER_BLUE, IndexedColors.LIGHT_GREEN, IndexedColors.WHITE};

    /**
     * Default constructor, which initializes the string/enum-map
     */
    public TPS_ParameterFileConverter() {
        Map<Enum<?>, String> map0 = new HashMap<>();

        map0.put(ParamFlag.FLAG_USE_SCHOOLBUS, "main.UseSchoolbus");
        map0.put(ParamFlag.FLAG_USE_BLOCK_LEVEL, "tpsMode.UseBlockLevel");
        map0.put(ParamFlag.FLAG_USE_EXIT_MAUT, "tpsMode.UseExitMaut");
        map0.put(ParamFlag.FLAG_INTRA_INFOS_MATRIX, "tpsMode.IntraTVZMatrix");
        map0.put(ParamFlag.FLAG_USE_DRIVING_LICENCE, "tpsPerson.useDrivingLicence");
        map0.put(ParamFlag.FLAG_USE_FIXED_LOCS_ON_BASE, "tpsScheme.useFixedLocsOnBasis");
        map0.put(ParamFlag.FLAG_REJUVENATE_RETIREE, "tpsPersons.rejuventateRetiree");
        map0.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_SELECTION_PROBS, "tpsSelectSchemes.manipulateSelectionProbs");
        map0.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_BY_WORKINGCHAINS, "tpsPersons.manipulateByWorkingChains");
        map0.put(ParamFlag.FLAG_SCHEMES_MANIPULATE_BY_WORK_AT_HOME, "tpsPersons.manipulateByWorkAtHome");
        map0.put(ParamFlag.FLAG_CHECK_BUDGET_CONSTRAINTS, "tpsPersons.checkBudgetConstraint");
        map0.put(ParamFlag.FLAG_SELECT_LOCATIONS_DIFF_PERSON_GROUP, "tpsSelectLocations.diffPersonGroup");
        map0.put(ParamFlag.FLAG_RUN_SZENARIO, "tpsScheme.RunSzenario");
        map0.put(ParamFlag.FLAG_LOCATION_POCKET_COSTS, "tpsSelectLocations.pocketCosts");
        //map0.put(ParamFlag.FLAG_USE_TT_BASE_MIT, "tpsMain.UseBasisMIV");
        //map0.put(ParamFlag.FLAG_USE_TT_BASE_PT, "tpsMain.UseBasisOEV");
        map0.put(ParamFlag.FLAG_INFLUENCE_RANDOM_NUMBER, "tpsMain.influenceRandomNumber");

        map0.put(ParamString.DB_DBNAME, "main.databaseTapas");

        map0.put(ParamString.PATH_ABS_INPUT, "main.pathDataInput");
        map0.put(ParamString.PATH_ABS_OUTPUT, "main.pathDataOutput");

        map0.put(ParamString.UTILITY_FUNCTION_CLASS, "tpsModeDistribution.useBetaBudgets");

        map0.put(ParamValue.AVERAGE_DISTANCE_PT_STOP, "main.tvzInformation");
        map0.put(ParamValue.VELOCITY_BIKE, "tpsMode.VelocityBike");
        map0.put(ParamValue.VELOCITY_FOOT, "tpsMode.VelocityFoot");
        map0.put(ParamValue.REJUVENATE_BY_NB_YEARS, "tpsPersons.rejuventateBy");
        map0.put(ParamValue.MAX_TRIES_PERSON, "tpsPersons.maxTrials");
        map0.put(ParamValue.MAX_TIME_DIFFERENCE, "tpsPersons.maxTimeDifference");
        map0.put(ParamValue.WEIGHT_WORKING_CHAINS, "tpsPersons.weightWorkingChains");
        map0.put(ParamValue.WEIGHT_WORKING_AT_HOME, "tpsPersons.weightWorkAtHome");
        map0.put(ParamValue.TIME_BUDGET_E, "tpsPersons.timeBudgetE");
        map0.put(ParamValue.TIME_BUDGET_F, "tpsPersons.timeBudgetF");
        map0.put(ParamValue.TIME_BUDGET_WP, "tpsPersons.timeBudgetWP");
        map0.put(ParamValue.FINANCE_BUDGET_E, "tpsPersons.financeBudgetE");
        map0.put(ParamValue.FINANCE_BUDGET_F, "tpsPersons.financeBudgetF");
        map0.put(ParamValue.FINANCE_BUDGET_WP, "tpsPersons.financeBudgetWP");
        map0.put(ParamValue.GAMMA_LOCATION_WEIGHT, "tpsSelectLocations.gammaLocationWeight");
        map0.put(ParamValue.LOC_CHOICE_MOD_CFN4, "tpsSelectLocations.ModCfn4");
//		map0.put(ParamValue.MNL1_L_BETA_TIME, "tpsModeDistribution.betaTimeNoBudget");
//		map0
//				.put(ParamValue.MNL1_L_BETA_DISTANCE_FUEL_COSTS_MIT,
//						"tpsModeDistribution.betaDistanceFuelCostsMIVNoBudget");
//		map0.put(ParamValue.MNL1_L_BETA_DISTANCE_VARIABLE_COSTS,
//				"tpsModeDistribution.betaDistanceVariableCostsNoBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_TIME_PC, "tpsModeDistribution.betaTimeMIVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_TIME_PASS, "tpsModeDistribution.betaTimeMIV_PASSBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_TIME_PT, "tpsModeDistribution.betaTimeOEVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_TIME_SQUARE_PC, "tpsModeDistribution.betaTimeSquareMIVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_TIME_SQUARE_PASS, "tpsModeDistribution.betaTimeSquareMIV_PASSBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_TIME_SQUARE_PT, "tpsModeDistribution.betaTimeSquareOEVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_PARKING_PC, "tpsModeDistribution.betaParkingMIVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_SQUARED_PARKING_PC, "tpsModeDistribution.betaParkingSquareMIVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_DISTANCE_FUEL_COSTS_MIT, "tpsModeDistribution.betaDistanceFuelCostsMIVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_DISTANCE_FUEL_COSTS_SQUARED_MIT,
//				"tpsModeDistribution.betaDistanceFuelSquareCostsMIVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_DISTANCE_COSTS_PT, "tpsModeDistribution.betaDistanceCostsOEVBudget");
//		map0.put(ParamValue.MNL2_NL_BETA_DISTANCE_COSTS_SQUARED_PT,
//				"tpsModeDistribution.betaDistanceCostsSquareOEVBudget");
        map0.put(ParamValue.TOLL_CAT_1_BASE, "tpsTVZElement.BasisTollCat1");
        map0.put(ParamValue.TOLL_CAT_1, "tpsTVZElement.SzenTollCat1");
        map0.put(ParamValue.TOLL_CAT_2_BASE, "tpsTVZElement.BasisTollCat2");
        map0.put(ParamValue.TOLL_CAT_2, "tpsTVZElement.SzenTollCat2");
        map0.put(ParamValue.TOLL_CAT_3_BASE, "tpsTVZElement.BasisTollCat3");
        map0.put(ParamValue.TOLL_CAT_3, "tpsTVZElement.SzenTollCat3");
        map0.put(ParamValue.PARKING_FEE_CAT_1_BASE, "tpsTVZElement.BasisParkingCat1");
        map0.put(ParamValue.PARKING_FEE_CAT_1, "tpsTVZElement.SzenParkingCat1");
        map0.put(ParamValue.PARKING_FEE_CAT_2_BASE, "tpsTVZElement.BasisParkingCat2");
        map0.put(ParamValue.PARKING_FEE_CAT_2, "tpsTVZElement.SzenParkingCat2");
        map0.put(ParamValue.PARKING_FEE_CAT_3_BASE, "tpsTVZElement.BasisParkingCat3");
        map0.put(ParamValue.PARKING_FEE_CAT_3, "tpsTVZElement.SzenParkingCat3");
        map0.put(ParamValue.MIT_GASOLINE_COST_PER_KM_BASE, "tpsModeMIV.BasisFuelCostPerKilometer");
        map0.put(ParamValue.MIT_FUEL_COST_PER_KM_COMMUTE_BASE, "tpsModeMIV.BasisFuelCostPendlerPerKilometer");
        map0.put(ParamValue.MIT_GASOLINE_COST_PER_KM, "tpsModeMIV.SzenFuelCostPerKilometer");
        map0.put(ParamValue.MIT_FUEL_COST_PER_KM_COMMUTE, "tpsModeMIV.SzenFuelCostPendlerPerKilometer");
        map0.put(ParamValue.MIT_VARIABLE_COST_PER_KM_BASE, "tpsModeMIV.BasisVariableCostPerKilometer");
        map0.put(ParamValue.MIT_VARIABLE_COST_PER_KM, "tpsModeMIV.SzenVariableCostPerKilometer");
        map0.put(ParamValue.PT_COST_PER_KM_BASE, "tpsModeOEV.BasisCostPerKilometer");
        map0.put(ParamValue.PT_COST_PER_KM, "tpsModeOEV.SzenCostPerKilometer");
        map0.put(ParamValue.TRAIN_COST_PER_KM_BASE, "tpsModeTrain.BasisCostPerKilometer");
        map0.put(ParamValue.TRAIN_COST_PER_KM, "tpsModeTrain.SzenCostPerKilometer");
        map0.put(ParamValue.TAXI_COST_PER_KM_BASE, "tpsModeTAXI.BasisCostPerKilometer");
        map0.put(ParamValue.TAXI_COST_PER_KM, "tpsModeTAXI.SzenCostPerKilometer");
        map0.put(ParamValue.DEFAULT_VOT, "tpsVOT.defaultVOT");
        map0.put(ParamValue.RANDOM_SEED_NUMBER, "tpsMain.randomSeed");

        this.map = new HashMap<>();
        for (Enum<?> e : map0.keySet()) {
            this.map.put(map0.get(e), e);
        }

    }

    static File chooseFile(File path, String title, int type) {
        JFileChooser fd = createDialog(path, title, type, false);
        return fd.getSelectedFile();
    }

    static File chooseFileToSave(File path, String title, int type, ExtensionFilter... fileFilter) {
        JFileChooser fd = createDialog(path, title, type, false, true, fileFilter);
        ExtensionFilter fileFilterElement = (ExtensionFilter) fd.getFileFilter();
        String extension = fileFilterElement.getExtension();

        File selectedFile = fd.getSelectedFile();
        if (selectedFile != null) if (!selectedFile.toString().endsWith(extension)) {
            selectedFile = new File(selectedFile.toString() + extension);
        }
        return selectedFile;
    }

    static File[] chooseFiles(File path, String title, int type) {
        JFileChooser fd = createDialog(path, title, type, true);
        return fd.getSelectedFiles();
    }

    private static JFileChooser createDialog(File path, String title, int type, boolean multisel) {
        JFileChooser fd = new JFileChooser();
        if (path.exists()) fd.setCurrentDirectory(path);
        fd.setFileSelectionMode(type);
        fd.setDialogTitle(title);
        fd.setMultiSelectionEnabled(multisel);
        fd.setVisible(true);
        fd.showOpenDialog(null);
        return fd;
    }

    private static JFileChooser createDialog(File path, String title, int type, boolean multisel, boolean save, javax.swing.filechooser.FileFilter... fileFilter) {
        JFileChooser fd = new JFileChooser();
        if (path.exists()) fd.setCurrentDirectory(path);
        fd.setFileSelectionMode(type);
        fd.setDialogTitle(title);
        fd.setMultiSelectionEnabled(multisel);
        fd.setVisible(true);
        if (fileFilter != null) for (javax.swing.filechooser.FileFilter fileFilterElement : fileFilter) {
            fd.addChoosableFileFilter(fileFilterElement);
        }
        if (save) {
            fd.showSaveDialog(null);
        } else {
            fd.showOpenDialog(null);
        }
        return fd;
    }

    /**
     * Main routine for this converter
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            System.err.println(
                    "Unable to set specific look and feel: com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            e.printStackTrace();
        }

        File path = new File("T:/TAPAS");
        TPS_ParameterClass parameterClass = new TPS_ParameterClass();

        File[] files = chooseFiles(path, "Choose config files to convert", JFileChooser.FILES_ONLY);
        if (files == null || files.length == 0) return;

        File def = chooseFile(new File(path, "Simulationen"), "Choose default config file", JFileChooser.FILES_ONLY);
        if (def == null) return;

        try {
            parameterClass.loadSingleParameterFile(def);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        File folder = chooseFile(path, "Choose directory for converted files", JFileChooser.DIRECTORIES_ONLY);
        if (folder == null) return;

        TPS_ParameterFileConverter converter = new TPS_ParameterFileConverter();
        for (File file : files) {
            System.out.println("convert " + file.getPath() + " into " + folder.getPath());
            converter.convert(file, folder, parameterClass);
        }
    }

    /**
     * File converter (obsolete?!)
     *
     * @param oldFile
     * @param newPath
     */
    private void convert(File oldFile, File newPath, TPS_ParameterClass parameterClass) {
        BufferedReader breader = null;
        HSSFRow row = null;
        HSSFCell cell = null;
        HSSFWorkbook wb = null;

        String name = oldFile.getName();
        name = name.substring(0, name.lastIndexOf('.'));

        wb = new HSSFWorkbook();
        CellStyle[] cellStyles = new CellStyle[header.length];
        for (int i = 0; i < header.length; i++) {
            CellStyle style = wb.createCellStyle();
            style.setFillForegroundColor(indexedColors[i].getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            cellStyles[i] = style;
        }

        HSSFSheet[] sheets = new HSSFSheet[prefixes.length];
        int[] rows = new int[prefixes.length];
        Arrays.fill(rows, 0);
        Font font = wb.createFont();
        font.setBold(true);

        for (int i = 0; i < sheets.length; i++) {
            sheets[i] = wb.createSheet(prefixes[i] + name);
            row = sheets[i].createRow(rows[i]++);
            for (int j = 0; j < header.length; j++) {
                cell = row.createCell(j);
                cell.setCellValue(header[j]);
                CellStyle cellStyle = wb.createCellStyle();
                cellStyle.cloneStyleFrom(cellStyles[j]);
                cellStyle.setFont(font);
                cell.setCellStyle(cellStyle);
            }
        }

        ParamString[] params = new ParamString[]{ParamString.FILE_DATABASE_PROPERTIES, ParamString.FILE_PARAMETER_PROPERTIES, ParamString.FILE_LOGGING_PROPERTIES};

        for (int i = 0; i < params.length; i++) {
            row = sheets[0].createRow(rows[0]++);
            cell = row.createCell(0);
            cell.setCellValue(params[i].name());
            cell.setCellStyle(cellStyles[0]);
            cell = row.createCell(1);
            cell.setCellValue(prefixes[i + 1] + name + ".csv");
            cell.setCellStyle(cellStyles[1]);
        }

        row = sheets[0].createRow(rows[0]++);
        cell = row.createCell(0);
        cell.setCellValue(ParamString.CLASS_DATA_SCOURCE_ORIGIN.name());
        cell.setCellStyle(cellStyles[0]);
        cell = row.createCell(1);
        cell.setCellValue(TPS_DB_IOManager.class.getName());
        cell.setCellStyle(cellStyles[1]);

        try {
            breader = new BufferedReader(new FileReader(oldFile));
            String[] entry = new String[header.length];
            Enum<?> e;
            ParamType pt = null;
            boolean defined = false;
            String definition = null;

            for (String line = breader.readLine(); line != null; line = breader.readLine()) {
                if (line.startsWith("#")) {
                    line = line.replaceAll("#", "");
                    line = line.replaceAll("\n", " ");
                    line = line.replaceAll("\t", " ");
                    int startIndex = 0;
                    while (startIndex < line.length() && line.charAt(startIndex) == ' ') {
                        startIndex++;
                    }
                    if (startIndex < line.length()) entry[2] += line.substring(startIndex) + " ";
                } else if (line.length() > 2) {
                    StringTokenizer st = new StringTokenizer(line, " =,");
                    if (st.countTokens() < 2) {
                        System.err.println("Skipped [no value] in " + oldFile.getName() + ": " + line);
                        entry[2] = "";
                        continue;
                    }
                    entry[0] = st.nextToken();
                    entry[1] = st.nextToken();
                    e = this.map.get(entry[0]);

                    if (e == null) {
                        System.err.println("Skipped [deprecated] in " + oldFile.getName() + ": " + line);
                        entry[2] = "";
                        continue;
                    }

                    if (e instanceof ParamFlag) {
                        ParamFlag pf = (ParamFlag) e;
                        pt = parameterClass.getType(pf);
                        if (defined = parameterClass.isDefined(pf)) definition = Boolean.toString(
                                parameterClass.isTrue(pf));
                    } else if (e instanceof ParamString) {
                        ParamString ps = (ParamString) e;
                        pt = parameterClass.getType(ps);
                        if (defined = parameterClass.isDefined(ps)) definition = parameterClass.getString(ps);
                    } else if (e instanceof ParamValue) {
                        ParamValue pv = (ParamValue) e;
                        pt = parameterClass.getType(pv);
                        if (defined = parameterClass.isDefined(pv)) definition = Double.toString(
                                parameterClass.getDoubleValue(pv));
                    } else {
                        System.err.println("Skipped [???] in " + oldFile.getName() + ": " + line);
                        entry[2] = "";
                        continue;
                    }

                    row = sheets[pt.getIndex()].createRow(rows[pt.getIndex()]++);

                    if (defined) {
                        String text =
                                "Parameter " + e.name() + " is already defined with the default value '" + definition +
                                        "'. Would you like to overwrite this parameter with the new value '" +
                                        entry[1] + "'?";
                        int option = JOptionPane.showConfirmDialog(null, text,
                                "Confirm overwrite default parameter value", JOptionPane.YES_NO_OPTION);
                        if (option == JOptionPane.NO_OPTION) {
                            entry[1] = definition;
                        }
                    }

                    for (int i = 0; i < entry.length; i++) {
                        cell = row.createCell(i);
                        cell.setCellValue(entry[i]);
                        cell.setCellStyle(cellStyles[i]);
                    }

                    entry[2] = "";
                }
            }

            breader.close();

            wb.write(new FileOutputStream(new File(newPath, name + ".xls")));
            wb.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    /***
     *
     * @param file
     * @param parameterClass parameter class container reference
     */
    public void load(File file, TPS_ParameterClass parameterClass) {
        try {
            BufferedReader breader = new BufferedReader(new FileReader(file));
            String value, key;
            Enum<?> e;

            for (String line = breader.readLine(); line != null; line = breader.readLine()) {
                if (!line.startsWith("#") && line.length() > 2) {
                    StringTokenizer st = new StringTokenizer(line, " =,");
                    if (st.countTokens() < 2) {
                        System.err.println("Skipped [no value] in " + file.getName() + ": " + line);
                        continue;
                    }
                    key = st.nextToken();
                    value = st.nextToken();
                    e = this.map.get(key);

                    if (e == null) {
                        System.err.println("Skipped [deprecated] in " + file.getName() + ": " + line);
                        continue;
                    }

                    if (e instanceof ParamFlag) {
                        ParamFlag pf = (ParamFlag) e;
                        parameterClass.setFlag(pf, Boolean.parseBoolean(value));
                    } else if (e instanceof ParamString) {
                        ParamString ps = (ParamString) e;
                        parameterClass.setString(ps, value);
                    } else if (e instanceof ParamValue) {
                        ParamValue pv = (ParamValue) e;
                        parameterClass.setValue(pv, Double.parseDouble(value));
                    } else {
                        System.err.println("Skipped [???] in " + file.getName() + ": " + line);
                    }
                }
            }
            breader.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void save(File file, TPS_ParameterClass parameterClass, String... parameters) {
        HSSFRow row = null;
        HSSFCell cell = null;
        HSSFWorkbook wb = null;

        String name = file.getName();
        int start = name.startsWith("run_") ? 4 : 0;
        name = name.substring(start, name.lastIndexOf('.'));

        wb = new HSSFWorkbook();

        CellStyle[] cellStyles = new CellStyle[header.length];
        for (int i = 0; i < header.length; i++) {
            CellStyle style = wb.createCellStyle();
            style.setFillForegroundColor(indexedColors[i].getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            cellStyles[i] = style;
        }

        HSSFSheet[] sheets = new HSSFSheet[prefixes.length];
        int[] rows = new int[prefixes.length];
        Arrays.fill(rows, 0);
        Font font = wb.createFont();
        font.setBold(true);

        for (int i = 0; i < sheets.length; i++) {
            sheets[i] = wb.createSheet(prefixes[i] + name);
            row = sheets[i].createRow(rows[i]++);
            for (int j = 0; j < header.length; j++) {
                cell = row.createCell(j);
                cell.setCellValue(header[j]);
                CellStyle cellStyle = wb.createCellStyle();
                cellStyle.cloneStyleFrom(cellStyles[j]);
                cellStyle.setFont(font);
                cell.setCellStyle(cellStyle);
            }
        }

        for (int i = 0; i < parameters.length / 3; i++) {
            row = sheets[0].createRow(rows[0]++);
            for (int j = 0; j < 3; j++) {
                cell = row.createCell(j);
                cell.setCellValue(parameters[3 * i + j]);
                cell.setCellStyle(cellStyles[j]);
            }
        }

        ParamString[] params = new ParamString[]{ParamString.FILE_DATABASE_PROPERTIES, ParamString.FILE_PARAMETER_PROPERTIES, ParamString.FILE_LOGGING_PROPERTIES};

        for (int i = 0; i < params.length; i++) {
            row = sheets[0].createRow(rows[0]++);
            cell = row.createCell(0);
            cell.setCellValue(params[i].name());
            cell.setCellStyle(cellStyles[0]);
            cell = row.createCell(1);
            cell.setCellValue(prefixes[i + 1] + name + ".csv");
            cell.setCellStyle(cellStyles[1]);
            cell = row.createCell(2);
            cell.setCellValue("");
            cell.setCellStyle(cellStyles[2]);
        }

        try {
            Map<ParamType, List<String[]>> list = new HashMap<>();
            list.put(ParamType.DB, new LinkedList<>());
            list.put(ParamType.RUN, new LinkedList<>());
            list.put(ParamType.FILE, new LinkedList<>());
            list.put(ParamType.LOG, new LinkedList<>());
            list.put(ParamType.DEFAULT, new LinkedList<>());

            for (ParamFlag pf : ParamFlag.values()) {
                ParamType pt = parameterClass.getType(pf);
                if (list.containsKey(pt) && parameterClass.isDefined(pf)) {
                    String[] entry = new String[header.length];
                    entry[0] = pf.name();
                    entry[1] = Boolean.toString(parameterClass.isTrue(pf));
                    entry[2] = "";
                    list.get(pt).add(entry);
                }
            }
            for (ParamString ps : ParamString.values()) {
                ParamType pt = parameterClass.getType(ps);
                if (list.containsKey(pt) && parameterClass.isDefined(ps)) {
                    String[] entry = new String[header.length];
                    entry[0] = ps.name();
                    entry[1] = parameterClass.getString(ps);
                    entry[2] = "";
                    list.get(pt).add(entry);
                }
            }
            for (ParamValue pv : ParamValue.values()) {
                ParamType pt = parameterClass.getType(pv);
                if (list.containsKey(pt) && parameterClass.isDefined(pv)) {
                    String[] entry = new String[header.length];
                    entry[0] = pv.name();
                    entry[1] = Double.toString(parameterClass.getDoubleValue(pv));
                    entry[2] = "";
                    list.get(pt).add(entry);
                }
            }

            for (ParamType pt : list.keySet()) {
                for (String[] entry : list.get(pt)) {
                    row = sheets[pt.getIndex()].createRow(rows[pt.getIndex()]++);

                    for (int i = 0; i < header.length; i++) {
                        cell = row.createCell(i);
                        cell.setCellValue(entry[i]);
                        cell.setCellStyle(cellStyles[i]);
                    }
                }
            }
            FileOutputStream fos = new FileOutputStream(file);
            wb.write(fos);
            fos.close();
            wb.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Scan for txt files in a directory or checks if given file is a txt file
     *
     * @param oldPath
     * @param newPath
     */
    public void scan(File oldPath, File newPath, TPS_ParameterClass parameterClass) {
        File dir = null;
        for (File file : oldPath.listFiles()) {
            if (file.isDirectory()) {
                dir = new File(newPath, file.getName());
                if (!dir.exists()) dir.mkdir();
                this.scan(file, dir, parameterClass);
            } else if (file.getName().endsWith(".txt")) {
                this.convert(file, newPath, parameterClass);
                System.out.println();
            }
        }
    }
}
