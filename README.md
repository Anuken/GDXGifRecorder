# GdxGifRecorder
A simple utility class for libGDX that records a GIF and saves it automatically.

###Usage

Since the recorder uses a SpriteBatch to draw its GUI, you'll first start by creating a GIF recorder with
`recorder = new GifRecorder(someSpriteBatch)`
somewhere in your `create()` method.

Then, in your `render()` method, call `recorder.update()` after you've drawn everything you want to record.

And that's it! You're done.

###Default Controls:
- E: opens the recorder
- Left-Ctrl + Left-Mouse: expands the recording bounds to the mouse position
- T: starts/stops recording


By default, recordings are saved to `./gifexports/recording-xxxxx.gif`.

Note that images are also exported to a working directory while the GIF is being compiled, `./.gifimages/`.
