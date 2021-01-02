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

    public enum DETAIL {
        CLASS,
        PACKAGE
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "structure101-file", true, "The file to use from Structure101's analysis");
        options.addRequiredOption("p", "pyne-file", true, "The file to use from Pyne's analysis");
        options.addOption("hr", "human-readable", false, "If provided, the file will be configured to be easier to read, this will lead to (much) bigger file sizes however. Cannot be used in combination with \"compact\"");
        options.addOption("i", "indent", false, "If provided, the file will be indented properly to be easier to read. The human-readable option implies this already");
        options.addRequiredOption("d", "detail-level", true, "CLASS: compares on a class level, PACKAGE: compares on a package level. Keep in mind that switching modes requires a different Structure101 file!");
        options.addOption("c", "compact", false, "If provided, the file will be generated to contain minimal duplicate data, to reduce the file size as much as possible. Cannot be used in combination with \"human-readable\"");


        String structure101Path;
        String pynePath;
        boolean classLevel = false;
        Comparator.OUTPUT_DETAIL outputDetail = Comparator.OUTPUT_DETAIL.NORMAL;

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            structure101Path = cmd.getOptionValue("s");
            pynePath = cmd.getOptionValue("p");

            if(cmd.getOptionValue("detail-level").equals(DETAIL.CLASS.name())) classLevel = true;
            else if(cmd.getOptionValue("detail-level").equals(DETAIL.PACKAGE.name())) classLevel = false;
            else {
                printHelp(options);
                System.exit(1);
            }

            if(cmd.hasOption("human-readable")) {
                if(cmd.hasOption("compact")) {
                    System.err.println("The \"human-readable\" option and the \"compact\" option cannot be used together");
                    printHelp(options);
                    System.exit(1);
                }
                else outputDetail = Comparator.OUTPUT_DETAIL.HUMAN_READABLE;
            }
            else if(cmd.hasOption("compact")) outputDetail = Comparator.OUTPUT_DETAIL.COMPACT;


            File structure101Matrix = new File(structure101Path);
            File pyneGraph = new File(pynePath);
            // TODO: make this a parameter in CLI
            File output = new File("./results/comparison-" + System.currentTimeMillis() + ".xml");
            output.getParentFile().mkdirs();
            output.createNewFile();

            // initialize comparator
            System.out.println("Initializing comparator");
            Comparator comparator = new Comparator(structure101Matrix, pyneGraph, classLevel, outputDetail).importFileData();

            // collect classes
			System.out.println("Collecting " + (classLevel ? "classes" : "packages"));
            comparator.collectAllItems();

            // compare packages
			System.out.println("Comparing " + (classLevel ? "classes" : "packages"));
            Document doc = comparator.compareResults();

            // output differences to xml file
            System.out.println("Writing to output file");
            XMLHandler.writeXML(doc, output, cmd.hasOption("human-readable") || cmd.hasOption("indent"));

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
            printHelp(options);
            System.exit(1);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar dependency_checker.jar", options);
    }
}
