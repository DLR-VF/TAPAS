package de.dlr.ivf.tapas.analyzer.core;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.TapasTrip;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

/**
 * Der Logik-Teil eines Moduls. Die Implementierung dieser Schnittstelle enthält
 * die gesamte Logik zum Ausführen eines Moduls. Die Implementierung sollte in
 * einem Thread ausgeführt werden.
 *
 * @author Marco
 */
public interface CoreProcessInterface extends Runnable {

    /**
     * Bricht den Prozess ab.
     *
     * @return
     */
    boolean cancelFinish();

    /**
     * Muss aufgerufen werden, wenn der Prozess beendet wird
     *
     * @return
     */
    boolean finish() throws BadLocationException;

    /**
     * Muss einmalig vor dem Start aufgerufen werden.
     *
     * @param outputPath   Pfad wo die Ergebnisse hingeschrieben werden sollen
     * @param console      Konsole für Info-Mitteilungen.
     * @param clearSources Flag ob vorherige Resultate gelöscht werden sollen.
     * @return
     */
    boolean init(String outputPath, StyledDocument console, boolean clearSources) throws BadLocationException;

    /**
     * Muss vor dem Start des Prozesses für jeden zu behandelnden Trip
     * aufgerufen werden.
     *
     * @param filePath Der Pfad zum Trip
     * @param trip     der Trip
     * @return
     */
    boolean prepare(String filePath, TapasTrip trip, TPS_ParameterClass parameterClass) throws BadLocationException;


}
