package functionality;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static functionality.XMLHandler.*;

public class Comparator {

    public enum TOOL_NAME {
        // IDEAL is reserved for keeping track of the total of all tools
        // it can be seen as a theoretical tool that has the knowledge of all other tools combined
        IDEAL,
        STRUCTURE101,
        PYNE
    }

    private Map<TOOL_NAME, ToolPerformance> toolPerformances = new HashMap<>();
    private Map<String, Pkg> dependencyMap = new HashMap<>();

    File structure101File;
    File pyneFile;
    Document structure101Doc;
    Document pyneDoc;

    DocumentBuilder dBuilder;

    public Comparator(File structure101File, File pyneFile) {
        this.structure101File = structure101File;
        this.pyneFile = pyneFile;

        // initialize tool performances
        for (TOOL_NAME tool : TOOL_NAME.values()) {
            toolPerformances.put(tool, new ToolPerformance(tool));
        }
    }

    /**
     * inits the xml related properties of this class
     * @return this, to allow it to be chained right after the constructor call
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Comparator initXML() throws ParserConfigurationException, SAXException, IOException {
        // initialize the document trees representing the XML files
        dBuilder = XMLHandler.getDocumentBuilder();
        // parse files
        structure101Doc = dBuilder.parse(structure101File);
        pyneDoc = dBuilder.parse(pyneFile);
        // sanitize tree
        structure101Doc.getDocumentElement().normalize();
        pyneDoc.getDocumentElement().normalize();

        return this;
    }

    /**
     * collects the dependencies for all tools
     */
    public void collectAllDependencies() {
        addStructure101Dependencies(structure101Doc);
        addPyneDependencies(pyneDoc);
        // this is where one would add calls to other tool specific methods
        // in case a new tool joins the comparison
    }

    /**
     * checks for each found dependency which tool was and wasn't able to find it, and reports the results
     * @return a Document tree containing the results per tool
     */
    public Document compareDependencies() {
        // store the ideal performance for reference
        ToolPerformance idealPerformance = toolPerformances.get(TOOL_NAME.IDEAL);

        // perform an ancient ritual to summon a List<String> from an Enum
        List<String> toolNames = new ArrayList<>(Arrays.asList(Stream.of(TOOL_NAME.values()).map(TOOL_NAME::toString).toArray(String[]::new)));
        // and make sure to remove the ideal tool, since it will be handled differently from normal tools
        toolNames.remove(TOOL_NAME.IDEAL.toString());

        // get the template doc from the XMLHandler
        Document doc = XMLHandler.initializeDoc(dBuilder, toolNames);

        // get the two children of the root element
        Node allDeps = doc.getElementsByTagName(XMLHandler.ALL_DEPS).item(0);
        Node tools = doc.getElementsByTagName(XMLHandler.TOOLS).item(0);

        // set the count of total dependencies in the document
        XMLHandler.setNodeAttribute(allDeps, COUNT, Integer.toString(idealPerformance.getHitCount()));

        // add all found dependencies to the list of dependencies
        for(String dependencyName : idealPerformance.getHits()) {
            Pkg dependency = dependencyMap.get(dependencyName);
            allDeps.appendChild(XMLHandler.createDependency(doc, dependency.getName(), dependency.isInternal()));
        }

        NodeList toolNodes = tools.getChildNodes();
        for(int i = 0; i < toolNodes.getLength(); i++) {
            Node toolNode = toolNodes.item(i);
            // get the performance of the tool that this node represents
            ToolPerformance performance = toolPerformances.get(TOOL_NAME.valueOf(
                    toolNode.getAttributes().getNamedItem(NAME).getTextContent()));

            Node foundDeps = null, missedDeps = null;
            NodeList childNodes = toolNode.getChildNodes();
            for(int j = 0; j < childNodes.getLength(); j++) {
                Node child = childNodes.item(j);
                String nodeName = child.getNodeName();
                // assign the correct variable based on tagname of the child
                if(nodeName.equals(FOUND_DEPS)) foundDeps = child;
                else if(nodeName.equals(MISSED_DEPS)) missedDeps = child;
            }

            // make sure these are not null before we proceed
            assert foundDeps != null;
            assert missedDeps != null;

            // get the missing internal dependencies from this tool
            // by starting with all (ideal) internal dependencies
            // and subtracting the actual found internal dependencies
            Set<String> internalMissing = new HashSet<>(idealPerformance.getInternalHits());
            internalMissing.removeAll(performance.getInternalHits());

            // same for external dependencies
            Set<String> externalMissing = new HashSet<>(idealPerformance.getExternalHits());
            externalMissing.removeAll(performance.getExternalHits());

            // add the found and missing dependencies to the tree
            XMLHandler.addAllDependencies(doc, foundDeps, getDependenciesByNames(performance.getHits()));
            XMLHandler.addAllDependencies(doc, missedDeps, getDependenciesByNames(internalMissing),
                    getDependenciesByNames(externalMissing));

            // calculate percentages
            float internalMissPercent = (float)internalMissing.size() /
                    idealPerformance.getInternalHitCount() * 100;

            float externalMissPercent = (float)externalMissing.size() /
                    idealPerformance.getExternalHitCount() * 100;

            float totalMissPercent = (float)(internalMissing.size() + externalMissing.size()) /
                    idealPerformance.getHitCount() * 100;

            setNodeAttribute(foundDeps, COUNT, Integer.toString(performance.getHitCount()));
            setNodeAttribute(foundDeps, PERCENTAGE_TOTAL, Float.toString(100 - totalMissPercent));
            setNodeAttribute(foundDeps, PERCENTAGE_INTERNAL, Float.toString(100 - internalMissPercent));
            setNodeAttribute(foundDeps, PERCENTAGE_EXTERNAL, Float.toString(100 - externalMissPercent));

            setNodeAttribute(missedDeps, COUNT, Integer.toString(internalMissing.size() + externalMissing.size()));
            setNodeAttribute(missedDeps, PERCENTAGE_TOTAL, Float.toString(totalMissPercent));
            setNodeAttribute(missedDeps, PERCENTAGE_INTERNAL, Float.toString(internalMissPercent));
            setNodeAttribute(missedDeps, PERCENTAGE_EXTERNAL, Float.toString(externalMissPercent));
        }

        return doc;
    }

    /**
     * gets the dependencies from the Structure101 output and adds them to the dependencyMap
     * and adds them to the list of found dependencies of Structure101
     * @param doc the parsed document that was output by Structure101
     */
    private void addStructure101Dependencies(Document doc) {
        NodeList cells = doc.getElementsByTagName("cell");

        for(int i = 0; i < cells.getLength(); i++) {
            Node cell = cells.item(i);

            //get the pattern attribute from each cell
            String pkg = cell.getAttributes().getNamedItem("pattern").getTextContent();

            //remove the ones that end with *, these are parent packages and we only want leaves
            if(!pkg.endsWith("*")) {
                // the most elegant way in the whole world to determine whether this is an internal package or not
                // external packages are descendants of a cell with name "(unknown)"
                // you might expect all descendant names to start with "(unknown)" as well then
                // but this is not true for one whole cell, so we have to resort to this beauty :)
                Boolean isInternal = !cell.getParentNode().getParentNode().getParentNode().getAttributes()
                        .getNamedItem("name").getTextContent().equals("(unknown)");

                // Structure101 prepends internal package patterns with the project name, so we remove that
                if(isInternal) {
                    pkg = pkg.substring(pkg.indexOf('.')+1);
                }

                //get rid of the last part of the pattern (.?)
                pkg = pkg.substring(0, pkg.length()-2);

                foundDependency(pkg, TOOL_NAME.STRUCTURE101, isInternal);
            }
        }
    }

    /**
     * gets the dependencies from the Pyne output and adds them to the dependencyMap
     * and adds them to the list of found dependencies of Pyne
     * @param doc the parsed document that was output by Pyne
     */
    private void addPyneDependencies(Document doc) {
        // we want to get all nodes called "node"
        NodeList nodes = doc.getElementsByTagName("node");

        for(int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            // each node contains a set of nodes called "data"
            NodeList datas = node.getChildNodes();
            String pkgName = "";
            Boolean internal = true;
            Boolean shouldAdd = true;

            for(int j = 0; j < datas.getLength(); j++) {
                Node data = datas.item(j);

                // each "data" node has an attribute "key" that specifies the type of data contained in this node
                String key = data.getAttributes().getNamedItem("key").getTextContent();

                // the "labelV" key indicates the type of object this node represents (package, class)
                // we are interested in the packages, so the rest gets filtered out
                if(key.equals("labelV") && !data.getTextContent().equals("package")) {
                    shouldAdd = false;
                    break;
                }

                // the "PackageType" key indicates whether the class is internal or external
                // (SystemPackage or RetrievedPackage, respectively)
                else if(key.equals("PackageType")) internal = data.getTextContent().equals("SystemPackage");

                    // the "name" key indicates the name of the package
                else if(key.equals("name")) pkgName = data.getTextContent();
            }

            if(shouldAdd) foundDependency(pkgName, TOOL_NAME.PYNE, internal);
        }
    }

    /**
     * marks a dependency as found by the given tool
     * by updating an existing or creating a new dependency in the dependencyMap,
     * depending on whether it already exists or not
     * and adding it to the list of found dependencies of the given tool
     * @param dependency the name of the dependency
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal package
     */
    private void foundDependency(String dependency, TOOL_NAME toolName, Boolean internal) {
        if(dependencyMap.containsKey(dependency))
            updateExistingDependency(dependency, toolName);
        else
            addNewDependency(dependency, toolName, internal);
    }

    /**
     * creates a new dependency in the dependencyMap
     * and adds it to the list of found dependencies of the given tool
     * @param dependency the name of the dependency
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal package
     */
    private void addNewDependency(String dependency, TOOL_NAME toolName, Boolean internal) {
        // create a new package with the given parameters in the dependencyMap
        Pkg pkg = new Pkg(dependency, internal);
        dependencyMap.put(dependency, pkg);

        // and add this package to the list of found dependencies of the given tool and of the ideal tool
        toolPerformances.get(toolName).addDepByPkg(pkg);
        toolPerformances.get(TOOL_NAME.IDEAL).addDepByPkg(pkg);
    }

    /**
     * updates an existing dependency in the dependencyMap
     * and adds it to the list of found dependencies of the given tool
     * @param dependency the name of the dependency
     * @param toolName the name of the tool that found it
     */
    private void updateExistingDependency(String dependency, TOOL_NAME toolName) {
        Pkg dep = dependencyMap.get(dependency);
        toolPerformances.get(toolName).addDepByPkg(dep);
    }

    private Set<Pkg> getDependenciesByNames(Set<String> names) {
        Set<Pkg> dependencies = new HashSet<>();
        for(String name : names) {
            dependencies.add(dependencyMap.get(name));
        }
        return dependencies;
    }
}
