# VMF2OBJ

A work-in-progress Java application to convert VMF files into OBJ files with materials (including brushes, displacements, entities, and models). This project is no where near complete, but updates are slowly being made.

## How to run

From the root directory, run:

`mvn package;java -jar ./target/VMF2OBJ-0.0.1-SNAPSHOT-jar-with-dependencies.jar [VMF_FILE] [OUT_FOLDER] [VPK_PATH]`

## Packaged Dependencies

This project packages the following software and uses them during the conversion process. This project would not be possible without them.

- [VTFLib by Nem](http://nemesis.thewavelength.net/index.php?p=40)

## TODO

### Process
- [X] Read Brush Geometry
    - [X] Deserialize data
    - [X] Complete data (three points make a *bounded* plane)
- [X] Collapse Brush Vertices
- [X] Write Brushes
    - [X] ~~Per object or collapsed?~~ Per object, but may add option later
- [ ] Extract Models
    - [ ] VPK integration
- [X] Extract materials
    - [X] Basic VTFs
    - [X] VMTs (blends etc)
- [X] Convert Materials
    - [X] VTFLib?
- [ ] Convert models to SMD
    - [ ] Crowbar?
- [ ] Convert models to OBJ
    - [ ] BST?
- [ ] Write Models
- [ ] Write Materials

### Support
- [ ] Brushes
    - [X] Regular
    - [X] Irregular
    - [ ] Textures
        - [X] From VPK
        - [ ] From external source (i.e decompiled map)
        - [X] Basic VTFs
        - [ ] VMTs
            - [X] Basic
            - [ ] Advanced (see below)
- [ ] Displacements
    - [X] Geometry
    - [ ] Textures
- [X] Brush entities
- [ ] Point Entities
- [ ] Models
    - [ ] Geometry
    - [ ] Textures

### Advanced VMT Support
After getting basic VMT support (support for the $basetexture tag), it might be worth looking implementing the other features of a VMT. These would be looked into waayy down the line though, as I consider them extra features

- [ ] $bumpmap
- [ ] $detail
	- [ ] $detailscale
	- [ ] $detailblendfactor