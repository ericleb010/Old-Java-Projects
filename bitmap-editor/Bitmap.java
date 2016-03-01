import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.JOptionPane;

/**
 * This class allows a user to import the contents of a regular Bitmap file into Java for manipulation.
 * @author Eric Leblanc
 * @version 1.0, 19/02/2015.
 * @since 1.8
 */
public class Bitmap {
	public static final int RED = 0;						// Public constant for RGB red.
	public static final int GREEN = 1;						// Public constant for RGB green.
	public static final int BLUE = 2;						// Public constant for RGB blue.
	private static final int DATA_OFFSET_VALUE = 54;		// The data offset assumed for all Bitmaps used in this class.
	private static final int COLOR_ENHANCE_VALUE = 40;		// The amount by which a color enhancement increases the RGB value.
	
	private int dataOffset = DATA_OFFSET_VALUE;				// The offset, in bytes, after which the actual color information is stored.
	private int width;										// The width, in pixels, of the image.
	private int height;										// The height, in pixels, of the image.
	private int numPaddingBytes;							// The number of padding bytes required in each row to satisfy the Bitmap standard.
	private int[] header = new int[dataOffset];				// An array of bytes which holds the information up to the data offset.
	private Color[][] pixels;								// A two-dimensional array of Color objects holding the image's RGB values.
	
	//-------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * The general use constructor for instantiating a Bitmap object.
	 * @param file a File object pointing to the requested bitmap file.
	 * @throws IOException if the file provided does not allow for reading.
	 */
	public Bitmap(File file) throws IOException {
		this.readBitmap(file);
	}
	
	/**
	 * A private constructor for generating a deep copy of a Bitmap object. 
	 * For use in this class only.
	 * @param width the width of the bitmap.
	 * @param height the height of the bitmap.
	 * @param numPaddingBytes the amount of padding per row of data.
	 * @param header the header data array corresponding to the Bitmap object.
	 * @param pixels the Color array corresponding to the Bitmap object.
	 */
	private Bitmap(int width, int height, int numPaddingBytes, int[] header, Color[][] pixels) {
		this.width = width;
		this.height = height;
		this.numPaddingBytes = numPaddingBytes;
		this.header = header;
		this.pixels = pixels;
	}

	//-------------------------------------------------------------------------------------------------------------------------

	/**
	 * Reads in the specified bitmap file and the data for our use.
	 * @param file a File object pointing to the requested bitmap file.
	 * @throws IOException if the file could not be read from the object provided.
	 */
	public void readBitmap(File file) throws IOException {
		// Open and read the header of the file in question.
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(0L);
		for (int i = 0; i < header.length; i++) {
			header[i] = raf.read();
		}
		
		// Now we can determine our width and height.
		for (int i = 0; i < 4; i++) {
			width += header[i + 18] * Math.pow(256, i);
			height += header[i + 22] * Math.pow(256, i);
		}
		
		// Create the pixels. Bitmap reads from bottom of image to the top.
		pixels = new Color[width][height];
		for (int y = height - 1; y >= 0; y--) {
			numPaddingBytes = 0;
			for (int x = 0; x < width; x++) {
				// Colors are formatted as BGR.
				int bv = raf.read();
				int gv = raf.read();
				pixels[x][y] = new Color(raf.read(), gv, bv);
			}
			// Padding may be necessary when reading a line per the bitmap standard. Record the amount.
			if (((width * 3) % 4) != 0) {
				for (int i = 0; i < (4 - (width * 3) % 4); i++) {
					raf.read();
					numPaddingBytes++;
				}
			}
		}
		raf.close();
	}
	
	/**
	 * Writes the Bitmap object information to a bitmap file specified by the parameter.
	 * @param file a File object pointing to the requested destination file.
	 * @throws IOException if the file could not be written to the location specified.
	 */
	public void writeBitmap(File file) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.seek(0L);
		for (int i = 0; i < header.length; i++) {
			raf.write(header[i]);
		}
		for (int y = height - 1; y >= 0 ; y--) {
			for (int x = 0; x < width; x++) {
				raf.write(pixels[x][y].getBlue());
				raf.write(pixels[x][y].getGreen());
				raf.write(pixels[x][y].getRed());
			}
			// Generate padding to match BMP specification.
			for (int i = 0; i < numPaddingBytes; i++) {
				raf.write(0);
			}
		}
		raf.close();
	}
	
	//-------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Flips the image vertically.
	 */
	public void flip() {
		// Allocate new memory for the results.
		Color[][] temp = new Color[width][height];
		
		// Generate a flipped version of the original.
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				temp[x][y] = pixels[x][height - y - 1];
			}
		}
		pixels = temp;
	}
	
	/**
	 * Averages a pixel's surrounding colour values to generate a blur effect.
	 */
	public void blur() {
		// Allocate new memory for the results.
		Color[][] temp = new Color[width][height];
		
		// Loop through all of the pixels.
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// We need to keep track of the surrounding pixel values for our average.
				int rTotal = 0;
				int gTotal = 0;
				int bTotal = 0;
				int num = 0;
							
				// Now we add the values of the surrounding pixels, if they exist.
				if (x > 0) {
					if (y > 0) {
						rTotal += pixels[x - 1][y - 1].getRed();
						gTotal += pixels[x - 1][y - 1].getGreen();
						bTotal += pixels[x - 1][y - 1].getBlue();
						num++;
					}
					rTotal += pixels[x - 1][y].getRed();
					gTotal += pixels[x - 1][y].getGreen();
					bTotal += pixels[x - 1][y].getBlue();
					num++;
					if (y < height - 1) {
						rTotal += pixels[x - 1][y + 1].getRed();
						gTotal += pixels[x - 1][y + 1].getGreen();
						bTotal += pixels[x - 1][y + 1].getBlue();
						num++;
					}
				}
				if (y > 0) {
					rTotal += pixels[x][y - 1].getRed();
					gTotal += pixels[x][y - 1].getGreen();
					bTotal += pixels[x][y - 1].getBlue();
					num++;
				}
				rTotal += pixels[x][y].getRed();
				gTotal += pixels[x][y].getGreen();
				bTotal += pixels[x][y].getBlue();
				num++;
				if (y < height - 1) {
					rTotal += pixels[x][y + 1].getRed();
					gTotal += pixels[x][y + 1].getGreen();
					bTotal += pixels[x][y + 1].getBlue();
					num++;
				}
				if (x < width - 1) {
					if (y > 0) {
						rTotal += pixels[x + 1][y - 1].getRed();
						gTotal += pixels[x + 1][y - 1].getGreen();
						bTotal += pixels[x + 1][y - 1].getBlue();
						num++;
					}
					rTotal += pixels[x + 1][y].getRed();
					gTotal += pixels[x + 1][y].getGreen();
					bTotal += pixels[x + 1][y].getBlue();
					num++;
					if (y < height - 1) {
						rTotal += pixels[x + 1][y + 1].getRed();
						gTotal += pixels[x + 1][y + 1].getGreen();
						bTotal += pixels[x + 1][y + 1].getBlue();
						num++;
					}
				}
				
				// Take the average and use this as the new colour value.
				temp[x][y] = new Color(rTotal / num, gTotal / num, bTotal / num);
			}
		}
		pixels = temp;
	}
	
	/**
	 * Enhances either the red, green, or blue colour values in the image. It adds COLOR_ENHANCE_VALUE to the specified color.
	 * @param selection an int defining which colour to enhance.
	 */
	public void enhanceColor(int selection) {
		// We need a temporary pixel grid to store the results.
		Color[][] temp = new Color[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Enhance the value of the colour selected earlier.
				temp[x][y] = new Color(((selection == RED) ? ((pixels[x][y].getRed() < 255 - COLOR_ENHANCE_VALUE) ? pixels[x][y].getRed() + COLOR_ENHANCE_VALUE : 255) : pixels[x][y].getRed()),
										((selection == GREEN) ? ((pixels[x][y].getGreen() < 255 - COLOR_ENHANCE_VALUE) ? pixels[x][y].getGreen() + COLOR_ENHANCE_VALUE : 255) : pixels[x][y].getGreen()),
										((selection == BLUE) ? ((pixels[x][y].getBlue() < 255 - COLOR_ENHANCE_VALUE) ? pixels[x][y].getBlue() + COLOR_ENHANCE_VALUE : 255) : pixels[x][y].getBlue()));
			}
		}
		pixels = temp;
	}
	
	/**
	 * Takes in an array of BufferedImages and combines them into this object.
	 * @param images an array of BufferedImage objects, with equal dimensions, to combine into this object.
	 * @throws Exception a generic exception in the event the image dimensions don't all match up.
	 */
	public void combine(BufferedImage[] images) throws Exception {
		// We need a temporary pixel grid to store the results.
		Color[][] temp = new Color[width][height];
		int numOfImages = images.length + 1;
		
		// First we need to make dimension checks.
		for (BufferedImage image : images) {
			if (image.getWidth() != width || image.getHeight() != height)
				// Oops! The user gave us an image with different dimensions...
				throw new Exception();
		}
		
		// Cycle through all pixels.
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Take the average of each colour value for every pixel we find and write that value.
				int rAvg = pixels[x][y].getRed();
				int gAvg = pixels[x][y].getGreen();
				int bAvg = pixels[x][y].getBlue();
				// And now for the images in the array...
				for (BufferedImage image : images) {
					Color color = new Color(image.getRGB(x, y));
					rAvg += color.getRed();
					gAvg += color.getGreen();
					bAvg += color.getBlue();
				}
				temp[x][y] = new Color(rAvg / numOfImages, gAvg / numOfImages, bAvg / numOfImages);
			}
		}
		pixels = temp;
	}
	
	//-------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Returns an image representation of the Bitmap object.
	 * @return a BufferedImage representation of the Bitmap.
	 */
	public BufferedImage getImage() {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, pixels[x][y].getRGB());
			}
		}
		return image;
	}
	
	/**
	 * A getter method for the width of the bitmap.
	 * @return an integer for the width of the image.
	 */
	public int getWidth() {
		return this.width;
	}
	
	/**
	 * A getter method for the height of the bitmap.
	 * @return an integer for the height of the image.
	 */
	public int getHeight() {
		return this.height;
	}
	
	//-------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Returns a deep copy of this Bitmap object.
	 * @return a deep copy of this Bitmap object.
	 */
	public Bitmap copy() {
		return new Bitmap(this.width, this.height, this.numPaddingBytes, this.header, this.pixels);
	}
	
	/**
	 * Overriding the equality functionality for Bitmap objects.
	 * @param bmp an object of type Bitmap.
	 * @return a flag if the Bitmap objects match or not.
	 */
	public boolean equals(Object bmp) {
		// Compare all simple instance variables first.
		if (this.dataOffset != ((Bitmap) bmp).dataOffset || this.width != ((Bitmap) bmp).width || this.height != ((Bitmap) bmp).height || !header.equals(((Bitmap) bmp).header))
			return false;
		// Then check the color grid.
		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				if (!this.pixels[x][y].equals(((Bitmap) bmp).pixels[x][y])) {
					return false;
				}
			}
		}
		return true;
		
	}
}
