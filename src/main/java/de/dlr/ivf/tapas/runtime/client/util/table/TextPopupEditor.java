package de.dlr.ivf.tapas.runtime.client.util.table;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;

public class TextPopupEditor extends AbstractCellEditor implements
		TableCellEditor, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8580680893618231459L;

	private class TextAreaDialog extends JDialog {
		/**
		 * 
		 */
		private static final long serialVersionUID = 159198378419035738L;
		private static final String LEAVE_SAVE = "leave_save";
		private static final String LEAVE_DISCARD = "leave_discard";
		private static final String ENTER = "enter";

		private JTextArea textArea;
		private String oldText;

		public TextAreaDialog() {
			super((Frame) null, false);
			setUndecorated(true);
			setAlwaysOnTop(true);

			textArea = new JTextArea();

			ScrollPane scroll = new ScrollPane();
			scroll.add(textArea);

			add(scroll);
			Action saveText = new AbstractAction() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1047949115734977230L;

				public void actionPerformed(ActionEvent actionEvent) {
					hide(true);
				}
			};

			Action discardText = new AbstractAction() {
				/**
				 * 
				 */
				private static final long serialVersionUID = 3004642044703844325L;

				public void actionPerformed(ActionEvent actionEvent) {
					hide(false);
				}
			};

			Action addNewLine = new AbstractAction() {
				/**
				 * 
				 */
				private static final long serialVersionUID = -6205098910123098253L;

				public void actionPerformed(ActionEvent actionEvent) {
					textArea.insert("\n", textArea.getCaretPosition());
				}
			};

			addWindowFocusListener(new WindowFocusListener() {

				@Override
				public void windowLostFocus(WindowEvent arg0) {
					hide(false);
				}

				@Override
				public void windowGainedFocus(WindowEvent arg0) {
					//
				}
			});

			textArea.getInputMap().put(
					KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), LEAVE_SAVE);

			textArea.getInputMap().put(
					KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
					LEAVE_DISCARD);

			textArea.getInputMap().put(
					KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
							InputEvent.CTRL_DOWN_MASK), ENTER);

			textArea.getActionMap().put(LEAVE_SAVE, saveText);
			textArea.getActionMap().put(LEAVE_DISCARD, discardText);
			textArea.getActionMap().put(ENTER, addNewLine);

		}

		public void show(String oldText, Point position) {

			SwingUtilities.convertPointToScreen(position, table);

			setLocation(position);
			if (!oldText.equals(this.oldText)) {
				this.oldText = oldText;
				textArea.setText(oldText);
			}

			setSize(new Dimension(table.getColumnModel().getColumn(1)
					.getWidth(), 100));

			setVisible(true);
		}

		public void hide(boolean save) {
			if (save) {
				oldText = textArea.getText();

			} else {
				textArea.setText(oldText);
			}

			setVisible(false);
			fireEditingStopped();
		}

		public String getText() {
			return textArea.getText();
		}
	}

	private static final String EDIT = "edit";

	private JButton button;
	private TextAreaDialog window;
	private JTable table;

	int curRow, curCol;
	String curText;

	public TextPopupEditor() {
		super();
		button = new JButton();
		button.setActionCommand(EDIT);
		button.addActionListener(this);
		button.setBorderPainted(false);

		window = new TextAreaDialog();

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (EDIT.equals(e.getActionCommand())) {
			window.show(curText, table.getCellRect(curRow, curCol, true)
					.getLocation());
			// fireEditingStopped();
		}

	}

	@Override
	public Object getCellEditorValue() {
		return window.getText();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		curCol = column;
		curRow = row;
		curText = (String) value;
		this.table = table;
		return button;
	}

}
