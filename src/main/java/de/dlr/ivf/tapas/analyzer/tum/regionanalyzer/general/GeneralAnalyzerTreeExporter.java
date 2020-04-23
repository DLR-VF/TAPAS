package de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.general;

import de.dlr.ivf.tapas.analyzer.tum.constants.TuMEnums.*;
import de.dlr.ivf.tapas.analyzer.tum.regionanalyzer.AnalyzerBase;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;

public class GeneralAnalyzerTreeExporter {

    private static final String ELEMENT_ROOT = "general_analyzer";
    private static final String ELEMENT_FILE = "file";
    private static final String ELEMENT_TAB = "tab";
    private static final String ELEMENT_SPLIT = "split";
    private static final String ELEMENT_ANALYZER = "analyzer";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_VERSION = "version";
    private static final String VERSION = "1.0";

    private static int getLevel(TreeWalker walker, Node currentNode) {

        if (currentNode.getNodeName().equals(ELEMENT_ROOT)) {
            return 0;
        } else if (currentNode.getNodeName().equals(ELEMENT_FILE)) {
            return 1;
        } else if (currentNode.getNodeName().equals(ELEMENT_TAB)) {
            return 2;
        } else if (currentNode.getNodeName().equals(ELEMENT_SPLIT)) {
            return 3;
        } else if (currentNode.getNodeName().equals(ELEMENT_ANALYZER)) {
            return 4;
        }
        return -1;
    }

    /**
     * For testing purposes only
     *
     * @throws IOException
     * @throws DOMException
     */
    @SuppressWarnings("rawtypes")
    public static void main(String[] args) throws IOException, DOMException {

        EnumMap<Categories, AnalyzerBase> analyzerList = new EnumMap<>(Categories.class);

        analyzerList.put(Categories.DistanceCategoryDefault, new DefaultDistanceCategoryAnalyzer());
        analyzerList.put(Categories.Mode, new ModeAnalyzer());
        analyzerList.put(Categories.PersonGroup, new PersonGroupAnalyzer());
        analyzerList.put(Categories.RegionCode, new RegionCodeAnalyzer());
        analyzerList.put(Categories.TripIntention, new TripIntentionAnalyzer());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        DefaultMutableTreeNode file = new DefaultMutableTreeNode(new AnalyzerCollection("TUMExport"));
        root.add(file);

        // wegelängen sheet
        DefaultMutableTreeNode tab = new DefaultMutableTreeNode(
                new AnalyzerCollection("Wegelängen", analyzerList.get(Categories.RegionCode)));
        file.add(tab);

        DefaultMutableTreeNode analyzers = new DefaultMutableTreeNode(
                new AnalyzerCollection(analyzerList.get(Categories.TripIntention)));
        tab.add(analyzers);

        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(analyzerList.get(Categories.TripIntention),
                analyzerList.get(Categories.DistanceCategoryDefault)));
        tab.add(analyzers);

        // modalsplit
        tab = new DefaultMutableTreeNode(new AnalyzerCollection("Modalsplit", analyzerList.get(Categories.RegionCode)));
        file.add(tab);
        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(analyzerList.get(Categories.Mode)));
        tab.add(analyzers);
        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(analyzerList.get(Categories.Mode),
                analyzerList.get(Categories.DistanceCategoryDefault)));
        tab.add(analyzers);
        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(analyzerList.get(Categories.Mode),
                analyzerList.get(Categories.DistanceCategoryDefault), analyzerList.get(Categories.TripIntention)));
        tab.add(analyzers);

        // personengruppen
        tab = new DefaultMutableTreeNode(
                new AnalyzerCollection("Personengruppen", analyzerList.get(Categories.RegionCode)));
        file.add(tab);
        analyzers = new DefaultMutableTreeNode(new AnalyzerCollection(analyzerList.get(Categories.PersonGroup)));
        tab.add(analyzers);

        writeTree(root, "H:/Temp/tree.xml");

        printTree(readTree("H:/Temp/tree.xml"));
    }

    /**
     * for debugging only
     *
     * @param root
     */
    @SuppressWarnings("rawtypes")
    private static void printTree(DefaultMutableTreeNode root) {
        Enumeration en = root.preorderEnumeration();
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();

            for (int i = 0; i < node.getLevel(); i++) {
                System.out.print("\t");
            }
            System.out.println(node + "\t(" + node.getLevel() + ")");
        }
    }

    /**
     * Reads an XML-file written by this class and converts it into a tree
     * readable by {@link TUMControlGeneral}.
     *
     * @param filename
     * @return the root node of the created tree. May be <code>null</code> if
     * the tree could not be parsed correctly.
     * @throws IOException if the file can't be accessed.
     */
    @SuppressWarnings("rawtypes")
    public static DefaultMutableTreeNode readTree(String filename) throws IOException {

        File xmlFile = new File(filename);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder docBuilder;
        Document doc;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.parse(xmlFile);
        } catch (ParserConfigurationException | SAXException e1) {
            e1.printStackTrace();
            return null;
        }

        doc.getDocumentElement().normalize();

        if (!doc.getDocumentElement().getAttribute(ATTRIBUTE_VERSION).equals(VERSION)) {
            System.err.println("Version does not match! Expected version is " + VERSION);
            return null;
        }

        Node domRoot = doc.getDocumentElement();
        DocumentTraversal traversal = (DocumentTraversal) doc;
        TreeWalker walker = traversal.createTreeWalker(domRoot, NodeFilter.SHOW_ELEMENT, null, false);

        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode();

        Node currentDOMNode;
        DefaultMutableTreeNode parentTreeNode = treeRoot;
        currentDOMNode = walker.nextNode();
        HashSet<AnalyzerBase> analyzerSet = new HashSet<>();
        String currentName = "";

        while (currentDOMNode != null) {
            if (currentDOMNode.getNodeType() != Node.ELEMENT_NODE) {
                System.err.println("TreeExporter read something strange while processing " + currentDOMNode);
                System.err.println("The node will be ignored.");
                continue;
            }

            if (ELEMENT_ANALYZER.equals(currentDOMNode.getNodeName())) {
                analyzerSet.clear();
                // TODO add support DC special cases

                AnalyzerBase analyzer;
                try {
                    analyzer = (AnalyzerBase) Class.forName(currentDOMNode.getTextContent()).getDeclaredConstructor()
                                                   .newInstance();
                } catch (InstantiationException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | DOMException e) {
                    e.printStackTrace();
                    return null;
                }
                analyzerSet.add(analyzer);
                currentDOMNode = walker.nextNode();
                while (currentDOMNode != null && ELEMENT_ANALYZER.equals(currentDOMNode.getNodeName())) {
                    try {
                        analyzer = (AnalyzerBase) Class.forName(currentDOMNode.getTextContent())
                                                       .getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | DOMException e) {
                        e.printStackTrace();
                        return null;
                    }
                    analyzerSet.add(analyzer);
                    currentDOMNode = walker.nextNode();
                }

                AnalyzerCollection col = new AnalyzerCollection(currentName, analyzerSet.toArray(new AnalyzerBase[0]));

                parentTreeNode.setUserObject(col);

                while (currentDOMNode != null && getLevel(walker, currentDOMNode) <= parentTreeNode.getLevel()) {
                    parentTreeNode = (DefaultMutableTreeNode) parentTreeNode.getParent();
                }
            } else {
                currentName = currentDOMNode.getAttributes().getNamedItem(ATTRIBUTE_NAME).getNodeValue();
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new AnalyzerCollection(currentName));
                parentTreeNode.add(node);
                parentTreeNode = node;
                currentDOMNode = walker.nextNode();
            }
        }

        return treeRoot;
    }

    /**
     * Exports a tree used by {@link TUMControlGeneral} to an XML-file that can
     * be imported again by this class.
     *
     * @param root
     * @param filename
     * @return <code>true</code> if the export is successful.
     * @throws FileNotFoundException if there is any problem writing the file.
     */
    @SuppressWarnings("rawtypes")
    public static boolean writeTree(DefaultMutableTreeNode root, String filename) throws FileNotFoundException {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
            return false;
        }
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement(ELEMENT_ROOT);
        rootElement.setAttribute(ATTRIBUTE_VERSION, VERSION);
        doc.appendChild(rootElement);
        Element currentElement = null;
        Element currentFile = null;
        Element currentTab = null;
        Element currentSplit;

        Enumeration en = root.preorderEnumeration();
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();

            AnalyzerCollection analyzers = (AnalyzerCollection) node.getUserObject();
            // System.out.println(node.getUserObject());

            int level = node.getLevel();
            if (level == 0) {
                continue; // ignore root
            }

            switch (level) {
                case 1:
                    currentFile = doc.createElement(ELEMENT_FILE);
                    currentElement = currentFile;
                    rootElement.appendChild(currentFile);
                    break;
                case 2:
                    currentTab = doc.createElement(ELEMENT_TAB);
                    currentElement = currentTab;
                    currentFile.appendChild(currentTab);
                    break;
                case 3:
                    currentSplit = doc.createElement(ELEMENT_SPLIT);
                    currentElement = currentSplit;
                    currentTab.appendChild(currentSplit);
                    break;
            }

            currentElement.setAttribute(ATTRIBUTE_NAME, analyzers.getName());
            for (AnalyzerBase a : analyzers.getAnalyzers()) {
                Element e = doc.createElement(ELEMENT_ANALYZER);
                e.appendChild(doc.createTextNode(a.getClass().getName()));
                currentElement.appendChild(e);
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new FileOutputStream(filename));
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

}
