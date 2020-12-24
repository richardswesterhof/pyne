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
    private Map<String, Pkg> pkgMap = new HashMap<>();
    private Map<String, Pkg> depMap = new HashMap<>();

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
        // sanitize trees
        structure101Doc.getDocumentElement().normalize();
        pyneDoc.getDocumentElement().normalize();

        return this;
    }

    /**
     * collects the packages for all tools
     */
    public void collectAllPackages() {
        addStructure101Packages(structure101Doc);
        addPynePackages(pyneDoc);
        // this is where one would add calls to other tool specific methods
        // in case a new tool joins the comparison
    }

    /**
     * checks for each found package which tool was and wasn't able to find it, and reports the results
     * @return a Document tree containing the results per tool
     */
    public Document comparePackages() {
        // store the ideal performance for reference
        ToolPerformance idealPerformance = toolPerformances.get(TOOL_NAME.IDEAL);

        // perform an ancient ritual to summon a List<String> from an Enum
        List<String> toolNames = new ArrayList<>(Arrays.asList(Stream.of(TOOL_NAME.values()).map(TOOL_NAME::toString).toArray(String[]::new)));
        // and make sure to remove the ideal tool, since it will be handled differently from normal tools
        toolNames.remove(TOOL_NAME.IDEAL.toString());

        // get the template doc from the XMLHandler
        Document doc = XMLHandler.initializeDoc(dBuilder, toolNames);

        // get the two children of the root element
        Node allPkgs = doc.getElementsByTagName(XMLHandler.ALL_PKGS).item(0);
        Node tools = doc.getElementsByTagName(XMLHandler.TOOLS).item(0);

        // set the count of total packages in the document
        XMLHandler.setNodeAttribute(allPkgs, COUNT, Integer.toString(idealPerformance.getHitCount()));

        // add all found packages to the list of packages
        for(String packageName : idealPerformance.getHits()) {
            Pkg pkg = pkgMap.get(packageName);
            allPkgs.appendChild(XMLHandler.createPackage(doc, pkg.getName(), pkg.isInternal()));
        }

        NodeList toolNodes = tools.getChildNodes();
        for(int i = 0; i < toolNodes.getLength(); i++) {
            Node toolNode = toolNodes.item(i);
            // get the performance of the tool that this node represents
            TOOL_NAME ToolName = TOOL_NAME.valueOf(toolNode.getAttributes().getNamedItem(NAME).getTextContent());
            ToolPerformance performance = toolPerformances.get(ToolName);

            Node foundPkgs = null, missedPkgs = null;
            NodeList childNodes = toolNode.getChildNodes();
            for(int j = 0; j < childNodes.getLength(); j++) {
                Node child = childNodes.item(j);
                String nodeName = child.getNodeName();
                // assign the correct variable based on tagname of the child
                if(nodeName.equals(FOUND_PKGS)) foundPkgs = child;
                else if(nodeName.equals(MISSED_PKGS)) missedPkgs = child;
            }

            // make sure these are not null before we proceed
            assert foundPkgs != null;
            assert missedPkgs != null;

            // get the missing internal packages from this tool
            // by starting with all (ideal) internal packages
            // and subtracting the actual found internal packages
            Set<String> internalMissing = new HashSet<>(idealPerformance.getInternalHits());
            internalMissing.removeAll(performance.getInternalHits());

            // same for external packages
            Set<String> externalMissing = new HashSet<>(idealPerformance.getExternalHits());
            externalMissing.removeAll(performance.getExternalHits());

            // add the found and missing packages to the tree
            XMLHandler.addAllPackages(doc, foundPkgs, getPackagesByNames(performance.getHits()));
            XMLHandler.addAllPackages(doc, missedPkgs, getPackagesByNames(internalMissing),
                    getPackagesByNames(externalMissing));

            // calculate percentages
            float internalMissPercent = (float)internalMissing.size() /
                    idealPerformance.getInternalHitCount() * 100;

            float externalMissPercent = (float)externalMissing.size() /
                    idealPerformance.getExternalHitCount() * 100;

            float totalMissPercent = (float)(internalMissing.size() + externalMissing.size()) /
                    idealPerformance.getHitCount() * 100;

            String fCount = Integer.toString(performance.getHitCount());
            String fPercentageTotal = Float.toString(100 - totalMissPercent);
            String fPercentageInternal = Float.toString(100 - internalMissPercent);
            String fPercentageExternal = Float.toString(100 - externalMissPercent);
            System.out.println("\n"+fCount+" found by "+ToolName+". "+fPercentageTotal+"% of total. "+fPercentageInternal+"% of internal. "+fPercentageExternal+"% of external");

            String mCount = Integer.toString(internalMissing.size() + externalMissing.size());
            String mPercentageTotal = Float.toString(totalMissPercent);
            String mPercentageInternal = Float.toString(internalMissPercent);
            String mPercentageExternal = Float.toString(externalMissPercent);
            System.out.println(mCount+" not found by "+ToolName+". "+mPercentageTotal+"% of total. "+mPercentageInternal+"% of internal. "+mPercentageExternal+"% of external\n");

            setNodeAttribute(foundPkgs, COUNT, fCount);
            setNodeAttribute(foundPkgs, PERCENTAGE_TOTAL, fPercentageTotal);
            setNodeAttribute(foundPkgs, PERCENTAGE_INTERNAL, fPercentageInternal);
            setNodeAttribute(foundPkgs, PERCENTAGE_EXTERNAL, fPercentageExternal);

            setNodeAttribute(missedPkgs, COUNT, mCount);
            setNodeAttribute(missedPkgs, PERCENTAGE_TOTAL, mPercentageTotal);
            setNodeAttribute(missedPkgs, PERCENTAGE_INTERNAL, mPercentageInternal);
            setNodeAttribute(missedPkgs, PERCENTAGE_EXTERNAL, mPercentageExternal);
        }

        return doc;
    }

    /**
     * gets the packages from the Structure101 output and adds them to the packageMap
     * and adds them to the list of found packages of Structure101
     * @param doc the parsed document that was output by Structure101
     */
    private void addStructure101Packages(Document doc) {
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

                foundPackage(pkg, TOOL_NAME.STRUCTURE101, isInternal);
            }
        }
    }

    /**
     * gets the package from the Pyne output and adds them to the packageMap
     * and adds them to the list of found packages of Pyne
     * @param doc the parsed document that was output by Pyne
     */
    private void addPynePackages(Document doc) {
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

            if(shouldAdd) foundPackage(pkgName, TOOL_NAME.PYNE, internal);
        }
    }

    /**
     * marks a package as found by the given tool
     * by updating an existing or creating a new package in the packageMap,
     * package on whether it already exists or not
     * and adding it to the list of found packages of the given tool
     * @param pkg the name of the pkg
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal package
     */
    private void foundPackage(String pkg, TOOL_NAME toolName, Boolean internal) {
        if(pkgMap.containsKey(pkg))
            updateExistingPackage(pkg, toolName);
        else
            addNewPackage(pkg, toolName, internal);
    }

    /**
     * creates a new pkg in the packageMap
     * and adds it to the list of found package of the given tool
     * @param pkg the name of the pkg
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal package
     */
    private void addNewPackage(String pkg, TOOL_NAME toolName, Boolean internal) {
        // create a new package with the given parameters in the packageMap
        Pkg pack = new Pkg(pkg, internal);
        pkgMap.put(pkg, pack);

        // and add this package to the list of found packages of the given tool and of the ideal tool
        toolPerformances.get(toolName).addPkg(pack);
        toolPerformances.get(TOOL_NAME.IDEAL).addPkg(pack);
    }

    /**
     * updates an existing pkg in the packageMap
     * and adds it to the list of found packages of the given tool
     * @param pkg the name of the pkg
     * @param toolName the name of the tool that found it
     */
    private void updateExistingPackage(String pkg, TOOL_NAME toolName) {
        Pkg pack = pkgMap.get(pkg);
        toolPerformances.get(toolName).addPkg(pack);
    }

    private Set<Pkg> getPackagesByNames(Set<String> names) {
        Set<Pkg> pkgs = new HashSet<>();
        for(String name : names) {
            pkgs.add(pkgMap.get(name));
        }
        return pkgs;
    }
}
