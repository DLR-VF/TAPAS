/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.fileModifier.filefilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ExtensionFilter extends FileFilter {
    private final String extension;

    private final String description;

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