package io.anuke.gif;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;

/** Records and saves GIFs. */
public class GifRecorder{
	private boolean saving;
	private float saveprogress;
	private static final float defaultSize = 300;
	private SpriteBatch batch;
	private Array<byte[]> frames = new Array<byte[]>();
	private int recordfps = 30;
	private boolean skipAlpha = false;
	private float gifx, gify, gifwidth, gifheight, giftime;
	private boolean recording, open;
	private int resizeKey = Keys.CONTROL_LEFT, openKey = Keys.E, recordKey = Keys.T;
	private float screenScale = 1f;
	private FileHandle exportdirectory, workdirectory;
	private File lastRecording;
	private TextureRegion region;
	private RecorderController controller = new DefaultController();
	private boolean disableGUI;

	public GifRecorder(SpriteBatch batch) {
		this(batch, 1f, Gdx.files.local("gifexport"), Gdx.files.local(".gifimages"));
	}

	public GifRecorder(SpriteBatch batch, float scale) {
		this(batch, scale, Gdx.files.local("gifexport"), Gdx.files.local(".gifimages"));
	}

	public GifRecorder(SpriteBatch batch, float scale, FileHandle exportdirectory, FileHandle workdirectory) {
		this.batch = batch;
		screenScale = scale;
		gifx = -defaultSize / 2 * screenScale;
		gify = -defaultSize / 2 * screenScale;
		gifwidth = defaultSize * screenScale;
		gifheight = defaultSize * screenScale;
		this.workdirectory = workdirectory;
		this.exportdirectory = exportdirectory;

		Pixmap pixmap = new Pixmap(1, 1, Format.RGBA8888);
		pixmap.setColor(Color.WHITE);
		pixmap.fill();

		region = new TextureRegion(new Texture(pixmap));
	}

	private void doInput(){
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
			}
		}
	}

	/** Updates the recorder and draws the GUI */
	public void update(){
		doInput();
		float delta = Gdx.graphics.getDeltaTime();
		if(!open)
			return;
		float wx = Gdx.graphics.getWidth() / 2 * screenScale;
		float wy = Gdx.graphics.getHeight() / 2 * screenScale;
		
		if(!disableGUI)
			batch.setColor(Color.YELLOW);

		if(controller.resizeKeyPressed()){
			
			if(!disableGUI)
				batch.setColor(Color.GREEN);
			
			float xs = Math.abs(Gdx.graphics.getWidth() / 2 - Gdx.input.getX()) * screenScale;
			float ys = Math.abs(Gdx.graphics.getHeight() / 2 - (Gdx.graphics.getHeight() - Gdx.input.getY())) * screenScale;
			gifx = -xs;
			gify = -ys;
			gifwidth = xs * 2;
			gifheight = ys * 2;
		}

		if(!disableGUI){
			
			if(recording)
				batch.setColor(Color.RED);
			
			if(region != null){
				batch.draw(region, gifx + wx, gify + wy, gifwidth, 1f);
				batch.draw(region, gifx + wx, gify + wy + gifheight, gifwidth, 1f);
				batch.draw(region, gifx + wx, gify + wy, 1f, gifheight);
				batch.draw(region, gifx + wx + gifwidth, gify + wy, 1f, gifheight + 1f);
			}

			if(saving){
				if(!disableGUI)
					batch.setColor(Color.BLACK);

				float w = 200 * screenScale, h = 50 * screenScale;
				batch.draw(region, Gdx.graphics.getWidth() / 2 * screenScale - w / 2, Gdx.graphics.getHeight() / 2 * screenScale - h / 2, w, h);

				//this just blends red and green
				Color a = Color.RED;
				Color b = Color.GREEN;

				float s = saveprogress;
				float i = 1f - saveprogress;

				batch.setColor(a.r * i + b.r * s, a.g * i + b.g * s, a.b * i + b.b * s, 1f);

				batch.draw(region, Gdx.graphics.getWidth() / 2 * screenScale - w / 2, Gdx.graphics.getHeight() / 2 * screenScale - h / 2, w * saveprogress, h);

			}

			batch.setColor(Color.WHITE);
		}

		if(recording){
			giftime += delta;
			if(Gdx.graphics.getFrameId() % (60 / recordfps) == 0){
				byte[] pix = ScreenUtils.getFrameBufferPixels((int) (gifx / screenScale) + 1 + Gdx.graphics.getWidth() / 2, (int) (gify / screenScale) + 1 + Gdx.graphics.getHeight() / 2, (int) (gifwidth / screenScale) - 2, (int) (gifheight / screenScale) - 2, true);
				frames.add(pix);
			}
		}
	}

	public void setGUIDisabled(boolean disabled){
		this.disableGUI = true;
	}

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

	public void setScreenScale(float scale){
		screenScale = scale;
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

	public FileHandle takeScreenshot(){
		return takeScreenshot(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

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
		final Array<String> strings = new Array<String>();
		final Array<Pixmap> pixmaps = new Array<Pixmap>();

		for(byte[] bytes : frames){
			Pixmap pixmap = createPixmap(bytes);
			pixmaps.add(pixmap);
		}

		new Thread(new Runnable(){
			public void run(){

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
			}
		}).start();
	}

	private File compileGIF(Array<String> strings, FileHandle inputdirectory, FileHandle directory){
		if(strings.size == 0){
			System.err.println("WARNING: no strings!");
			return null;
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
		return createPixmap(pixels, (int) (gifwidth / screenScale) - 2, (int) (gifheight / screenScale) - 2);
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
	}

	/**
	 * Provide an implementation and call recorder.setController() for custom
	 * input
	 */
	static interface RecorderController{
		public boolean openKeyPressed();

		public boolean recordKeyPressed();

		public boolean resizeKeyPressed();
	}
}
