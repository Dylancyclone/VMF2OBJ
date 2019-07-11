# VMF2OBJ

A work-in-progress Java application to convert VMF files into OBJ files with materials (including brushes, displacements, entities, and models). This project is no where near complete, but updates are slowly being made.

## How to run

From the root directory, run:

`mvn package;java -jar ./target/VMF2OBJ-0.0.1-SNAPSHOT-jar-with-dependencies.jar [VMF_FILE] [OUT_FOLDER]`

## TODO

Process
- [X] Read Brush Geometry
    - [X] Deserialize data
    - [X] Complete data (three points make a *bounded* plane)
- [X] Collapse Brush Vertices
- [X] Write Brushes
    - [X] ~~Per object or collapsed?~~ Per object, but may add option later
- [ ] Extract Models
    - [ ] VPK integration
- [ ] Extract materials
- [ ] Convert Materials
    - [ ] VTFLib?
- [ ] Convert models to SMD
    - [ ] Crowbar?
- [ ] Convert models to OBJ
    - [ ] BST?
- [ ] Write Models
- [ ] Write Materials

Support
- [ ] Brushes
    - [X] Regular
    - [X] Irregular
    - [ ] Textures
- [ ] Displacements
- [X] Brush entities
- [ ] Point Entities
- [ ] Models
    - [ ] Geometry
    - [ ] Textures
