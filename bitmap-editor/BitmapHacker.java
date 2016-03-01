import java.util.*;
import java.io.*;
import javax.swing.*;
/**
 * This program allows the user to perform BMP image manipulations: either flip the image, enhance a colour
 * within it, blur it, or combine it with another. 
 * @author Eric Leblanc
 * @version 1.0, 02/02/15
 */
public class BitmapHacker {

	/**
	 * The main method for the program.
	 */
	public static void main(String[] args) {
		// We will loop until the user decides to quit. We'll use the exit() function to quit.
		while (true) {			
			// Make some room on the console.
			for(int i = 0; i < 30; i++) {
				System.out.println();
			}
			
			// Display the menu.
			System.out.print("0. Exit\n1. Flip image top/bottom\n2. Enhance color\n3. Blur image\n4. Combine two images\n\nEnter your choice ----> ");
			
			// Take in input.
			Scanner sc = new Scanner(System.in);
			int option = 0;
			boolean next = false;
			do {
				try {
					option = Integer.parseInt(sc.nextLine());
					if (option < 0 || option > 4) {
						System.err.println("That's not a valid option.");
						continue;
					}
					next = true;
				} catch (NumberFormatException e) {
					System.err.println("That's not a number.");
				}
			} while (!next);
			
			// Quit if the user so desires.
			if (option == 0)
				System.exit(0);
			
			// If we get to here, we will need to receive at least one file.
			JFileChooser jf = new JFileChooser();
			if (jf.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
				JOptionPane.showMessageDialog(null, "File not chosen. Operation aborted.", "ERROR", JOptionPane.ERROR_MESSAGE);
				continue;
			}
			File file1 = jf.getSelectedFile();
			
			// Now let's dissect the option they chose.
			switch(option) {
			case 1: 
				flip(file1);
				break;
			case 2:
				enhance(file1);
				break;
			case 3:
				blur(file1);
				break;
			case 4: 
				// This option requires a second file specified.
				jf = new JFileChooser();
				if (jf.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
					JOptionPane.showMessageDialog(null, "File not chosen. Operation aborted.", "ERROR", JOptionPane.ERROR_MESSAGE);
					System.exit(-1);
				}
				File file2 = jf.getSelectedFile();
				combine(file1, file2);
				break;
			}
		}
	}
	
	//------------------------------------------------------------------------
	
	/**
	 * A helper class for all image manipulations.
	 * @author School
	 *
	 */
	private static class Pixel {
		int red;
		int green;
		int blue;
		
		public Pixel(int blue, int green, int red) {
			this.setRed(red);
			this.setGreen(green);
			this.setBlue(blue);
		}
		
		public int getRed() {
			return red;
		}

		public void setRed(int red) {
			if (red < 0 || red > 255) 
				System.err.println("You must enter a value from 0 to 255!");
			else
				this.red = red;
		}

		public int getGreen() {
			return green;
		}

		public void setGreen(int green) {
			if (green < 0 || green > 255) 
				System.err.println("You must enter a value from 0 to 255!");
			else
				this.green = green;
		}

		public int getBlue() {
			return blue;
		}

		public void setBlue(int blue) {
			if (blue < 0 || blue > 255) 
				System.err.println("You must enter a value from 0 to 255!");
			else
				this.blue = blue;
		}
		
	}
	
	//----------------------------------------------------------------------------
	
	/**
	 * This function takes in a BMP file and flips its pixels vertically.
	 * @param file the BMP image chosen by the user.
	 */
	private static void flip(File file) {
		try {			
			// Extracting the header information.
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			byte[] header = new byte[54];
			raf.seek(0L);
			raf.read(header);
			raf.close();
			// Create the pixel grid.
			Pixel[][] grid = createGridFromFile(file);

			// Creating a path to a newly generated file for output.
			String path = file.getAbsolutePath();
			File newFile = new File(path.substring(0, path.lastIndexOf('.')) + "Flipped.bmp");
			raf = new RandomAccessFile(newFile, "rw");
			
			// Begin writing to the new image.
			raf.seek(0L);
			raf.write(header);
			int width = grid.length;
			int height = grid[0].length;
			// Cycle through all pixels in our grid.
			for (int y = height - 1; y >= 0; y--) {
				for (int x = 0; x < width; x++) {
					raf.write(grid[x][y].getBlue());
					raf.write(grid[x][y].getGreen());
					raf.write(grid[x][y].getRed());
				}
				// Generate padding every row to match BMP specification.
				if (((width * 3) % 4) != 0) {
					for (int i = 0; i < (4 - (width * 3) % 4); i++) {
						raf.write(0);
					}
				}
			}
			// Close the stream to our file, flushing remaining data.
			raf.close();
		} catch (IOException e) {
			System.out.println("There was an error while attempting to read your file.");
		}
	}
	
	//-----------------------------------------------------------------------------------
	
	/** 
	 * Takes in a BMP file and enhances either the red, green, or blue colour values in the image.
	 * @param file a BMP file the user chose.
	 */
	private static void enhance(File file) {
		try {			
			// Extracting the header information.
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			byte[] header = new byte[54];
			raf.seek(0L);
			raf.read(header);
			raf.close();
			// Create the Pixel grid.
			Pixel[][] grid = createGridFromFile(file);
			
			// Pick a colour!
			int selection = JOptionPane.showOptionDialog(null, "Which colour would you like to enhance?", "Select color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {"R", "G", "B"}, null);			
			if (selection == JOptionPane.CLOSED_OPTION) {
				JOptionPane.showMessageDialog(null, "Option not chosen. Operation aborted.", "ERROR", JOptionPane.ERROR_MESSAGE);
				raf.close();
				return;
			}

			// Open a new file for writing.
			String path = file.getAbsolutePath();
			File newFile = new File(path.substring(0, path.lastIndexOf('.')) + "Enhanced.bmp");
			raf = new RandomAccessFile(newFile, "rw");
			
			// Start writing.
			raf.seek(0L);
			raf.write(header);
			int width = grid.length;
			int height = grid[0].length;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// Enhance the value of the colour selected earlier. Currently adding 40.
					raf.write((selection == 2) ? ((grid[x][y].getBlue() < 215) ? grid[x][y].getBlue() + 40 : 255) : grid[x][y].getBlue());
					raf.write((selection == 1) ? ((grid[x][y].getGreen() < 215) ? grid[x][y].getGreen() + 40 : 255) : grid[x][y].getGreen());
					raf.write((selection == 0) ? ((grid[x][y].getRed() < 215) ? grid[x][y].getRed() + 40 : 255) : grid[x][y].getRed());
				}
				// Generate padding to match BMP specification.
				if (((width * 3) % 4) != 0) {
					for (int i = 0; i < (4 - (width * 3) % 4); i++) {
						raf.write(0);
					}
				}
			}
			raf.close();
		} catch (IOException e) {
			System.out.println("There was an error while attempting to read your file.");
		}
	}
	
	//-----------------------------------------------------------------------------------
	
	/**
	 * Takes in a BMP file and averages a pixel's surrounding pixels' colour values to generate a blur effect.
	 * @param file a BMP image file the user chose.
	 */
	private static void blur(File file) {
		try {			
			// Extracting the header information.
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			byte[] header = new byte[54];
			raf.seek(0L);
			raf.read(header);
			raf.close();
			// Create the Pixel grid.
			Pixel[][] grid = createGridFromFile(file);

			// Create a new file to write to.
			String path = file.getAbsolutePath();
			File newFile = new File(path.substring(0, path.lastIndexOf('.')) + "Blurred.bmp");
			raf = new RandomAccessFile(newFile, "rw");
			
			// Start writing.
			raf.seek(0L);
			raf.write(header);
			int width = grid.length;
			int height = grid[0].length;
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
							rTotal += grid[x - 1][y - 1].getRed();
							gTotal += grid[x - 1][y - 1].getGreen();
							bTotal += grid[x - 1][y - 1].getBlue();
							num++;
						}
						rTotal += grid[x - 1][y].getRed();
						gTotal += grid[x - 1][y].getGreen();
						bTotal += grid[x - 1][y].getBlue();
						num++;
						if (y < height - 1) {
							rTotal += grid[x - 1][y + 1].getRed();
							gTotal += grid[x - 1][y + 1].getGreen();
							bTotal += grid[x - 1][y + 1].getBlue();
							num++;
						}
					}
					if (y > 0) {
						rTotal += grid[x][y - 1].getRed();
						gTotal += grid[x][y - 1].getGreen();
						bTotal += grid[x][y - 1].getBlue();
						num++;
					}
					rTotal += grid[x][y].getRed();
					gTotal += grid[x][y].getGreen();
					bTotal += grid[x][y].getBlue();
					num++;
					if (y < height - 1) {
						rTotal += grid[x][y + 1].getRed();
						gTotal += grid[x][y + 1].getGreen();
						bTotal += grid[x][y + 1].getBlue();
						num++;
					}
					if (x < width - 1) {
						if (y > 0) {
							rTotal += grid[x + 1][y - 1].getRed();
							gTotal += grid[x + 1][y - 1].getGreen();
							bTotal += grid[x + 1][y - 1].getBlue();
							num++;
						}
						rTotal += grid[x + 1][y].getRed();
						gTotal += grid[x + 1][y].getGreen();
						bTotal += grid[x + 1][y].getBlue();
						num++;
						if (y < height - 1) {
							rTotal += grid[x + 1][y + 1].getRed();
							gTotal += grid[x + 1][y + 1].getGreen();
							bTotal += grid[x + 1][y + 1].getBlue();
							num++;
						}
					}
					
					// Take the average for each colour value.
					int rAvg = rTotal / num;
					int gAvg = gTotal / num;
					int bAvg = bTotal / num;
					
					// Write the colour values in order.
					raf.write(bAvg);
					raf.write(gAvg);
					raf.write(rAvg);
				}
				// Generate padding to match BMP specification.
				if (((width * 3) % 4) != 0) {
					for (int i = 0; i < (4 - (width * 3) % 4); i++) {
						raf.write(0);
					}
				}
			}
			// Close the file stream.
			raf.close();
		} catch (IOException e) {
			System.out.println("There was an error while attempting to read your file.");
		}
	}
	
	//-----------------------------------------------------------------------------------
	
	/**
	 * Takes in two files and combines them, storing the result in a file in the same folder as the first.
	 * @param file1 a file picked by the user.
	 * @param file2 another file picked by the user, same size.
	 */
	private static void combine(File file1, File file2) {
		try {			
			// Extracting the header information.
			RandomAccessFile raf = new RandomAccessFile(file1, "r");
			byte[] header = new byte[54];
			raf.seek(0L);
			raf.read(header);
			raf.close();
			// Create the Pixel grids.
			Pixel[][] grid1 = createGridFromFile(file1);
			Pixel[][] grid2 = createGridFromFile(file2);
			
			// If the dimensions don't match, we can't use this method. Complain if that's the case.
			if ((grid1.length != grid2.length) || (grid1[0].length != grid2[0].length)) {
				JOptionPane.showMessageDialog(null, "Images are not the same width. Please choose compatible images.", "ERROR", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Make a path to an output file in the same folder as the first file.
			String path1 = file1.getAbsolutePath();
			String path2 = file2.getAbsolutePath();
			File newFile = new File(path1.substring(0, path1.lastIndexOf('.')) + path2.substring(path2.lastIndexOf('\\') + 1, path2.lastIndexOf('.') + 1) + "Merger.bmp");
			raf = new RandomAccessFile(newFile, "rw");

			// Start writing.
			raf.seek(0L);
			raf.write(header);
			int width = grid1.length;
			int height = grid1[0].length;
			// Loop through every pixel.
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// Take the average of each colour value for every pixel we find and write that value.
					raf.write((grid1[x][y].getBlue() + grid2[x][y].getBlue()) / 2);
					raf.write((grid1[x][y].getGreen() + grid2[x][y].getGreen()) / 2);
					raf.write((grid1[x][y].getRed() + grid2[x][y].getRed()) / 2);
				}
				// Generate padding to match BMP specification.
				if (((width * 3) % 4) != 0) {
					for (int i = 0; i < (4 - (width * 3) % 4); i++) {
						raf.write(0);
					}
				}
			}
			// Close the file stream.
			raf.close();
		} catch (IOException e) {
			System.err.println("There was an error while attempting to read your file.");
		}
	}
	
	//-----------------------------------------------------------------------------------
	
	/**
	 * A helper function to facilitate the organization of a bitmap file into a 2-D grid of pixels.
	 * @param file a File object linking to the input file.
	 * @return a 2-D array of Pixel objects.
	 */
	private static Pixel[][] createGridFromFile(File file) {
		Pixel[][] grid = null;
		try {
			// Accessing the file in question.
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(18L);
			// Finding the width.
			int width = 0;
			for (int i = 0; i < 4; i++) {
				width += raf.read() * Math.pow(256, i);
			}
			// Finding the height.
			int height = 0;
			for (int i = 0; i < 4; i++) {
				height += raf.read() * Math.pow(256, i);
			}
			
			raf.seek(54L);
			// Creating the pixel grid.
			grid = new Pixel[width][height];
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					grid[x][y] = new Pixel(raf.read(), raf.read(), raf.read());
				}
				if (((width * 3) % 4) != 0) {
					for (int i = 0; i < (4 - (width * 3) % 4); i++) {
						raf.read();
					}
				}
				
			}
			raf.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
		} catch (IOException e) {
			System.out.println("There was an error while attempting to read your file.");
		}
		return grid;
	}
}