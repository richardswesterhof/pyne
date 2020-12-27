import functionality.Comparator;
import functionality.XMLHandler;
import org.apache.commons.cli.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("s", "structure101-file", true, "The file to use for Structure101's analysis");
        options.addOption("p", "pyne-file", true, "The file to use for Pyne's analysis");
        options.addOption("hr", "human-readable", false, "If specified, the file will be formatted to be easier to read, this will lead to (much) bigger file sizes however");
        options.addOption("indent", false, "If specified, the file will be indented properly to be easier to read. The human-readable option implies this already");


        String structure101Path;
        String pynePath;

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            structure101Path = cmd.getOptionValue("s");
            pynePath = cmd.getOptionValue("p");

            if(structure101Path == null || pynePath == null) {
                printHelp(options);
                System.exit(1);
            }

            File structure101Matrix = new File(structure101Path);
            File pyneGraph = new File(pynePath);
            // TODO: make this a parameter in CLI
            File output = new File("./results/comparison-" + System.currentTimeMillis() + ".xml");
            output.getParentFile().mkdirs();
            output.createNewFile();

            // initialize comparator
            System.out.println("Initializing comparator");
            Comparator comparator = new Comparator(structure101Matrix, pyneGraph, cmd).importFileData();

            // collect classes
			System.out.println("Collecting classes");
            comparator.collectAllClasses();

            // compare packages
			System.out.println("Comparing packages");
            Document doc = comparator.compareResults(cmd.hasOption("hr"));

            // output differences to xml file
            System.out.println("Writing to output file");
            XMLHandler.writeXML(doc, output, cmd.hasOption("hr") || cmd.hasOption("indent"));

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
        } catch (ParseException e) {
            System.err.println("Something went wrong when parsing cli arguments");
            e.printStackTrace();
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar dependency_checker.jar", options);
    }
}
