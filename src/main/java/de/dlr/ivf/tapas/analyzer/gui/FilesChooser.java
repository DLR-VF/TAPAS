package de.dlr.ivf.tapas.analyzer.gui;

import de.dlr.ivf.tapas.analyzer.inputfileconverter.FileTripReader;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Eine UI-Komponente die das Auswählen mehrerer Dateien ermöglicht. Die ausgewählten Dateien werden in einer Liste angezeigt.
 * 
 * @author Marco
 * 
 */
public class FilesChooser extends JPanel {
	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -8388208545993125633L;
	private JList<File>			lstFiles;

	private final List<File>	files				= new ArrayList<>();
	private FilesListModel		dataModel;
	private TitledBorder		border;
	private final String		name;
	
	/**
	 * Model which get all Files from {@link FilesChooser#files}
	 * 
	 * @author mbrehme
	 * 
	 */
	private class FilesListModel implements ListModel<File> {
		
		private final List<ListDataListener>	listeners	= new ArrayList<>();
		
		public File getElementAt(int index) {
			return files.get(index);
		}
		
		public int getSize() {
			return files.size();
		}
		
		public void addListDataListener(ListDataListener l) {
			listeners.add(l);
		}
		
		public void removeListDataListener(ListDataListener l) {
			listeners.remove(l);
		}
		
		public void changed() {
			for (ListDataListener listener : listeners)
				listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, files.size() - 1));
		}
		
	}
	
	/**
	 * Create the panel.
	 */
	public FilesChooser(String name, String title) {
		
		this.name = name;
		
		createContents(title);
		
	}
	
	/**
	 * Create the panel.
	 */
	public FilesChooser(String name) {
		this.name = name;
		
		createContents("Files");
		
	}
	
	private void createContents(String title) {
		border = new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), title, TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0));
		setBorder(border);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);
		
		dataModel = new FilesListModel();
		lstFiles = new JList<>(dataModel);
		lstFiles.setVisibleRowCount(4);
		lstFiles.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				do_lstFiles_mouseClicked(e);
			}
		});
		lstFiles.setBorder(null);
		JScrollPane scrollPane = new JScrollPane(lstFiles);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 5, 0);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(scrollPane, gbc);

		JButton btnAdd = new JButton("Add");
		btnAdd.addActionListener(this::do_btnAdd_actionPerformed);
		GridBagConstraints gbc_1 = new GridBagConstraints();
		gbc_1.anchor = GridBagConstraints.EAST;
		gbc_1.gridx = 0;
		gbc_1.gridy = 1;
		add(btnAdd, gbc_1);
		
	}
	
	protected void do_btnAdd_actionPerformed(ActionEvent e) {
		addFiles(chooseFile());
		lstFiles.repaint();
	}
	
	private void addFiles(List<File> files) {
		if (files != null) {
			this.files.addAll(files);
			dataModel.changed();
			doLayout();
		}
	}
	
	protected List<File> chooseFile() {
		JFileChooser fd = new JFileChooser(FileChooserHistoryManager.getLastDirectory(name));
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		fd.setMultiSelectionEnabled(true);
		fd.setVisible(true);
		
		fd.setDialogTitle("Dateiauswahl");
		int value = fd.showOpenDialog(this);
		if ((value == JFileChooser.APPROVE_OPTION) && fd.getSelectedFile() != null && fd.getSelectedFile().isFile() && fd.getSelectedFile().exists()) {
			FileChooserHistoryManager.updateLastDirectory(name, fd.getSelectedFile().getPath().substring(0, fd.getSelectedFile().getPath().lastIndexOf(File.separatorChar)));
			return new ArrayList<>(Arrays.asList(fd.getSelectedFiles()));
		} else if (value != JFileChooser.CANCEL_OPTION) {
			JOptionPane.showMessageDialog(this, "Keine Datei ausgewählt: ", "Fehler", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}
	
	public List<File> getFiles() {
		return files;
	}
	
	protected void do_lstFiles_mouseClicked(MouseEvent e) {
		
		int selectedIndex = lstFiles.locationToIndex(e.getPoint());
		if (selectedIndex > -1 && !lstFiles.getCellBounds(selectedIndex, selectedIndex).contains(e.getPoint()))
			selectedIndex = -1; //no actual hit 
		if (selectedIndex > -1 && !getFiles().isEmpty()) {
			getFiles().remove(selectedIndex);
			dataModel.changed();
		}
		
	}
	
	public void setTitle(String title) {
		border.setTitle(title);
	}
	
	public String getTitle() {
		return border.getTitle();
	}
	
	public FileTripReader createFileTripReader(StyledDocument console){
		if (files.size() > 0){
			return new FileTripReader(files, console);
		}
		
		return null;		
	}
	
	public boolean isReady(){
		return files.size() > 0;
	}
}
