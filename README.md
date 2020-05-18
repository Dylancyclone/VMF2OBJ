# VMF2OBJ

A work-in-progress Java application to convert VMF files into OBJ files with materials (including brushes, displacements, entities, and models). This project is no where near complete, but updates are slowly being made.

## How to run

From the root directory, run:

`mvn package;java -jar ./target/VMF2OBJ-0.0.1-SNAPSHOT-jar-with-dependencies.jar <inFile> <outPath> <vpkPath> [externalResourcesPath]`

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
- [X] Extract Models
    - [X] VPK integration
- [X] Extract materials
    - [X] Basic VTFs
    - [X] VMTs
- [X] Convert Materials
    - [X] VTFLib?
- [X] Convert models to SMD
    - [X] Crowbar?
- [X] Convert models to OBJ
- [X] Write Models
- [X] Write Materials
- [X] Clean up

### Support

- [X] Input Resources
    - [X] From VPK
    - [X] From external source (i.e decompiled map)
- [X] Brushes
    - [X] Regular Geometry
    - [X] Irregular Geometry
    - [X] Materials
        - [X] Basic VTFs
        - [X] VMTs
            - [X] Basic
            - [X] Advanced
    - [X] Displacements
        - [X] Geometry
        - [X] Materials
- [X] Entities
    - [X] Brush entities
    - [X] Point Entities
        - [X] prop_*
            - [X] Geometry
            - [X] Geometry Normals
            - [X] Materials

### To do list

- Rebuild argument system

### Feature Wishlist

This list contains little improvements that while dont HAVE to be done, they would be nice and could improve the performance of the application a little.

- [ ] Optimizations
    - [ ] Collapse Vertex Textures
    - [ ] Collapse Vertex Normals
    - [ ] Collapse *almost* duplicate verticies
    - [ ] Sort faces by texture
    - [ ] Recycle prop data

### Unsupported Features

These are features that I don't have any plans to implement, either because I don't know how to, or the feature is too inconsistant, or would require extreme reworks to the current implementation. Of course, if you have an idea on how to implement any of these please feel free to submit a PR or issue discussing the idea.

- [ ] prop_* skins
    - Textures are defined per triangle in a model's decompiled SMD, but skins are defined in it's QC file. I don't know of a good way to line up skins with multiple textures to a single model. I would theoretically be possible to implement single-material skins, but the results may end up being inconsistant and produce unexpected results.
- [ ] Displacement blend materials
    - All the data needed for a blend material is included in the displacement object, but in order to implement it into an obj object either a separate texture must be generated with the blend built in, or a per-vertex material must be applied and blended between. The downsides of the first option is that it requires a lot of processing before hand (and Java's compatibility with Targa files is kinda potato at best), and the resulting texture is not editable. The downside of the second option is that the each vertex has to be either entirely one texture or entirely the other, and the linear blend will always go between the two. This will make for very strange looking textures when comparing it to the hammer counterpart, and it would require a complete rework of how the application handles textures. The perfect-world solution is to use a format that supports this kind of thing out of the box, but then you loose compatibility pretty quickly.
- [ ] infodecal
    - infodecal entities don't store any data about where the decal is displayed, meaning it is projected from it's origin to the brush and clipped/sized accordingly. I personally don't know enough about how this process is done, and I don't feel comfortable trying to brute force it. I looked around for the source code associated with it, but I could not find anything to reverse engineer. I know it's a pretty important feature, but I don't know how to make it work *correctly*.
- [ ] info_overlay
    - info_overlay is basically nextgen infodecal. Instead of just projecting to one side, an info_overlay can be projected to multiple faces, including different orientations and brushes so that the decal can "wrap" around. Again, I honestly don't really know how to approach this without brute forcing every face to create a seperate object with it's own UV wrapping. And that doesn't even include the fact that info_overlays can be distorted before being placed.