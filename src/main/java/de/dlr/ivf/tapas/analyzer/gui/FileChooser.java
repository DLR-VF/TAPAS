package de.dlr.ivf.tapas.analyzer.gui;

import com.jgoodies.forms.layout.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Eine UI-Komponente die das Auswählen einer Datei ermöglicht. Der Pfad der ausgewählten Datei wird in einem Label angezeigt.
 *
 * @author Marco
 */
public class FileChooser extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = 3701553818018480999L;
    // private Properties properties = new Properties();
    private final String name;
    private JLabel lblPath;
    private JButton btnSetzen;
    private File file = null;
    private boolean isChanged = false;

    /**
     * Create the panel.
     */
    public FileChooser(String name) {

        if (name == null) throw new IllegalArgumentException("name must be set");

        this.name = name;

        createContents();

    }

    protected File chooseFile() {
        JFileChooser fd = new JFileChooser(FileChooserHistoryManager.getLastDirectory(name));
        fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fd.setVisible(true);

        fd.setDialogTitle("Dateiauswahl");
        int value = fd.showOpenDialog(this);

        if ((value == JFileChooser.APPROVE_OPTION) && fd.getSelectedFile() != null && fd.getSelectedFile().isFile() &&
                fd.getSelectedFile().exists()) {
            FileChooserHistoryManager.updateLastDirectory(name, fd.getSelectedFile().getPath().substring(0,
                    fd.getSelectedFile().getPath().lastIndexOf(File.separatorChar)));
            return fd.getSelectedFile();
        } else if (value != JFileChooser.CANCEL_OPTION) {
            JOptionPane.showMessageDialog(this, "Keine Datei ausgewählt: ", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void createContents() {
        setLayout(new FormLayout(new ColumnSpec[]{FormSpecs.RELATED_GAP_COLSPEC, new ColumnSpec(ColumnSpec.FILL,
                Sizes.bounded(Sizes.DEFAULT, Sizes.constant("50dlu", true), Sizes.constant("50dlu", true)),
                1), FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblPath = new JLabel("Pfad zur Datei");
        add(lblPath, "2, 2, 3, 1");

        btnSetzen = new JButton("Setzen");
        btnSetzen.setHorizontalAlignment(SwingConstants.RIGHT);
        btnSetzen.addActionListener(this::do_btnSetzen_actionPerformed);
        add(btnSetzen, "4, 4");
    }

    protected void do_btnSetzen_actionPerformed(ActionEvent e) {

        file = chooseFile();

        if (file != null) {
            lblPath.setText(file.getAbsolutePath());
            lblPath.setToolTipText(file.getAbsolutePath());
            lblPath.doLayout();

            isChanged = true;
        }
    }

    public String getButtonText() {
        return btnSetzen.getText();
    }

    public void setButtonText(String text) {
        btnSetzen.setText(text);
    }

    public String getDefaultLabelText() {
        return lblPath.getText();
    }

    public void setDefaultLabelText(String text) {
        if (!isChanged) lblPath.setText(text);
    }

    public File getFile() {
        return file;
    }

    public boolean isChanged() {
        return isChanged;
    }
}
