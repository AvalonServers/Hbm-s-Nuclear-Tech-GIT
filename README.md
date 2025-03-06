# <u>**HBM Nuclear Tech Mod - 1.12.2 Extended Edition**</u>

This is a custom 1.12.2 version of NTM:EE based off Alcatergit's fork. This is intended to be a stopgap until TheSlize's update fork is complete, and its goal is to make the current version of the mod more playable and performant.

Releases may be downloaded from GitHub, or you can build this fork yourself. As this is an experimental version, no support will be provided unless you are a Descent: Frozen Hell player.

Bugfixes:
- Prevent a server-side crash when TileEntityFEL attempts to load a corrupted wavelength
- Make the fluid pipe logic more robust, ensuring pipes don't randomly break
- Improve assembler item handling code to handle input/output inventories better, and additionally handle oredicted input items in recipes correctly.
- Forcibly disable shaders when a Mac client is detected
- Don't spawn coal gas unless coal dust is actually enabled
- Ensure HBM prefers its own outputs in recipes over outputs belonging to other mods in oredict entries
- Prevent multiblock duping when broken with tools such as the IF Infinity Drill
- Force power pylons in unloaded chunks to add themselves to the network

Performance improvements:
- Port to the new energy system introduced in TheSlize's version
- Port to the new model system also introduced in TheSlize's version, which uses VBOs
- Copy less ItemStacks in assembler item handling methods, making assemblers and chemplants faster

QoL improvements:
- Add configuration options to multiply the yield of RBMK fuel and fusion blankets
- Prevent the thunder sound produced by the Nuclear Transmutation Device from playing globally
