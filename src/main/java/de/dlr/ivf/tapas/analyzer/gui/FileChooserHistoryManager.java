package de.dlr.ivf.tapas.analyzer.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.dlr.ivf.tapas.analyzer.core.Constants;

/**
 * Ein Manager der sich um das Speihern der zuletzt selektierten Dateipfade kümmert. Wird der {@link FileChooser} oder {@link FilesChooser} verwendet, speichern diese den selektierten Pfad mithilfe
 * dieser Klasse in einer Property-Datei. Beim nächsten Start der Anwendung wird diese Datei dann wieder ausgelesen und dient der Initialisierung der {@link FileChooser} bzw. {@link FilesChooser}
 * 
 * @author Marco
 * 
 */
public class FileChooserHistoryManager {
	
	private static Properties	properties;
	
	private static Properties getProperties() {
		if (properties == null) {
			InputStream inStream = null;
			properties = new Properties();
			try {
				File f = new File(Constants.PATH_PROPERTIES);
				if (f.exists()) {
					inStream = new FileInputStream(f);
					if (inStream != null)
						properties.load(inStream);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (inStream != null)
					try {
						inStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
		
		return properties;
	}
	
	public static String getLastDirectory(String name) {
		return getProperties().getProperty(name);
	}
	
	public static void updateLastDirectory(String name, String path) {
		getProperties().setProperty(name, path);
		save();
	}
	
	private static void save() {
		FileOutputStream out = null;
		File file = new File(Constants.PATH_PROPERTIES);
		try {
			if (!file.exists())
				file.createNewFile();
			out = new FileOutputStream(file);
			getProperties().store(out, "Dateipfade");
		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
}
