/*
 * Copyright (c) 2020 DLR Institute of Transport Research
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.dlr.ivf.tapas.runtime.util;

import de.dlr.ivf.tapas.logger.TPS_Logger;
import de.dlr.ivf.tapas.logger.SeverityLogLevel;

import javax.swing.*;
import java.io.*;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * This class provides multiple language support for any GUI. The language settings are stored for each user of the
 * software so everybody can choose his own language. For the multiple languages the Java ResourceBundle is
 * used.<br>
 * <br>
 * The class provides also a menu for the GUI to change the current language settings. These changes only take place
 * when the program is restarted.<br>
 * <br>
 * To use this language support you have to create a subfolder called 'resource' into the folder where the main GUI
 * class is located. In this subfolder you have to put on file called '$guiClass.getSimpleName()$.properties' where you
 * define which languages you provide for this GUI. You enter the line 'languages = ...', where the dots can be 'de',
 * 'en' and so on. Then you have to create a label file called '$guiClass.getSimpleName()$Labels_$language$.properties'
 * for each language you defined. These files have to be created how it is explained in ResourceBundle. For each
 * key and for each language you define a label text.
 *
 * @author mark_ma
 * @see ResourceBundle
 */
public class MultilanguageSupport {

    /**
     * Resource Bundle with the strings in the current language.
     */
    private static ResourceBundle RB;

    /**
     * Locale setting
     */
    private static Locale locale;

    /**
     * The method changes the current settings and stores them in the properties file.
     *
     * @param locale new local settings
     */
    private static void change(Locale locale) {
        try {
            MultilanguageSupport.locale = locale;
            store(locale);
        } catch (IOException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, e);
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error storing user language information",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Returns current global locale setting
     *
     * @return current global locale setting
     */
    public static Locale getLocale() {
        return locale;
    }

    public static void setLocale(Locale loc) {
        locale = loc;
    }

    /**
     * @param key key for the string to show in the current language
     * @return string to this key in the current language
     */
    public static String getString(String key) {
        return RB.getString(key);
    }

    /**
     * @param guiClass the main class of the GUI
     * @return file for the current users language properties for the given GUI
     */
    private static File getUsersPropertiesFile(Class<?> guiClass) {
        return new File(System.getProperty("user.dir"), guiClass.getSimpleName() + "Users.properties");
    }

    /**
     * This method loads the language settings to this GUI for the current user and returns a menu for the gui where the
     * settings can be changed.
     *
     * @param guiClass the main class of the GUI
     * @return menu with language options
     */
    public static JMenu init(final Class<?> guiClass) {
        JMenu menu = new JMenu("-");
        try {

            String user = System.getProperty("user.name");
            Properties userProperties = load(MultilanguageSupport.class);
            if (userProperties.containsKey(user)) {
                setLocale(new Locale(userProperties.getProperty(user)));
            } else {
                setLocale(new Locale(Locale.getDefault().getLanguage()));
                store(locale);
            }
            ResourceBundle MLSRB = ResourceBundle.getBundle(MultilanguageSupport.class.getSimpleName() + "Labels",
                    locale);
            menu.setText(MLSRB.getString("menu"));
            Properties languages = new Properties();
            String resource = "MultilanguageSupport.properties";
            InputStream is = ClassLoader.getSystemResourceAsStream(resource);
            languages.load(is);
            StringTokenizer st = new StringTokenizer(languages.getProperty("languages"), ", ");
            while (st.hasMoreTokens()) {
                String language = st.nextToken();
                JMenuItem item = new JMenuItem(MLSRB.getString("menu.item." + language));
                item.setName(language);
                item.addActionListener(e -> {
                    change(new Locale(((JMenuItem) e.getSource()).getName()));
                    JOptionPane.showMessageDialog(null, MLSRB.getString("message"));
                });
                menu.add(item);
            }
            RB = ResourceBundle.getBundle(guiClass.getSimpleName() + "Labels", locale);
        } catch (IOException e) {
            TPS_Logger.log(SeverityLogLevel.ERROR, e);
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error storing user language information",
                    JOptionPane.ERROR_MESSAGE);
        }

        return menu;
    }

    /**
     * This method loads the user properties from the user properties file corresponding to the given GUI
     *
     * @param guiClass the main class of the GUI
     * @return user properties to this GUI
     * @throws IOException This exception is thrown if the user properties wasn't loaded
     */
    private static Properties load(Class<?> guiClass) throws IOException {
        Properties userProperties = new Properties();
        File file = getUsersPropertiesFile(guiClass);
        if (file.exists()) userProperties.load(new FileInputStream(getUsersPropertiesFile(guiClass)));
        return userProperties;
    }

    /**
     * This method stores the current local settings for a specific user.
     *
     * @param locale current local language
     * @throws IOException This exception is thrown if the local settings wasn't stored
     */
    private static void store(Locale locale) throws IOException {
        Properties userProperties = load(MultilanguageSupport.class);
        setLocale(locale);
        userProperties.put(System.getProperty("user.name"), locale.getLanguage());
        String comment = "Auto generated users properties file " + "storing language information for each user. " +
                "The main class for this GUI is " + MultilanguageSupport.class.getName();
        userProperties.store(new FileOutputStream(getUsersPropertiesFile(MultilanguageSupport.class)), comment);
    }
}
