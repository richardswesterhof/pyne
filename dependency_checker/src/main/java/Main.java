import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
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
        File structure101Graph = new File("../graphs/tajo_by_structure101.xml");
        File pyneGraph = new File("../graphs/tajo_dependencies_pyne.graphml");
        Map<String, Pkg> dependencyMap = new HashMap<>();

        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document structure101Doc = dBuilder.parse(structure101Graph);
            Document pyneDoc = dBuilder.parse(pyneGraph);
            structure101Doc.getDocumentElement().normalize();
            pyneDoc.getDocumentElement().normalize();

            addStructure101Dependencies(dependencyMap, structure101Doc);
            addPyneDependencies(dependencyMap, pyneDoc);
        } catch(ParserConfigurationException e) {
            System.err.println("Something went wrong when getting DocumentBuilder");
            e.printStackTrace();
        } catch(SAXException | IOException e) {
            System.err.println("Something went wrong when parsing a file");
            e.printStackTrace();
        }

        compareDependencies(dependencyMap);
    }

    public static void compareDependencies(Map<String, Pkg> dependencyMap) {
        Map<TOOL_NAME, Integer> missCounts = new HashMap<>();
        for(TOOL_NAME tool : TOOL_NAME.values()) {
            missCounts.put(tool, 0);
        }

        for(Pkg pkg : dependencyMap.values()) {
            List<TOOL_NAME> missedBy = new ArrayList<>();
            Map<TOOL_NAME, Boolean> recognizedBy = pkg.recognizedBy();
            for(TOOL_NAME tool : TOOL_NAME.values()) {
                if(!recognizedBy.get(tool)) {
                    missedBy.add(tool);
                    missCounts.put(tool, missCounts.get(tool) + 1);
                }
            }
            if(missedBy.size() > 0)
                System.out.println(pkg.getName() + " (" +
                        (pkg.isInternal() ? "in" : "ex") + "ternal) was missed by " + missedBy);
        }

        // add a newline in between for readability
        System.out.println();

        for(TOOL_NAME tool : missCounts.keySet()) {
            System.out.println(tool + " missed " + missCounts.get(tool) + " dependencies that the other tool" +
                    (TOOL_NAME.values().length > 2 ? "s" : "") + " did find");
        }
    }

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

                setDependency(dependencyMap, pkg, TOOL_NAME.STRUCTURE101, internal);
            }
        }
    }

    public static void addPyneDependencies(Map<String, Pkg> dependencyMap, Document doc) {
        NodeList nodes = doc.getElementsByTagName("node");
        for(int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            NodeList datas = node.getChildNodes();
            String pkgName = "";
            Boolean internal = true;
            Boolean shouldAdd = true;
            for(int j = 0; j < datas.getLength(); j++) {
                Node data = datas.item(j);
                String key = data.getAttributes().getNamedItem("key").getTextContent();
                if(key.equals("labelV") && !data.getTextContent().equals("package")) {
                    shouldAdd = false;
                    break;
                }
                else if(key.equals("PackageType")) internal = data.getTextContent().equals("SystemPackage");
                else if(key.equals("name")) pkgName = data.getTextContent();
            }

            if(shouldAdd) setDependency(dependencyMap, pkgName, TOOL_NAME.PYNE, internal);
        }
    }

    public static void setDependency(Map<String, Pkg> dependencyMap,
                                     String dependency, TOOL_NAME toolName, Boolean internal)
    {
        if(dependencyMap.containsKey(dependency)) setExistingDependency(dependencyMap, dependency, toolName);
        else addNewDependency(dependencyMap, dependency, toolName, internal);
    }

    public static void addNewDependency(Map<String, Pkg> dependencyMap,
                                        String dependency, TOOL_NAME toolName, Boolean internal)
    {
        dependencyMap.put(dependency, new Pkg(dependency, internal, toolName));
    }

    public static void setExistingDependency(Map<String, Pkg> dependencyMap, String dependency, TOOL_NAME toolName) {
        dependencyMap.get(dependency).addRecognizedBy(toolName);
    }
}
