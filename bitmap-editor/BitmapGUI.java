import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

/**
 * This is a GUI for manipulating Bitmap objects and corresponding images. 
 * The user is able to blur, vertically flip, enhance the color of, and combine other images with the selected object.
 * @author Eric Leblanc
 * @version 1.0, 19/02/2015
 * @since 1.8
 */
public class BitmapGUI extends JFrame {
	
	private static final Dimension INITIAL_CENTER_DIM = new Dimension(640, 480);		// A constant for the initial display size, in pixels, for the main window.
	private static final int ICON_WIDTH = 150;											// A constant for the default width, in pixels, of a preview icon in the Combine Images window.
	private static final int OPTION_BUTTONS_SPTR_X = 150;								// A constant, in pixels, for the whitespace between the center view and image buttons on the main window.
	private static final int OPTION_BUTTONS_SPTR_Y = 15;								// A constant, in pixels, for the whitespace between each button on the main window.
	
	private Bitmap bmp;																	// The Bitmap object currently in use. 
	private boolean modified;															// A flag for whether or not the current Bitmap object has been saved.
	private File mostRecentInputFile;													// The bitmap file that was most recently opened or saved to.
	private Bitmap lastSavedBMP;														// The Bitmap object last subject to a save operation.
	
	private Stack<Bitmap> undoHistory;													// A Stack implementation of the undo function.
	private Stack<Bitmap> redoHistory;													// A Stack implementation of the redo function.
	
	private JLabel imageDisplayed = new JLabel("");										// A JLabel to handle the display the center image.
	private JLabel state;																// A JLabel to handle the display of the modified variable.
	private JButton flipButton = new JButton("Flip Image");								// A globally accessible button for vertically flipping an image.
	private JButton blurButton = new JButton("Blur Image");								// A globally accessible button for blurring an image.
	private JButton enhanceButton = new JButton("Enhance Color");						// A globally accessible button for enhancing color in an image.
	private Container colorChooser = new Container();									// A Container for all buttons related to the enhance color functionality.
	private JButton combineButton = new JButton("Combine Image");						// A globally accessible button for combining multiple images together.
	private JButton undoButton = new JButton("Undo");									// A globally accessible button for undoing the last change made.
	private JButton redoButton = new JButton("Redo");									// A globally accessible button for redoing the last change made.
	private Component centerBox = Box.createRigidArea(INITIAL_CENTER_DIM);				// A globally accessible whitespace box for between the center view and image buttons on the main window.

	private JMenuItem save = new JMenuItem("Save");										// A globally accessible menu button for saving to the mostRecentInputFile.
	private JMenuItem saveAs = new JMenuItem("Save as...");								// A globally accessible menu button for saving to a file of one's choosing.
	private JMenuItem close = new JMenuItem("Close");									// A globally accessible menu button for closing the opened image.
	
	private ArrayList<BufferedImage> images;											// An ArrayList of BufferedImages for the Combine function.
	private JWindow imageChooser;														// A JWindow for the Combine function UI.
	private Container imageDisplay;														// A globally accessible Container for the Combine function image preview icons.
	
	//-----------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * The constructor for the user interface.
	 */
	public BitmapGUI() {
		
		// Instantiating the GUI and setting its position.
		super("Bitmap Enhancement Tool");
		repositionScreen();
		this.setVisible(true);
		addComponents();
		
		// Make sure that we have full control over when the program exits.
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		// Add a dummy object in the middle for now.
		this.add(imageDisplayed);
		
		//-------------------------------------------------------------------------------------------------------------------------------------
		
		// Define a listener for any closing events, so that we can override and prompt the user for saving if required.
		this.addWindowListener(new WindowListener() {
			
			// When the window is about to close. This is the first event triggered when closing the window.
			public void windowClosing(WindowEvent e) {
				// Check if image has been saved.
				if (modified) {
					int choice = JOptionPane.showConfirmDialog(null, "There are unsaved changes to your image. Would you like to do so first?", "Save?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
					if (choice == JOptionPane.YES_OPTION)
						try {
							saveImage(false);
						} catch (IOException ev) {
							// If we can't write, warn user and cancel.
							JOptionPane.showMessageDialog(null, "Could not write the image to where it was. No changes made.", "ERROR", JOptionPane.ERROR_MESSAGE);
							return;
						}
					// If they ask to cancel, do NOT close the window.
					if (choice == JOptionPane.CANCEL_OPTION)
						return;
				}
				// As long they don't say cancel, proceed to closing the window.
				this.windowClosed(null);
			}
			
			// When the window is finally closed.
			public void windowClosed(WindowEvent e) {
				getMe().dispose();
				System.exit(0);
			}
			
			// Unused methods.
			public void windowActivated(WindowEvent arg0) {}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
		});
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * A helper function for the addition of the components to the main window.
	 */
	private void addComponents() {
		// Make sure we're using BorderLayout here.
		this.setLayout(new BorderLayout());	
		
		// Add the menu and appropriate buttons.
		JMenuBar menu = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem open = new JMenuItem("Open Image...");
		JMenuItem exit = new JMenuItem("Exit");
		setJMenuBar(menu);
		menu.add(file);
		menu.add(exit);
		file.add(open);
		file.addSeparator();
		file.add(save);
		file.add(saveAs);
		file.addSeparator();
		file.add(close);
		file.add(exit);
		
		// Set up the various components for each section.
		Container optionButtons = new Container();
		optionButtons.setLayout(new BoxLayout(optionButtons, BoxLayout.Y_AXIS));
		Container bottomStuff = new Container();
		bottomStuff.setLayout(new FlowLayout());
		Container topInfo = new Container();
		topInfo.setLayout(new FlowLayout());
		colorChooser.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 0));
		state = new JLabel("");
		
		// Make sure the right-side buttons are center-justified.
		flipButton.setAlignmentX(0.5f);
		blurButton.setAlignmentX(0.5f);
		enhanceButton.setAlignmentX(0.5f);
		combineButton.setAlignmentX(0.5f);
		
		// Add these buttons to their appropriate containers.
		topInfo.add(state);
		topInfo.add(Box.createRigidArea(new Dimension(1, state.getFont().getSize() * 2)));
		bottomStuff.add(undoButton);
		bottomStuff.add(redoButton);
		optionButtons.add(Box.createVerticalGlue());
		optionButtons.add(flipButton);
		optionButtons.add(Box.createRigidArea(new Dimension(OPTION_BUTTONS_SPTR_X, OPTION_BUTTONS_SPTR_Y)));
		optionButtons.add(blurButton);
		optionButtons.add(Box.createRigidArea(new Dimension(OPTION_BUTTONS_SPTR_X, OPTION_BUTTONS_SPTR_Y)));
		optionButtons.add(enhanceButton);
		optionButtons.add(Box.createRigidArea(new Dimension(OPTION_BUTTONS_SPTR_X, OPTION_BUTTONS_SPTR_Y)));
		
		// Now to deal with the initially hidden color chooser for Enhance.
		optionButtons.add(colorChooser);
		colorChooser.setVisible(false);
		
		JButton redButton = new JButton("R");
		redButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// If the button is clicked, we are making a modification to the image.
				modified = true;
				state.setText("MODIFIED");
				undoHistory.push(bmp);
				redoHistory = new Stack<Bitmap>();
				undoButton.setEnabled(true);
				redoButton.setEnabled(false);
				// Create a deep copy of the old Bitmap so we preserve the old one in the stacks.
				bmp = bmp.copy();
				bmp.enhanceColor(Bitmap.RED);
				// Reset the button text, enable all buttons we turned off, and refresh.
				enhanceButton.setText("Enhance Color");	
				colorChooserToggle();
				refreshImage();
			}
		});
		JButton greenButton = new JButton("G");
		greenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// If the button is clicked, we are making a modification to the image.
				modified = true;
				state.setText("MODIFIED");
				undoHistory.push(bmp);
				redoHistory = new Stack<Bitmap>();
				undoButton.setEnabled(true);
				redoButton.setEnabled(false);
				// Create a deep copy of the old Bitmap so we preserve the old one in the stacks.
				bmp = bmp.copy();
				bmp.enhanceColor(Bitmap.GREEN);
				// Reset the button text, enable all buttons we turned off, and refresh.
				enhanceButton.setText("Enhance Color");	
				colorChooserToggle();
				refreshImage();
			}
		});
		JButton blueButton = new JButton("B");
		blueButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// If the button is clicked, we are making a modification to the image.
				modified = true;
				state.setText("MODIFIED");
				undoHistory.push(bmp);
				redoHistory = new Stack<Bitmap>();
				undoButton.setEnabled(true);
				redoButton.setEnabled(false);
				// Create a deep copy of the old Bitmap so we preserve the old one in the stacks.
				bmp = bmp.copy();
				bmp.enhanceColor(Bitmap.BLUE);
				// Reset the button text, enable all buttons we turned off, and refresh.
				enhanceButton.setText("Enhance Color");	
				colorChooserToggle();
				refreshImage();
			}
		});
		// Add the buttons.
		colorChooser.add(redButton);
		colorChooser.add(greenButton);
		colorChooser.add(blueButton);
		
		// A few more to add...
		optionButtons.add(combineButton);
		optionButtons.add(Box.createVerticalGlue());
		
		// Housekeeping...
		toggleImageEnabled();
		addButtonListeners();
		undoButton.setEnabled(false);
		redoButton.setEnabled(false);
		
		//-----------------------------------------------------------------------------------------------------------------------------------------
		
		// Time for more listeners.
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Check to see if we need to save first.
				if (modified) {
					int choice = JOptionPane.showConfirmDialog(null, "There are unsaved changes to your image. Would you like to do so first?", "Save?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
					if (choice == JOptionPane.YES_OPTION)
						try {
							saveImage(false);
						} catch (IOException ev) {
							// If we can't write, warn user and cancel.
							JOptionPane.showMessageDialog(null, "Could not write the image to where it was. No changes made.", "ERROR", JOptionPane.ERROR_MESSAGE);
							return;
						}
					// If they ask to cancel, do NOT open new file.
					if (choice == JOptionPane.CANCEL_OPTION)
						return;
				}
				
				// As long they didn't say cancel, prompt the user for a new file.
				JFileChooser jf = new JFileChooser((mostRecentInputFile != null) ? mostRecentInputFile.getParent() : null);
				jf.setFileFilter(new FileNameExtensionFilter("Bitmap Files (.bmp)", "bmp"));
				if (jf.showOpenDialog(getMe()) != JFileChooser.APPROVE_OPTION)
					// If they cancel, abort.
					return;
				
				// Refresh the recent file variable.
				try {
					mostRecentInputFile = jf.getSelectedFile();
					bmp = new Bitmap(mostRecentInputFile);
					lastSavedBMP = bmp;
				} catch (IOException d) { /* We shouldn't ever go in here. */ }
				
				// Housekeeping...
				modified = false;
				state.setText("");
				undoHistory = new Stack<Bitmap>();
				redoHistory = new Stack<Bitmap>();
				centerBox.setVisible(false);
				toggleImageEnabled();
				refreshImage();
				pack();
				revalidate();
				repaint();
			}
		});
		
		// This button saves to the file the user opened the image with.
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					saveImage(false);
					lastSavedBMP = bmp;
				} catch (IOException ev) {
					// If we can't write, warn user and re-fire this process.
					JOptionPane.showMessageDialog(null, "Could not write the image to location specified. No changes made. Please choose \"Save As...\"", "ERROR", JOptionPane.ERROR_MESSAGE);
					actionPerformed(e);
				}
			}
		});
		
		// This allows the user to choose a new file to save to.
		saveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					saveImage(true);
					lastSavedBMP = bmp;
				} catch (IOException ev) {
					// If we can't write, warn user and re-fire this process.
					JOptionPane.showMessageDialog(null, "Could not write the image to location specified.", "ERROR", JOptionPane.ERROR_MESSAGE);
					actionPerformed(e);
				}
			}
		});
		
		// This allows the user to close the currently displayed image.
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Check to see if we need to save first.
				if (modified) {
					int choice = JOptionPane.showConfirmDialog(null, "There are unsaved changes to your image. Would you like to do so first?", "Save?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
					if (choice == JOptionPane.YES_OPTION)
						try {
							saveImage(false);
						} catch (IOException ev) {
							// If we can't write, warn user and cancel.
							JOptionPane.showMessageDialog(null, "Could not write the image to where it was. No changes made.", "ERROR", JOptionPane.ERROR_MESSAGE);
							return;
						}
					// If they ask to cancel, do NOT close the image.
					if (choice == JOptionPane.CANCEL_OPTION)
						return;
				}
				
				// As long they don't say cancel:
				modified = false;
				state.setText("");
				remove(imageDisplayed);
				undoButton.setEnabled(false);
				redoButton.setEnabled(false);
				toggleImageEnabled();
				revalidate();
				repaint();
			}
		});
		
		// Simply exits the whole program in the same way clicking "x" would.
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Fire a WindowEvent so the WindowListener can handle this.
				processEvent(new WindowEvent(getMe(), WindowEvent.WINDOW_CLOSING));
			}
		});
		
		// Add our containers.
		add(topInfo, BorderLayout.NORTH);
		add(centerBox, BorderLayout.CENTER);
		add(optionButtons, BorderLayout.EAST);
		add(bottomStuff, BorderLayout.SOUTH);
		add(Box.createRigidArea(new Dimension(OPTION_BUTTONS_SPTR_X / 2, OPTION_BUTTONS_SPTR_Y)), BorderLayout.WEST);
		
		pack();
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Another helper method for our <strong>globally</strong> accessible instance variable components.
	 */
	private void addButtonListeners() {
		// For the flip button:
		flipButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// We've modified the image.
				modified = true;
				state.setText("MODIFIED");
				undoHistory.push(bmp);
				redoHistory = new Stack<Bitmap>();
				undoButton.setEnabled(true);
				redoButton.setEnabled(false);
				// Create a deep copy of the old Bitmap so we preserve the old one in the stacks.
				bmp = bmp.copy();
				bmp.flip();
				refreshImage();
			}
		});
		
		//----------------------------------------------------------------------------------------------------
		
		// For the blur button:
		blurButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// We've modified the image.
				modified = true;
				state.setText("MODIFIED");
				undoHistory.push(bmp);
				redoHistory = new Stack<Bitmap>();
				undoButton.setEnabled(true);
				redoButton.setEnabled(false);
				// Create a deep copy of the old Bitmap so we preserve the old one in the stacks.
				bmp = bmp.copy();
				bmp.blur();
				refreshImage();
			}
		});
		
		//----------------------------------------------------------------------------------------------------
		
		// For the enhance button:
		enhanceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Toggle the color chooser container's availability and change the text correspondingly.
				colorChooserToggle();
				if (colorChooser.isVisible())
					enhanceButton.setText("CANCEL");
				else 
					enhanceButton.setText("Enhance Color");		
			}
		});
		
		//----------------------------------------------------------------------------------------------------
		
		// For the combine button:
		combineButton.addActionListener(new ActionListener() {
			
			// We need to define a listener which creates a button within the new window, and then assigns itself this listener again.
			final class AddListener implements ActionListener {
				public void actionPerformed(ActionEvent e) {
					// Prompt for a new image to add.
					JFileChooser jf = new JFileChooser((mostRecentInputFile != null) ? mostRecentInputFile.getParent() : null);
					jf.setFileFilter(new FileNameExtensionFilter("Bitmap Files (.bmp)", "bmp"));
					if (jf.showOpenDialog(getMe()) != JFileChooser.APPROVE_OPTION)
						// Cancel the button creation if they request it.
						return;
					
					// Create the new Bitmap object and add the corresponding BufferedImage to our queue of selected images.
					try {
						images.add((new Bitmap(jf.getSelectedFile())).getImage());
					} catch (IOException d) { /* Shouldn't ever enter here */ }
					
					// Now we remove the button which triggered this event...
					imageDisplay.remove(((Component) e.getSource()));
					// ...and add the scaled-down image to replace it.
					imageDisplay.add(new JLabel(new ImageIcon(images.get(images.size() - 1).getScaledInstance(-1, 150, Image.SCALE_FAST))));
					
					// Then, we can add a new button and assign it this listener class.
					JButton addMore = new JButton("Add image...");
					addMore.addActionListener(new AddListener());
					imageDisplay.add(addMore);
					imageChooser.pack();
					imageChooser.revalidate();
					imageChooser.repaint();
				}
			}
			
			// This is when we actually hit the Combine button the first time.
			public void actionPerformed(ActionEvent e) {
				
				// Disable the buttons for now.
				toggleImageEnabled();
				
				// Create the new JWindow (with a pretty border). 
				imageChooser = new JWindow(getMe());
				imageChooser.getRootPane().setBorder(new LineBorder(Color.RED, 4, true));
				imageChooser.setLayout(new BorderLayout());				
				imageDisplay = new Container();
				imageDisplay.setLayout(new GridLayout(0, 3));
				imageChooser.setVisible(true);
				Point p = imageDisplayed.getLocationOnScreen();
				p.translate((imageDisplayed.getWidth() / 2) - (ICON_WIDTH * 2), (imageDisplayed.getHeight() / 4));
				imageChooser.setLocation(p);
				
				// Add the original bitmap image to this first tile.
				imageDisplay.add(new JLabel(new ImageIcon(bmp.getImage().getScaledInstance(-1, ICON_WIDTH, Image.SCALE_FAST))));
				// And then add a button allowing them to select another image.
				JButton addMore = new JButton("Add image...");
				addMore.addActionListener(new AddListener());
				imageDisplay.add(addMore);
				
				// Create the list of BufferedImages to pass to the Bitmap function later.
				images = new ArrayList<BufferedImage>();
				
				// Create the confirm buttons.
				Container confirmButtons = new Container();
				confirmButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
				JButton confirm = new JButton("Confirm");
				
				// When the user is done choosing...
				confirm.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e1) {
						
						// Save the current Bitmap object, make a copy of it.
						Bitmap oldBMP = bmp;
						bmp = bmp.copy();
						
						// Now we create an array out of the ArrayList.
						BufferedImage[] list = new BufferedImage[images.size()];
						try {
							// Pass the array in.
							bmp.combine(images.toArray(list));
						} catch (Exception e2) {
							// If we enter here, the sizes didn't match.
							JOptionPane.showMessageDialog(null, "One or more of the images selected did not match the dimensions of the initial image.", "ERROR", JOptionPane.ERROR_MESSAGE);
							toggleImageEnabled();
							imageChooser.dispose();
							return;
						}
						
						// We modified the image. 
						modified = true;
						state.setText("MODIFIED");
						undoButton.setEnabled(true);
						redoButton.setEnabled(false);
						undoHistory.push(oldBMP);
						redoHistory = new Stack<Bitmap>();
						
						// Close the window.
						imageChooser.dispose();
						
						// Housekeeping...
						toggleImageEnabled();
						refreshImage();
					}
				});
				
				// If the user changes their mind.
				JButton cancel = new JButton("Cancel");
				cancel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e1) {
						toggleImageEnabled();
						imageChooser.dispose();
					}
				});
				
				// Add these two buttons to their container (and containers to the window).
				confirmButtons.add(confirm);
				confirmButtons.add(cancel);
				imageChooser.add(imageDisplay);
				imageChooser.add(confirmButtons, BorderLayout.SOUTH);
				// We also will post a disclaimer.
				JLabel disclaimer = new JLabel("Make sure the dimensions are the same!");
				Container temp = new Container();
				temp.setLayout(new FlowLayout(FlowLayout.CENTER));
				temp.add(disclaimer);
				imageChooser.add(temp, BorderLayout.NORTH);
				
				// Housekeeping...
				imageChooser.pack();
				imageChooser.revalidate();
				imageChooser.repaint();
			}
		});
		
		//----------------------------------------------------------------------------------------------------
		
		// For the undo button:
		undoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Save the current Bitmap as a possible redo, and grab the last undo we stored.
				redoHistory.push(bmp);
				redoButton.setEnabled(true);
				bmp = undoHistory.pop();
				refreshImage();
				
				// If this new Bitmap is equal to the last saved one, it technically isn't "modified".
				if (bmp.equals(lastSavedBMP)) {
					state.setText("");
					modified = false;
				}
				else {
					state.setText("MODIFIED");
					modified = true;
				}
				
				// If there are no more undos, prevent the user from doing so.
				if (undoHistory.isEmpty()) 
					undoButton.setEnabled(false);
			}
		});
		
		//----------------------------------------------------------------------------------------------------
		
		// For the redo button:
		redoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Save the current Bitmap as a possible undo, and grab the last redo we stored.
				undoHistory.push(bmp);
				undoButton.setEnabled(true);
				bmp = redoHistory.pop();
				refreshImage();
				
				// If this new Bitmap is equal to the last saved one, it technically isn't "modified".
				if (bmp.equals(lastSavedBMP)) {
					state.setText("");
					modified = false;
				}
				else {
					state.setText("MODIFIED");
					modified = true;
				}
				
				// If there are no more redos, prevent the user from doing so.
				if (redoHistory.isEmpty()) 
					redoButton.setEnabled(false);
			}
		});
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This is a helper function for prompting the user to save the current image.
	 * @param differentFile a flag indicating whether (<code>true</code>) or not (<code>false</code>) the user wishes to store it in a different location.
	 * @throws IOException thrown if we could not write to the designated location.
	 */
	private void saveImage (boolean differentFile) throws IOException {
		
		// If the user wishes to save elsewhere (through "Save As..."), prompt them for the location.
		if (differentFile) {
			JFileChooser jf = new JFileChooser(mostRecentInputFile.getParent());
			jf.setFileFilter(new FileNameExtensionFilter("Bitmap Files (.bmp)", "bmp"));
			if (jf.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
				// Cancel the operation if they wish to.
				return;
			// Write to the location, appending the bitmap extension.
			bmp.writeBitmap((mostRecentInputFile = new File(jf.getSelectedFile().getAbsolutePath() + ".bmp")));
		}
		// Otherwise, just save to wherever they opened it from.
		else
			bmp.writeBitmap(mostRecentInputFile);
		
		// Now that the image is saved, we tell the user it is no longer unsaved.
		modified = false;
		state.setText("");
		pack();
		revalidate();
		repaint();
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * A helper function which toggles the availability of the color chooser container for Enhance Color.
	 */
	private void colorChooserToggle() {
		colorChooser.setVisible(!colorChooser.isVisible());
		flipButton.setEnabled(!flipButton.isEnabled());
		blurButton.setEnabled(!blurButton.isEnabled());
		combineButton.setEnabled(!combineButton.isEnabled());
		pack();
		revalidate();
		repaint();
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * A helper method which repositions the screen with regards to how large the user's screen is.
	 * In a later version, the initial position would reflect the visible elements on screen.
	 */
	private void repositionScreen() {
		double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		
		this.setLocation((int) screenWidth / 4, (int) screenHeight / 4);
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * A helper method which refreshes the bitmap image displayed on the main screen.
	 */
	private void refreshImage() {
		remove(imageDisplayed);
		imageDisplayed = new JLabel(new ImageIcon(bmp.getImage()));
		add(imageDisplayed);
		pack();
		revalidate();
		repaint();
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * A helper method which toggles the availability of some image buttons.
	 */
	private void toggleImageEnabled() {
		flipButton.setEnabled(!flipButton.isEnabled());
		blurButton.setEnabled(!blurButton.isEnabled());
		enhanceButton.setEnabled(!enhanceButton.isEnabled());
		combineButton.setEnabled(!combineButton.isEnabled());
		close.setEnabled(!close.isEnabled());
		save.setEnabled(!save.isEnabled());
		saveAs.setEnabled(!saveAs.isEnabled());
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * An accessor method for this instance of BitmapGUI. For when we are not in its immediate scope.
	 * @return this BitmapGUI instance.
	 */
	private JFrame getMe() {
		return this;
	}
	
	//-----------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * The main method.
	 */
	public static void main(String[] args) {
		new BitmapGUI();
	}
}
