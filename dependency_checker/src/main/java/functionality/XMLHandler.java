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
    public static final String ALL_CLSS = "allClasses";
    public static final String COUNT = "count";
    public static final String COUNT_EXTERNAL = "countExternal";
    public static final String COUNT_INTERNAL = "countInternal";
    public static final String DEPENDENCY = "dep";
    public static final String FOUND_CLSS = "foundClasses";
    public static final String FOUND_DEPS = "foundDependencies";
    public static final String FROM_ID = "fromID";
    public static final String FROM_IS_INTERNAL = "fromIsInternal";
    public static final String FROM_NAME = "fromName";
    public static final String ID = "id";
    public static final String INTERNAL = "internal";
    public static final String MISSED_CLSS = "missedClasses";
    public static final String MISSED_DEPS = "missedDependencies";
    public static final String NAME = "name";
    public static final String CLASS = "class";
    public static final String PERCENTAGE_TOTAL = "percentageTotal";
    public static final String PERCENTAGE_INTERNAL = "percentageInternal";
    public static final String PERCENTAGE_EXTERNAL = "percentageExternal";
    public static final String RESULTS = "results";
    public static final String TO_ID = "toID";
    public static final String TO_IS_INTERNAL = "toIsInternal";
    public static final String TO_NAME = "toName";
    public static final String TOOL = "tool";
    public static final String TOOLS = "tools";
    public static final String UNINITIALIZED_INT = "0";


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

        // create allClasses Element in root
        Element allClss = template.createElement(ALL_CLSS);
        allClss.setAttribute(COUNT, UNINITIALIZED_INT);
        allClss.setAttribute(COUNT_INTERNAL, UNINITIALIZED_INT);
        allClss.setAttribute(COUNT_EXTERNAL, UNINITIALIZED_INT);
        root.appendChild(allClss);

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

            // create the list of found classes
            Element foundClss = createClassList(template, FOUND_CLSS);
            tool.appendChild(foundClss);

            // create the list of missed classes
            Element missedClss = createClassList(template, MISSED_CLSS);
            tool.appendChild(missedClss);

            // create the list of found dependencies
            Element foundDeps = template.createElement(FOUND_DEPS);
            foundDeps.setAttribute(COUNT, UNINITIALIZED_INT);
            foundDeps.setAttribute(PERCENTAGE_TOTAL, UNINITIALIZED_INT);
            tool.appendChild(foundDeps);

            // create the list of missed dependencies
            Element missedDeps = template.createElement(MISSED_DEPS);
            missedDeps.setAttribute(COUNT, UNINITIALIZED_INT);
            missedDeps.setAttribute(PERCENTAGE_TOTAL, UNINITIALIZED_INT);
            tool.appendChild(missedDeps);

            tools.appendChild(tool);
        }

        template.appendChild(root);
        return template;
    }

    public static Element createClass(Document doc, Cls cls) {
        return createClass(doc, cls.getCleanName(), cls.isInternal(), cls.getToolId(Comparator.TOOL_NAME.IDEAL));
    }

    private static Element createClass(Document doc, String cls, Boolean internal, Integer id) {
        Element clas = doc.createElement(CLASS);
        clas.setAttribute(ID, id.toString());
        clas.setAttribute(INTERNAL, internal.toString());
        clas.appendChild(doc.createTextNode(cls));

        return clas;
    }

    public static Element createClassList(Document template, String listName) {
        Element list = template.createElement(listName);
        list.setAttribute(COUNT, UNINITIALIZED_INT);
        list.setAttribute(COUNT_INTERNAL, UNINITIALIZED_INT);
        list.setAttribute(COUNT_EXTERNAL, UNINITIALIZED_INT);
        list.setAttribute(PERCENTAGE_TOTAL, UNINITIALIZED_INT);
        list.setAttribute(PERCENTAGE_INTERNAL, UNINITIALIZED_INT);
        list.setAttribute(PERCENTAGE_EXTERNAL, UNINITIALIZED_INT);

        return list;
    }

    public static Element createExtendedDependency(Document doc, Dep dep) {
        Element dependency = createSimpleDependency(doc, dep);

        Element fii = doc.createElement(FROM_IS_INTERNAL);
        Element fn = doc.createElement(FROM_NAME);
        fii.appendChild(doc.createTextNode(Boolean.toString(dep.getFrom().isInternal())));
        fn.appendChild(doc.createTextNode(dep.getFrom().getCleanName()));
        dependency.appendChild(fii);
        dependency.appendChild(fn);

        Element tii = doc.createElement(TO_IS_INTERNAL);
        Element tn = doc.createElement(TO_NAME);
        tii.appendChild(doc.createTextNode(Boolean.toString(dep.getTo().isInternal())));
        tn.appendChild(doc.createTextNode(dep.getTo().getCleanName()));
        dependency.appendChild(tii);
        dependency.appendChild(tn);

        return dependency;
    }

    public static Element createSimpleDependency(Document doc, Dep dep) {
        Element dependency = doc.createElement(DEPENDENCY);

        dependency.setAttribute(FROM_ID, Integer.toString(dep.getFrom().getId()));
        dependency.setAttribute(TO_ID, Integer.toString(dep.getTo().getId()));
        dependency.appendChild(doc.createTextNode(""));

        return dependency;
    }

    public static void addAllClasses(Document doc, Node pkgListNode, Set<Cls> classes) {
        for(Cls cls : classes) {
            pkgListNode.appendChild(createClass(doc, cls));
        }
    }

    public static void addAllClasses(Document doc, Node pkgListNode, Set<Cls> internalCls, Set<Cls> externalCls) {
        addAllClasses(doc, pkgListNode, internalCls);
        addAllClasses(doc, pkgListNode, externalCls);
    }

    public static void addAllSimpleDependencies(Document doc, Node depListNode, Set<Dep> dependencies) {
        for(Dep dep : dependencies) {
            depListNode.appendChild(createSimpleDependency(doc, dep));
        }
    }

    public static void addAllExtendedDependencies(Document doc, Node depListNode, Set<Dep> dependencies) {
        for(Dep dep : dependencies) {
            depListNode.appendChild(createExtendedDependency(doc, dep));
        }
    }

    public static void setNodeAttribute(Node node, String attribute, String value) {
        node.getAttributes().getNamedItem(attribute).setNodeValue(value);
    }

    public static void writeXML(Document doc, File output, boolean pretty) throws TransformerException, IOException {
        // get all the necessary classes to write xml to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        FileWriter writer = new FileWriter(output);
        StreamResult result = new StreamResult(writer);

        // settings to pretty print to make it more human readable
        if(pretty) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        }

        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        // write the xml
        transformer.transform(source, result);
    }
}
