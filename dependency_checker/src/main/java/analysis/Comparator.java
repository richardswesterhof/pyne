package analysis;

import com.opencsv.CSVReader;
import items.Dep;
import items.SrcItm;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import xmlUtils.XMLHandler;
import xmlUtils.XML_TAG;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static xmlUtils.XMLHandler.*;

public class Comparator {

    public enum TOOL_NAME {
        // IDEAL is reserved for keeping track of the total of all tools
        // it can be seen as a theoretical tool that has the knowledge of all other tools combined
        IDEAL,
        STRUCTURE101,
        PYNE
    }

    public enum OUTPUT_DETAIL {
        NORMAL,
        HUMAN_READABLE,
        COMPACT
    }

    private Map<String, SrcItm> itmMap = new HashMap<>();
    private Set<Dep> depSet = new HashSet<>();

    private IDProvider idProvider = new IDProvider();

    File structure101File;
    File pyneFile;
    List<List<String>> structure101Matrix;
    Document pyneDoc;

    private static final int ITM_NAME_INDEX = 0;

    private DocumentBuilder dBuilder;

    private boolean classLevel;
    private OUTPUT_DETAIL outputDetail;

    public Comparator(File structure101File, File pyneFile, boolean classLevel, OUTPUT_DETAIL outputDetail) {
        this.structure101File = structure101File;
        this.pyneFile = pyneFile;
        this.classLevel = classLevel;
        this.outputDetail = outputDetail;
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
     * @return this, to allow it to be chained with the other public methods.
     *         this will allow an entire analysis to be done in one line, if the user desires so
     */
    public Comparator collectAllItems() {
        addStructure101Items(structure101Matrix, classLevel);
        addPyneItems(pyneDoc, classLevel);
        // this is where one would add calls to other tool specific methods
        // in case a new tool joins the comparison

        return this;
    }

    /**
     * checks for each found class which tool was and wasn't able to find it, and reports the results
     * @return a Document tree containing the results per tool
     */
    public Document compareResults() {
        // perform an ancient ritual to summon a List<String> from an Enum
        List<String> toolNames = new ArrayList<>(Arrays.asList(Stream.of(TOOL_NAME.values()).map(TOOL_NAME::toString).toArray(String[]::new)));
        // and make sure to remove the ideal tool, since it will be handled differently from normal tools
        toolNames.remove(TOOL_NAME.IDEAL.toString());

        // get the template doc from the XMLHandler
        Document doc = classLevel ? initClsDoc(dBuilder, toolNames) : initPkgDoc(dBuilder, toolNames);

        // get the three children of the root element
        Node allDeps = doc.getElementsByTagName(XML_TAG.ALL_DEPS).item(0);
        Node allItms = classLevel ?
                doc.getElementsByTagName(XML_TAG.ALL_CLSS).item(0) :
                doc.getElementsByTagName(XML_TAG.ALL_PKGS).item(0);
        Node tools = doc.getElementsByTagName(XML_TAG.TOOLS).item(0);

        int totalItms = itmMap.size();
        int totalDeps = depSet.size();
        int internalItms = 0;
        int externalItms = 0;
        int unknownItms = 0;

        for(SrcItm itm : itmMap.values()) {
            if(itm.isInternal() == null) unknownItms++;
            else if(itm.isInternal()) internalItms++;
            else externalItms++;
        }

        // set the count of total classes and dependencies in the document
        setNodeAttribute(allItms, XML_TAG.COUNT, Integer.toString(totalItms));
        setNodeAttribute(allItms, XML_TAG.COUNT_INTERNAL, Integer.toString(internalItms));
        setNodeAttribute(allItms, XML_TAG.COUNT_EXTERNAL, Integer.toString(externalItms));
        setNodeAttribute(allItms, XML_TAG.COUNT_UNKNOWN, Integer.toString(unknownItms));

        setNodeAttribute(allDeps, XML_TAG.COUNT, Integer.toString(totalDeps));

        Map<TOOL_NAME, Node> toolNodeMap = new HashMap<>();

        NodeList toolNodes = tools.getChildNodes();
        for(int i = 0; i < toolNodes.getLength(); i++) {
            Node node = toolNodes.item(i);
            // get the TOOL_NAME from the name of the node
            TOOL_NAME tool = TOOL_NAME.valueOf(node.getAttributes().getNamedItem(XML_TAG.NAME).getTextContent());
            toolNodeMap.put(tool, node);
        }

        // check which tools found which items
        for(SrcItm itm : itmMap.values()) {
            // first add this classes to the list of all classes/packages
            // when creating the list of all classes/packages,
            // we always want to get the complete versions
            allItms.appendChild(classLevel ? createClass(doc, itm) : createPackage(doc, itm));

            // check for each tool if it was found this class
            for(Node node : toolNodeMap.values()) {
                NodeList childNodes = node.getChildNodes();
                TOOL_NAME tool = TOOL_NAME.valueOf(node.getAttributes().getNamedItem(XML_TAG.NAME).getTextContent());

                for(int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if((itm.wasFoundBy(tool) && child.getNodeName().equals(classLevel ? XML_TAG.FOUND_CLSS : XML_TAG.FOUND_PKGS)) ||
                            (!itm.wasFoundBy(tool) && child.getNodeName().equals(classLevel ? XML_TAG.MISSED_CLSS : XML_TAG.MISSED_PKGS)))
                    {
                        // get the appropriate field names depending on whether this class is in- or external
                        String ternal, ternalPercName;
                        int ternalTotal;
                        if(itm.isInternal() == null) {
                            ternal = XML_TAG.COUNT_UNKNOWN;
                            ternalPercName = XML_TAG.PERCENTAGE_UNKNOWN;
                            ternalTotal = unknownItms;
                        }
                        else if(itm.isInternal()) {
                            ternal = XML_TAG.COUNT_INTERNAL;
                            ternalPercName = XML_TAG.PERCENTAGE_INTERNAL;
                            ternalTotal = internalItms;
                        }
                        else {
                            ternal = XML_TAG.COUNT_EXTERNAL;
                            ternalPercName = XML_TAG.PERCENTAGE_EXTERNAL;
                            ternalTotal = externalItms;
                        }

                        // get the current count from the tree
                        int count = Integer.parseInt(child.getAttributes().getNamedItem(XML_TAG.COUNT).getTextContent());
                        int ternalCount = Integer.parseInt(child.getAttributes().getNamedItem(ternal).getTextContent());

                        // recalculate numbers
                        count++;
                        ternalCount++;
                        float perc = (float)count / totalItms * 100;
                        float ternalPerc = (float)ternalCount / ternalTotal * 100;

                        // set the new values in the tree and add class to the list
                        setNodeAttribute(child, XML_TAG.COUNT, Integer.toString(count));
                        setNodeAttribute(child, ternal, Integer.toString(ternalCount));
                        setNodeAttribute(child, XML_TAG.PERCENTAGE_TOTAL, Float.toString(perc));
                        setNodeAttribute(child, ternalPercName, Float.toString(ternalPerc));
                        // create a class or package, depending on which detail level was selected
                        // and create the correct detail version of the item
                        child.appendChild(classLevel ?
                                (outputDetail.equals(OUTPUT_DETAIL.COMPACT) ?
                                        createCompactClass(doc, itm) :
                                        createClass(doc, itm)
                                ) :
                                (outputDetail.equals(OUTPUT_DETAIL.COMPACT) ?
                                        createCompactPackage(doc, itm) :
                                        createPackage(doc, itm)
                                )
                        );
                    }
                }
            }
        }

        // same for found dependencies
        for(Dep dep : depSet) {
            // create simple or extended dependency based on cli option
            if(outputDetail.equals(OUTPUT_DETAIL.HUMAN_READABLE)) allDeps.appendChild(createExtendedDependency(doc, dep));
            else allDeps.appendChild(createSimpleDependency(doc, dep));

            // check for each tool if it was found this dependency
            for(Node node : toolNodeMap.values()) {
                NodeList childNodes = node.getChildNodes();
                TOOL_NAME tool = TOOL_NAME.valueOf(node.getAttributes().getNamedItem(XML_TAG.NAME).getTextContent());
                for(int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if((dep.wasFoundBy(tool) && child.getNodeName().equals(XML_TAG.FOUND_DEPS)) ||
                            !dep.wasFoundBy(tool) && child.getNodeName().equals(XML_TAG.MISSED_DEPS))
                    {
                        // get the current count from the tree
                        int count = Integer.parseInt(child.getAttributes().getNamedItem(XML_TAG.COUNT).getTextContent());

                        // recalculate numbers
                        count++;
                        float perc = (float)count / totalDeps * 100;

                        // set the new values in the tree and add dependency to the list
                        setNodeAttribute(child, XML_TAG.COUNT, Integer.toString(count));
                        setNodeAttribute(child, XML_TAG.PERCENTAGE_TOTAL, Float.toString(perc));

                        // create simple or extended dependency based on cli option
                        child.appendChild(outputDetail.equals(OUTPUT_DETAIL.HUMAN_READABLE) ?
                                createExtendedDependency(doc, dep) :
                                createSimpleDependency(doc, dep)
                        );
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
    private void addStructure101Items(List<List<String>> matrix, boolean classLevel) {
        int id = 0;

        for(List<String> row : matrix) {
            // skip the first row, since that contains the headers
            if(id > 0) {
                String itemName = row.get(ITM_NAME_INDEX);
                Boolean isInternal = !itemName.startsWith("(unknown)");
                if(classLevel) isInternal = itemName.startsWith("(") ? false : null;
                foundItem(itemName, TOOL_NAME.STRUCTURE101, isInternal, id, classLevel);

                for(int i = 1; i < row.size(); i++) {
                    String dependencyCount = row.get(i);
                    if(!dependencyCount.isBlank()) {
                        String from = matrix.get(i).get(ITM_NAME_INDEX);
                        Boolean isFromInternal = !from.startsWith("(unknown)");
                        // since structure101 doesn't always start external classes with "("
                        // we can only tell when it definitely is internal,
                        // but we can never be sure it is external if "(" is missing
                        if(classLevel) isFromInternal = from.startsWith("(") ? false : null;
                        foundDependency(from, isFromInternal, i, itemName, isInternal, id,
                                TOOL_NAME.STRUCTURE101, Integer.parseInt(dependencyCount), classLevel);
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
    private void addPyneItems(Document doc, boolean classLevel) {
        // we want to get all nodes called "node" and "edge"
        NodeList nodes = doc.getElementsByTagName("node");
        NodeList edges = doc.getElementsByTagName("edge");
        Map<String, SrcItm> idMap = new HashMap<>();

        // process item nodes
        for(int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            // get the id of the node for easy cross referencing
            String id = node.getAttributes().getNamedItem("id").getTextContent();

            // each node contains a set of nodes called "data"
            NodeList datas = node.getChildNodes();
            String itmName = "";
            // for pyne we can always tell whether something is internal or not, so a boolean is fine
            boolean internal = true;
            boolean shouldAdd = true;

            for(int j = 0; j < datas.getLength(); j++) {
                Node data = datas.item(j);

                // each "data" node has an attribute "key" that specifies the type of data contained in this node
                String key = data.getAttributes().getNamedItem("key").getTextContent();

                // the "labelV" key indicates the type of object this node represents (package, class)
                // so we filter out the ones we are not interested in according to classLevel
                if(key.equals("labelV") && !data.getTextContent().equals(classLevel ? "class" : "package")) {
                    shouldAdd = false;
                    break;
                }

                // the "ClassType" or "PackageType" key indicates whether the class is internal or external
                // (SystemClass/Package or RetrievedClass/Package, respectively)
                else if(key.equals(classLevel ? "ClassType" : "PackageType"))
                    internal = data.getTextContent().equals(classLevel ? "SystemClass" : "SystemPackage");

                // the "name" key indicates the name of the class
                else if(key.equals("name")) itmName = data.getTextContent();
            }

            if(shouldAdd) idMap.put(id, foundItem(itmName, TOOL_NAME.PYNE, internal, Integer.parseInt(id), classLevel));
        }

        // process edge nodes
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
                // so we filter the ones we are not interested in according to classLevel
                // the expression: 'data.getTextContent().contains("package") != classLevel'
                // essentially inverts 'data.getTextContent().contains("package")' if classLevel is true
                // consider this truth table, where a = 'data.getTextContent().contains("package")', b = 'classLevel',
                // which explains the result we want: if(b) return !a else return a. we can see this is equal to a != b
                // a b
                // 1 1 --> 0
                // 1 0 --> 1
                // 0 1 --> 1
                // 0 0 --> 0
                if(key.equals("labelE") && (data.getTextContent().contains("package") != classLevel)) {
                    shouldAdd = false;
                    break;
                }
            }

            if(shouldAdd) {
                SrcItm from = idMap.get(sourceId);
                SrcItm to = idMap.get(targetId);
                if(from != null && to != null) foundDependency(from.getName(), from.isInternal(), from.getId(),
                            to.getName(), to.isInternal(), to.getId(), TOOL_NAME.PYNE, -1, classLevel);
            }
        }
    }

    /**
     * marks an item as found by the given tool
     * by updating an existing or creating a new item in the itemMap,
     * depending on whether it already exists or not
     * and adding it to the list of found items of the given tool
     * @param itm the name of the itm
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal class
     */
    private SrcItm foundItem(String itm, TOOL_NAME toolName, Boolean internal, int id, boolean classLevel) {
        if(itmMap.containsKey(itm)) return updateExistingItem(itm, toolName, internal);
        else return addNewItem(itm, toolName, internal, id, classLevel);
    }

    /**
     *
     * @param from the name of the item where the dependency comes from
     * @param isFromInternal whether from is internal or not
     * @param fromId the id of the from item
     * @param to
     * @param isToInternal
     * @param toId
     * @param toolName
     * @param amount
     * @param classLevel
     * @return
     */
    private Dep foundDependency(String from, Boolean isFromInternal, int fromId,
                                String to, Boolean isToInternal, int toId, TOOL_NAME toolName,
                                int amount, boolean classLevel)
    {
        // make sure both the from and to items exist in the itemMap already
        SrcItm fromItm = foundItem(from, toolName, isFromInternal, fromId, classLevel);
        SrcItm toItm = foundItem(to, toolName, isToInternal, toId, classLevel);

        for(Dep dep : depSet) {
            if(dep.getFrom().getId() == fromItm.getId() && dep.getTo().getId() == toItm.getId()) {
                dep.addFoundBy(toolName);
                return dep;
            }
        }
        // if we get here the dependency was not in the set yet

        // it's johnny Dep :)
        Dep johnny = new Dep(fromItm, toItm, amount, toolName);

        depSet.add(johnny);
        return johnny;
    }

    /**
     * creates a new itm in the itemMap
     * and adds it to the list of found items of the given tool
     * @param itm the name of the itm
     * @param toolName the name of the tool that found it
     * @param internal whether or not this is an internal item
     * @param id the id of the item, that was assigned by the tool
     */
    private SrcItm addNewItem(String itm, TOOL_NAME toolName, Boolean internal, Integer id, boolean classLevel) {
        // check if the class was not already in the classMap by another name (longer/shorter)
        for(String key : itmMap.keySet()) {
            String pyneName, s101Name;
            // determine which string is from which tool
            if(itmMap.get(key).wasFoundBy(TOOL_NAME.PYNE)) {
                pyneName = key;
                s101Name = itm;
            }
            else if(itmMap.get(key).wasFoundBy(TOOL_NAME.STRUCTURE101)){
                pyneName = itm;
                s101Name = key;
            }
            // should never actually happen, but just in case
            else throw new RuntimeException("Cannot determine which item name is from which tool " +
                        "to make a comparison between: \"" + key + "\" and \"" + itm + "\"");

            if((pyneName.equals(key) && toolName.equals(TOOL_NAME.PYNE)) ||
                    (s101Name.equals(key) && toolName.equals(TOOL_NAME.STRUCTURE101)))
                continue;
            else if((classLevel && isSameCls(pyneName, s101Name)) || (!classLevel && isSamePkg(pyneName, s101Name))) {
                // update the existing class by its known name
                return updateExistingItem(key, toolName, internal);
            }
        }

        // class really doesn't exists yet

        // create a new class with the given parameters in the classMap
        SrcItm item = classLevel ? SrcItm.createClass(itm, internal, toolName) : SrcItm.createPackage(itm, internal, toolName);
        item.setToolId(toolName, id);
        item.setToolId(TOOL_NAME.IDEAL, idProvider.getNextId());
        itmMap.put(itm, item);

        return item;
    }

    public static boolean isSamePkg(String pynePack, String s101Pack) {
        if(pynePack.equals(s101Pack)) return true;

        List<String> s101Parts = new ArrayList<>(Arrays.asList(s101Pack.split("\\.")));
        List<String> pyneParts = new ArrayList<>(Arrays.asList(pynePack.split("\\.")));

        int s101Ind = s101Parts.size() - 1;
        int pyneInd = pyneParts.size() - 1;

        while(pyneInd >= 0) {
            if(s101Ind < 0 || !s101Parts.get(s101Ind).equals(pyneParts.get(pyneInd))) return false;

            s101Ind--;
            pyneInd--;
        }

        return true;
    }


    public static boolean isSameCls(String pyneName, String s101Name) {
        if(pyneName.equals(s101Name)) return true;

        List<String> s101Parts = new ArrayList<>(Arrays.asList(s101Name.split("_")));
        List<String> pyneParts = new ArrayList<>(Arrays.asList(pyneName.split("\\.")));

        // split the last part (i.e. the real class name) on underscores, so it will line up with the s101Parts
        // in case the class name itself contains underscores
        List<String> split = Arrays.asList(pyneParts.get(pyneParts.size() - 1).split("_"));
        // remove the un-split version to avoid duplication
        pyneParts.remove(pyneParts.size() - 1);
        pyneParts.addAll(split);

        int s101Ind = s101Parts.size() - 1;
        int pyneInd = pyneParts.size() - 1;

        while(pyneInd >= 0) {
            if(s101Ind < 0) return true;
            String p = pyneParts.get(pyneInd);
            String s = s101Parts.get(s101Ind);

            if(!s.startsWith("(") && !p.startsWith(s))
                return false;

            s101Ind--;
            pyneInd--;
        }

        return true;
    }

    /**
     * updates an existing itm in the classMap
     * and adds it to the list of found classes of the given tool
     * @param itm the name of the itm
     * @param toolName the name of the tool that found it
     */
    private SrcItm updateExistingItem(String itm, TOOL_NAME toolName, Boolean isInternal) {
        SrcItm item = itmMap.get(itm);
        item.addFoundBy(toolName);
        if(isInternal != null) item.setInternal(isInternal);
        return item;
    }
}
