# FloatingActionMenu

This library is an extension of https://github.com/futuresimple/android-floating-action-button

Included in this extension is support for left/right directional labels that appear on long press of the floating action buttons as well as a few other additions. To enable this within your menu, simply add ```fab:fab_displayHelpLabelsInHorizontalMode="true"```

To include this in your project, either copy out the library project and include in your own, or follow these steps.

1. Copy com.github.gfranks.fab.menu-1.0.aar into your projects ```libs/``` directory.
2. Include the following either in your top level ```build.gradle``` file or your module specific one:
```
   repositories {
        flatDir {
            dirs 'libs'
        }
    }
```
3. Under your dependencies for your main module's build.gradle file, you can reference that aar file like so:
```compile 'com.github.gfranks.fab.menu:com.com.github.gfranks.fab.menu-1.0@aar'```
