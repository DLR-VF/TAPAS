package de.dlr.ivf.tapas.matrixtool.matrixTool_v2.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import de.dlr.ivf.tapas.matrixtool.common.events.IOEvent;
import de.dlr.ivf.tapas.matrixtool.common.fileFormat.AbstractFileFilter;
import de.dlr.ivf.tapas.matrixtool.common.fileFormat.CSVSemicolonFileFilter;
import de.dlr.ivf.tapas.matrixtool.common.fileFormat.VisumFileFilter;
import de.dlr.ivf.tapas.matrixtool.common.localisation.Localisation;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.FilterController;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.controller.OuterControl;
import de.dlr.ivf.tapas.matrixtool.matrixTool_v2.model.MemoryModel;

public class OuterGui extends JFrame implements ActionListener, InternalFrameListener{

	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem open;
	private OuterControl control;
	private JMenuItem neu;
	private JMenuItem saveAs;
	private StatusBar bar;
	private HashMap<Integer,JInternalFrame> documents;
	private JDesktopPane desktop;
	private JMenuItem save;


	public OuterGui(String name, OuterControl control, StatusBar statusBar) {

		super(name);
		this.control = control;
		this.bar = statusBar;

		documents = new HashMap<Integer,JInternalFrame>();
		desktop = new JDesktopPane();
		desktop.setBackground(getBackground());
		desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

		getContentPane().setLayout(new BorderLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		getContentPane().add(BorderLayout.CENTER,desktop);

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		getContentPane().add(BorderLayout.SOUTH,statusBar);

		setSize(1000, 750);
		setDefaultCloseOperation(EXIT_ON_CLOSE);;
	}


	public void createAndShowGUI(){

		menuBar = new JMenuBar();

		fileMenu = new JMenu(Localisation.getLocaleGuiTerm("FILE"));

		open = new JMenuItem(Localisation.getLocaleGuiTerm("OPEN"));
		open.addActionListener(this);

		neu = new JMenuItem(Localisation.getLocaleGuiTerm("NEW"));
		neu.addActionListener(this);

		saveAs = new JMenuItem(Localisation.getLocaleGuiTerm("SAVE_AS"));
		saveAs.addActionListener(this);

		save = new JMenuItem(Localisation.getLocaleGuiTerm("SAVE"));
		save.addActionListener(this);

		fileMenu.add(neu);
		fileMenu.add(open);
		fileMenu.add(saveAs);
		fileMenu.add(save);
		menuBar.add(fileMenu);
		
		JWindowsMenu windowsMenu = new JWindowsMenu(Localisation.getLocaleGuiTerm("WINDOW"),
				desktop);
		windowsMenu.setWindowPositioner(new CascadingWindowPositioner(desktop));
		menuBar.add(windowsMenu);

		setJMenuBar(menuBar);

		setVisible(true);
	}


	public void initDocument(MemoryModel model, int docCounter) {

		String title = (control.getDocumentFile(docCounter) == null) ? Localisation.getLocaleGuiTerm("NEW") : 
			control.getDocumentFile(docCounter).getName();

		JInternalFrame doc = new JInternalFrame(title, true, true, true, true);
		doc.addInternalFrameListener(this);
		documents.put(docCounter, doc);

		FilterPanel filter = createFilter(model,bar); 
		JTabbedPane inter = createInteractionPanel(model,bar,filter);
		JTabbedPane visual = createVisualisation(model,bar,filter);

		doc.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		doc.add(filter,c);

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		doc.add(inter,c);

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 2;
		doc.add(visual,c);

//		doc.pack();
		doc.setSize(980,700);
		doc.setVisible(true);
		desktop.add(doc);
		try {
			doc.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {

		}
	}


	private JTabbedPane createVisualisation(MemoryModel model, StatusBar bar,
			FilterPanel filter) {

		JTabbedPane visPan = new JTabbedPane();

		TableVisualisation tVis = new TableVisualisation(filter.getController(),model);
		DiagramVisualisation dVis = new DiagramVisualisation(model);

		visPan.addTab(Localisation.getLocaleGuiTerm("TABLE_VIS"), tVis);
		visPan.addTab(Localisation.getLocaleGuiTerm("DIAG_VIS"), dVis);
		
		visPan.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("VISUAL")));

		return visPan;
	}


	private JTabbedPane createInteractionPanel(MemoryModel model, StatusBar bar, FilterPanel filter) {

		JTabbedPane tabPan = new JTabbedPane();

		InteractionPanel manipModule = new InteractionPanel(filter.getController(),model,bar);
		AnalysePanel analyseModule = new AnalysePanel(filter.getController(),model,bar);

		tabPan.addTab(Localisation.getLocaleGuiTerm("MANIPULATION"),manipModule);
		tabPan.addTab(Localisation.getLocaleGuiTerm("ANALYSIS"),analyseModule);

		tabPan.setBorder(BorderFactory.createTitledBorder(Localisation.getLocaleGuiTerm("INTERAKT")));
		
		return tabPan;
	}


	private FilterPanel createFilter(MemoryModel model, StatusBar bar) {

		return new FilterPanel(new FilterController(model,bar));
	}


	public void actionPerformed(ActionEvent e) {


		if (e.getSource() == neu){
			control.initDocument(null,null);
		}

		if (e.getSource() == open){
			
			int ret;
			JFileChooser fc = new JFileChooser();
			if (!documents.isEmpty()){
				TreeSet<Integer> sortedKeys = new TreeSet<Integer>(documents.keySet());
				if (control.getDocumentFile(sortedKeys.last()) != null)
					fc.setSelectedFile(control.getDocumentFile(sortedKeys.last()));
			}
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setFileFilter(new CSVSemicolonFileFilter());
			fc.setFileFilter(new VisumFileFilter());

			ret = fc.showOpenDialog(this);
			if (ret == JFileChooser.APPROVE_OPTION) {
				if (!(fc.getFileFilter() instanceof AbstractFileFilter)){

					control.doNotify(new IOEvent(control,IOEvent.Type.ERROR_READING,
							fc.getSelectedFile().getAbsolutePath(),
							Localisation.getLocaleMessageTerm("WRONG_FORMAT")));

				} else {
					control.initDocument(fc.getSelectedFile(), 
							(AbstractFileFilter)fc.getFileFilter());
				}
			}
		}

		if (e.getSource() == saveAs){
			JInternalFrame selFrame = desktop.getSelectedFrame();
			for (Integer key : documents.keySet()){
				
				if (selFrame == documents.get(key)){
					
					int ret;
					JFileChooser fc = new JFileChooser();
					if (control.getDocumentFile(key) != null)
						fc.setSelectedFile(control.getDocumentFile(key));
					fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
					fc.setFileFilter(new CSVSemicolonFileFilter());
					fc.setFileFilter(new VisumFileFilter());

					ret = fc.showSaveDialog(this);
					if (ret == JFileChooser.APPROVE_OPTION) {
						if (!(fc.getFileFilter() instanceof AbstractFileFilter)){
							
							control.doNotify(new IOEvent(control,IOEvent.Type.ERROR_READING,
									fc.getSelectedFile().getAbsolutePath(),
									Localisation.getLocaleMessageTerm("WRONG_FORMAT")));

						} else {

							control.writeDocument(fc.getSelectedFile(), 
									(AbstractFileFilter)fc.getFileFilter(),
									key);
							
							selFrame.setTitle(fc.getSelectedFile().getName());
						}
					}
				}
			}
		}

		if (e.getSource() == save){

			JInternalFrame selFrame = desktop.getSelectedFrame();
			for (Integer key : documents.keySet()){
				if (selFrame == documents.get(key)){

					if (control.getDocumentFile(key) == null){
						control.doNotify(new IOEvent(control,IOEvent.Type.ERROR_WRITING,
								Localisation.getLocaleMessageTerm("NO_PATH")));
					} else if (control.getDocumentFilter(key) == null){
						control.doNotify(new IOEvent(control,IOEvent.Type.ERROR_WRITING,
								Localisation.getLocaleMessageTerm("NO_FORMAT")));
					} else {
						control.writeDocument(control.getDocumentFile(key),
								control.getDocumentFilter(key),key);
					}
				}					
			}
		}
	}





	public void internalFrameActivated(InternalFrameEvent arg0) {
		// TODO Auto-generated method stub

	}


	public void internalFrameClosed(InternalFrameEvent arg0) {

		JInternalFrame closed = arg0.getInternalFrame();
		int docToRemove = 0;
		for (Integer i : documents.keySet()){
			if (documents.get(i) == closed){
				control.disposeDocument(i);
				docToRemove = i;
			}
		}
		documents.remove(docToRemove);
	}


	public void internalFrameClosing(InternalFrameEvent arg0) {
		// TODO Auto-generated method stub

	}


	public void internalFrameDeactivated(InternalFrameEvent arg0) {
		// TODO Auto-generated method stub

	}


	public void internalFrameDeiconified(InternalFrameEvent arg0) {
		// TODO Auto-generated method stub

	}


	public void internalFrameIconified(InternalFrameEvent arg0) {
		// TODO Auto-generated method stub

	}


	public void internalFrameOpened(InternalFrameEvent arg0) {
		// TODO Auto-generated method stub

	}
}
