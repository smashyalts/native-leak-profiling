# native-leak-profiling
A simple repository where I make random docker images that make native memory leaks more bearable to debug in Java applications.

These docker images are mainly intended for Minecraft servers, for use in Pterodactyl installations.\
These images use Jemalloc to create small heapdumps and converts them into readable GIFs (via jeprof).

You can read more about native memory leaks, how to debug them, and how to read Jeprof GIFs [here](https://github.com/jeffgriffith/native-jvm-leaks/blob/master/README.md).

## Images
Eclipse Temurin Java: `ghcr.io/skullians/native-leak-profiling:java_REPLACE_ME` (Supported: 21, 17, 11, 8)

## Usage
Once you have set the custom docker image, let the server run and allow Jeprof GIFs to accumulate in `dumps/output`.\
You can then analyze these GIFs (see https://github.com/jeffgriffith/native-jvm-leaks/blob/master/README.md)

## Acknowledgements
These are adapted Dockerfiles and entrypoints from [pterodactyl/yolks](https://github.com/pterodactyl/yolks/tree/master/java).