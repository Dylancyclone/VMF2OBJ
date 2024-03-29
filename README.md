# VMF2OBJ

Convert source-engine VMF files from any game into OBJ files with materials (including brushes, displacements, entities, and models)

Watch a demonstration video:

[![Youtube demonstration](https://img.youtube.com/vi/3CgoCSRIGqI/0.jpg)](https://www.youtube.com/watch?v=3CgoCSRIGqI)

## How to run

Download the latest version from the [Releases](https://github.com/Dylancyclone/VMF2OBJ/releases) page, then double click on the .jar file to open it.

![The VMF2OBJ GUI](demo/gui.jpg)

Then, simply fill out which VMF file you'd like to convert, and the VPK files (and custom folders) that house the models, materials, etc for the map.

![An example](demo/example.jpg)

There is also a Command Line Interface:

`java -jar ./VMF2OBJ.jar [VMF_FILE] [args...]`

```
usage: vmf2obj [VMF_FILE] [args...]
 -h,--help                  Show this message
 -o,--output <arg>          Name of the output files. Defaults to the name
                            of the VMF file
 -q,--quiet                 Suppress warnings
 -r,--resourcePaths <arg>   Semi-colon separated list of VPK files and
                            folders for external custom content (such as
                            materials or models)
 -t,--tools                 Ignore tool brushes
```

Example:

```
java -jar .\vmf2obj.jar .\input.vmf -o .\output -r "D:\SteamLibrary\steamapps\common\Half-Life 2\hl2\hl2_misc_dir.vpk;D:\SteamLibrary\steamapps\common\Half-Life 2\hl2\hl2_textures_dir.vpk;C:\path\to\custom\content\;C:\path\to\more\custom\content\" -t
```

To use external resources that are not in a VPK file, simply add the path to the folder that contains the "models"/"materials" folders.

```
custom-content/        <----- Select this folder
├── materials/         <----- DON'T select this folder
│   └── models/
│       └── props/
│           ├── clock.vmt
│           └── clock.vtf
└── models/            <----- DON'T select this folder
    └── props/
        ├── clock.dx80.vtx
        ├── clock.dx90.vtx
        ├── clock.mdl
        ├── clock.phy
        ├── clock.sw.vtx
        └── clock.vvd
```

## Building

To build the app from source, simply run:

`mvn package`

The compiled .jar file will be placed in the `target` directory.

## Packaged Dependencies

This project packages the following software and uses them during the conversion process. This project would not be possible without them.

- [VTFLib by Nem](http://nemesis.thewavelength.net/index.php?p=40) v1.3.3
- [Crowbar-Command-Line by ZeqMacaw and UltraTechX](https://github.com/UltraTechX/Crowbar-Command-Line) 0.68-v1

## Support

- Brushes
- Displacements
- Materials
  - Textures
  - Bump Maps
  - Transparency
- Brush Entities
- prop\_\* Entities
  - Geometry
  - Geometry Normals
  - Materials

### Unsupported Features

These are features that I don't have any plans to implement, either because I don't know how to, or the feature is too inconsistant, or would require extreme reworks to the current implementation. Of course, if you have an idea on how to implement any of these please feel free to submit a PR or issue discussing the idea.

- [ ] prop\_\* skins
  - Textures are defined per triangle in a model's decompiled SMD, but skins are defined in it's QC file. I don't know of a good way to line up skins with multiple textures to a single model. I would theoretically be possible to implement single-material skins, but the results may end up being inconsistent and produce unexpected results.
- [ ] Displacement blend materials
  - All the data needed for a blend material is included in the displacement object, but in order to implement it into an obj object either a separate texture must be generated with the blend built in, or a per-vertex material must be applied and blended between. The downsides of the first option is that it requires a lot of processing before hand (and Java's compatibility with Targa files is kinda potato at best), and the resulting texture is not editable. The downside of the second option is that the each vertex has to be either entirely one texture or entirely the other, and the linear blend will always go between the two. This will make for very strange looking textures when comparing it to the hammer counterpart, and it would require a complete rework of how the application handles textures. The perfect-world solution is to use a format that supports this kind of thing out of the box, but then you loose compatibility pretty quickly.
- [ ] infodecal
  - infodecal entities don't store any data about where the decal is displayed, meaning it is projected from it's origin to the brush and clipped/sized accordingly. I personally don't know enough about how this process is done, and I don't feel comfortable trying to brute force it. I looked around for the source code associated with it, but I could not find anything to reverse engineer. I know it's a pretty important feature, but I don't know how to make it work _correctly_.
- [ ] info_overlay
  - info_overlay is basically nextgen infodecal. Instead of just projecting to one side, an info_overlay can be projected to multiple faces, including different orientations and brushes so that the decal can "wrap" around. Again, I honestly don't really know how to approach this without brute forcing every face to create a separate object with it's own UV wrapping. And that doesn't even include the fact that info_overlays can be distorted before being placed.

## Other Notes

Depending on where you import the converted result to, you might run into a problem where all the geometry looks very dark. This is due to the Source engine using additional normal data that might cause side effects in other software. In Blender, this can be solved with [this quick script](https://gist.github.com/Dylancyclone/d9bd1b53dbdd02702814661d8d82be5d). Simply select all the objects, and run this script in a new text editor. See [this](https://youtu.be/3CgoCSRIGqI?t=334) for more information.
