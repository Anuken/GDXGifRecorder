[![](https://jitpack.io/v/Anuken/GdxGifRecorder.svg)](https://jitpack.io/#Anuken/GdxGifRecorder)

## GdxGifRecorder
A simple utility class for libGDX that records a GIF and saves it automatically.

### Usage

To begin, add this project as a dependency in your `build.gradle` file. See [this JITPack link](https://jitpack.io/#Anuken/GdxGifRecorder/1.3) or click the JITPack badge above to see how.

Since the recorder uses a SpriteBatch to draw its GUI, you'll first start by creating a GIF recorder with
`recorder = new GifRecorder(someSpriteBatch)`
somewhere in your `create()` method.

Then, in your `render()` method, call `recorder.update()` after you've drawn everything you want to record.

And that's it! You're done.

### Default Controls:
- E: opens the recorder
- Left-Ctrl + Left-Mouse: expands the recording bounds to the mouse position
- Left-Shift + Left-Mouse: moves the recording window to the mouse position
- T: starts/stops recording

By default, recordings are saved to `./gifexports/recording-xxxxxx.gif`.

Note that images are also exported to a temporary working directory while the GIF is being compiled, `./.gifimages/`.

### Known Issues

Starting the write of the output GIF on Mac seems to cause a freeze. Cause is currently unknown.
