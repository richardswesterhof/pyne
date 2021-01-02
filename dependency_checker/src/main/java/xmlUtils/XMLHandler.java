package xmlUtils;

import analysis.Comparator;
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

/**
 * class to deal with XML
 * this aims to abstract interactions with the raw tree to method calls in this class
 * to achieve a more consistent tree structure and more ease of use
 */
public class XMLHandler {
    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    private static Document initDoc(DocumentBuilder dBuilder, List<String> toolNames, boolean classLevel) {
        Document template = dBuilder.newDocument();
        // create root element in results
        Element root = template.createElement(XML_TAG.RESULTS);

        // create allClasses Element in root
        Element allItms = template.createElement(classLevel ? XML_TAG.ALL_CLSS : XML_TAG.ALL_PKGS);
        allItms.setAttribute(XML_TAG.COUNT, XML_TAG.UNINITIALIZED_INT);
        allItms.setAttribute(XML_TAG.COUNT_INTERNAL, XML_TAG.UNINITIALIZED_INT);
        allItms.setAttribute(XML_TAG.COUNT_EXTERNAL, XML_TAG.UNINITIALIZED_INT);
        allItms.setAttribute(XML_TAG.COUNT_UNKNOWN, XML_TAG.UNINITIALIZED_INT);
        root.appendChild(allItms);

        // create allDependencies Element in root
        Element allDeps = template.createElement(XML_TAG.ALL_DEPS);
        allDeps.setAttribute(XML_TAG.COUNT, XML_TAG.UNINITIALIZED_INT);
        root.appendChild(allDeps);

        // create tools Element in root
        Element tools = template.createElement(XML_TAG.TOOLS);
        tools.setAttribute(XML_TAG.COUNT, Integer.toString(toolNames.size()));
        root.appendChild(tools);

        // create Elements for each tool in the toolNames in the tools Element
        for(String toolName : toolNames) {
            Element tool = template.createElement(XML_TAG.TOOL);
            tool.setAttribute(XML_TAG.NAME, toolName);

            // create the list of found items
            Element foundItms = classLevel ?
                    createList(template, XML_TAG.FOUND_CLSS) :
                    createList(template, XML_TAG.FOUND_PKGS);
            tool.appendChild(foundItms);

            // create the list of missed items
            Element missedItms = classLevel ?
                    createList(template, XML_TAG.MISSED_CLSS) :
                    createList(template, XML_TAG.MISSED_PKGS);
            tool.appendChild(missedItms);

            // create the list of found dependencies
            Element foundDeps = template.createElement(XML_TAG.FOUND_DEPS);
            foundDeps.setAttribute(XML_TAG.COUNT, XML_TAG.UNINITIALIZED_INT);
            foundDeps.setAttribute(XML_TAG.PERCENTAGE_TOTAL, XML_TAG.UNINITIALIZED_INT);
            tool.appendChild(foundDeps);

            // create the list of missed dependencies
            Element missedDeps = template.createElement(XML_TAG.MISSED_DEPS);
            missedDeps.setAttribute(XML_TAG.COUNT, XML_TAG.UNINITIALIZED_INT);
            missedDeps.setAttribute(XML_TAG.PERCENTAGE_TOTAL, XML_TAG.UNINITIALIZED_INT);
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

    private static Element createItem(Document doc, String itm, Boolean internal, Integer id, boolean classLevel) {
        Element item = createCompactItem(doc, id, classLevel);
        item.setAttribute(XML_TAG.INTERNAL, internal == null ? "" : internal.toString());
        item.appendChild(doc.createTextNode(itm));

        return item;
    }

    private static Element createCompactItem(Document doc, Integer id, boolean classLevel) {
        Element item = doc.createElement(classLevel ? XML_TAG.CLASS : XML_TAG.PACKAGE);
        item.setAttribute(XML_TAG.ID, id.toString());

        return item;
    }

    public static Element createClass(Document doc, SrcItm cls) {
        return createItem(doc, cls.getName(), cls.isInternal(), cls.getToolId(Comparator.TOOL_NAME.IDEAL), true);
    }

    public static Element createCompactClass(Document doc, SrcItm cls) {
        return createCompactItem(doc, cls.getToolId(Comparator.TOOL_NAME.IDEAL), true);
    }

    public static Element createPackage(Document doc, SrcItm pkg) {
        return createItem(doc, pkg.getName(), pkg.isInternal(), pkg.getToolId(Comparator.TOOL_NAME.IDEAL), false);
    }

    public static Element createCompactPackage(Document doc, SrcItm pkg) {
        return createCompactItem(doc, pkg.getToolId(Comparator.TOOL_NAME.IDEAL), false);
    }


    private static Element createList(Document template, String listName) {
        Element list = template.createElement(listName);
        list.setAttribute(XML_TAG.COUNT, XML_TAG.UNINITIALIZED_INT);
        list.setAttribute(XML_TAG.COUNT_INTERNAL, XML_TAG.UNINITIALIZED_INT);
        list.setAttribute(XML_TAG.COUNT_EXTERNAL, XML_TAG.UNINITIALIZED_INT);
        list.setAttribute(XML_TAG.COUNT_UNKNOWN, XML_TAG.UNINITIALIZED_INT);
        list.setAttribute(XML_TAG.PERCENTAGE_TOTAL, XML_TAG.UNINITIALIZED_INT);
        list.setAttribute(XML_TAG.PERCENTAGE_INTERNAL, XML_TAG.UNINITIALIZED_INT);
        list.setAttribute(XML_TAG.PERCENTAGE_EXTERNAL, XML_TAG.UNINITIALIZED_INT);
        list.setAttribute(XML_TAG.PERCENTAGE_UNKNOWN, XML_TAG.UNINITIALIZED_INT);

        return list;
    }

    public static Element createExtendedDependency(Document doc, Dep dep) {
        Element dependency = createSimpleDependency(doc, dep);

        Element fii = doc.createElement(XML_TAG.FROM_IS_INTERNAL);
        Element fn = doc.createElement(XML_TAG.FROM_NAME);
        fii.appendChild(doc.createTextNode(dep.getFrom().isInternal() == null ? "" : Boolean.toString(dep.getFrom().isInternal())));
        fn.appendChild(doc.createTextNode(dep.getFrom().getName()));
        dependency.appendChild(fii);
        dependency.appendChild(fn);

        Element tii = doc.createElement(XML_TAG.TO_IS_INTERNAL);
        Element tn = doc.createElement(XML_TAG.TO_NAME);
        tii.appendChild(doc.createTextNode(dep.getTo().isInternal() == null ? "" : Boolean.toString(dep.getTo().isInternal())));
        tn.appendChild(doc.createTextNode(dep.getTo().getName()));
        dependency.appendChild(tii);
        dependency.appendChild(tn);

        return dependency;
    }

    public static Element createSimpleDependency(Document doc, Dep dep) {
        Element dependency = doc.createElement(XML_TAG.DEPENDENCY);

        dependency.setAttribute(XML_TAG.FROM_ID, Integer.toString(dep.getFrom().getId()));
        dependency.setAttribute(XML_TAG.TO_ID, Integer.toString(dep.getTo().getId()));
        dependency.appendChild(doc.createTextNode(""));

        return dependency;
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
