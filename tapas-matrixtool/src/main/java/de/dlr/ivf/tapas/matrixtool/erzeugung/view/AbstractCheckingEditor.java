package de.dlr.ivf.tapas.matrixtool.erzeugung.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;

public abstract class AbstractCheckingEditor extends JTextField implements 
	TableCellEditor, DocumentListener {

		private List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();


		public AbstractCheckingEditor() {
			getDocument().addDocumentListener( this );
			setBorder(null);
			setHorizontalAlignment(JTextField.RIGHT);
		}

		public abstract Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column);

		public void addCellEditorListener(CellEditorListener l) {

			listeners.add( l );
		}

		public void cancelCellEditing() {
			// Falls abgebrochen wird, werden alle Listeners informiert
			ChangeEvent event = new ChangeEvent( this );
			for( CellEditorListener listener : listeners.toArray( new CellEditorListener[ listeners.size() ] ))
				listener.editingCanceled( event );
		}

		public Object getCellEditorValue() {
			// Gibt den aktuellen Wert des Editors zurück
			return getText();
		}

		public boolean isCellEditable( EventObject anEvent ) {
			// Im Falle eines MouseEvents, muss ein Doppelklick erfolgen, um den Editor zu aktivieren.
			// Ansonsten wird der Editor auf jeden Fall aktiviert
			if( anEvent instanceof MouseEvent )
				return ((MouseEvent)anEvent).getClickCount() > 1;

				return true;
		}

		public void removeCellEditorListener( CellEditorListener l ) {
			listeners.remove( l );
		}

		public boolean shouldSelectCell( EventObject anEvent ) {
			return true;
		}

		public boolean stopCellEditing() {
			// Sollte die Eingabe falsch sein, darf das editieren nich gestoppt werden
			try {
				if( !isValidValue() )
					return false;
			} catch (InvalidValueException e) {
				return false;
			}

			// Ansonsten werden die Listener vom stop unterrichtet
			ChangeEvent event = new ChangeEvent( this );
			for( CellEditorListener listener : listeners.toArray( new CellEditorListener[ listeners.size() ] ))
				listener.editingStopped( event );

			return true;
		}

		public void changedUpdate( DocumentEvent e ) {
			update();
		}

		public void insertUpdate( DocumentEvent e ) {
			update();
		}

		public void removeUpdate( DocumentEvent e ) {
			update();
		}

		protected abstract boolean isValidValue() throws InvalidValueException;

		public void update(){

			try {
				if(!isValidValue()){
					setBackground(Color.RED);
				} else {
					setBackground(Color.WHITE);
				}
			} catch (InvalidValueException e) {
				System.out.println(e.getLocalizedMessage());
				setBackground(Color.RED);
			}
			
//	      setBorder( BorderFactory.createLineBorder( color ));
			
		}
}
