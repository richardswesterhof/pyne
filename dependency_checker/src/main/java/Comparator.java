import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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
     */
    public void compareDependencies() {

        ToolPerformance idealPerformance = toolPerformances.get(TOOL_NAME.IDEAL);

        // print the total amount of dependencies for reference
        System.out.println("Total unique dependencies found: " + idealPerformance.getHitCount());

        System.out.println("Performance per tool:");

        // evaluate the performance of each tool
        for(ToolPerformance performance : toolPerformances.values()) {
            // TODO: refactor some of this for loop into methods?

            // except the ideal tool, since we already know it would be perfect
            if(performance.getName().equals(TOOL_NAME.IDEAL)) continue;

            // get the missing dependencies from this tool by starting with all (ideal) dependencies
            // and subtracting the actual found dependencies
            Set<String> internalMissing = new HashSet<>(idealPerformance.getInternalHits());
            internalMissing.removeAll(performance.getInternalHits());

            // same for external dependencies
            Set<String> externalMissing = new HashSet<>(idealPerformance.getExternalHits());
            externalMissing.removeAll(performance.getExternalHits());

            float internalMissPercent = (float)internalMissing.size() /
                    idealPerformance.getInternalHitCount() * 100;

            float externalMissPercent = (float)externalMissing.size() /
                    idealPerformance.getExternalHitCount() * 100;

            float totalMissPercent = (float)(internalMissing.size() + externalMissing.size()) /
                    idealPerformance.getHitCount() * 100;


            // print the results
            System.out.println(performance.getName());

            // amount of internal (+ percentage of total internal)
            System.out.println("\t" + internalMissing.size() + " internal dependencies missed (" +
                    internalMissPercent + "% of total internal dependencies):");
            for(String dep : internalMissing) {
                System.out.println("\t\t" + dependencyMap.get(dep).getBasicInfoString());
            }

            // amount of external (+ percentage of total external)
            System.out.println("\t" + externalMissing.size() + " external dependencies missed (" +
                    externalMissPercent + "% of total external dependencies):");
            for(String dep : externalMissing) {
                System.out.println("\t\t" + dependencyMap.get(dep).getBasicInfoString());
            }

            // the total misses
            System.out.println("\t" + (internalMissing.size() + externalMissing.size()) + " total dependencies missed (" +
                    totalMissPercent + "% of total dependencies)");

            // newline for readability
            System.out.println();
        }
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
                Boolean internal = false;

                // structure101 prepends internal packages with some form of "tajo-",
                // in that case remove the first part (everything until and including the first .)
                // TODO: make the program automatically detect this
                //  to remove the one hardcoded reference to Tajo here
                if(pkg.startsWith("tajo")) {
                    internal = true;
                    pkg = pkg.substring(pkg.indexOf('.')+1);
                }

                //get rid of the last part of the pattern (.?)
                pkg = pkg.substring(0, pkg.length()-2);

                foundDependency(pkg, TOOL_NAME.STRUCTURE101, internal);
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
}
