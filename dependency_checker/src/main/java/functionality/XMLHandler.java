package functionality;

import items.Cls;
import items.Dep;
import items.SrcItm;
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
    public static final String ALL_CLSS = "allClasses";
    public static final String ALL_DEPS = "allDependencies";
    public static final String ALL_PKGS = "allPackages";
    public static final String CLASS = "cls";
    public static final String COUNT = "count";
    public static final String COUNT_EXTERNAL = "countExternal";
    public static final String COUNT_INTERNAL = "countInternal";
    public static final String COUNT_UNKNOWN = "countUnknown";
    public static final String DEPENDENCY = "dep";
    public static final String FOUND_CLSS = "foundClasses";
    public static final String FOUND_DEPS = "foundDependencies";
    public static final String FOUND_PKGS = "foundPackages";
    public static final String FROM_ID = "fromID";
    public static final String FROM_IS_INTERNAL = "fromIsInternal";
    public static final String FROM_NAME = "fromName";
    public static final String ID = "id";
    public static final String INTERNAL = "internal";
    public static final String MISSED_CLSS = "missedClasses";
    public static final String MISSED_DEPS = "missedDependencies";
    public static final String MISSED_PKGS = "missedPackages";
    public static final String NAME = "name";
    public static final String PACKAGE = "pkg";
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

    private static Document initDoc(DocumentBuilder dBuilder, List<String> toolNames, boolean classLevel) {
        Document template = dBuilder.newDocument();
        // create root element in results
        Element root = template.createElement(RESULTS);

        // create allClasses Element in root
        Element allItms = template.createElement(classLevel ? ALL_CLSS : ALL_PKGS);
        allItms.setAttribute(COUNT, UNINITIALIZED_INT);
        allItms.setAttribute(COUNT_INTERNAL, UNINITIALIZED_INT);
        allItms.setAttribute(COUNT_EXTERNAL, UNINITIALIZED_INT);
        allItms.setAttribute(COUNT_UNKNOWN, UNINITIALIZED_INT);
        root.appendChild(allItms);

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

            // create the list of found items
            Element foundItms = classLevel ? createList(template, FOUND_CLSS) : createList(template, FOUND_PKGS);
            tool.appendChild(foundItms);

            // create the list of missed items
            Element missedItms = classLevel ? createList(template, MISSED_CLSS) : createList(template, MISSED_PKGS);
            tool.appendChild(missedItms);

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

    public static Document initClsDoc(DocumentBuilder dBuilder, List<String> toolNames) {
        return initDoc(dBuilder, toolNames, true);
    }

    public static Document initPkgDoc(DocumentBuilder dBuilder, List<String> toolNames) {
        return initDoc(dBuilder, toolNames, false);
    }

    public static Element createClass(Document doc, SrcItm cls) {
        return createItem(doc, cls.getName(), cls.isInternal(), cls.getToolId(Comparator.TOOL_NAME.IDEAL), true);
    }

    private static Element createItem(Document doc, String itm, Boolean internal, Integer id, boolean classLevel) {
        Element item = doc.createElement(classLevel ? CLASS : PACKAGE);
        item.setAttribute(ID, id.toString());
        item.setAttribute(INTERNAL, internal == null ? "" : internal.toString());
        item.appendChild(doc.createTextNode(itm));

        return item;
    }

    public static Element createPackage(Document doc, SrcItm pkg) {
        return createItem(doc, pkg.getName(), pkg.isInternal(), pkg.getToolId(Comparator.TOOL_NAME.IDEAL), false);
    }

    private static Element createList(Document template, String listName) {
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
        fn.appendChild(doc.createTextNode(dep.getFrom().getName()));
        dependency.appendChild(fii);
        dependency.appendChild(fn);

        Element tii = doc.createElement(TO_IS_INTERNAL);
        Element tn = doc.createElement(TO_NAME);
        tii.appendChild(doc.createTextNode(Boolean.toString(dep.getTo().isInternal())));
        tn.appendChild(doc.createTextNode(dep.getTo().getName()));
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

    public static void addAllItems(Document doc, Node itmListNode, Set<SrcItm> classes, boolean classLevel) {
        for(SrcItm itm : classes) {
            itmListNode.appendChild(classLevel ? createClass(doc, itm) : createPackage(doc, itm));
        }
    }

    public static void addAllItems(Document doc, Node pkgListNode,
                                   Set<SrcItm> internalItms, Set<SrcItm> externalItms, boolean classLevel)
    {
        addAllItems(doc, pkgListNode, internalItms, classLevel);
        addAllItems(doc, pkgListNode, externalItms, classLevel);
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
