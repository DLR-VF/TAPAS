/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.tools.fileModifier;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

@SuppressWarnings("unused")
public class DropDemo extends JFrame implements DropTargetListener {
    static final long serialVersionUID = 385983557;

    private final Container conPane;
    private final String targetDir = "T:/";

    public DropDemo() {
        conPane = getContentPane();
        initCenter();
        initFrame();
    }

    public static void main(String[] args) {
        DropDemo wnd = new DropDemo();
    }

    public void dragEnter(DropTargetDragEvent e) {
    }

    // -------------- vom DropTargetListener gefordete methoden -------------- \\

    public void dragExit(DropTargetEvent e) {
    }

    public void dragOver(DropTargetDragEvent e) {
    }

    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent e) {
        Transferable tr = e.getTransferable();
        if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            try {
                List<File> files = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : files) {
                    System.out.println(file.getClass());
                }
            } catch (UnsupportedFlavorException | IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void dropActionChanged(DropTargetDragEvent e) {
    }

    // ----------------------------- initCenter ----------------------------- \\
    private void initCenter() {
        File source = new File(targetDir);
        File[] files = source.listFiles();
        Vector<String> fileVector = new Vector<>();
        for (File file : files) {
            if (file.isDirectory()) fileVector.add("<dir>  " + file.getName());
            else fileVector.add("<file> " + file.getName());
        }

        JList<String> dropFileList = new JList<>(fileVector);

        dropFileList.setDragEnabled(true);
        DropTarget dropTarget = new DropTarget(dropFileList, this);

        JScrollPane sp = new JScrollPane(dropFileList);
        conPane.add(dropFileList, BorderLayout.CENTER);
    }

    // ------------------------------ initFrame ------------------------------ \\
    private void initFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(" DropDemo listing of " + targetDir);
        setSize(300, 400);
        setLocation(47, 47);
        setVisible(true);
    }
}
