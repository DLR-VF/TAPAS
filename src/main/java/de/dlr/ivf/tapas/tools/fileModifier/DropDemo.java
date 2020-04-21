package de.dlr.ivf.tapas.tools.fileModifier;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import java.awt.datatransfer.*;

import java.awt.dnd.DropTarget;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;

@SuppressWarnings("unused")
public class DropDemo extends JFrame implements DropTargetListener {
	static final long serialVersionUID = 385983557;

	private Container conPane;
	private String targetDir = "T:/";

	public DropDemo() {
		conPane = getContentPane();
		initCenter();
		initFrame();
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

	// -------------- vom DropTargetListener gefordete methoden -------------- \\

	public void dragEnter(DropTargetDragEvent e) {}

	public void dragExit(DropTargetEvent e) {}

	public void dragOver(DropTargetDragEvent e) {}

	@SuppressWarnings("unchecked")
	public void drop(DropTargetDropEvent e) {
		Transferable tr = e.getTransferable();
        if (tr.isDataFlavorSupported (DataFlavor.javaFileListFlavor))
        {
           e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
           try {
			List<File> files = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
			for (File file : files){
				System.out.println(file.getClass());
			}
		} catch (UnsupportedFlavorException | IOException e1) {
			e1.printStackTrace();
		}
		}
	}

	public void dropActionChanged(DropTargetDragEvent e) {}

	public static void main(String[] args) {
		DropDemo wnd = new DropDemo();
	}
}
