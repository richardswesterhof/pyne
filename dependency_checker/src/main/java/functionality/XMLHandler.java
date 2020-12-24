package functionality;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class XMLHandler {

    // final String declarations
    public static final String ALL_DEPS = "allDependencies";
    public static final String ALL_PKGS = "allPackages";
    public static final String COUNT = "count";
    public static final String DEPENDENCY = "dependency";
    public static final String FOUND_PKGS = "foundPackages";
    public static final String FOUND_DEPS = "foundDependencies";
    public static final String INTERNAL = "internal";
    public static final String MISSED_PKGS = "missedPackages";
    public static final String MISSED_DEPS = "missedDependencies";
    public static final String NAME = "name";
    public static final String PACKAGE = "package";
    public static final String PERCENTAGE_TOTAL = "percentageTotal";
    public static final String PERCENTAGE_INTERNAL = "percentageInternal";
    public static final String PERCENTAGE_EXTERNAL = "percentageExternal";
    public static final String RESULTS = "results";
    public static final String TOOL = "tool";
    public static final String TOOLS = "tools";
    public static final String UNINITIALIZED_INT = "-1";


    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    /**
     * initializes an xml document according to the template stored in ./documentation/template_report.xml
     * @param dBuilder the document builder to use for building this document
     * @param toolNames the names of all tools
     * @return the document representing the template
     */
    public static Document initializeDoc(DocumentBuilder dBuilder, List<String> toolNames) {
        Document template = dBuilder.newDocument();
        // create root element in results
        Element root = template.createElement(RESULTS);

        // create allPackages Element in root
        Element allPkgs = template.createElement(ALL_PKGS);
        allPkgs.setAttribute(COUNT, UNINITIALIZED_INT);
        root.appendChild(allPkgs);

        // create tools Element in root
        Element tools = template.createElement(TOOLS);
        tools.setAttribute(COUNT, Integer.toString(toolNames.size()));
        root.appendChild(tools);

        // create Elements for each tool in the toolNames in the tools Element
        for(String toolName : toolNames) {
            Element tool = template.createElement(TOOL);
            tool.setAttribute(NAME, toolName);

            // create the list of found packages
            Element foundPkgs = template.createElement(FOUND_PKGS);
            foundPkgs.setAttribute(COUNT, UNINITIALIZED_INT);
            foundPkgs.setAttribute(PERCENTAGE_TOTAL, UNINITIALIZED_INT);
            foundPkgs.setAttribute(PERCENTAGE_INTERNAL, UNINITIALIZED_INT);
            foundPkgs.setAttribute(PERCENTAGE_EXTERNAL, UNINITIALIZED_INT);
            tool.appendChild(foundPkgs);

            // create the list of missed packages
            Element missedPkgs = template.createElement(MISSED_PKGS);
            missedPkgs.setAttribute(COUNT, UNINITIALIZED_INT);
            missedPkgs.setAttribute(PERCENTAGE_TOTAL, UNINITIALIZED_INT);
            missedPkgs.setAttribute(PERCENTAGE_INTERNAL, UNINITIALIZED_INT);
            missedPkgs.setAttribute(PERCENTAGE_EXTERNAL, UNINITIALIZED_INT);
            tool.appendChild(missedPkgs);

            tools.appendChild(tool);
        }

        template.appendChild(root);
        return template;
    }

    public static Element createPackage(Document doc, Pkg pkg) {
        return createPackage(doc, pkg.getName(), pkg.isInternal());
    }

    public static Element createPackage(Document doc, String pkg, Boolean internal) {
        Element pack = doc.createElement(PACKAGE);
        pack.setAttribute(INTERNAL, internal.toString());
        pack.appendChild(doc.createTextNode(pkg));

        return pack;
    }

    public static void addAllPackages(Document doc, Node pkgListNode, Set<Pkg> packages) {
        for(Pkg pkg : packages) {
            pkgListNode.appendChild(createPackage(doc, pkg));
        }
    }

    public static void addAllPackages(Document doc, Node pkgListNode, Set<Pkg> internalPkgs, Set<Pkg> externalPkgs) {
        addAllPackages(doc, pkgListNode, internalPkgs);
        addAllPackages(doc, pkgListNode, externalPkgs);
    }

    public static void setNodeAttribute(Node node, String attribute, String value) {
        node.getAttributes().getNamedItem(attribute).setNodeValue(value);
    }

    public static void writeXML(Document doc, File output) throws TransformerException, IOException {
        // get all the necessary classes to write xml to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        FileWriter writer = new FileWriter(output);
        StreamResult result = new StreamResult(writer);

        // settings to pretty print to make it more human readable
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        // write the xml
        transformer.transform(source, result);
    }
}