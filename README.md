IngameInfo
=================
DaftPVF's IngameInfo mod for Minecraft.  Displays many different details for the player/world on the in-game HUD.
This repo contains source files for Forge (dependant on bspkrsCore).

### Links of Interest
 - [Official Minecraft Forum Thread](http://www.minecraftforum.net/topic/1009577-)
 - [ModLoader Downloads](http://bspk.rs/MC/IngameInfo/index.html)
 - [Issue Tracking System](https://github.com/bspkrs/IngameInfo/issues)
 
* * *

#### How to install and use the source code ####

1. Download the latest recommended [MinecraftForge](http://files.minecraftforge.net) src distribution.
2. Extract the Forge src zip file and run install.bat/.sh in the forge folder.
3. Clone this git repo and the [bspkrsCore repo](https://github.com/bspkrs/bspkrsCore) (https://github.com/bspkrs/bspkrsCore.git) to whatever locations you like.
4. Use the eclipse folder in your Forge/MCP setup as your Eclipse workspace.
5. Under the Minecraft project, add the "src" folder from both git repos as linked folders (rename as necessary) and set them as source folders.

#### How to build from the source code ####

1. Download and install [Apache Ant](http://ant.apache.org) on your system. Make sure it is available on the path environment variable.
2. In the IngameInfo repo folder, make a copy of build.properties_example and name it build.properties.
3. Edit the values in build.properties to contain valid paths on your system for each property. Details can be found in build.properties_example.
4. From a console window run "ant" from the IngameInfo repo folder. The build will create its output in the bin folder.
5. Install the resulting mod archive by copying it to the mods folder on the client.

* * *

<a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/3.0/"><img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-nc-sa/3.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/3.0/">Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License</a>.
