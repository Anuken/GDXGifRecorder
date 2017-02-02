# GdxGifRecorder
A simple utility class for libGDX that records a GIF and saves it automatically.

###Basic example:

```java

	GifRecorder recorder;
	SpriteBatch batch;

	public void create(){
	
		batch = new SpriteBatch();
		
		//batch is the SpriteBatch that is going to be used for drawing the recording GUI
		//if you want to set the recording bounds programmatically, disable GUI with recorder.setGUIDisabled(true)
		
		//scale is the scaling factor that you use for the SpriteBatch - used 
		//for calculating the center of the screen
		
		//for example, if you call 
		//batch.getProjectionMatrix().setToOrtho2D(0, 0, width/5, height/5); in resize(),
		//you should use 1/5f (or 0.2f) as your scale
		//(the default scale is 1)
		
		recorder = new GifRecorder(batch, scale);
		
	}
	
	
	public void render(){
		//this is important! everything necessary has to be rendered, 
		//and the batch has to be end()-ed to start recording
		
		drawEverything();
		
		//since the recorder uses the batch for drawing, you need to begin
		batch.begin();
		
		//this updates the recorder input and draws the recorder GUI
		recorder.update();
		
		batch.end();
	}


```

###Default Controls:
- E: opens the recorder
- Left-Ctrl + Left-Mouse: expands the recording bounds to the mouse position
- T: starts/stops recording


By default, recordings are saved to `./gifexports/recording-xxxxx.gif`.

Note that images are also exported to a working directory while the GIF is being compiled, `./.gifimages/`.
