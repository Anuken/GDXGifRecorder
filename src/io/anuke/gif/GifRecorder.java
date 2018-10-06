package io.anuke.gif;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;

/** Records and saves GIFs. */
public class GifRecorder{
	private static final float defaultSize = 300;

	private int resizeKey = Keys.CONTROL_LEFT,
			openKey = Keys.E,
			recordKey = Keys.T,
			shiftKey = Keys.SHIFT_LEFT,
			fullscreenKey = Keys.F;

	private RecorderController controller = new DefaultController();
	
	private Batch batch;
	private Matrix4 matrix = new Matrix4();
	private TextureRegion region;
	
	private boolean skipAlpha = false;
	private int recordfps = 30;
	private float gifx, gify, gifwidth, gifheight, giftime;
	private float offsetx, offsety;
	private FileHandle exportdirectory, workdirectory;
	private boolean disableGUI;
	private float speedMultiplier = 1f;
	
	private Array<byte[]> frames = new Array<>();
	private File lastRecording;
	private float frametime;
	private boolean recording, open;
	private boolean saving;
	private float saveprogress;

	public GifRecorder(Batch batch) {
		this(batch, Gdx.files.local("gifexport"), Gdx.files.local(".gifimages"));
	}

	public GifRecorder(Batch batch, FileHandle exportdirectory, FileHandle workdirectory) {
		this.batch = batch;
		gifx = -defaultSize / 2;
		gify = -defaultSize / 2;
		gifwidth = defaultSize;
		gifheight = defaultSize;
		this.workdirectory = workdirectory;
		this.exportdirectory = exportdirectory;

		Pixmap pixmap = new Pixmap(1, 1, Format.RGBA8888);
		pixmap.setColor(Color.WHITE);
		pixmap.fill();

		region = new TextureRegion(new Texture(pixmap));
	}

	protected void doInput(){
		if(controller.openKeyPressed() && !saving){
			if(recording){
				finishRecording();
				clearFrames();
			}
			open = !open;
		}

		if(open){
			if(controller.recordKeyPressed() && !saving){
				if(!recording){
					startRecording();
				}else{
					finishRecording();
					writeGIF(workdirectory, exportdirectory);
				}
			} else if (controller.fullscreenPressed()) {
				offsetx = 0;
				offsety = 0;
				gifx = Gdx.graphics.getWidth() * -0.5f;
				gify = Gdx.graphics.getHeight() * -0.5f;
				gifwidth = Gdx.graphics.getWidth();
				gifheight = Gdx.graphics.getHeight();
			}
		}
	}

	/** Updates the recorder and draws the GUI */
	public void update(){
		doInput();
		float delta = Gdx.graphics.getDeltaTime();
		
		if(!open)
			return;
		
		matrix.set(batch.getProjectionMatrix());
		
		batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		boolean wasDrawing = batch.isDrawing();
		
		if(wasDrawing) batch.end();
		
		batch.begin();
		
		float wx = Gdx.graphics.getWidth() / 2;
		float wy = Gdx.graphics.getHeight() / 2;
		
		if(!disableGUI)
			batch.setColor(Color.YELLOW);

		if(controller.resizeKeyPressed()){
			
			if(!disableGUI)
				batch.setColor(Color.GREEN);
			
			float xs = Math.abs(Gdx.graphics.getWidth() / 2 + offsetx - Gdx.input.getX());
			float ys = Math.abs(Gdx.graphics.getHeight() / 2 + offsety - (Gdx.graphics.getHeight() - Gdx.input.getY()));
			gifx = -xs;
			gify = -ys;
			gifwidth = xs * 2;
			gifheight = ys * 2;
		}
		
		if(controller.shiftKeyPressed()){
			if(!disableGUI)
				batch.setColor(Color.ORANGE);
			
			float xs = (Gdx.graphics.getWidth() / 2 - Gdx.input.getX());
			float ys = (Gdx.graphics.getHeight() / 2 - (Gdx.graphics.getHeight() - Gdx.input.getY()));
			offsetx = -xs;
			offsety = -ys;
		}

		if(!disableGUI){
			
			if(recording)
				batch.setColor(Color.RED);
			
			if(region != null){
				batch.draw(region, gifx + wx + offsetx, gify + wy + offsety, gifwidth, 1f);
				batch.draw(region, gifx + wx + offsetx, gify + wy + gifheight + offsety, gifwidth, 1f);
				batch.draw(region, gifx + wx + offsetx, gify + wy + offsety, 1f, gifheight);
				batch.draw(region, gifx + wx + offsetx + gifwidth, gify + wy + offsety, 1f, gifheight + 1f);
			}

			if(saving){
				if(!disableGUI)
					batch.setColor(Color.BLACK);

				float w = 200, h = 50;
				batch.draw(region, Gdx.graphics.getWidth() / 2 - w / 2, Gdx.graphics.getHeight() / 2 - h / 2, w, h);

				//this just blends red and green
				Color a = Color.RED;
				Color b = Color.GREEN;

				float s = saveprogress;
				float i = 1f - saveprogress;

				batch.setColor(a.r * i + b.r * s, a.g * i + b.g * s, a.b * i + b.b * s, 1f);

				batch.draw(region, Gdx.graphics.getWidth() / 2 - w / 2, Gdx.graphics.getHeight() / 2 - h / 2, w * saveprogress, h);

			}

			batch.setColor(Color.WHITE);
		}

		if(recording){
			giftime += delta;
			frametime += delta*61f*speedMultiplier;
			if(frametime >= (60 / recordfps)){
				byte[] pix = ScreenUtils.getFrameBufferPixels((int) (gifx + offsetx) + 1 + Gdx.graphics.getWidth() / 2, 
						(int) (gify + offsety) + 1 + Gdx.graphics.getHeight() / 2, 
						(int) (gifwidth) - 2, (int) (gifheight) - 2, true);
				frames.add(pix);
				frametime = 0;
			}
		}
		
		batch.end();
		
		batch.getProjectionMatrix().set(matrix);
		
		if(wasDrawing) batch.begin();
		
	}
	
	/**Sets the speed multiplier. Higher numbers make the gif go slower, lower numbers make it go faster*/
	public void setSpeedMultiplier(float m){
		this.speedMultiplier = m;
	}
	
	/**Set to true to disable drawing the UI.*/
	public void setGUIDisabled(boolean disabled){
		this.disableGUI = true;
	}
	
	/**Sets the controller (or class that controls input)*/
	public void setController(RecorderController controller){
		this.controller = controller;
	}

	public boolean isSaving(){
		return saving;
	}

	public boolean isOpen(){
		return open;
	}

	public void open(){
		open = true;
	}

	public void close(){
		open = false;
	}

	public boolean isRecording(){
		return recording;
	}

	public void startRecording(){
		clearFrames();
		recording = true;
	}

	public float getTime(){
		return giftime;
	}

	public void finishRecording(){
		recording = false;
		giftime = 0;
	}

	public void clearFrames(){
		frames.clear();
		giftime = 0;
		recording = false;
	}

	public void setExportDirectory(FileHandle handle){
		exportdirectory = handle;
	}

	public void setWorkingDirectory(FileHandle handle){
		workdirectory = handle;
	}

	public void setResizeKey(int key){
		this.resizeKey = key;
	}

	public void setOpenKey(int key){
		this.openKey = key;
	}

	public void setRecordKey(int key){
		this.recordKey = key;
	}

	public void setFPS(int fps){
		recordfps = fps;
	}

	public File getLastRecording(){
		return lastRecording;
	}

	public void setSkipAlpha(boolean skipAlpha){
		this.skipAlpha = skipAlpha;
	}

	/** Sets the bounds for recording, relative to the center of the screen */
	public void setBounds(float x, float y, float width, float height){
		this.gifx = x;
		this.gify = y;
		this.gifwidth = width;
		this.gifheight = height;
	}

	public void setBounds(Rectangle rect){
		setBounds(rect.x, rect.y, rect.width, rect.height);
	}
	
	/**Takes a full-screen screenshot and saves it to a file.*/
	public FileHandle takeScreenshot(){
		return takeScreenshot(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	/**Takes a full-screen screenshot of the specified region saves it to a file.*/
	public FileHandle takeScreenshot(int x, int y, int width, int height){
		byte[] pix = ScreenUtils.getFrameBufferPixels(x, y, width, height, true);

		Pixmap pixmap = createPixmap(pix, width, height);

		FileHandle file = exportdirectory.child("screenshot-" + TimeUtils.millis() + ".png");
		PixmapIO.writePNG(file, pixmap);
		pixmap.dispose();
		return file;
	}
	
	public void writeGIF(){
		writeGIF(workdirectory, exportdirectory);
	}

	private void writeGIF(final FileHandle directory, final FileHandle writedirectory){
		if(saving)
			return;
		saving = true;
		final Array<String> strings = new Array<>();
		final Array<Pixmap> pixmaps = new Array<>();

		for(byte[] bytes : frames){
			Pixmap pixmap = createPixmap(bytes);
			pixmaps.add(pixmap);
		}

		new Thread(() -> {

            saveprogress = 0;
            int i = 0;
            for(Pixmap pixmap : pixmaps){
                PixmapIO.writePNG(Gdx.files.absolute(directory.file().getAbsolutePath() + "/frame" + i + ".png"), pixmap);
                strings.add("frame" + i + ".png");
                saveprogress += (0.5f / pixmaps.size);
                i++;
            }

            lastRecording = compileGIF(strings, directory, writedirectory);
            directory.deleteDirectory();
            for(Pixmap pixmap : pixmaps){
                pixmap.dispose();
            }
            saving = false;
        }).start();
	}

	private File compileGIF(Array<String> strings, FileHandle inputdirectory, FileHandle directory){
		if(strings.size == 0){
			throw new RuntimeException("No strings!");
		}

		try{
			String time = "" + (int) (System.currentTimeMillis() / 1000);
			String dirstring = inputdirectory.file().getAbsolutePath();
			new File(directory.file().getAbsolutePath()).mkdir();
			BufferedImage firstImage = ImageIO.read(new File(dirstring + "/" + strings.get(0)));
			File file = new File(directory.file().getAbsolutePath() + "/recording" + time + ".gif");
			ImageOutputStream output = new FileImageOutputStream(file);
			GifSequenceWriter writer = new GifSequenceWriter(output, firstImage.getType(), (int) (1f / recordfps * 1000f), true);

			writer.writeToSequence(firstImage);

			for(int i = 1; i < strings.size; i++){
				BufferedImage after = ImageIO.read(new File(dirstring + "/" + strings.get(i)));
				saveprogress += (0.5f / frames.size);
				writer.writeToSequence(after);
			}
			writer.close();
			output.close();
			return file;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	private Pixmap createPixmap(byte[] pixels, int width, int height){
		Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
		BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);

		Color color = new Color();

		if(!skipAlpha)
			for(int x = 0; x < pixmap.getWidth(); x++){
				for(int y = 0; y < pixmap.getHeight(); y++){
					color.set(pixmap.getPixel(x, y));
					if(color.a <= 0.999f){
						color.a = 1f;
						pixmap.setColor(color);
						pixmap.drawPixel(x, y);
					}
				}
			}

		return pixmap;
	}

	private Pixmap createPixmap(byte[] pixels){
		return createPixmap(pixels, (int) (gifwidth) - 2, (int) (gifheight) - 2);
	}

	/** Default controller implementation, uses the provided keys */
	class DefaultController implements RecorderController{
		
		public boolean openKeyPressed(){
			return Gdx.input.isKeyJustPressed(openKey);
		}

		public boolean recordKeyPressed(){
			return Gdx.input.isKeyJustPressed(recordKey);
		}

		public boolean resizeKeyPressed(){
			return Gdx.input.isButtonPressed(Buttons.LEFT) && Gdx.input.isKeyPressed(resizeKey);
		}
		
		public boolean shiftKeyPressed(){
			return Gdx.input.isButtonPressed(Buttons.LEFT) && Gdx.input.isKeyPressed(shiftKey);
		}

		@Override
		public boolean fullscreenPressed() {
			return Gdx.input.isKeyJustPressed(fullscreenKey);
		}
	}

	/**
	 * Provide an implementation and call recorder.setController() for custom
	 * input
	 */
	public interface RecorderController{
		boolean openKeyPressed();

		boolean recordKeyPressed();

		boolean resizeKeyPressed();
		
		boolean shiftKeyPressed();

		boolean fullscreenPressed();
	}
}
