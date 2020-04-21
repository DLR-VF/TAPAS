package de.dlr.ivf.tapas.tools.persitence.db.dbImport;

import static de.dlr.ivf.tapas.tools.persitence.db.dbImport.DBImportTableModel.COL_ACC;
import static de.dlr.ivf.tapas.tools.persitence.db.dbImport.DBImportTableModel.COL_F_NAME;
import static de.dlr.ivf.tapas.tools.persitence.db.dbImport.DBImportTableModel.COL_ID;
import static de.dlr.ivf.tapas.tools.persitence.db.dbImport.DBImportTableModel.COL_T_KEY;
import static de.dlr.ivf.tapas.tools.persitence.db.dbImport.DBImportTableModel.COL_T_NAME;

import java.awt.Cursor;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JTable;

import de.dlr.ivf.tapas.persistence.db.TPS_DB_Connector;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IOManager;
import de.dlr.ivf.tapas.util.parameters.ParamString;

public class DBImportController {

	enum DBImportMethod {
		/**
		 * block
		 */
		BLK,
		/**
		 * normal csv
		 */
		CSV,
		/**
		 * episodes
		 */
		EPS,
		/**
		 * fees and tolls
		 */
		FAT,
		/**
		 * household
		 */
		HHD,
		/**
		 * location
		 */
		LOC,
		/**
		 * matrices (tt, distance)
		 */
		MAT,
		/**
		 * mode choice tree
		 */
		MCT,
		/**
		 * next pt stop
		 */
		NPT,
		/**
		 * person
		 */
		PER,
		/**
		 * scheme class distribution
		 */
		SCD,
		/**
		 * scores (block and taz)
		 */
		SCO,
		/**
		 * scheme class time usage
		 */
		SCT,
		/**
		 * taz
		 */
		TAZ,
		/**
		 * intra taz infos pt
		 */
		TIP,
		/**
		 * intra taz infos MIT
		 */
		TIM,
		/**
		 * values of time
		 */
		VOT
	}

    static final Map<ParamString, List<ParamString>> DB_2_FILE_MAP;

	static final Set<ParamString> DB_IDENTIFIABLE_TABLES;

	static final List<List<ParamString>> DB_HIERARCHY;

	static final List<DBImportMethod> DB_IMPORT_METHOD;

	static final Set<ParamString> DB_REGION_BASED;

	static {
		DB_REGION_BASED = new HashSet<>();
		DB_REGION_BASED.add(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS);
		DB_REGION_BASED.add(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS);
		DB_REGION_BASED.add(ParamString.DB_TABLE_TAZ_SCORES);
		DB_REGION_BASED.add(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP);
		DB_REGION_BASED.add(ParamString.DB_TABLE_BLOCK_SCORES);
		DB_REGION_BASED.add(ParamString.DB_TABLE_BLOCK);
		DB_REGION_BASED.add(ParamString.DB_TABLE_CFN4);
		DB_REGION_BASED.add(ParamString.DB_TABLE_CFN4_IND);
		DB_REGION_BASED.add(ParamString.DB_TABLE_CFNX);
		DB_REGION_BASED.add(ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION);
		DB_REGION_BASED.add(ParamString.DB_TABLE_HOUSEHOLD);
		DB_REGION_BASED.add(ParamString.DB_TABLE_LOCATION);
		DB_REGION_BASED.add(ParamString.DB_TABLE_MATRICES);
		DB_REGION_BASED.add(ParamString.DB_TABLE_PERSON);
		DB_REGION_BASED.add(ParamString.DB_TABLE_TAZ);
		DB_REGION_BASED.add(ParamString.DB_TABLE_TAZ_FEES_TOLLS);

		DB_2_FILE_MAP = new TreeMap<>(new Comparator<ParamString>() {

			public int compare(ParamString o1, ParamString o2) {
				return o1.name().compareTo(o2.name());
			}

		});

		// Declare, which tables need which file sources

//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_ACTIVITY, getList(FILE_CONSTANT_ACTIVITY));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_AGE, getList(FILE_CONSTANT_AGE));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_CARS, getList(FILE_CONSTANT_CARS));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_DISTANCE, getList(FILE_CONSTANT_DISTANCE));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION,
//				getList(FILE_CONSTANT_DRIVING_LICENSE_INFORMATION));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_INCOME, getList(FILE_CONSTANT_INCOME));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_HOUSEHOLD, getList(FILE_CONSTANT_HOUSEHOLD));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_LOCATION, getList(FILE_CONSTANT_LOCATION));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_MODE, getList(FILE_CONSTANT_MODE));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_SETTLEMENT, getList(FILE_CONSTANT_SETTLEMENT));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_SEX, getList(FILE_CONSTANT_SEX));
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_PERSON, getList(FILE_CONSTANT_PERSON));
//
//		DB_2_FILE_MAP.put(DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION, getList(FILE_CONSTANT_ACTIVITY_2_LOCATION));
//
//		DB_2_FILE_MAP.put(DB_TABLE_VOT, getList(FILE_VOTS));
//
//		DB_2_FILE_MAP.put(DB_TABLE_CFNX, getList(FILE_PARAMETER_CFNX));
//		DB_2_FILE_MAP.put(DB_TABLE_CFN4, getList(FILE_PARAMETER_CFN4));
//		DB_2_FILE_MAP.put(DB_TABLE_CFN4_IND, getList(FILE_PARAMETER_CFN4_INDEXED));
//
//		DB_2_FILE_MAP.put(DB_TABLE_EPISODE, getList(FILE_EPISODES));
//		DB_2_FILE_MAP.put(DB_TABLE_SCHEME_CLASS_DISTRIBUTION, getList(FILE_TABLE_DIARY_DISTRIBUTION));
//		DB_2_FILE_MAP.put(DB_TABLE_SCHEME_CLASS, getList(FILE_DIARY_CLASSES_TIMEBUDGETS));
//
//		DB_2_FILE_MAP.put(DB_TABLE_TAZ, getList(FILE_TAZ));
//		DB_2_FILE_MAP.put(DB_TABLE_TAZ_FEES_TOLLS, getList(FILE_TAZ_FEES_TOLLS));
//		DB_2_FILE_MAP.put(DB_TABLE_TAZ_SCORES, getList(FILE_PT_SCORES_TVZ_BASE));
//		DB_2_FILE_MAP.put(DB_TABLE_TAZ_INTRA_PT_INFOS, getList(FILE_INTRA_TVZ_INFOS_PT, FILE_INTRA_TVZ_INFOS_PT_BASE));
//		DB_2_FILE_MAP.put(DB_TABLE_TAZ_INTRA_MIT_INFOS,
//				getList(FILE_INTRA_TVZ_INFOS_MIT, FILE_INTRA_TVZ_INFOS_MIT_BASE));
//
//		DB_2_FILE_MAP.put(DB_TABLE_MATRICES, getList(FILE_DISTANCES, FILE_TRAVEL_TIMES_MIT_BASE, FILE_TRAVEL_TIMES_MIT,
//				FILE_TRAVEL_TIMES_PT_BASE, FILE_TRAVEL_TIMES_PT, FILE_ACCESS_PT, FILE_EGRESS_PT, FILE_ACCESS_PT_BASE,
//				FILE_EGRESS_PT_BASE));
//
//		DB_2_FILE_MAP.put(DB_TABLE_MCT, getList(FILE_MODE_CHOICE_TREE));
//
//		DB_2_FILE_MAP.put(DB_TABLE_BLOCK, getList(FILE_BLK));
//		DB_2_FILE_MAP.put(DB_TABLE_BLOCK_NEXT_PT_STOP, getList(FILE_NEAREST_PT_STOP));
//		DB_2_FILE_MAP.put(DB_TABLE_BLOCK_SCORES, getList(FILE_PT_SCORES_BLK_BASE));
//
//		DB_2_FILE_MAP.put(DB_TABLE_LOCATION, getList(FILE_LOCATIONS));
//
//		DB_2_FILE_MAP.put(DB_TABLE_PERSON, getList(FILE_PERSON_DATA));
//
//		DB_2_FILE_MAP.put(DB_TABLE_HOUSEHOLD, getList(FILE_HOUSEHOLD_DATA));

		DB_HIERARCHY = new LinkedList<>();
		DB_IMPORT_METHOD = new LinkedList<>();

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_CONSTANT_ACTIVITY, ParamString.DB_TABLE_CONSTANT_AGE, ParamString.DB_TABLE_CONSTANT_CARS,
				ParamString.DB_TABLE_CONSTANT_DISTANCE, ParamString.DB_TABLE_CONSTANT_DRIVING_LICENSE_INFORMATION, ParamString.DB_TABLE_CONSTANT_INCOME,
				ParamString.DB_TABLE_CONSTANT_HOUSEHOLD, ParamString.DB_TABLE_CONSTANT_LOCATION, ParamString.DB_TABLE_CONSTANT_MODE,
				ParamString.DB_TABLE_CONSTANT_SETTLEMENT, ParamString.DB_TABLE_CONSTANT_SEX, ParamString.DB_TABLE_CONSTANT_PERSON,
				ParamString.DB_TABLE_CONSTANT_ACTIVITY_2_LOCATION, ParamString.DB_TABLE_CFN4, ParamString.DB_TABLE_CFN4_IND, ParamString.DB_TABLE_CFNX));
		DB_IMPORT_METHOD.add(DBImportMethod.CSV);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_EPISODE));
		DB_IMPORT_METHOD.add(DBImportMethod.EPS);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_VOT));
		DB_IMPORT_METHOD.add(DBImportMethod.VOT);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_SCHEME_CLASS_DISTRIBUTION));
		DB_IMPORT_METHOD.add(DBImportMethod.SCD);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_SCHEME_CLASS));
		DB_IMPORT_METHOD.add(DBImportMethod.SCT);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_TAZ));
		DB_IMPORT_METHOD.add(DBImportMethod.TAZ);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_TAZ_FEES_TOLLS));
		DB_IMPORT_METHOD.add(DBImportMethod.FAT);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_TAZ_SCORES));
		DB_IMPORT_METHOD.add(DBImportMethod.SCO);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS));
		DB_IMPORT_METHOD.add(DBImportMethod.TIM);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_TAZ_INTRA_PT_INFOS));
		DB_IMPORT_METHOD.add(DBImportMethod.TIP);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_MATRICES));
		DB_IMPORT_METHOD.add(DBImportMethod.MAT);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_MCT));
		DB_IMPORT_METHOD.add(DBImportMethod.MCT);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_BLOCK));
		DB_IMPORT_METHOD.add(DBImportMethod.BLK);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_BLOCK_SCORES));
		DB_IMPORT_METHOD.add(DBImportMethod.SCO);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP));
		DB_IMPORT_METHOD.add(DBImportMethod.NPT);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_LOCATION));
		DB_IMPORT_METHOD.add(DBImportMethod.LOC);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_HOUSEHOLD));
		DB_IMPORT_METHOD.add(DBImportMethod.HHD);

		DB_HIERARCHY.add(getList(ParamString.DB_TABLE_PERSON));
		DB_IMPORT_METHOD.add(DBImportMethod.PER);

		DB_IDENTIFIABLE_TABLES = new HashSet<>();
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_BLOCK_NEXT_PT_STOP);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_BLOCK_SCORES);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_HOUSEHOLD);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_MATRICES);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_PERSON);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_TAZ_FEES_TOLLS);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_TAZ_INTRA_MIT_INFOS);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_TAZ_SCORES);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_MCT);
		DB_IDENTIFIABLE_TABLES.add(ParamString.DB_TABLE_VOT);
	}

	private static List<ParamString> getList(ParamString... fileKeyArray) {
		return new LinkedList<>(Arrays.asList(fileKeyArray));
	}

	private DBImportGUI control;

	public DBImportController(DBImportGUI control) {
		this.control = control;
	}

	public void dbimport(final JTable fileTable) {
		final ExecutorService es = Executors.newFixedThreadPool(1);

		Callable<Boolean> callable = new Callable<Boolean>() {
			public Boolean call() {

				boolean success = false;
				control.print("Creation of Tables and Loading Table Contents");
				control.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				try {
					TPS_DB_Connector db = new TPS_DB_Connector("daniel", "daniel", control.getParameters());


					control.getParameters().setString(ParamString.DB_SCHEMA_CORE, control.getSchema() + ".");
					db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "create_region_based_tables", control.getRegion(), control.getSchema()),this);

					db.execute("SET CONSTRAINTS ALL DEFERRED", this);

					DBImportMethod method;
					List<ParamString> dbKeyList;
					String fileName;
					String tableName;
					String name;

					for (int i = 0; i < DB_IMPORT_METHOD.size(); i++) {
						method = DB_IMPORT_METHOD.get(i);
						dbKeyList = DB_HIERARCHY.get(i);
						for (ParamString dbKey : dbKeyList) {
							for (int j = 0; j < fileTable.getRowCount(); j++) {
								if (!fileTable.getValueAt(j, COL_T_KEY).equals(dbKey.name()) || method == null
										|| Boolean.FALSE.equals(fileTable.getValueAt(j, COL_ACC))) {
									continue;
								}

								File tempFile = new File(control.getParameters().getString(ParamString.PATH_ABS_DB), control.getParameters().INPUT_DIR);
								fileName = new File(tempFile, fileTable.getValueAt(j, COL_F_NAME).toString()).getPath()
										.replace('\\', '/');
								tableName = fileTable.getValueAt(j, COL_T_NAME).toString();
								name = fileTable.getValueAt(j, COL_ID).toString();

								control.print("IMPORT " + fileName + " INTO " + tableName + " WITH NAME " + name);

								switch (method) {
								case CSV:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_csvfile_to_table", fileName, tableName),this);
									break;
								case EPS:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_episodes", fileName),this);
									break;
								case VOT:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_values_of_time", fileName, name),this);
									break;
								case SCD:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_scheme_class_distributions", fileName, name), this);
									break;
								case SCT:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_scheme_class_time_usage", fileName),this);
									break;
								// case TAZ:
								// TPS_DBIOManager.execute(st, "load_taz", fileName, tableName);
								// break;
								case TIM:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_taz_intra_mit_infos", fileName, name, tableName), this);
									break;
								case TIP:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_taz_intra_pt_infos", fileName, name, tableName),this);
									break;
								case MAT:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_matrix", fileName, name, tableName),this);
									break;
								case MCT:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_mode_choice_tree", fileName, name),this);
									break;
								// case BLK:
								// TPS_DBIOManager.execute(st, "load_blocks", fileName, tableName);
								// break;
								// case LOC:
								// TPS_DBIOManager.execute(st, "load_locations", fileName, tableName);
								// break;
								case PER:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_persons", fileName, tableName, name),this);
									break;
								case HHD:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_households", fileName, tableName, name),this);
									break;
								case FAT:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_taz_fees_tolls", fileName, name, tableName),this);
									break;
								case SCO:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_scores", fileName, name, tableName),this);
									break;
								case NPT:
									db.execute(TPS_DB_IOManager.buildQuery(control.getParameters(), "load_next_pt_stop", fileName, name, tableName),this);
									break;
								default:
									//nothing
								}
							}
						}
					}
					db.closeConnection(this);
					success = true;
				} catch (Exception e) {
					control.handleException("db import", e);
				} finally {
					control.print("Database Closed\n");
					control.activateStart();
					control.resetCursor();
					es.shutdown();
					control.repaint();
				}

				return success;
			}
		};

		es.submit(callable);
	}

	int getIntFromString(String input) {
		int outInt = 0;

		StringTokenizer strTok = new StringTokenizer(input, "_");

		while (strTok.hasMoreElements()) {
			try {
				outInt = Integer.parseInt(strTok.nextToken());
				break;
			} catch (NumberFormatException e) {
				// nothing to do here
			}

		}

		return outInt;
	}
}