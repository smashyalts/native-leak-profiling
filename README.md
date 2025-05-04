# native-leak-profiling
A simple repository where I make random docker images that make native memory leaks more bearable to debug in Java applications.

These docker images are mainly intended for Minecraft servers, for use in Pterodactyl installations.\
These images use Jemalloc to create small heapdumps and converts them into readable GIFs (via jeprof).

You can read more about native memory leaks, how to debug them, and how to read Jeprof GIFs [here](https://github.com/jeffgriffith/native-jvm-leaks/blob/master/README.md).
