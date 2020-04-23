package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer;

import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import jxl.JXLException;
import jxl.Workbook;
import jxl.write.Number;
import jxl.write.*;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("rawtypes")
public class LegacyExcelWriter {

    private final RegionAnalyzer regionAnalyzer;
    private final WritableWorkbook workbook;

    private final int xOffset = 0;
    private final int yOffset = 0;

    /**
     * @param filename
     * @param regionAnalyzer
     * @throws IOException if the Excel workbook could not be initialized. Handle retry!
     */
    public LegacyExcelWriter(String filename, RegionAnalyzer regionAnalyzer) throws IOException {
        this.regionAnalyzer = regionAnalyzer;

        workbook = Workbook.createWorkbook(new File(filename + File.separatorChar + "LegacyTuM.xls"));

    }

    private void addCNF4Config(int x, int y, WritableSheet writableSheet) throws WriteException {
        x++;
        writableSheet.addCell(new Label(x, y++, "Standardwert cfn4"));
        writableSheet.addCell(new Label(x, y++, "Arbeit"));
        writableSheet.addCell(new Label(x, y++, "Schule"));
        writableSheet.addCell(new Label(x, y++, "Studium"));
        writableSheet.addCell(new Label(x, y++, "Shopping"));
        writableSheet.addCell(new Label(x, y++, "Erledigungen"));
        writableSheet.addCell(new Label(x, y++, "Freizeit"));
        writableSheet.addCell(new Label(x, y++, "Sonstiges"));
        writableSheet.addCell(new Label(x, y++, "5Min-Trips"));
    }

    private void addDistanceClasses(int x, int y, WritableSheet sheet) throws WriteException {
        sheet.addCell(new Label(x, y++, "< 1km"));
        sheet.addCell(new Label(x, y++, "1-3 km"));
        sheet.addCell(new Label(x, y++, "3-5 km"));
        sheet.addCell(new Label(x, y++, "5-7 km"));
        sheet.addCell(new Label(x, y++, "7-10 km"));
        sheet.addCell(new Label(x, y++, "10-25 km"));
        sheet.addCell(new Label(x, y++, "25-100 km"));
        sheet.addCell(new Label(x, y++, ">100km"));
    }

    private void initFurtherValuesSheet(WritableSheet writableSheet) throws WriteException {
        int x = 0 + xOffset;
        int y = 1 + yOffset;
        writableSheet.addCell(new Label(x + 2, y++, "SOLL"));
        writableSheet.addCell(new Label(x, y++, "Region"));
        writableSheet.addCell(new Label(x, y++, "Laufdatum"));
        writableSheet.addCell(new Label(x, y++, "Name"));
        writableSheet.addCell(new Label(x, y++, "Personenzahl"));
        writableSheet.addCell(new Label(x, y++, "Anzahl der Trips"));
        writableSheet.addCell(new Label(x, y++, "Wegelängendifferenzierung verwendet"));
        writableSheet.addCell(new Label(x, y, "cnf4-Konfiguration"));
        addCNF4Config(x, y, writableSheet);
        y = y + 9;
        writableSheet.addCell(new Label(x, y++, "Anzahl trips - Dist = 0"));
        y++;
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 1"));
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 2"));
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 3"));
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 4"));
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 5"));
        y = y + 2;
        writableSheet.addCell(new Label(x + 4, y, "Anzahl Trips"));
        writableSheet.addCell(new Label(x, y++, "Durchschnittliche Weglängen in Metern"));
        for (TripIntention ti : TripIntention.values()) {
            writableSheet.addCell(new Label(x, y++, ti.getName()));
        }
        writableSheet.addCell(new Label(x, y++, "Alle Wegezwecke"));

        y = y + 2;
        writableSheet.addCell(new Label(x + 4, y, "Anzahl Trips"));
        writableSheet.addCell(new Label(x, y++, "Ergebnisse nach Wegezwecken in Prozent"));
        for (TripIntention zweck : TripIntention.values()) {
            writableSheet.addCell(new Label(x, y, zweck.getName()));
            addDistanceClasses(x + 1, y, writableSheet);
            y += 9;
        }
        writableSheet.addCell(new Label(x, y, "Alle Wegezwecke"));
        addDistanceClasses(x + 1, y, writableSheet);
    }

    private void initModalsplitSheet(WritableSheet writableSheet) throws WriteException {
        int x = 0 + xOffset;
        int y = 1 + yOffset;
        writableSheet.addCell(new Label(x, y++, "Region"));
        writableSheet.addCell(new Label(x, y++, "Laufdatum"));
        writableSheet.addCell(new Label(x, y++, "Nr"));
        y++;
        writableSheet.addCell(new Label(x, y++, "Modal Split"));
        for (Mode m : Mode.values()) {
            writableSheet.addCell(new Label(x, y++, m.getDescription()));
        }
        y++;
        writableSheet.addCell(new Label(x, y++, "Modal Split pro Wegelänge"));
        for (Mode m : Mode.values()) {
            for (DistanceCategory cat : DistanceCategory.values()) {
                writableSheet.addCell(new Label(x, y++,
                        m.getDescription() + " - " + regionAnalyzer.getDistanceCategoryDescription(cat)));
            }
            y++;
        }
        y++;
        writableSheet.addCell(new Label(x, y++, "Modal Split pro Wegelänge und Wegezweck"));
        for (Mode m : Mode.values()) {
            for (DistanceCategory cat : DistanceCategory.values()) {
                String catName = regionAnalyzer.getDistanceCategoryDescription(cat);
                for (TripIntention ti : TripIntention.values()) {
                    writableSheet.addCell(
                            new Label(x, y++, m.getDescription() + " - " + catName + " - " + ti.getCaption()));
                }
                // writableSheet.addCell(new Label(x, y++, m.getDescription()
                // + " - " + catName + " - " + "Alle anderen"));
                y++;
            }
            y++;
        }
    }

    private void initPersongroupSheet(WritableSheet writableSheet) throws WriteException {
        int x = 0 + xOffset;
        int y = 1 + yOffset;
        writableSheet.addCell(new Label(x, y++, "Region"));
        writableSheet.addCell(new Label(x, y++, "Laufdatum"));
        writableSheet.addCell(new Label(x, y++, "Nr"));
        for (PersonGroup pg : PersonGroup.values()) {
            writableSheet.addCell(new Label(x, y++, pg.name() + ": " + pg.getName()));
            writableSheet.addCell(new Label(x + 1, y++, "Wege"));
            writableSheet.addCell(new Label(x + 1, y++, "durchschn. Länge"));
        }

    }

    private void initWegelaengenSheet(WritableSheet writableSheet) throws WriteException {
        int x = 0 + xOffset;
        int y = 1 + yOffset;
        writableSheet.addCell(new Label(x + 2, y++, "SOLL"));
        writableSheet.addCell(new Label(x, y++, "Region"));
        writableSheet.addCell(new Label(x, y++, "Laufdatum"));
        writableSheet.addCell(new Label(x, y++, "Name"));
        writableSheet.addCell(new Label(x, y++, "Personenzahl"));
        writableSheet.addCell(new Label(x, y++, "Anzahl der Trips"));
        writableSheet.addCell(new Label(x, y++, "Wegelängendifferenzierung verwendet"));
        writableSheet.addCell(new Label(x, y, "cnf4-Konfiguration"));
        addCNF4Config(x, y, writableSheet);
        y = y + 9;
        writableSheet.addCell(new Label(x, y++, "Anzahl trips - Dist = 0"));
        y++;
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 1"));
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 2"));
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 3"));
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 4"));
        writableSheet.addCell(new Label(x, y++, "cnfX für Raumtyp 5"));
        y = y + 2;
        writableSheet.addCell(new Label(x, y++, "Durchschnittliche Weglängen in Metern"));

        for (TripIntention ti : TripIntention.values()) {
            writableSheet.addCell(new Label(x, y++, ti.getName()));
        }
        writableSheet.addCell(new Label(x, y++, "Alle Wegezwecke"));
        y = y + 2;
        writableSheet.addCell(new Label(x, y++, "Ergebnisse nach Wegezwecken in Prozent"));
        for (TripIntention ti : TripIntention.values()) {
            writableSheet.addCell(new Label(x, y, ti.getName()));
            addDistanceClasses(x + 1, y, writableSheet);
            y += 9;
        }
        writableSheet.addCell(new Label(x, y, "Alle Wegezwecke"));
        addDistanceClasses(x + 1, y, writableSheet);
    }

    private double saveDiv(double n, double d) {
        return d != 0 ? n / d : 0.0;
    }

    private void writeFurtherValuesData(RegionCode region, WritableSheet furtherValuesSheet) throws WriteException {
        int x = 4;
        int y = 27;

        WritableCellFormat dc2cell = new WritableCellFormat(NumberFormats.FLOAT);

        furtherValuesSheet.addCell(new Number(3 + xOffset, 5 + yOffset, regionAnalyzer.getCntPersons()));
        furtherValuesSheet.addCell(
                new Number(3 + xOffset, 6 + yOffset, regionAnalyzer.getCntTrips(Categories.RegionCode, region)));
        // TODO write region differentiation
        furtherValuesSheet.addCell(new Label(3 + xOffset, 7 + yOffset, "" + ""));

        // Durchschnittliche Wegelänge in Metern
        Categories[] catRcTi = {Categories.RegionCode, Categories.TripIntention};
        Enum[] insRcTi = {region, TripIntention.TRIP_31};

        for (TripIntention ti : TripIntention.values()) {
            insRcTi[1] = ti;
            furtherValuesSheet.addCell(
                    new Number(x + xOffset, y + yOffset, regionAnalyzer.getAvgDistByTrip(catRcTi, insRcTi), dc2cell));
            furtherValuesSheet.addCell(
                    new Number(x + 1 + xOffset, y + yOffset, regionAnalyzer.getCntTrips(catRcTi, insRcTi)));

            y++;
        }
        //
        furtherValuesSheet.addCell(
                new Number(x + xOffset, y + yOffset, regionAnalyzer.getAvgDistByTrip(Categories.RegionCode, region),
                        dc2cell));
        furtherValuesSheet.addCell(
                new Number(x + 1 + xOffset, y + yOffset, regionAnalyzer.getCntTrips(Categories.RegionCode, region)));

        y += 4;

        // Ergebnisse nach Wegezweck in %
        Categories[] catRcDcTi = {Categories.RegionCode, Categories.DistanceCategory, Categories.TripIntention};
        Enum[] insRcDcTi = {region, DistanceCategory.CAT_1, TripIntention.TRIP_31};

        for (TripIntention ti : TripIntention.values()) {
            insRcDcTi[2] = ti;
            insRcTi[1] = ti;
            long cntTi = regionAnalyzer.getCntTrips(catRcTi, insRcTi);
            for (DistanceCategory cat : DistanceCategory.values()) {
                insRcDcTi[1] = cat;
                long cntDcTi = regionAnalyzer.getCntTrips(catRcDcTi, insRcDcTi);
                furtherValuesSheet.addCell(
                        new Number(x + xOffset, y + yOffset, saveDiv(cntDcTi, cntTi) * 100.0, dc2cell));
                furtherValuesSheet.addCell(new Number(x + 1 + xOffset, y + yOffset, cntDcTi));
                y++;
            }
            y++;
        }

        // Alle Wegezwecke
        Categories[] catRcDc = {Categories.RegionCode, Categories.DistanceCategory};
        Enum[] insRcDc = {region, DistanceCategory.CAT_1};

        long cntRc = regionAnalyzer.getCntTrips(Categories.RegionCode, region);

        for (DistanceCategory cat : DistanceCategory.values()) {
            insRcDc[1] = cat;
            long cntRcDc = regionAnalyzer.getCntTrips(catRcDc, insRcDc);

            furtherValuesSheet.addCell(new Number(x + xOffset, y + yOffset, saveDiv(cntRcDc, cntRc) * 100.0, dc2cell));
            furtherValuesSheet.addCell(new Number(x + xOffset, y + yOffset, cntRcDc));
            y++;

        }

    }

    private void writeModalsplitData(RegionCode region, WritableSheet modalsplitSheet) throws WriteException {
        int x = 1;
        int y = 6;
        WritableCellFormat dc2cell = new WritableCellFormat(NumberFormats.FLOAT);

        // Modalsplit gesamt

        long cntRc = regionAnalyzer.getCntTrips(Categories.RegionCode, region);

        Categories[] catRcMo = {Categories.RegionCode, Categories.Mode};
        Enum[] instRcMo = {region, Mode.BIKE};

        for (Mode m : Mode.values()) {
            instRcMo[1] = m;
            long cntRcMo = regionAnalyzer.getCntTrips(catRcMo, instRcMo);
            modalsplitSheet.addCell(new Number(x + xOffset, y + yOffset, saveDiv(cntRcMo, cntRc) * 100, dc2cell));
            y++;
        }
        y += 2;
        // Modalsplit über Distanzkategorien (Nicht selektierte
        // Distanzkategorien werden in der nächst höheren zusamengefasst)
        //TODO fix order for Distance Categories

        Categories[] catRcMoDc = {Categories.RegionCode, Categories.Mode, Categories.DistanceCategory};
        Enum[] instRcMoDc = {region, Mode.BIKE, DistanceCategory.CAT_1};

        for (Mode mo : Mode.values()) {

            instRcMo[1] = mo;
            long cntRcMo = regionAnalyzer.getCntTrips(catRcMo, instRcMo);

            instRcMoDc[1] = mo;
            for (DistanceCategory dc : DistanceCategory.values()) {
                instRcMoDc[2] = dc;
                long cntRcMoDc = regionAnalyzer.getCntTrips(catRcMoDc, instRcMoDc);
                modalsplitSheet.addCell(
                        new Number(x + xOffset, y + yOffset, saveDiv(cntRcMoDc, cntRcMo) * 100, dc2cell));
                y++;
            }
            y++;
        }

        y += 2;
        // Modal split by distance and trip intention

        Categories[] catRcMoDcTi = {Categories.RegionCode, Categories.Mode, Categories.DistanceCategory, Categories.TripIntention};
        Enum[] instRcMoDcTi = {region, Mode.BIKE, DistanceCategory.CAT_1, TripIntention.TRIP_31};

        for (Mode mo : Mode.values()) {
            for (DistanceCategory dc : DistanceCategory.values()) {
                instRcMoDc[1] = mo;
                instRcMoDc[2] = dc;
                long cntRcMoDc = regionAnalyzer.getCntTrips(catRcMoDc, instRcMoDc);

                instRcMoDcTi[1] = mo;
                instRcMoDcTi[2] = dc;

                for (TripIntention ti : TripIntention.values()) {
                    instRcMoDcTi[3] = ti;
                    long cntRcMoDcTi = regionAnalyzer.getCntTrips(catRcMoDcTi, instRcMoDcTi);
                    modalsplitSheet.addCell(
                            new Number(x + xOffset, y + yOffset, saveDiv(cntRcMoDcTi, cntRcMoDc) * 100, dc2cell));
                    y++;
                }
                y++;
            }
            y++;
        }
    }

    private void writePersongroupData(RegionCode region, WritableSheet persongroupSheet) throws WriteException {
        int x = 2;
        int y = 5;
        WritableCellFormat dc2cell = new WritableCellFormat(NumberFormats.FLOAT);

        Categories[] catRcPg = {Categories.RegionCode, Categories.PersonGroup};
        Enum[] instRcPg = {region, PersonGroup.PG_1};
        for (PersonGroup pg : PersonGroup.values()) {
            // if (pg == PersonGroup.PG_12) {
            // y += 3;
            // continue;
            // }

            instRcPg[1] = pg;
            long cntRcPg = regionAnalyzer.getCntTrips(catRcPg, instRcPg);
            int nbP = regionAnalyzer.getCntPersons(pg);
            double distRcPg = regionAnalyzer.getDist(catRcPg, instRcPg);

            persongroupSheet.addCell(new Number(x, y, saveDiv(cntRcPg, nbP), dc2cell));
            persongroupSheet.addCell(new Number(x, y + 1, saveDiv(distRcPg, cntRcPg), dc2cell));

            y += 3;
        }
    }

    public void writeStatistics() {
        try {
            for (RegionCode region : RegionCode.values()) {
                if (regionAnalyzer.getCntTrips(Categories.RegionCode, region) > 0) {

                    WritableSheet wegelaengenSheet = workbook.createSheet(region.getName() + " Wegelängen", 0);
                    initWegelaengenSheet(wegelaengenSheet);
                    WritableSheet modalSplitSheet = workbook.createSheet(region.getName() + " Modalsplit", 1);
                    initModalsplitSheet(modalSplitSheet);
                    WritableSheet persongroupSheet = workbook.createSheet(region.getName() + " Personengruppen", 2);
                    initPersongroupSheet(persongroupSheet);
                    WritableSheet furtherValuesSheet = workbook.createSheet(region.getName() + " Weitere Werte", 3);
                    initFurtherValuesSheet(furtherValuesSheet);

                    writeWegelaengenData(region, wegelaengenSheet);
                    writeModalsplitData(region, modalSplitSheet);
                    writePersongroupData(region, persongroupSheet);
                    writeFurtherValuesData(region, furtherValuesSheet);

                }
            }
            workbook.write();
            workbook.close();

        } catch (JXLException | IOException e) {
            // TODO handle JXL exception
            e.printStackTrace();
        } // TODO handle output exception

    }

    private void writeWegelaengenData(RegionCode region, WritableSheet wegelaengenSheet) throws WriteException {
        int x = 3;
        int y = 27;
        WritableCellFormat dc2cell = new WritableCellFormat(NumberFormats.FLOAT);

        wegelaengenSheet.addCell(new Number(x + xOffset, 5 + yOffset, regionAnalyzer.getCntPersons()));
        wegelaengenSheet.addCell(
                new Number(x + xOffset, 6 + yOffset, regionAnalyzer.getCntTrips(Categories.RegionCode, region)));
        wegelaengenSheet.addCell(new Label(x + xOffset, 7 + yOffset, "" + ""/* regionalDifferentiation */));

        // Durchschnittliche Wegelänge in Metern
        Categories[] catRcTi = {Categories.RegionCode, Categories.TripIntention};
        Enum[] insRcTi = {region, TripIntention.TRIP_31};

        for (TripIntention ti : TripIntention.values()) {
            insRcTi[1] = ti;
            wegelaengenSheet.addCell(
                    new Number(x + xOffset, y + yOffset, regionAnalyzer.getAvgDistByTrip(catRcTi, insRcTi), dc2cell));
            y++;
        }

        wegelaengenSheet.addCell(
                new Number(x + xOffset, y + yOffset, regionAnalyzer.getAvgDistByTrip(Categories.RegionCode, region),
                        dc2cell));

        y += 4;

        // Ergebnisse nach Wegezweck in %
        Categories[] catRcDcTi = {Categories.RegionCode, Categories.DistanceCategory, Categories.TripIntention};
        Enum[] insRcDcTi = {region, DistanceCategory.CAT_1, TripIntention.TRIP_31};

        for (TripIntention ti : TripIntention.values()) {
            insRcDcTi[2] = ti;
            insRcTi[1] = ti;
            long cntTi = regionAnalyzer.getCntTrips(catRcTi, insRcTi);
            for (DistanceCategory cat : DistanceCategory.values()) {
                insRcDcTi[1] = cat;
                long cntDcTi = regionAnalyzer.getCntTrips(catRcDcTi, insRcDcTi);
                wegelaengenSheet.addCell(
                        new Number(x + xOffset, y + yOffset, saveDiv(cntDcTi, cntTi) * 100.0, dc2cell));
                y++;
            }
            y++;
        }

        // Alle Wegezwecke
        Categories[] catRcDc = {Categories.RegionCode, Categories.DistanceCategory};
        Enum[] insRcDc = {region, DistanceCategory.CAT_1};

        long cntRc = regionAnalyzer.getCntTrips(Categories.RegionCode, region);

        for (DistanceCategory cat : DistanceCategory.values()) {
            insRcDc[1] = cat;
            long cntRcDc = regionAnalyzer.getCntTrips(catRcDc, insRcDc);

            wegelaengenSheet.addCell(new Number(x + xOffset, y + yOffset, saveDiv(cntRcDc, cntRc) * 100.0, dc2cell));
            y++;

        }
    }

}
