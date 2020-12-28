package functionality;

import com.opencsv.CSVReader;
import org.apache.commons.cli.CommandLine;
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

    private Map<String, Cls> clsMap = new HashMap<>();
    private Set<Dep> depSet = new HashSet<>();

    private IDProvider idProvider = new IDProvider();

    File structure101File;
    File pyneFile;
    List<List<String>> structure101Matrix;
    Document pyneDoc;

    private static final int CLS_NAME_INDEX = 0;

    DocumentBuilder dBuilder;

    CommandLine cmd;

    public Comparator(File structure101File, File pyneFile, CommandLine cmd) {
        this.structure101File = structure101File;
        this.pyneFile = pyneFile;
        this.cmd = cmd;
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

    /**
     * imports pynes data from the given file
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private void importPyneData() throws ParserConfigurationException, SAXException, IOException {
        dBuilder = XMLHandler.getDocumentBuilder();
        // parse file
        pyneDoc = dBuilder.parse(pyneFile);
        // sanitize tree
        pyneDoc.getDocumentElement().normalize();
    }

    /**
     * collects the classes for all tools
     */
    public void collectAllClasses() {
        addStructure101Classes(structure101Matrix);
        addPyneClasses(pyneDoc);
        // this is where one would add calls to other tool specific methods
        // in case a new tool joins the comparison
    }

    /**
     * checks for each found class which tool was and wasn't able to find it, and reports the results
     * @param extensive whether or not the resulting tree should be focussed on being easier to read,
     *                  this will lead to a much bigger tree!
     * @return a Document tree containing the results per tool
     */
    public Document compareResults(boolean extensive) {
        // perform an ancient ritual to summon a List<String> from an Enum
        List<String> toolNames = new ArrayList<>(Arrays.asList(Stream.of(TOOL_NAME.values()).map(TOOL_NAME::toString).toArray(String[]::new)));
        // and make sure to remove the ideal tool, since it will be handled differently from normal tools
        toolNames.remove(TOOL_NAME.IDEAL.toString());

        // get the template doc from the XMLHandler
        Document doc = initializeDoc(dBuilder, toolNames);

        // get the three children of the root element
        Node allDeps = doc.getElementsByTagName(ALL_DEPS).item(0);
        Node allClss = doc.getElementsByTagName(ALL_CLSS).item(0);
        Node tools = doc.getElementsByTagName(TOOLS).item(0);

        int totalClss = clsMap.size();
        int totalDeps = depSet.size();
        int internalClss = 0;
        int externalClss = 0;

        for(Cls cls : clsMap.values()) {
            if(cls.isInternal()) internalClss++;
            else externalClss++;
        }

        // set the count of total classes and dependencies in the document
        setNodeAttribute(allClss, COUNT, Integer.toString(totalClss));
        setNodeAttribute(allClss, COUNT_INTERNAL, Integer.toString(internalClss));
        setNodeAttribute(allClss, COUNT_EXTERNAL, Integer.toString(externalClss));

        setNodeAttribute(allDeps, COUNT, Integer.toString(totalDeps));

        Map<TOOL_NAME, Node> toolNodeMap = new HashMap<>();

        NodeList toolNodes = tools.getChildNodes();
        for(int i = 0; i < toolNodes.getLength(); i++) {
            Node node = toolNodes.item(i);
            // get the TOOL_NAME from the name of the node
            TOOL_NAME tool = TOOL_NAME.valueOf(node.getAttributes().getNamedItem(NAME).getTextContent());
            toolNodeMap.put(tool, node);
        }

        // check which tools found which classes
        for(Cls cls : clsMap.values()) {
            // first add this classes to the list of all classes
            allClss.appendChild(createClass(doc, cls));

            // check for each tool if it was found this class
            for(Node node : toolNodeMap.values()) {
                NodeList childNodes = node.getChildNodes();
                TOOL_NAME tool = TOOL_NAME.valueOf(node.getAttributes().getNamedItem(NAME).getTextContent());

                for(int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if((cls.wasFoundBy(tool) && child.getNodeName().equals(FOUND_CLSS)) ||
                            (!cls.wasFoundBy(tool) && child.getNodeName().equals(MISSED_CLSS)))
                    {
                        // get the appropriate field names depending on whether this class is in- or external
                        String ternal, ternalPercName;
                        int ternalTotal;
                        if(cls.isInternal()) {
                            ternal = COUNT_INTERNAL;
                            ternalPercName = PERCENTAGE_INTERNAL;
                            ternalTotal = internalClss;
                        }
                        else {
                            ternal = COUNT_EXTERNAL;
                            ternalPercName = PERCENTAGE_EXTERNAL;
                            ternalTotal = externalClss;
                        }

                        // get the current count from the tree
                        int count = Integer.parseInt(child.getAttributes().getNamedItem(COUNT).getTextContent());
                        int ternalCount = Integer.parseInt(child.getAttributes().getNamedItem(ternal).getTextContent());

                        // recalculate numbers
                        count++;
                        ternalCount++;
                        float perc = (float)count / totalClss * 100;
                        float ternalPerc = (float)ternalCount / ternalTotal * 100;

                        // set the new values in the tree and add class to the list
                        setNodeAttribute(child, COUNT, Integer.toString(count));
                        setNodeAttribute(child, ternal, Integer.toString(ternalCount));
                        setNodeAttribute(child, PERCENTAGE_TOTAL, Float.toString(perc));
                        setNodeAttribute(child, ternalPercName, Float.toString(ternalPerc));
                        child.appendChild(createClass(doc, cls));
                    }
                }
            }
        }

        // same for found dependencies
        for(Dep dep : depSet) {
            // create simple or extended dependency based on cli option
            if(extensive) allDeps.appendChild(createExtendedDependency(doc, dep));
            else allDeps.appendChild(createSimpleDependency(doc, dep));

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

                        // create simple or extended dependency based on cli option
                        if(extensive) child.appendChild(createExtendedDependency(doc, dep));
                        else child.appendChild(createSimpleDependency(doc, dep));
                    }
                }
            }
        }

        return doc;
    }

    /**
     * gets the classes from the Structure101 output and adds them to the classMap
     * and adds them to the list of found classes of Structure101
     * @param matrix the parsed matrix that was output by Structure101
     */
    private void addStructure101Classes(List<List<String>> matrix) {
        int id = 0;
        for(List<String> row : matrix) {
            if(id > 0) {
                String className = row.get(CLS_NAME_INDEX);
                boolean isInternal = !className.startsWith("(unknown)");
                foundClass(className, TOOL_NAME.STRUCTURE101, isInternal, matrix.indexOf(row));
                for(int i = 1; i < row.size(); i++) {
                    String dependencyCount = row.get(i);
                    if(!dependencyCount.isBlank()) {
                        String from = matrix.get(i).get(CLS_NAME_INDEX);
                        boolean isFromInternal = !from.startsWith("(unknown)");
                        foundDependency(from, isFromInternal, i, className, isInternal, id,
                                TOOL_NAME.STRUCTURE101, Integer.parseInt(dependencyCount));
                    }
                }
            }
            id++;
        }
    }

    /**
     * gets the class from the Pyne output and adds them to the classMap
     * and adds them to the list of found classes of Pyne
     * @param doc the parsed document that was output by Pyne
     */
    private void addPyneClasses(Document doc) {
        // we want to get all nodes called "node" and "edge"
        NodeList nodes = doc.getElementsByTagName("node");
        NodeList edges = doc.getElementsByTagName("edge");
        Map<String, Cls> idMap = new HashMap<>();

        for(int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            // get the id of the node for easy cross referencing
            String id = node.getAttributes().getNamedItem("id").getTextContent();

            // each node contains a set of nodes called "data"
            NodeList datas = node.getChildNodes();
            String clsName = "";
            boolean internal = true;
            boolean shouldAdd = true;

            for(int j = 0; j < datas.getLength(); j++) {
                Node data = datas.item(j);

                // each "data" node has an attribute "key" that specifies the type of data contained in this node
                String key = data.getAttributes().getNamedItem("key").getTextContent();

                // the "labelV" key indicates the type of object this node represents (package, class)
                // we are interested in the classes, so the rest gets filtered out
                if(key.equals("labelV") && !data.getTextContent().equals("class")) {
                    shouldAdd = false;
                    break;
                }

                // the "ClassType" key indicates whether the class is internal or external
                // (SystemClass or RetrievedClass, respectively)
                else if(key.equals("ClassType")) internal = data.getTextContent().equals("SystemClass");

                // the "name" key indicates the name of the class
                else if(key.equals("name")) clsName = data.getTextContent();
            }

            if(shouldAdd) idMap.put(id, foundClass(clsName, TOOL_NAME.PYNE, internal, Integer.parseInt(id)));
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
                // we are interested in class dependencies, so the rest gets filtered out
                if (key.equals("labelE") && !data.getTextContent().contains("class")) {
                    shouldAdd = false;
                    break;
                }
            }

            if(shouldAdd) {
                Cls from = idMap.get(sourceId);
                Cls to = idMap.get(targetId);
                foundDependency(from.getName(), from.isInternal(), from.getId(),
                        to.getName(), to.isInternal(), to.getId(), TOOL_NAME.PYNE, -1);
            }
        }
    }

    /**
     * marks a class as found by the given tool
     * by updating an existing or creating a new class in the classMap,
     * depending on whether it already exists or not
     * and adding it to the list of found classes of the given tool
     * @param cls the name of the cls
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal class
     */
    private Cls foundClass(String cls, TOOL_NAME toolName, boolean internal, int id) {
        if(clsMap.containsKey(cls))
            return updateExistingClass(cls, toolName);
        else
            return addNewPackage(cls, toolName, internal, id);
    }

    /**
     *
     * @param from
     * @param isFromInternal
     * @param fromId
     * @param to
     * @param isToInternal
     * @param toId
     * @param toolName
     * @param amount
     * @return
     */
    private Dep foundDependency(String from, boolean isFromInternal, int fromId,
                                 String to, boolean isToInternal, int toId, TOOL_NAME toolName, int amount)
    {
        // make sure both the from and to class exist in the classMap already
        Cls fromCls = foundClass(from, toolName, isFromInternal, fromId);
        Cls toCls = foundClass(to, toolName, isToInternal, toId);

        for(Dep dep : depSet) {
            if(dep.getFrom().getId() == fromCls.getId() && dep.getTo().getId() == toCls.getId()) {
                dep.addFoundBy(toolName);
                return dep;
            }
        }
        // if we get here the dependency was not in the set yet

        // it's johnny Dep :)
        Dep johnny = new Dep(fromCls, toCls, amount, toolName);

        depSet.add(johnny);
        return johnny;
    }

    /**
     * creates a new cls in the classMap
     * and adds it to the list of found class of the given tool
     * @param cls the name of the cls
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal class
     * @param id the id of the class that was assigned by the tool
     */
    private Cls addNewPackage(String cls, TOOL_NAME toolName, Boolean internal, Integer id) {
        // check if the class was not already in the classMap by another name (longer/shorter)
        for(String key : clsMap.keySet()) {
            if(isSame(key, cls)) {
                // update the existing class by its known name
                return updateExistingClass(key, toolName);
            }
        }

        // class really doesn't exists yet

        // create a new class with the given parameters in the classMap
        Cls clas = new Cls(cls, internal, toolName);
        clas.setToolId(toolName, id);
        clas.setToolId(TOOL_NAME.IDEAL, idProvider.getNextId());
        clsMap.put(cls, clas);

        return clas;
    }

    /**
     * checks whether 2 strings indicate the same class
     * @param s1 the first string
     * @param s2 the second string
     * @return
     */
    public static boolean isSame(String s1, String s2) {
        String structure101Name;
        String pyneName;

        // find out which string is which
        // since class names cannot have dots in them,
        // if it contains a dot it must be the full class name including the packages
        // that's what Pyne does, so we know that is the Pyne String
        if(s1.contains(".")) {
            pyneName = s1;
            structure101Name = s2;
        }
        else {
            pyneName = s2;
            structure101Name = s1;
        }

        List<String> s101Parts = new ArrayList<>(Arrays.asList(structure101Name.split("_")));
        List<String> pParts = new ArrayList<>(Arrays.asList(pyneName.split("\\.")));

        // split the last part (i.e. the real class name) on underscores, so it will line up with the s101Parts
        // in case the class name itself contains underscores
        List<String> split = Arrays.asList(pParts.get(pParts.size() - 1).split("_"));
        // remove the un-split version to avoid duplication
        pParts.remove(pParts.size() - 1);
        pParts.addAll(split);

//        if(print) System.out.println(s101Parts + "\n" + pParts);

        int s101Ind = s101Parts.size() - 1;
        int pInd = pParts.size() - 1;

        while(pInd >= 0) {
            if(s101Ind < 0) return true;
            String p = pParts.get(pInd);
            String s = s101Parts.get(s101Ind);

//            if(print) System.out.println(p + ", " + s);
            if(!s.startsWith("(") && !p.startsWith(s))
                return false;

            s101Ind--;
            pInd--;
        }

        return true;
    }

    /**
     * updates an existing cls in the classMap
     * and adds it to the list of found classes of the given tool
     * @param cls the name of the cls
     * @param toolName the name of the tool that found it
     */
    private Cls updateExistingClass(String cls, TOOL_NAME toolName) {
        Cls clas = clsMap.get(cls);
        clas.addFoundBy(toolName);
        return clas;
    }
}
