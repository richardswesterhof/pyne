# Information on generating the input
This program works with any graph that Pyne outputs (in theory)

To generate the expected graph from Structure101, one must right-click a root package in the main graph view,
hover over the "Flatten" option, and select "To leaf packages" from the sub-menu.

This must be done for all root packages, after which all root packages must be opened by double-clicking on them.

Finally, an XML file must be exported by clicking "Tools" in the top left menu, clicking "Export LSM as image", select XML in the "Export as" dropdown menu, and choose a file location in the "Target file" textfield.

# Running the program with the generated input
The program simply takes 2 arguments on the command line, the first should be the path to the Structure101 XML file, and the second should be the path to the Pyne graphml file.

```
java -jar dependency_checker.jar [PATH/TO/STRUCTURE101.xml] [PATH/TO/PYNE.graphml]
```

e.g.:
```
java -jar dependency_checker.jar ../graphs/tajo_dependencies_by_structure101.xml ../graphs/tajo_dependencies_by_pyne.graphml
```
