package de.dlr.ivf.tapas.tools.fileModifier.filefilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ExtensionFilter extends FileFilter {
	private String	extension;
	
	private String	description;
	
	public ExtensionFilter(String description, String extension) {
		this.description = description;
		this.extension = extension;
	}
	
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		String path = file.getAbsolutePath();
        return path.endsWith(extension) && (path.charAt(path.length() - extension.length()) == '.');
    }
	
	public String getDescription() {
		return (description == null ? extension : description);
	}
	
	public String getExtension() {
		return extension;
	}
}