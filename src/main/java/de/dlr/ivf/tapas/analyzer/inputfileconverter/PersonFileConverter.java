/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.analyzer.inputfileconverter;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class PersonFileConverter {

    String[] header;
    private TPS_FileReader reader;
    private final ArrayList<PersonFileElement> personFileElements = new ArrayList<>();
    private final File personFile;

    public PersonFileConverter(File file) {
        this.personFile = file;
        // catch empty tripFilesList
        try {
            reader = new TPS_FileReader(this.personFile.getAbsolutePath(), true);
            header = reader.getHeaders();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("File not found constructor");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IOException constructor");
        }
    }

    /**
     * Wird vom core aufgerufen. bekommt die personFiles Liste übergeben und holt sich daraus die Zeilen der personfiles gibt diese dann an PersonFileElement weiter
     *
     * @param console
     * @throws BadLocationException
     */
    public void convertPersonFileElements(StyledDocument console) throws BadLocationException {
        String[] record = null;
        do {
            try {
                record = reader.getRecord();
                // System.out.println("A..."+record[0].toString());
            } catch (NullPointerException e) {
                // if end of file reached...
                // e.printStackTrace();
                // System.out.println("End of File reached (NullPointer) Personfile parsed");
                console.insertString(console.getLength(), "FileConv gelesen " + reader.getFileName() + "\n", null);
                try {
                    reader = new TPS_FileReader(personFile.getAbsolutePath(), true);
                    record = reader.getRecord();
                    // System.out.println("B..."+record[0].toString());
                } catch (FileNotFoundException e1) {
                    // If no further file available
                    // e1.printStackTrace();
                    System.out.println("File not found convertNextElement (FileIteration)");
                } catch (IOException e1) {
                    // e1.printStackTrace();
                    System.out.println("IOException convertNextElement (FileIteration)");
                } catch (IndexOutOfBoundsException e1) {
                    // System.out.println("Index out of bounds, All tripfiles parsed");
                    console.insertString(console.getLength(), "PersonFileConv.: Alle Personendateien eingelesen\n",
                            null);
                }
            } catch (IOException e1) {
                System.out.println("IOException convertNextElement (IOException)");
            }
            if (record != null) {
                PersonFileElement element = new PersonFileElement();
                element.generateKeyMap(header);
                element.setValues(record);
                personFileElements.add(element);
            }
        } while (record != null);
        // //////

    }

    public ArrayList<PersonFileElement> getPersonFileElements() {
        return personFileElements;
    }

}