
Ashwake Main Menu
=================

Custom NeoForge client module for the Ashwake modpack front-end.

It replaces vanilla main flow screens with an Ashwake-styled UI system, including:
- Main menu replacement
- Play hub, custom singleplayer/multiplayer flows
- Custom loading overlay
- Guidance/FAQ/changelog screens
- Window branding (title + icon)
- UI performance and accessibility controls

Repository
----------
- GitHub: https://github.com/Ashwake-MC/Ashwake_MainMenu

Target stack
------------
- Minecraft: 1.21.1
- NeoForge: 21.1.219
- Java: 21

Development
-----------
- Open in IntelliJ IDEA.
- Build:
  - `./gradlew compileJava`
  - `./gradlew jar`
- Run client dev environment:
  - `./gradlew runClient`

Output
------
- Built jar: `build/libs/ashwake_mainmenu-<version>.jar`
- Local runtime test mod path used in this project:
  - `run/mods/ashwake-1.0.0.jar`

License
-------
This project is distributed under an `All Rights Reserved` license.
See [LICENSE](LICENSE).
