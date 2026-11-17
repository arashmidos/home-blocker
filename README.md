# Home Blocker

Blocks taps on the Android Home button, so a faulty touchscreen cannot press it.

After a screen replacement, my phone's new panel registered ghost taps at the bottom centre of the screen, right on the Home button. This app puts an invisible box over the Home button. Taps on the box do nothing. Hold it to go Home.

## How it works

- An Accessibility Service draws a `TYPE_ACCESSIBILITY_OVERLAY` window. These windows sit above the navigation bar, so the box gets every touch on the Home button.
- Back and Recents are not covered, so they work as normal.
- Holding the box (600 ms by default) calls `performGlobalAction(GLOBAL_ACTION_HOME)`.
- When the phone rotates, the box follows the physical bottom edge, the same as the navigation bar.
- The service subscribes to no accessibility events and reads no screen content.

Use it with 3-button navigation. In Gestures mode, apps draw under the bottom strip, so ghost taps hit app content instead.

## Settings

Open the app to set:

- box width and height, in pixels
- hold time to go Home
- a red preview of the box, to help you place it

To see where ghost taps land, turn on Developer options > Pointer location.

Keep the height at the navigation bar height or less. A taller box also covers the keyboard's space bar.

## Build and install

Needs JDK 17 or later and the Android SDK (compileSdk 36). minSdk is 30 (Android 11).

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then turn on the service: Settings > Accessibility > Home Blocker.

On MIUI, the service is under Settings > Additional settings > Accessibility > Downloaded apps. Also turn on Autostart and set Battery saver to "No restrictions", or MIUI may stop the service.

Tested on a Redmi 9T (MIUI 14, Android 12) and an Android 16 emulator.
