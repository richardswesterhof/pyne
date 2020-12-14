# Information on generating the input
This program works with any graph that Pyne outputs (in theory)

To generate the expected graph from Structure101, one must right-click a root package in the main graph view,
hover over the "Flatten" option, and select "To leaf packages" from the sub-menu.

This must be done for all root packages, after which all root packages must be opened by double-clicking on them.

Finally, an XML file must be exported by clicking "Tools" in the top left menu, clicking "Export LSM as image", select XML in the "Export as" dropdown menu, and choose a file location in the "Target file" textfield.

(Note, currently this only works with hardcoded paths relative to the working directory at the time of execution, however we plan to change this later to allow the file locations to be specified through a CLI)