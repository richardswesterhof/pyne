# Information on generating the input
This program works with any graph that Pyne outputs (in theory)

To generate the expected matrix from Structure101, one must right-click anywhere outside the packages in the main graph view,
hover over the "Flatten" option, and select "To leaf packages" from the sub-menu.

Next, go to the "View" tab and select the composition view.
You should now see the matrix that shows the amount of dependencies between each package.

After this, a CSV file must be exported by clicking the export icon in the top right toolbar in the matrix panel.
Make sure that you select "file" for the "Export to" option, then select "Matrix as CSV" in the "Export as" dropdown.
Finally, select a file location to save the file to in the "Target file" field, and click "Ok" to export.

# Running the program with the generated input
The program simply takes 2 arguments on the command line, the first should be the path to the Structure101 CSV file, 
and the second should be the path to the Pyne graphml file. 
In short, the format should be as follows:

```
java -jar dependency_checker.jar -s [PATH/TO/STRUCTURE101_FILE.csv] -p [PATH/TO/PYNE_FILE.graphml] -hr -d[CLASS or PACKAGE]
```

e.g.:
```
java -jar dependency_checker.jar -s ../graph-files/tajo_dependencies_by_structure101.csv -p ../graph-files/tajo_dependencies_by_pyne.graphml -hr -dPACKAGE
```
