# Personal UI Backgrounds — RuneLite plugin

Customize the Old School RuneScape inventory/side-panel and chatbox with your own locally stored PNG or JPEG images.

> Community-developed plugin. **Not yet listed in the RuneLite Plugin Hub.** RuneLite's reviewers decide whether a submission is accepted. This project does not modify Jagex authentication or connect to third-party services.

## Features

- Choose separate local images for inventory and chatbox using the sidebar's **Browse for image** buttons.
- Adjust image strength (opacity) separately for each background.
- Select **crop to fill**, **fit** or **stretch** image scaling.
- Item icons and chat text remain game-rendered rather than being replaced by a top-layer overlay.
- The plugin restores its replaced sprites when disabled and attempts not to overwrite another plugin's subsequent override.
- **Local-only image handling:** no image uploads, network requests, or account-credential access.

## Usage

1. Enable **Personal UI Backgrounds** in the RuneLite plugin configuration.
2. Open the plugin's picture icon on the sidebar and choose a PNG or JPG for each background.
3. In the settings panel adjust **Inventory image strength**, **Chatbox image strength** and scaling.
4. If photos are hidden by another interface-modifying plugin, try disabling that conflicting skin or gameframe plugin.

The plugin targets fixed and classic resizable gameframes. Modern resizable chat may use a transparent chatbox unrelated to these background sprites and may not be affected. The inventory sprite can also be reused by other side-panel tabs.

## Develop and test

Open the folder containing the Gradle build file in IntelliJ IDEA. Import the Gradle project and run **Tasks → other → run**. A JDK compatible with your RuneLite/Gradle installation is required; Java 17 was used for local development.

The run task launches a RuneLite **development client**, not a client authenticated through the Jagex Launcher. Consult [RuneLite's Jagex Account developer guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) for official login support.

Run the Gradle test task to run the included JUnit tests. In addition to unit tests, manually verify with the current RuneLite client:

- Fixed mode and resizable-classic inventory rendering
- Chat text readability at different opacity settings
- Changing images while the client is open
- Clearing a selected image / disabling and re-enabling the plugin
- Interaction with other sprite-override plugins

Images larger than **20 MiB** or **8192 pixels** in either dimension are rejected.

## Source layout

- PersonalUiBackgroundsPlugin.java — RuneLite integration and sprite overrides
- PersonalUiBackgroundsConfig.java — settings
- PersonalUiBackgroundsPanel.java — local image picker and preview
- BackgroundImageRenderer.java — compositing and scaling
- ScalingMode.java — scaling modes

## Privacy

User-selected image paths remain in the local RuneLite configuration. The images are loaded from your PC; they are **not distributed with this plugin**. Do not commit your personal images, RuneLite profiles, Jagex credentials, or log files to this repository.

## Submitting to RuneLite Plugin Hub

Follow the [official Plugin Hub submission guide](https://github.com/runelite/plugin-hub#submitting-a-plugin). This repository's runelite-plugin.properties uses build=standard and does not require extra third-party dependencies. Plugin Hub listing is subject to RuneLite review and acceptance.

## License

BSD 2-Clause License. See [LICENSE](LICENSE).
