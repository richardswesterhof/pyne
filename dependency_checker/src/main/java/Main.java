import functionality.Comparator;
import functionality.XMLHandler;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

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
            // TODO: make this a parameter in CLI
            File output = new File("./results/comparison-" + System.currentTimeMillis() + ".xml");
            output.getParentFile().mkdirs();
            output.createNewFile();

            // initialize comparator
            System.out.println("Initializing comparator");
            Comparator comparator = new Comparator(structure101Graph, pyneGraph).initXML();

            // collect packages
			System.out.println("Collecting packages");
            comparator.collectAllPackages();

            // compare packages
			System.out.println("Comparing packages");
            Document doc = comparator.comparePackages();

            // output differences to xml file
            System.out.println("Writing to output file");
            XMLHandler.writeXML(doc, output);

            // live happily ever after :)
            System.out.println("Done!");
        } catch(FileNotFoundException e) {
            System.err.println("Could not find file to open");
            e.printStackTrace();
        } catch(ParserConfigurationException e) {
            System.err.println("Something went wrong when getting DocumentBuilder");
            e.printStackTrace();
        } catch(SAXException | IOException e) {
            System.err.println("Something went wrong when parsing a file");
            e.printStackTrace();
        } catch(TransformerException e) {
            System.err.println("Something went wrong when writing output");
            e.printStackTrace();
        }
    }
}
