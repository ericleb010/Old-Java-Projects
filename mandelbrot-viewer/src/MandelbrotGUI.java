import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.plaf.basic.*;
/**
 * Allows for the display of the Mandelbrot set on the user's screen, permitting them to zoom in/out, and scroll around the complex
 * plane visually to observe the fractal properties the set generates. Some configurations can also be changed.
 * @author Eric Leblanc
 * @version 1.0, 20/03/15.
 */
public class MandelbrotGUI extends JFrame {

	private static final int FRAME_WIDTH = 600;											// The width of the window on the user's screen.
	private static final int FRAME_POSITION = 150;										// The initial position of the window on the user's screen.
	private static final double INITIAL_LEFT_REAL = -2.2;								// The real component for the position of the initial top-left corner.
	private static final double INITIAL_LEFT_IMAG = 1.8;								// The imaginary component for the position of the initial top-left-corner.
	private static final double INITIAL_VIEW_WIDTH = 3.6;								// The initial width of the frame on the complex plane.
	
																						// The initial maximum number of iterations we'll process before assuming the sequence tends to infinity.
	private static final double INITIAL_MAX_ITERATIONS = 100;							// The higher this value, the more detail, but will also cause more overhead processing.
																						// Can be changed by the user by entering the File menu.
	
	private static final double ZOOM_AMOUNT = 0.65;										// The percentage by which the zoom in function affects the view.
	private static final double SCROLL_AMOUNT = 0.2;									// The percentage of the current plane width we move when the user scrolls.
	private static final double COLOR_THRESHOLD = 90;									// The threshold in RG values, below which we consider for "low" iterations, above which we consider for more.
	
	private static final int NORTH = SwingConstants.NORTH;								// <--------------
	private static final int SOUTH = SwingConstants.SOUTH;								// Direction constants matching those 
	private static final int EAST = SwingConstants.EAST;								// used in the Swing package.
	private static final int WEST = SwingConstants.WEST;								// <--------------
	
	private double maxIterations = INITIAL_MAX_ITERATIONS;								// The current max iterations value.
	private double colorIncrementLow = COLOR_THRESHOLD / maxIterations * 10;			// The incrementation by which the red and green values increases for lower iterations required to represent a point.
	private double colorIncrementHigh = (255 - COLOR_THRESHOLD) / maxIterations;		// The incrementation by which the red and green values increases for higher iterations required to represent a point.
	private double planeWidth = INITIAL_VIEW_WIDTH;										// The current width of the complex plane displayed. Set to the default value.
	private double scale = INITIAL_VIEW_WIDTH / FRAME_WIDTH;							// The number of units on the plane per pixel. Set to the default value.
	
	private JLabel frame = null;														// The component holding the graphical representation of the set.
	BufferedImage display = null;														// The graphical representation of the set.
	
	private Complex curTopLeft = new Complex(INITIAL_LEFT_REAL, INITIAL_LEFT_IMAG);		// Holds the current top-left corner coordinate in the Complex plane.
	
	//---------------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Constructor for the GUI, setting up all components and appropriate listeners.
	 */
	public MandelbrotGUI() {
		// Set up the GUI frame.
		super("Mandelbrot Set Viewer - by Eric Leblanc");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLocation(FRAME_POSITION, FRAME_POSITION / 2);
		this.setVisible(true);
		this.setResizable(false);
		this.setLayout(new BorderLayout());
		
		// Create the scroll buttons.
		JButton uArrow = new BasicArrowButton(NORTH);
		JButton dArrow = new BasicArrowButton(SOUTH);
		JButton rArrow = new BasicArrowButton(EAST);
		JButton lArrow = new BasicArrowButton(WEST);
		uArrow.addActionListener(new ArrowListener());
		dArrow.addActionListener(new ArrowListener());
		rArrow.addActionListener(new ArrowListener());
		lArrow.addActionListener(new ArrowListener());
		
		// Create the menu bar.
		JMenuBar menu = new JMenuBar();
		this.setJMenuBar(menu);
		JMenu file = new JMenu("File");
		JMenuItem restore = new JMenuItem("Restore image");
		JMenuItem options = new JMenuItem("Options...");
		JMenuItem exit = new JMenuItem("Exit");
		
		// Add some listeners...
		restore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Restore all the defaults.
				curTopLeft = new Complex(INITIAL_LEFT_REAL, INITIAL_LEFT_IMAG);
				scale = INITIAL_VIEW_WIDTH / FRAME_WIDTH;
				planeWidth = INITIAL_VIEW_WIDTH;
				redrawFrame();
			}
		});
		options.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Open a new box offering to change a few of the options
				JFrame optionBox = new JFrame("Options");
				optionBox.setVisible(true);
				optionBox.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				Container mainContainer = new Container();
				mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
				Container buttonContainer = new Container();
				buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.X_AXIS));
				// Some instructions for the slider deciding maxIterations.
				JLabel iterInstruct = new JLabel("Maximum iterations performed:");
				JSlider iterChoice = new JSlider(50, 500, (int) maxIterations);
				iterChoice.setMinorTickSpacing(50);
				iterChoice.setSnapToTicks(true);
				iterChoice.setPaintTicks(true);
				// Confirmation buttons.
				JButton apply = new JButton("Apply");
				JButton cancel = new JButton("Cancel");
				apply.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// Apply any changes.
						maxIterations = iterChoice.getValue();
						colorIncrementLow = COLOR_THRESHOLD / maxIterations * 10;
						colorIncrementHigh = (255 - COLOR_THRESHOLD) / maxIterations;
						optionBox.dispose();
						((MandelbrotGUI) getMe()).redrawFrame();
					}
				});
				cancel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// Don't apply any changes. Just close.
						optionBox.dispose();
					}
				});
				
				// Add to the frame and repaint!
				buttonContainer.add(apply);
				buttonContainer.add(cancel);
				mainContainer.add(iterInstruct);
				mainContainer.add(iterChoice);
				mainContainer.add(buttonContainer);
				optionBox.add(mainContainer);
				optionBox.pack();
				optionBox.revalidate();
				optionBox.repaint();
			}
		});
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		//------------------------------------------------------------------------------------------------------------------------
		
		// Now we can determine the window size. Preferably enough space for everything, since user can't resize.
		this.setSize(FRAME_WIDTH + lArrow.getWidth() * 2, FRAME_WIDTH + uArrow.getHeight() * 2 + menu.getHeight());
		
		// Create an image for use as a visual display of the Mandelbrot set.
		display = new BufferedImage(FRAME_WIDTH, FRAME_WIDTH, BufferedImage.TYPE_INT_RGB);
		frame = new JLabel(new ImageIcon(display));
		frame.addMouseListener(new MouseListener() {
			// Fired when the user clicks on their mouse.
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e))
					((MandelbrotGUI) getMe()).zoom(true, e.getPoint());
				else if (SwingUtilities.isRightMouseButton(e))
					((MandelbrotGUI) getMe()).zoom(false, e.getPoint());
			}			
			// Unimplemented methods.
			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) {}
			public void mousePressed(MouseEvent arg0) {}
			public void mouseReleased(MouseEvent arg0) {}
		});
		
		// Add all of these pieces to our frame.
		frame.setVisible(true);
		uArrow.setVisible(true);
		dArrow.setVisible(true);
		lArrow.setVisible(true);
		rArrow.setVisible(true);
		this.add(frame);
		this.add(uArrow, BorderLayout.NORTH);
		this.add(dArrow, BorderLayout.SOUTH);
		this.add(rArrow, BorderLayout.EAST);
		this.add(lArrow, BorderLayout.WEST);
		menu.add(file);
		file.add(restore);
		file.add(options);
		file.addSeparator();
		file.add(exit);
		
		// Paint our frame and refresh.
		pack();
		redrawFrame();
	}
	
	//------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * The method which redraws the frame containing the imagery for the user, whenever it needs refreshing.
	 */
	private void redrawFrame() {
		// We have to go through all of the pixels in our frame.
		for (int x = 0; x < FRAME_WIDTH; x++) {
			for (int y = 0; y < FRAME_WIDTH; y++) {
				// Determine where this pixel is located on the plane. Then we run Mandelbrot's Rule and determine the appropriate color.
				Complex thisPoint = new Complex(curTopLeft.getReal() + (x * scale), curTopLeft.getImag() - (y * scale));
				display.setRGB(x, y, (pickColor(doIterations(thisPoint))).getRGB());
			}
		}
		// Repaint the frame.
		revalidate();
		repaint();
	}
	
	/**
	 * Takes in a point on the complex plane and calculates the number of iterations of the Mandelbrot Rule
	 * @param c a Complex object, which will serve as Z<sub>0</sub>.
	 * @return an integer, the number of iterations of the Mandelbrot Rule before we leave a circle of radius 2.
	 */
	private int doIterations(Complex orig) {
		// Duplicate the Complex object and set the counter.
		Complex c = new Complex(0, 0);
		int counter = 0;
		
		// As long as our c doesn't cross the circle of radius 2, we keep squaring it and adding orig to itself.
		while (c.modulus() <= 2.0 && counter < maxIterations) {
			c.multiply(c);
			c.add(orig);
			counter++;
		}
		// We'll return the counter value which last met the Mandelbrot Rule.
		return counter;
	}
	
	/**
	 * Allows the frame to zoom in or out from a specified point.
	 * @param inward a flag, which determines if we are zooming in (<code>TRUE</code>) or out (<code>FALSE</code>).
	 * @param location the point on the image which was clicked.
	 */
	private void zoom(boolean inward, Point location) {
		// Extract the coordinates from the Point object, which was obtained from our user's mouse click event.
		int x = (int) location.getX();
		int y = (int) location.getY();
		// Determine the corresponding point on the complex plane.
		Complex thisPoint = new Complex(curTopLeft.getReal() + (x * scale), curTopLeft.getImag() - (y * scale));
		
		// Check if we were asked to zoom in or out.
		if (inward)
			planeWidth *= ZOOM_AMOUNT;
		else
			planeWidth /= ZOOM_AMOUNT;
		
		// Recalculate the resulting number of units on the plane per pixel, and then the new top-left coordinate. Redraw the image!
		scale = planeWidth / FRAME_WIDTH;
		curTopLeft = new Complex(thisPoint.getReal() - (FRAME_WIDTH / 2 * scale), thisPoint.getImag() + (FRAME_WIDTH / 2 * scale));
		redrawFrame();
	}
	
	/**
	 * Returns a Color object, varying depending on the number of iterations performed for that pixel.
	 * @param numIter an integer for the number of iterations performed
	 * @return a correponding Color object.
	 */
	private Color pickColor(int numIter) {
		Color result = null;
		// We're distinguishing the cases where the number of iterations was low vs. high. Provides a nicer contrast while still having detail at deeper zoom levels.
		if (numIter <= maxIterations / 10)
			result = new Color((int) (colorIncrementLow * numIter) / 2, (int) (colorIncrementLow * numIter), 0);
		else if (numIter < maxIterations)
			result = new Color((int) (COLOR_THRESHOLD + (colorIncrementHigh * numIter)) / 2, (int) (COLOR_THRESHOLD + (colorIncrementHigh * numIter)), 0);
		else 
			result = Color.BLACK;
		return result;
	}
	
	//------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Implementation of an ActionListener permitting the functionality of one of the arrow scroll buttons.
	 * @author Eric Leblanc
	 */
	private class ArrowListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// Get the button pressed and determine its direction.
			switch (((BasicArrowButton) e.getSource()).getDirection()) {
			case NORTH:
				curTopLeft.add(new Complex(0, planeWidth * SCROLL_AMOUNT));
				break;
			case SOUTH:
				curTopLeft.subtract(new Complex(0, planeWidth * SCROLL_AMOUNT));
				break;
			case EAST:
				curTopLeft.add(new Complex(planeWidth * SCROLL_AMOUNT, 0));
				break;
			case WEST:
				curTopLeft.subtract(new Complex(planeWidth * SCROLL_AMOUNT, 0));
				break;
			}
			// Redraw the image for the user.
			redrawFrame();
		}
	}
	
	/**
	 * Gives us access to this GUI if we are not necessarily in its scope.
	 * @return this object.
	 */
	public JFrame getMe() {
		return this;
	}
	/**
	 * The main method to create the JFrame.
	 */
	public static void main(String[] args) {
		new MandelbrotGUI();
	}
}
