package functionality;

import com.opencsv.CSVReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    private Map<String, Pkg> pkgMap = new HashMap<>();
    private Set<Dep> depSet = new HashSet<>();

    private IDProvider idProvider = new IDProvider();

    File structure101File;
    File pyneFile;
    List<List<String>> structure101Matrix;
    Document pyneDoc;

    private static final int PKG_NAME_INDEX = 0;


    DocumentBuilder dBuilder;

    public Comparator(File structure101File, File pyneFile) {
        this.structure101File = structure101File;
        this.pyneFile = pyneFile;
    }

    /**
     * imports the data from the given files in the constructor into datastructures in this class
     * @return this, to allow it to be chained right after the constructor call
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Comparator importFileData() throws ParserConfigurationException, SAXException, IOException {
        importPyneData();
        importStructure101Data();

        return this;
    }

    /**
     * imports structure101s data from the csv file
     * @throws IOException
     */
    private void importStructure101Data() throws IOException {
        // read csv file
        structure101Matrix = new ArrayList<>();
        CSVReader csvReader = new CSVReader(new FileReader(structure101File));
        String[] values = null;
        // read all lines, and get the values split on commas already
        while((values = csvReader.readNext()) != null) {
            List<String> trimmed = new ArrayList<>(Arrays.asList(values));
            // the reason we drop the first element is because
            // 1. it only contains the index of the row, which we can easily deduce
            // 2. if we remove the first column, the indices in the headers match up with the actual indices in the List
            trimmed.remove(0);
            structure101Matrix.add(trimmed);
        }
    }

    private void importPyneData() throws ParserConfigurationException, SAXException, IOException {
        dBuilder = XMLHandler.getDocumentBuilder();
        // parse file
        pyneDoc = dBuilder.parse(pyneFile);
        // sanitize tree
        pyneDoc.getDocumentElement().normalize();
    }

    /**
     * collects the packages for all tools
     */
    public void collectAllPackages() {
        addStructure101Packages(structure101Matrix);
        addPynePackages(pyneDoc);
        // this is where one would add calls to other tool specific methods
        // in case a new tool joins the comparison
    }

    /**
     * checks for each found package which tool was and wasn't able to find it, and reports the results
     * @return a Document tree containing the results per tool
     */
    public Document compareResults() {
        // perform an ancient ritual to summon a List<String> from an Enum
        List<String> toolNames = new ArrayList<>(Arrays.asList(Stream.of(TOOL_NAME.values()).map(TOOL_NAME::toString).toArray(String[]::new)));
        // and make sure to remove the ideal tool, since it will be handled differently from normal tools
        toolNames.remove(TOOL_NAME.IDEAL.toString());

        // get the template doc from the XMLHandler
        Document doc = initializeDoc(dBuilder, toolNames);

        // get the three children of the root element
        Node allDeps = doc.getElementsByTagName(ALL_DEPS).item(0);
        Node allPkgs = doc.getElementsByTagName(ALL_PKGS).item(0);
        Node tools = doc.getElementsByTagName(TOOLS).item(0);

        int totalPkgs = pkgMap.size();
        int totalDeps = depSet.size();
        int internalPkgs = 0;
        int externalPkgs = 0;

        for(Pkg pkg : pkgMap.values()) {
            if(pkg.isInternal()) internalPkgs++;
            else externalPkgs++;
        }

        // set the count of total packages and dependencies in the document
        setNodeAttribute(allPkgs, COUNT, Integer.toString(totalPkgs));
        setNodeAttribute(allPkgs, COUNT_INTERNAL, Integer.toString(internalPkgs));
        setNodeAttribute(allPkgs, COUNT_EXTERNAL, Integer.toString(externalPkgs));

        setNodeAttribute(allDeps, COUNT, Integer.toString(totalDeps));

        Map<TOOL_NAME, Node> toolNodeMap = new HashMap<>();

        NodeList toolNodes = tools.getChildNodes();
        for(int i = 0; i < toolNodes.getLength(); i++) {
            Node node = toolNodes.item(i);
            // get the TOOL_NAME from the name of the node
            TOOL_NAME tool = TOOL_NAME.valueOf(node.getAttributes().getNamedItem(NAME).getTextContent());
            toolNodeMap.put(tool, node);
        }

        // check which tools found which packages
        for(Pkg pkg : pkgMap.values()) {
            // first add this package to the list of all packages
            allPkgs.appendChild(createPackage(doc, pkg));

            // check for each tool if it was found this package
            for(Node node : toolNodeMap.values()) {
                NodeList childNodes = node.getChildNodes();
                TOOL_NAME tool = TOOL_NAME.valueOf(node.getAttributes().getNamedItem(NAME).getTextContent());

                for(int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if((pkg.wasFoundBy(tool) && child.getNodeName().equals(FOUND_PKGS)) ||
                            (!pkg.wasFoundBy(tool) && child.getNodeName().equals(MISSED_PKGS)))
                    {
                        // get the appropriate field names depending on whether this package is in- or external
                        String ternal, ternalPercName;
                        int ternalTotal;
                        if(pkg.isInternal()) {
                            ternal = COUNT_INTERNAL;
                            ternalPercName = PERCENTAGE_INTERNAL;
                            ternalTotal = internalPkgs;
                        }
                        else {
                            ternal = COUNT_EXTERNAL;
                            ternalPercName = PERCENTAGE_EXTERNAL;
                            ternalTotal = externalPkgs;
                        }

                        // get the current count from the tree
                        int count = Integer.parseInt(child.getAttributes().getNamedItem(COUNT).getTextContent());
                        int ternalCount = Integer.parseInt(child.getAttributes().getNamedItem(ternal).getTextContent());

                        // recalculate numbers
                        count++;
                        ternalCount++;
                        float perc = (float)count / totalPkgs * 100;
                        float ternalPerc = (float)ternalCount / ternalTotal * 100;

                        // set the new values in the tree and add package to the list
                        setNodeAttribute(child, COUNT, Integer.toString(count));
                        setNodeAttribute(child, ternal, Integer.toString(ternalCount));
                        setNodeAttribute(child, PERCENTAGE_TOTAL, Float.toString(perc));
                        setNodeAttribute(child, ternalPercName, Float.toString(ternalPerc));
                        child.appendChild(createPackage(doc, pkg));
                    }
                }
            }
        }

        // same for found dependencies
        for(Dep dep : depSet) {
            allDeps.appendChild(createDependency(doc, dep));

            // check for each tool if it was found this dependency
            for(Node node : toolNodeMap.values()) {
                NodeList childNodes = node.getChildNodes();
                TOOL_NAME tool = TOOL_NAME.valueOf(node.getAttributes().getNamedItem(NAME).getTextContent());
                for(int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if((dep.wasFoundBy(tool) && child.getNodeName().equals(FOUND_DEPS)) ||
                            !dep.wasFoundBy(tool) && child.getNodeName().equals(MISSED_DEPS))
                    {
                        // get the current count from the tree
                        int count = Integer.parseInt(child.getAttributes().getNamedItem(COUNT).getTextContent());

                        // recalculate numbers
                        count++;
                        float perc = (float)count / totalDeps * 100;

                        // set the new values in the tree and add dependency to the list
                        setNodeAttribute(child, COUNT, Integer.toString(count));
                        setNodeAttribute(child, PERCENTAGE_TOTAL, Float.toString(perc));
                        child.appendChild(createDependency(doc, dep));
                    }
                }
            }
        }

        return doc;
    }

    /**
     * gets the packages from the Structure101 output and adds them to the packageMap
     * and adds them to the list of found packages of Structure101
     * @param matrix the parsed matrix that was output by Structure101
     */
    private void addStructure101Packages(List<List<String>> matrix) {
        int id = 0;
        for(List<String> row : matrix) {
            if(id > 0) {
                String packageName = row.get(PKG_NAME_INDEX);
                boolean isInternal = !packageName.startsWith("(unknown)");
                foundPackage(packageName, TOOL_NAME.STRUCTURE101, isInternal, matrix.indexOf(row));
                for (int i = 1; i < row.size(); i++) {
                    String dependencyCount = row.get(i);
                    if (!dependencyCount.isBlank()) {
                        String from = matrix.get(i).get(PKG_NAME_INDEX);
                        boolean isFromInternal = !from.startsWith("(unknown)");
                        foundDependency(from, isFromInternal, i, packageName, isInternal, id,
                                TOOL_NAME.STRUCTURE101, Integer.parseInt(dependencyCount));
                    }
                }
            }
            id++;
        }
    }

    /**
     * gets the package from the Pyne output and adds them to the packageMap
     * and adds them to the list of found packages of Pyne
     * @param doc the parsed document that was output by Pyne
     */
    private void addPynePackages(Document doc) {
        // we want to get all nodes called "node" and "edge"
        NodeList nodes = doc.getElementsByTagName("node");
        NodeList edges = doc.getElementsByTagName("edge");
        Map<String, Pkg> idMap = new HashMap<>();

        for(int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            // get the id of the node for easy cross referencing
            String id = node.getAttributes().getNamedItem("id").getTextContent();

            // each node contains a set of nodes called "data"
            NodeList datas = node.getChildNodes();
            String pkgName = "";
            boolean internal = true;
            boolean shouldAdd = true;

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

            if(shouldAdd) idMap.put(id, foundPackage(pkgName, TOOL_NAME.PYNE, internal, Integer.parseInt(id)));
        }

        for(int i = 0; i < edges.getLength(); i++) {
            Node edge = edges.item(i);

            String sourceId = edge.getAttributes().getNamedItem("source").getTextContent();
            String targetId = edge.getAttributes().getNamedItem("target").getTextContent();

            NodeList datas = edge.getChildNodes();
            boolean shouldAdd = true;

            for(int j = 0; j < datas.getLength(); j++) {
                Node data = datas.item(j);

                // each "data" node has an attribute "key" that specifies the type of data contained in this node
                String key = data.getAttributes().getNamedItem("key").getTextContent();

                // the "labelE" key indicates the type of edge this node represents
                // we are interested in package dependencies, so the rest gets filtered out
                if (key.equals("labelE") && !data.getTextContent().contains("package")) {
                    shouldAdd = false;
                    break;
                }
            }

            if(shouldAdd) {
                Pkg from = idMap.get(sourceId);
                Pkg to = idMap.get(targetId);
                foundDependency(from.getName(), from.isInternal(), from.getId(),
                        to.getName(), to.isInternal(), to.getId(), TOOL_NAME.PYNE, -1);
            }
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
    private Pkg foundPackage(String pkg, TOOL_NAME toolName, boolean internal, int id) {
        if(pkgMap.containsKey(pkg))
            return updateExistingPackage(pkg, toolName);
        else
            return addNewPackage(pkg, toolName, internal, id);
    }

    private Dep foundDependency(String from, boolean isFromInternal, int fromId,
                                 String to, boolean isToInternal, int toId, TOOL_NAME toolName, int amount)
    {
        // make sure both the from and to package exist in the packageMap already
        Pkg fromPkg = foundPackage(from, toolName, isFromInternal, fromId);
        Pkg toPkg = foundPackage(to, toolName, isToInternal, toId);

        for(Dep dep : depSet) {
            if(dep.getFrom().getId() == fromPkg.getId() && dep.getTo().getId() == toPkg.getId()) {
                dep.addFoundBy(toolName);
                return dep;
            }
        }
        // if we get here the dependency was not in the set yet

        // it's johnny Dep :)
        Dep johnny = new Dep(fromPkg, toPkg, amount, toolName);

        depSet.add(johnny);
        return johnny;
    }

    /**
     * creates a new pkg in the packageMap
     * and adds it to the list of found package of the given tool
     * @param pkg the name of the pkg
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal package
     * @param id the id of the package that was assigned by the tool
     */
    private Pkg addNewPackage(String pkg, TOOL_NAME toolName, Boolean internal, Integer id) {
        // check if the package was not already in the package map by another name (longer/shorter)
        for(String key : pkgMap.keySet()) {
            if(pkg.endsWith(key) || key.endsWith(pkg)) {
                // update the existing package by its known name
                return updateExistingPackage(key, toolName);
            }
        }

        // package really doesn't exists yet

        // create a new package with the given parameters in the packageMap
        Pkg pack = new Pkg(pkg, internal, toolName);
        pack.setToolId(toolName, id);
        pack.setToolId(TOOL_NAME.IDEAL, idProvider.getNextId());
        pkgMap.put(pkg, pack);

        return pack;
    }

    /**
     * updates an existing pkg in the packageMap
     * and adds it to the list of found packages of the given tool
     * @param pkg the name of the pkg
     * @param toolName the name of the tool that found it
     */
    private Pkg updateExistingPackage(String pkg, TOOL_NAME toolName) {
        Pkg pack = pkgMap.get(pkg);
        pack.addFoundBy(toolName);
        return pack;
    }
}
