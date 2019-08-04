# VMF2OBJ

A work-in-progress Java application to convert VMF files into OBJ files with materials (including brushes, displacements, entities, and models). This project is no where near complete, but updates are slowly being made.

## How to run

From the root directory, run:

`mvn package;java -jar ./target/VMF2OBJ-0.0.1-SNAPSHOT-jar-with-dependencies.jar [VMF_FILE] [OUT_FOLDER] [VPK_PATH]`

## Packaged Dependencies

This project packages the following software and uses them during the conversion process. This project would not be possible without them.

- [VTFLib by Nem](http://nemesis.thewavelength.net/index.php?p=40) v1.3.3
- [Crowbar-Command-Line by ZeqMacaw and UltraTechX](https://github.com/UltraTechX/Crowbar-Command-Line) 0.58-v2

## TODO

### Process
- [X] Read Brush Geometry
    - [X] Deserialize data
    - [X] Complete data (three points make a *bounded* plane)
- [X] Collapse Brush Vertices
- [X] Write Brushes
    - [X] ~~Per object or collapsed?~~ Per object, but may add option later
- [X] Extract Models
    - [X] VPK integration
- [X] Extract materials
    - [X] Basic VTFs
    - [X] VMTs (blends etc)
- [X] Convert Materials
    - [X] VTFLib?
- [X] Convert models to SMD
    - [X] Crowbar?
- [X] Convert models to OBJ
- [X] Write Models
- [X] Write Materials

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
        - [X] Regular VMT
        - [ ] Blends
- [X] Brush entities
- [ ] Point Entities
    - [ ] prop_*
        - [X] Geometry
        - [ ] Geometry Normals
        - [X] Textures
            - [ ] Skins
        - [ ] Special cases
            - [ ] prop_exploding_barrel
    - [ ] info_overlay

### Feature Wishlist
Wow todo list after todo list after wishlist. This list contains little improvements that while dont HAVE to be done, they would be nice and could improve the performance of the application a little.

- [ ] Collapse Vertex Textures
- [ ] Collapse *almost* duplicate verticies
- [ ] Sort faces by texture
- [ ] Advanced VMT Support
    - [ ] $bumpmap
    - [ ] $detail
        - [ ] $detailscale
        - [ ] $detailblendfactor