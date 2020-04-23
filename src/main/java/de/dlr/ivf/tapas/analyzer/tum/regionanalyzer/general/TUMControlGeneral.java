package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general;

import de.dlr.ivf.tapas.analyzer.core.CoreProcessInterface;
import de.dlr.ivf.tapas.analyzer.gui.ControlInputInterface;
import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.AnalyzerBase;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;


@SuppressWarnings("rawtypes")
public class TUMControlGeneral implements ControlInputInterface {

    private static final String[] treeLevel = {"ROOT", "FILE", "TAB", "SPLIT"};
    private static final int BUTTON_WIDTH = 80;
    private boolean TUMactivated = true;
    private GeneralAnalyzer analyzer = null;
    private JTree tree;
    private JList<AnalyzerBase> baseAnalyzerList;
    private JPanel pMain;
    private JTextField edtName;
    private JLabel lblType;
    private JList<AnalyzerBase> activeAnalyzerList;
    private JCheckBox ckbxActive;
    private JCheckBox ckbxDatabaseSummary;
    private DefaultMutableTreeNode root;
    private DefaultTreeModel treeModel;

    /**
     * for testing purposes only
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("TUM Control");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        TUMControlGeneral tum = new TUMControlGeneral();
        JComponent panel = tum.getComponent();
        panel.setOpaque(true);
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
        // tum.loadTree("H:/Temp/tree.xml");

        // String simkey = "2013y_09m_10d_11h_43m_03s_321ms";
        // CoreProcessInterface process = tum.getProcessImpl();
        // process.init("H:\\Temp\\", null);
        //
        // String loginInfo = "T:\\Simulationen\\runtime_perseus.csv";
        // TPS_Parameters.loadRuntimeParameters(new File(loginInfo));
        // TPS_DB_Connector dbCon = TPS_DB_Connector.login();
        //
        // DBTripReader tripReader = new DBTripReader(simkey, null, null,
        // dbCon);
        //
        // while (tripReader.getIterator().hasNext()) {
        // TapasTrip tt = tripReader.getIterator().next();
        // process.prepare(simkey, tt);
        // }
        //
        // System.out.println("Finished walking through.");
        // process.finish();
        //
        // System.out.println("Finished exporting.");

    }

    private void addDefaultAnalyzers() {
        DefaultMutableTreeNode file = new DefaultMutableTreeNode(new AnalyzerCollection("TUMExport"));

        // wegelängen sheet
        DefaultMutableTreeNode tab = new DefaultMutableTreeNode(
                new AnalyzerCollection("Wegelängen", getAnalyzer(Categories.RegionCode)));
        file.add(tab);

        DefaultMutableTreeNode analyzers = new DefaultMutableTreeNode(
                new AnalyzerCollection(getAnalyzer(Categories.TripIntention)));
        tab.add(analyzers);


        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(getAnalyzer(Categories.TripIntention),
                getAnalyzer(Categories.DistanceCategoryDefault)));
        tab.add(analyzers);

        // modalsplit
        tab = new DefaultMutableTreeNode(new AnalyzerCollection("Modalsplit", getAnalyzer(Categories.RegionCode)));
        file.add(tab);
        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(getAnalyzer(Categories.Mode)));
        tab.add(analyzers);
        analyzers = new DefaultMutableTreeNode(
                new AnalyzerCollection(getAnalyzer(Categories.Mode), getAnalyzer(Categories.DistanceCategoryDefault)));
        tab.add(analyzers);
        analyzers = new DefaultMutableTreeNode(
                new AnalyzerCollection(getAnalyzer(Categories.Mode), getAnalyzer(Categories.DistanceCategoryDefault),
                        getAnalyzer(Categories.TripIntention)));
        tab.add(analyzers);

        // personengruppen
        tab = new DefaultMutableTreeNode(new AnalyzerCollection("Personengruppen", getAnalyzer(Categories.RegionCode)));
        file.add(tab);
        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(getAnalyzer(Categories.PersonGroup)));
        tab.add(analyzers);

        treeModel.insertNodeInto(file, root, 0);

    }

    /**
     * Builds the layout and initiates all global fields.
     *
     * @return
     */
    private JPanel createGUI() {
        JPanel main = new JPanel(new GridBagLayout());
        Insets zeroInset = new Insets(0, 0, 0, 0);
        GridBagConstraints gbc;

        initTree();
        gbc = new GridBagConstraints(0, 0, 1, 7, 0.5, 1, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
                zeroInset, 0, 0);
        main.add(new JScrollPane(tree), gbc);

        initBaseList();
        gbc = new GridBagConstraints(1, 0, 2, 1, 0, 0, GridBagConstraints.FIRST_LINE_START,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 0, 0);
        main.add(new JScrollPane(baseAnalyzerList), gbc);

        gbc = new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
                new Insets(0, 3, 0, 0), 6, 0);
        main.add(new JLabel("Name"), gbc);
        gbc.gridy++;
        main.add(new JLabel("Type"), gbc);
        gbc.gridy++;
        main.add(new JLabel("Analyzer"), gbc);

        gbc = new GridBagConstraints(2, 1, 1, 1, 0.5, 0, GridBagConstraints.FIRST_LINE_START,
                GridBagConstraints.HORIZONTAL, zeroInset, 0, 0);
        edtName = new JTextField();
        main.add(edtName, gbc);
        gbc.gridy++;
        lblType = new JLabel();
        main.add(lblType, gbc);
        gbc.gridy++;
        initActiveList();
        main.add(new JScrollPane(activeAnalyzerList), gbc);

        gbc = new GridBagConstraints(1, 4, 2, 1, 0, 0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
                zeroInset, 2, 0);
        main.add(getControlListPanel(), gbc);
        gbc.gridy++;
        main.add(getControlTreePanel(), gbc);

        gbc.gridy++;

        ckbxDatabaseSummary = new JCheckBox("Results to DB");
        ckbxDatabaseSummary.setSelected(true);
        main.add(ckbxDatabaseSummary, gbc);

        return main;
    }

    private void deleteSelectedTreeNode() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return;
        }

        treeModel.removeNodeFromParent((DefaultMutableTreeNode) path.getLastPathComponent());

    }

    public void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        // Traverse tree from root
        expandAll(tree, new TreePath(root), expand);
    }

    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    private AnalyzerBase getAnalyzer(Categories c) {

        Enumeration<AnalyzerBase> en = ((DefaultListModel<AnalyzerBase>) baseAnalyzerList.getModel()).elements();

        while (en.hasMoreElements()) {
            AnalyzerBase a = en.nextElement();
            if (a.getCategories() == c) return a;
        }

        return null;
    }

    @Override
    public JComponent getComponent() {

        JPanel moduleMatrix = new JPanel(new BorderLayout());
        moduleMatrix.setBorder(
                new TitledBorder(new LineBorder(new Color(46, 90, 214), 2, true), "TUM", TitledBorder.LEADING,
                        TitledBorder.TOP, null, new Color(0, 0, 0)));
        ckbxActive = new JCheckBox("TAPAS Text- und Excelausgabe");
        ckbxActive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                TUMactivated = ckbxActive.isSelected();
                pMain.setVisible(TUMactivated);
                expandAll(tree, true);
            }
        });
        moduleMatrix.add(ckbxActive, BorderLayout.PAGE_START);

        pMain = new JPanel(new GridBagLayout());
        pMain = createGUI();
        pMain.setVisible(false);
        addDefaultAnalyzers();
        moduleMatrix.add(pMain, BorderLayout.CENTER);
        return moduleMatrix;
    }

    private JPanel getControlListPanel() {
        JPanel panel = new JPanel();

        JButton btnAdd = makeConstantWidthButton("Add", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = -8807945887071042980L;

            @Override
            public void actionPerformed(ActionEvent e) {
                updateNode(true);
            }
        });
        btnAdd.setToolTipText("Fügt eine neue Node zum Baum hinzu.");
        panel.add(btnAdd);

        JButton btnDelete = makeConstantWidthButton("Delete", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = -678136576520427023L;

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedTreeNode();
            }
        });
        btnDelete.setToolTipText("Löscht die markierte Node vom Baum");
        panel.add(btnDelete);

        JButton btnUpdate = makeConstantWidthButton("Update", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 455209366482955180L;

            @Override
            public void actionPerformed(ActionEvent e) {
                updateNode(false);
            }
        });
        btnUpdate.setToolTipText("<html>Überschreibt die ausgewählte Node");
        panel.add(btnUpdate);

        return panel;
    }

    private JPanel getControlTreePanel() {
        JPanel panel = new JPanel();

        JButton btnLoadTree = makeConstantWidthButton("Load", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = -4092263533146773245L;

            @Override
            public void actionPerformed(ActionEvent e) {
                loadTree();
            }
        });
        btnLoadTree.setToolTipText("Läd einen Analyzer-Baum aus einer .xml Datei.");
        panel.add(btnLoadTree);

        JButton btnSaveTree = makeConstantWidthButton("Save", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 5033866262337867204L;

            @Override
            public void actionPerformed(ActionEvent e) {
                loadTree();
            }
        });
        btnSaveTree.setToolTipText("Speichert den Analyzer-Baum in eine .xml Datei.");
        panel.add(btnSaveTree);

        return panel;
    }

    @Override
    public int getIndex() {
        return 1;
    }

    private String getLevelAddition(DefaultMutableTreeNode node) {
        switch (node.getLevel()) {
            case 0:
                return "Name: ";
            case 1:
                return "File: ";
            case 2:
                return "Tab: ";
            case 3:
                return "Analyzers: ";
        }
        return "";
    }

    @Override
    public CoreProcessInterface getProcessImpl() {
        //if (analyzer == null){
        analyzer = new GeneralAnalyzer(root, false);
        //}

        return analyzer;
    }

    private void initActiveList() {
        activeAnalyzerList = new JList<>(new DefaultListModel<>());
        activeAnalyzerList.setVisibleRowCount(4);
        activeAnalyzerList.setDragEnabled(true);
        activeAnalyzerList.setDropMode(DropMode.INSERT);
        activeAnalyzerList.setTransferHandler(new NodeListTransferHandler());

        activeAnalyzerList.setToolTipText(
                "<html>Es wird nach der letzten Kategorie<br>" + "gesplittet in der gegebenen Reihenfolge.");
        String del = "DEL";
        activeAnalyzerList.getActionMap().put(del, new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1026590436808769207L;

            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = activeAnalyzerList.getSelectedIndex();
                if (idx > -1) {
                    ((DefaultListModel) activeAnalyzerList.getModel()).remove(idx);
                }
            }
        });
        activeAnalyzerList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), del);
    }

    private void initBaseList() {
        baseAnalyzerList = new JList<>(new DefaultListModel<>());

        DefaultListModel<AnalyzerBase> model = (DefaultListModel<AnalyzerBase>) baseAnalyzerList.getModel();

        baseAnalyzerList.setDragEnabled(true);
        baseAnalyzerList.setTransferHandler(new BaseListTransferHandler());

        model.addElement(new RegionCodeAnalyzer());
        model.addElement(new DefaultDistanceCategoryAnalyzer());
        model.addElement(new EquiFiveDistanceCategoryAnalyzer());
        model.addElement(new EquiTenDistanceCategoryAnalyzer());
        model.addElement(new ModeAnalyzer());
        model.addElement(new PersonGroupAnalyzer());
        model.addElement(new TravelTimeAnalyzer());
        model.addElement(new TripIntentionAnalyzer());

        for (int i = 0; i < model.getSize(); i++) {
            System.out.println(model.getElementAt(i) + " --- " + model.getElementAt(i).getClass());

        }
    }

    private void initTree() {
        root = new DefaultMutableTreeNode(new AnalyzerCollection("ROOT"));

        treeModel = new DefaultTreeModel(root, true) {
            /**
             *
             */
            private static final long serialVersionUID = -7127969609545756611L;

            @Override
            public boolean isLeaf(Object node) {
                return ((DefaultMutableTreeNode) node).getLevel() == 3;
            }

        };
        tree = new JTree(treeModel) {
            /**
             *
             */
            private static final long serialVersionUID = -9041746877670000859L;

            @Override
            public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

                return getLevelAddition((DefaultMutableTreeNode) value) + super.convertValueToText(value, selected,
                        expanded, leaf, row, hasFocus);

            }
        };

        tree.setToolTipText("<html>DoubleClick auf Node läd <br>Informationen in Infobereich.");

        // prevent collapsing of the tree
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillCollapse(TreeExpansionEvent arg0) throws ExpandVetoException {
                throw new ExpandVetoException(arg0);
            }

            @Override
            public void treeWillExpand(TreeExpansionEvent arg0) {
            }
        });

        String del = "DEL";
        tree.getActionMap().put(del, new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = -2095681990428710036L;

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedTreeNode();
            }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), del);

        tree.setRootVisible(false);
        tree.setVisibleRowCount(15);
        tree.setShowsRootHandles(true);
        tree.setToggleClickCount(0);

        // allow deselection
        tree.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row == -1) {
                    tree.clearSelection();
                    ((DefaultListModel) activeAnalyzerList.getModel()).clear();
                } else if (e.getClickCount() > 1) {
                    loadSelectedNodeInfo();
                }
            }
        });

        tree.setTransferHandler(new AnalyzerTreeTransferHandler());
        tree.setDropMode(DropMode.ON_OR_INSERT);
    }

    @Override
    public boolean isActive() {
        return TUMactivated;
    }

    private void loadSelectedNodeInfo() {
        TreePath selectionPath = tree.getSelectionPath();
        ((DefaultListModel<AnalyzerBase>) activeAnalyzerList.getModel()).clear();
        if (selectionPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            AnalyzerCollection analyzers = (AnalyzerCollection) node.getUserObject();

            lblType.setText(treeLevel[node.getLevel()]);

            edtName.setText(analyzers.getName());

            DefaultListModel<AnalyzerBase> model = (DefaultListModel<AnalyzerBase>) activeAnalyzerList.getModel();
            for (AnalyzerBase a : analyzers.getAnalyzers()) {
                model.addElement(a);
            }

        } else {
            lblType.setText("");
            edtName.setText("");
        }

    }

    public void loadTree() {

        if (tree == null) {
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("XML Files", "xml"));
        if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(null)) {
            File file = fc.getSelectedFile();

            try {

                DefaultMutableTreeNode newRoot = GeneralAnalyzerTreeExporter.readTree(file.getAbsolutePath());
                root.removeAllChildren();
                for (int i = 0; i < newRoot.getChildCount(); i++) {
                    treeModel.insertNodeInto((DefaultMutableTreeNode) newRoot.getChildAt(i), root, i);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Could not load tree from " + file.getAbsolutePath());
            }
        }

        treeModel.reload();
    }

    private JButton makeConstantWidthButton(String text, Action a) {
        JButton button = new JButton();
        button.setAction(a);
        button.setText(text);
        button.setPreferredSize(new Dimension(BUTTON_WIDTH, button.getPreferredSize().height));
        return button;
    }

    public void saveTree() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("XML Files", "xml"));
        if (JFileChooser.APPROVE_OPTION == fc.showSaveDialog(null)) {
            File file = fc.getSelectedFile();
            try {
                if (GeneralAnalyzerTreeExporter.writeTree(root, file.getAbsolutePath())) {
                    System.out.println("Tree successfully saved to " + file.getAbsolutePath());
                } else {
                    System.err.println("Could not save tree to " + file.getAbsolutePath());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.err.println("Could not save tree to " + file.getAbsolutePath());
            }
        }
    }

    private void updateNode(boolean add) {
        if (tree == null || baseAnalyzerList == null) {
            return;
        }
        TreePath path = tree.getSelectionPath();

        DefaultMutableTreeNode node;
        if (path == null && !add) {
            return;
        } else if (path != null) {
            node = (DefaultMutableTreeNode) path.getLastPathComponent();
        } else {
            node = root;
        }

        String name = edtName.getText();
        DefaultListModel<AnalyzerBase> analyzers = (DefaultListModel<AnalyzerBase>) activeAnalyzerList.getModel();

        AnalyzerCollection col;
        if (analyzers.size() == 0) {
            col = new AnalyzerCollection(name);
        } else {
            AnalyzerBase[] a = new AnalyzerBase[analyzers.size()];
            for (int i = 0; i < analyzers.size(); i++) {
                a[i] = analyzers.get(i);
            }
            col = new AnalyzerCollection(name, a);
        }
        if (add) {
            treeModel.insertNodeInto(new DefaultMutableTreeNode(col), node, 0);
        } else {
            node.setUserObject(col);
            treeModel.nodeChanged(node);
        }

    }

    private static class AnalyzerTransferable implements Transferable {
        public static final DataFlavor ANALYZER_DATA_FLAVOR = new DataFlavor(AnalyzerBase.class, "Analyzer");

        private final AnalyzerBase analyzer;

        public AnalyzerTransferable(AnalyzerBase analyzer) {
            this.analyzer = analyzer;
        }

        @Override
        public AnalyzerBase getTransferData(DataFlavor flavor) {
            return analyzer;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{ANALYZER_DATA_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(ANALYZER_DATA_FLAVOR);
        }

    }

    private static class BaseListTransferHandler extends TransferHandler {
        /**
         *
         */
        private static final long serialVersionUID = 751841181393387149L;

        @Override
        protected Transferable createTransferable(JComponent c) {
            Transferable t = null;
            JList list = (JList) c;

            Object value = list.getSelectedValue();
            if (value instanceof AnalyzerBase) {
                AnalyzerBase li = (AnalyzerBase) value;
                t = new AnalyzerTransferable(li);
            }
            return t;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY;
        }
    }

    private static class AnalyzerTreeTransferHandler extends TransferHandler {

        /**
         *
         */
        private static final long serialVersionUID = -5580351519596596981L;

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(AnalyzerTransferable.ANALYZER_DATA_FLAVOR);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (canImport(support)) {
                Transferable transferable = support.getTransferable();
                try {
                    AnalyzerBase analyzer = (AnalyzerBase) transferable.getTransferData(
                            AnalyzerTransferable.ANALYZER_DATA_FLAVOR);

                    JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();

                    TreePath path = dl.getPath();
                    int childIndex = dl.getChildIndex();

                    if (path == null) {
                        return false;
                    }

                    DefaultMutableTreeNode node;
                    DefaultTreeModel model = (DefaultTreeModel) ((JTree) support.getComponent()).getModel();

                    node = (DefaultMutableTreeNode) path.getPathComponent(path.getPathCount() - 1);

                    if (childIndex < 0) {
                        AnalyzerCollection col = (AnalyzerCollection) node.getUserObject();
                        col.addAnalyzer(analyzer);
                        model.nodeChanged(node);
                    } else {
                        model.insertNodeInto(new DefaultMutableTreeNode(new AnalyzerCollection(analyzer)), node,
                                childIndex);
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    private class NodeListTransferHandler extends TransferHandler {

        /**
         *
         */
        private static final long serialVersionUID = 1719812088758497800L;
        int selectionIdx = -1;
        int dropIdx = -1;

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(AnalyzerTransferable.ANALYZER_DATA_FLAVOR);
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            Transferable t = null;
            JList list = (JList) c;

            selectionIdx = list.getSelectedIndex();
            Object value = list.getSelectedValue();
            if (value instanceof AnalyzerBase) {
                AnalyzerBase li = (AnalyzerBase) value;
                t = new AnalyzerTransferable(li);
            }
            return t;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {

            DefaultListModel<AnalyzerBase> model = (DefaultListModel<AnalyzerBase>) activeAnalyzerList.getModel();
            if (action == TransferHandler.MOVE) {
                if (selectionIdx < dropIdx) {
                    model.remove(selectionIdx);
                } else {
                    model.remove(selectionIdx + 1);
                }
            }
            super.exportDone(source, data, action);
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (canImport(support)) {
                Transferable transferable = support.getTransferable();
                try {
                    AnalyzerBase analyzer = (AnalyzerBase) transferable.getTransferData(
                            AnalyzerTransferable.ANALYZER_DATA_FLAVOR);

                    JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();

                    DefaultListModel<AnalyzerBase> listModel = (DefaultListModel<AnalyzerBase>) activeAnalyzerList
                            .getModel();

                    dropIdx = dl.getIndex();
                    listModel.insertElementAt(analyzer, dl.getIndex());

                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            return false;
        }

    }

}
