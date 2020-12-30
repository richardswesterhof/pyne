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
    public static final String COUNT = "count";
    public static final String DEPENDENCY = "dependency";
    public static final String FOUND_DEPS = "foundDependencies";
    public static final String INTERNAL = "internal";
    public static final String MISSED_DEPS = "missedDependencies";
    public static final String NAME = "name";
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

        // create allDependencies Element in root
        Element allDeps = template.createElement(ALL_DEPS);
        allDeps.setAttribute(COUNT, UNINITIALIZED_INT);
        root.appendChild(allDeps);

        // create tools Element in root
        Element tools = template.createElement(TOOLS);
        tools.setAttribute(COUNT, Integer.toString(toolNames.size()));
        root.appendChild(tools);

        // create Elements for each tool in the toolNames in the tools Element
        for(String toolName : toolNames) {
            Element tool = template.createElement(TOOL);
            tool.setAttribute(NAME, toolName);

            // create the list of found dependencies
            Element foundDeps = template.createElement(FOUND_DEPS);
            foundDeps.setAttribute(COUNT, UNINITIALIZED_INT);
            foundDeps.setAttribute(PERCENTAGE_TOTAL, UNINITIALIZED_INT);
            foundDeps.setAttribute(PERCENTAGE_INTERNAL, UNINITIALIZED_INT);
            foundDeps.setAttribute(PERCENTAGE_EXTERNAL, UNINITIALIZED_INT);
            tool.appendChild(foundDeps);

            // create the list of missed dependencies
            Element missedDeps = template.createElement(MISSED_DEPS);
            missedDeps.setAttribute(COUNT, UNINITIALIZED_INT);
            missedDeps.setAttribute(PERCENTAGE_TOTAL, UNINITIALIZED_INT);
            missedDeps.setAttribute(PERCENTAGE_INTERNAL, UNINITIALIZED_INT);
            missedDeps.setAttribute(PERCENTAGE_EXTERNAL, UNINITIALIZED_INT);
            tool.appendChild(missedDeps);

            tools.appendChild(tool);
        }

        template.appendChild(root);
        return template;
    }

    public static Element createDependency(Document doc, Pkg dependency) {
        return createDependency(doc, dependency.getName(), dependency.isInternal());
    }

    public static Element createDependency(Document doc, String dependency, Boolean internal) {
        Element dep = doc.createElement(DEPENDENCY);
        dep.setAttribute(INTERNAL, internal.toString());
        dep.appendChild(doc.createTextNode(dependency));

        return dep;
    }

    public static void addAllDependencies(Document doc, Node depList, Set<Pkg> dependencies) {
        for(Pkg dep : dependencies) {
            depList.appendChild(createDependency(doc, dep));
        }
    }

    public static void addAllDependencies(Document doc, Node depList, Set<Pkg> internalDeps, Set<Pkg> externalDeps) {
        addAllDependencies(doc, depList, internalDeps);
        addAllDependencies(doc, depList, externalDeps);
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
