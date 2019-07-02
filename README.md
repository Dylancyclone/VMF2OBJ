# VMF2OBJ

A work-in-progress Java application to convert VMF files into OBJ files with materials (including brushes, displacements, entities, and models). This project is no where near complete, but updates are slowly being made.

## How to run

From the root directory, run:

`mvn package;java -jar ./target/VMF2OBJ-0.0.1-SNAPSHOT-jar-with-dependencies.jar [VMF_FILE] [OUT_FOLDER]`

## TODO

Process
- [ ] Read Geometry
    - [ ] Deserialize data
    - [ ] Complete data (three points make a *bounded* plane)
- [ ] Collapse Vertices
- [ ] Write objects
    - [ ] Per object or collapsed?
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
    - [ ] Regular
    - [ ] Irregular
    - [ ] Textures
- [ ] Displacements
- [ ] Brush entities
- [ ] Point Entities
- [ ] Models
    - [ ] Geometry
    - [ ] Textures
