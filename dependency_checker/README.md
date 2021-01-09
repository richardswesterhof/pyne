# Information on generating the input
This program works with any graph that Pyne outputs (in theory)

To generate the expected matrix from Structure101, one must right-click anywhere outside the packages in the main graph view,
hover over the "Flatten" option, and select "To leaf packages" (for comparing on package level) or "To classes" (for comparing on a class level) from the sub-menu.

Next, go to the "View" tab and select the composition view.
You should now see the matrix that shows the amount of dependencies between each class/package.

After this, a CSV file must be exported by clicking the export icon in the top right toolbar in the matrix panel.
Make sure that you select "file" for the "Export to" option, then select "Matrix as CSV" in the "Export as" dropdown.
Finally, select a file location to save the file to in the "Target file" field, and click "Ok" to export.

# Running the program with the generated input
The program takes 3 mandatory arguments on the command line, one is the path to the Structure101 CSV file, 
one is the path to the Pyne graphml file, and the last one is the level of detail to use (classes/packages).
There are also more arguments available regarding the formatting of the output.
In short, the format should be as follows:

```
java -jar dependency_checker.jar -s [PATH/TO/STRUCTURE101_FILE.csv] -p [PATH/TO/PYNE_FILE.graphml] -hr -d[CLASS|PACKAGE]
```
For a full list of options, you can simply run the dependency checker without any options, and help will be displayed.

A full command might look like this:
```
java -jar dependency_checker.jar -s ../graph-files/tajo_dependencies_by_structure101.csv -p ../graph-files/tajo_dependencies_by_pyne.graphml -hr -dPACKAGE
```

One thing to keep in mind is that Structure101 only has one level of detail in its output, therefore, if you want to do a comparison on both a class level and a package level, 2 Structure101 files should be generated. 
Pyne contains all this information in its one output file, so the same file from Pyne can be used for both.
