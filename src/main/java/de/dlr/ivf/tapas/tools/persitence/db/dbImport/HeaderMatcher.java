package de.dlr.ivf.tapas.tools.persitence.db.dbImport;

import de.dlr.ivf.tapas.runtime.client.util.list.ToggleSelectionModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is a panel to match csvHeaders to dbHeaders. It needs two lists of
 * headers and returns a mapping between the do usable with {@link DbCsvMap}.
 * 
 * @see JPanel
 * @author boec_pa
 * 
 */
public class HeaderMatcher extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8144993710660146669L;

	/**
	 * This class handles the key pairs shown in the bottom JList. The pairs are
	 * hold as two independent Lists and only put together on request. All
	 * access methods are synchronized to ensure thread safety.
	 * 
	 * @author boec_pa
	 * 
	 */
	private class KeyPairListModel extends AbstractListModel<String> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4039203363152111876L;

		private static final String SEPERATOR = " -- ";

		private ArrayList<String> dbKeys = new ArrayList<>();
		private ArrayList<String> csvKeys = new ArrayList<>();

		public KeyPairListModel() {
			super();
		}

		// /**
		// * Constructs a ListModel with the given pairs as values.
		// *
		// * @throws IllegalArgumentException
		// * if the sizes of the lists do not coincide.
		// */
		// public KeyPairListModel(List<String> dbKeys, List<String> csvKeys) {
		// super();
		// if (dbKeys.size() != csvKeys.size()) {
		// throw new IllegalArgumentException(
		// "The size of the lists must coincide.");
		// }
		// this.dbKeys.addAll(dbKeys);
		// this.csvKeys.addAll(csvKeys);
		// }

		/**
		 * @return <code>true</code> if at least one of the given keys is
		 *         already in the pairing.
		 */
		public synchronized boolean contains(String dbKey, String csvKey) {
			return dbKeys.contains(dbKey) || csvKeys.contains(csvKey);
		}

		@Override
		public synchronized String getElementAt(int index) {
			return dbKeys.get(index) + SEPERATOR + csvKeys.get(index);
		}

		@Override
		public synchronized int getSize() {
			return dbKeys.size();
		}

		public synchronized void addElement(String dbKey, String csvKey) {
			dbKeys.add(dbKey);
			csvKeys.add(csvKey);
			fireIntervalAdded(this, dbKeys.size() - 1, dbKeys.size() - 1);
		}

		public synchronized String remove(int index) {
			String result = getElementAt(index);
			dbKeys.remove(index);
			csvKeys.remove(index);
			fireIntervalRemoved(this, index, index);
			return result;
		}

		public synchronized void remove(int[] indices) {
			int n = indices.length;
			for (int i = n - 1; i >= 0; --i) {
				remove(indices[i]);
			}
		}

		public synchronized HashMap<String, String> getMapping() {
			HashMap<String, String> result = new HashMap<>();
			for (int i = 0; i < dbKeys.size(); ++i) {
				result.put(dbKeys.get(i), csvKeys.get(i));
			}
			return result;
		}
	}

	private static final int CONTROL_WIDTH = 100;

	private KeyPairListModel lstPairsModel;

	private JList<String> lstDB;
	private JList<String> lstCSV;
	private JList<String> lstPairs;
	private JLabel lblStatus;
	private JButton btnAdd;

	public HeaderMatcher(List<String> dbHeaders, List<String> csvHeaders) {
		super();
		initGUI(dbHeaders, csvHeaders);
	}

	private void initGUI(List<String> dbHeaders, List<String> csvHeaders) {

		DefaultListModel<String> lstDBModel = new DefaultListModel<>();
		DefaultListModel<String> lstCSVModel = new DefaultListModel<>();
		lstPairsModel = new KeyPairListModel();

		for (String s : dbHeaders) {
			lstDBModel.addElement(s);
		}

		for (String s : csvHeaders) {
			lstCSVModel.addElement(s);
		}

		lstDB = new JList<>(lstDBModel);
		lstCSV = new JList<>(lstCSVModel);
		lstPairs = new JList<>(lstPairsModel);

		lstDB.setSelectionModel(new ToggleSelectionModel());
		lstCSV.setSelectionModel(new ToggleSelectionModel());
		lstPairs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		lstPairs.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent arg0) {
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					lstPairsModel.remove(lstPairs.getSelectedIndices());
				}
			}
		});

		ListSelectionListener selectionListener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				btnAdd.setEnabled(lstDB.getSelectedIndex() >= 0
						&& lstCSV.getSelectedIndex() >= 0);
			}
		};

		lstDB.getSelectionModel().addListSelectionListener(selectionListener);
		lstCSV.getSelectionModel().addListSelectionListener(selectionListener);

		JPanel controlPanel = new JPanel();
		controlPanel.setMinimumSize(new Dimension(CONTROL_WIDTH, 0));
		controlPanel.setMaximumSize(new Dimension(CONTROL_WIDTH, 0));
		controlPanel.setPreferredSize(new Dimension(CONTROL_WIDTH, 0));

		btnAdd = new JButton("Add");
		btnAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String dbKey = lstDB.getSelectedValue();
				String csvKey = lstCSV.getSelectedValue();
				if (!lstPairsModel.contains(dbKey, csvKey)) {
					lstPairsModel.addElement(dbKey, csvKey);
				} else {
					lblStatus
							.setText("<html><p>Parts of this pair are already used.</p>");
				}
			}
		});
		lblStatus = new JLabel();

		// TODO @PB fix jumping gui

		controlPanel
				.setLayout(new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS));
		btnAdd.setAlignmentX(CENTER_ALIGNMENT);
		btnAdd.setEnabled(false);

		lblStatus.setAlignmentX(CENTER_ALIGNMENT);
		controlPanel.add(btnAdd);
		controlPanel.add(Box.createVerticalGlue());
		controlPanel.add(lblStatus);

		// delete status again
		addMouseListener(new MouseAdapter() {
			// TODO @PB listen to global mouse click
			@Override
			public void mouseClicked(MouseEvent e) {
				lblStatus.setText("");
			}
		});

		// put components to panel
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.ipadx = 10;
		gbc.ipady = 10;
		gbc.fill = GridBagConstraints.BOTH;

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0.4;
		gbc.weighty = 0.7;
		add(new JScrollPane(lstDB), gbc);

		gbc.gridx = 2;
		add(new JScrollPane(lstCSV), gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.2;
		add(controlPanel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0.3;
		gbc.gridwidth = 3;
		add(new JScrollPane(lstPairs), gbc);
	}

	public HashMap<String, String> getMapping() {
		return lstPairsModel.getMapping();
	}

	/**
	 * Testing purposes only
	 */
	public static void main(String[] args) {
		JFrame frame = new JFrame("Header Matcher");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		List<String> dbHeaders = new ArrayList<>();
		List<String> csvHeaders = new ArrayList<>();

		dbHeaders.add("Ein dbKey");
		dbHeaders.add("Ein weiterer dbKey");
		dbHeaders.add("Und noch ein dbKey");

		csvHeaders.add("Ein csvKey");
		csvHeaders.add("Ein weiterer csvKey");
		csvHeaders.add("Und noch ein csvKey");

		HeaderMatcher hm = new HeaderMatcher(dbHeaders, csvHeaders);
		hm.setOpaque(true);
		frame.setContentPane(hm);
		frame.pack();
		frame.setVisible(true);
	}

}
