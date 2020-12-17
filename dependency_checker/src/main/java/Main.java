import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public enum TOOL_NAME {
        PYNE,
        STRUCTURE101
    }

    public static void main(String[] args) {
        if(args.length < 2) {
            System.err.println("Must specify the paths of the Structure101 xml file and the Pyne graphml file");
            System.err.println("Example: java -jar dependency_checker.jar " +
                    "\"../graphs/tajo_dependencies_by_structure101.xml\" " +
                    "\"../graphs/tajo_dependencies_by_pyne.graphml\" ");
            System.exit(1);
        }
        String structure101Path = args[0];
        String pynePath = args[1];

        try {
            File structure101Graph = new File(structure101Path);
            File pyneGraph = new File(pynePath);
            Map<String, Pkg> dependencyMap = new HashMap<>();

            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document structure101Doc = dBuilder.parse(structure101Graph);
            Document pyneDoc = dBuilder.parse(pyneGraph);
            structure101Doc.getDocumentElement().normalize();
            pyneDoc.getDocumentElement().normalize();

            addStructure101Dependencies(dependencyMap, structure101Doc);
            addPyneDependencies(dependencyMap, pyneDoc);

            compareDependencies(dependencyMap);
        } catch(FileNotFoundException e) {
            System.err.println("Could not find file to open");
            e.printStackTrace();
        } catch(ParserConfigurationException e) {
            System.err.println("Something went wrong when getting DocumentBuilder");
            e.printStackTrace();
        } catch(SAXException | IOException e) {
            System.err.println("Something went wrong when parsing a file");
            e.printStackTrace();
        }
    }

    /**
     * checks for each found dependency which tool was and wasn't able to find it, and reports the results
     * @param dependencyMap the map that has the information for which dependencies were found by which tools
     */
    public static void compareDependencies(Map<String, Pkg> dependencyMap) {
        // count the total amount of missed dependencies per tool
        Map<TOOL_NAME, Integer> missCounts = new HashMap<>();
        for(TOOL_NAME tool : TOOL_NAME.values()) {
            missCounts.put(tool, 0);
        }

        // for every package check if every tool found it
        for(Pkg pkg : dependencyMap.values()) {
            // keep a list of tools that missed this package
            List<TOOL_NAME> missedBy = new ArrayList<>();
            Map<TOOL_NAME, Boolean> recognizedBy = pkg.recognizedBy();
            for(TOOL_NAME tool : TOOL_NAME.values()) {
                // if there is a tool that didn't find this package, add it to the list, and increase its missed count
                if(!recognizedBy.get(tool)) {
                    missedBy.add(tool);
                    missCounts.put(tool, missCounts.get(tool) + 1);
                }
            }
            // only print the packages that were not found by all tools
            // TODO: make this a setting in CLI?
            if(missedBy.size() > 0)
                System.out.println(pkg.getName() + " (" +
                        (pkg.isInternal() ? "in" : "ex") + "ternal) was missed by " + missedBy);
        }

        // add a newline in between for readability
        System.out.println();

        // finally print the amount of misses per tool
        for(TOOL_NAME tool : missCounts.keySet()) {
            System.out.println(tool + " missed " + missCounts.get(tool) + " dependencies that the other tool" +
                    (TOOL_NAME.values().length > 2 ? "s" : "") + " did find");
        }
    }

    /**
     * gets the dependencies from the structure101 output and adds them to the dependencyMap
     * @param dependencyMap the map to store the dependencies in
     * @param doc the parsed document that was output by structure101
     */
    public static void addStructure101Dependencies(Map<String, Pkg> dependencyMap, Document doc) {
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
                if(pkg.startsWith("tajo")) {
                    internal = true;
                    pkg = pkg.substring(pkg.indexOf('.')+1);
                }
                //get rid of the last part of the pattern (.?)
                pkg = pkg.substring(0, pkg.length()-2);

                foundDependency(dependencyMap, pkg, TOOL_NAME.STRUCTURE101, internal);
            }
        }
    }

    /**
     * gets the dependencies from the pyne output and adds them to the dependencyMap
     * @param dependencyMap the map to store the dependencies in
     * @param doc the parsed document that was output by pyne
     */
    public static void addPyneDependencies(Map<String, Pkg> dependencyMap, Document doc) {
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

            if(shouldAdd) foundDependency(dependencyMap, pkgName, TOOL_NAME.PYNE, internal);
        }
    }

    /**
     * marks a dependency as found by the given tool
     * by updating an existing or creating a new dependency in the dependencyMap,
     * depending on whether it already exists or not
     * @param dependencyMap the map to store the dependencies in
     * @param dependency the name of the dependency
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal package
     */
    public static void foundDependency(Map<String, Pkg> dependencyMap,
                                       String dependency, TOOL_NAME toolName, Boolean internal)
    {
        if(dependencyMap.containsKey(dependency)) updateExistingDependency(dependencyMap, dependency, toolName);
        else addNewDependency(dependencyMap, dependency, toolName, internal);
    }

    /**
     * creates a new dependency in the dependencyMap
     * @param dependencyMap the map to store the dependencies in
     * @param dependency the name of the dependency
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal package
     */
    public static void addNewDependency(Map<String, Pkg> dependencyMap,
                                        String dependency, TOOL_NAME toolName, Boolean internal)
    {
        dependencyMap.put(dependency, new Pkg(dependency, internal, toolName));
    }

    /**
     * updates an existing dependency in the dependencyMap
     * @param dependencyMap the map to store the dependencies in
     * @param dependency the name of the dependency
     * @param toolName the name of the tool that found it
     */
    public static void updateExistingDependency(Map<String, Pkg> dependencyMap, String dependency, TOOL_NAME toolName) {
        dependencyMap.get(dependency).addRecognizedBy(toolName);
    }
}
