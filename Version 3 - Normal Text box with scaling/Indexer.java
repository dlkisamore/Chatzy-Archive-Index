/**

*/

package kumiho.chatzy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.Enumeration;
import java.util.zip.ZipInputStream;
import java.io.InputStream;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Cursor;
import javax.swing.JCheckBox;

public class Indexer extends JFrame {
	//GUI elements
	private JTextField searchBox = new JTextField();
	private JTextArea displayArea = new JTextArea();
	private JScrollPane scroller = new JScrollPane(displayArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JButton searchButton = new JButton("SEARCH");
	private JButton backButton = new JButton("BACK");
	private JButton nextButton = new JButton("NEXT");
	private final int MIN_WINDOW_WIDTH = 0;
	private final int MAX_WINDOW_HEIGHT = 0;
	private int windowWidth = 500;
	private int windowHeight = 500;
	private Font font = new Font("monospaced", Font.PLAIN, 12);
	private JFileChooser fc = new JFileChooser();
	private JTextField currentPage = new JTextField();
	private JLabel resultSummary = new JLabel();
	private JCheckBox toggleCaseBox = new JCheckBox("MATCH CASE");
	
	//non-GUI elements
	private ArrayList<String> search = new ArrayList<String>();
	private ArrayList<String> omit = new ArrayList<String>();
	private ArrayList<String> results = new ArrayList<String>();
	private final int EPP = 10; //entries per page
	private int currentIndex = 0; //tracks the current location of the displayed text
	private File folder;
	private boolean threadLock = false;
	
	public Indexer() {
		initComponents();
	}
	
	public void initComponents() {
		//get source folder from text file
		this.folder = getSourceFolder();
		//window properties
		setTitle("Zipped Text File Indexer");
		setResizable(true);
		setLayout(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(windowWidth, windowHeight));
		addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				updateDimensions();
				updateComponents();
			}
			
			public void componentHidden(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
		});
		//add listener for the enter key to start a search
		this.searchBox.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				char keyPressed = e.getKeyChar();
				if(keyPressed == KeyEvent.VK_ENTER) {
					search();
				}
			}
		});
		this.searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
				search();
            }
        });
		this.backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
				backPage();
            }
        });
		this.nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
				nextPage();
            }
        });
		this.displayArea.setEditable(false);
		this.currentPage.setVisible(false);
		//add active listening and indexing to currentPage
		this.currentPage.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent evt) {
				changePage();
			}
			public void removeUpdate(DocumentEvent evt) {
				changePage();
			}
			public void insertUpdate(DocumentEvent evt) {
				changePage();
			}
		});
		//restrict entry to only numbers
		this.currentPage.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				char keyPressed = e.getKeyChar();
				if((keyPressed < '0' || keyPressed > '9') && keyPressed != KeyEvent.VK_BACK_SPACE && keyPressed != KeyEvent.VK_DELETE) {
					e.consume();
				}
			}
		});
		this.resultSummary.setVisible(false);
		updateComponents();
		//finish setting up GUI
		add(this.searchBox);
		add(this.searchButton);
		add(this.toggleCaseBox);
		add(this.scroller);
		add(this.backButton);
		add(this.currentPage);
		add(this.resultSummary);
		add(this.nextButton);
		pack();
	}
	
	public void changePage() {
		if(!this.threadLock) {
			this.threadLock = true;
			if(this.currentPage.getText().length() > 0) {
				int tempIndex = Integer.valueOf(this.currentPage.getText());
				if(tempIndex <= 0) {
					tempIndex = 1;
				}
				if(tempIndex > results.size()) {
					tempIndex = results.size();
				}
				currentIndex = tempIndex - 1;
				displayEntries();
				updateSummary();
			}
			this.threadLock = false;
		}
	}
	
	public void search() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		this.searchButton.setEnabled(false);
		this.backButton.setEnabled(false);
		this.nextButton.setEnabled(false);
		this.search.clear();
		this.omit.clear();
		this.results.clear();
		this.currentIndex = 0;
		this.displayArea.setText("");
		//get zip files from folder
		ArrayList<File> zipFiles = new ArrayList<File>();
		for(File f : folder.listFiles()) {
			zipFiles.add(f);
		}
		//get search parameters
		String searchParam = this.searchBox.getText();
		parseSearch(searchParam);
		//search all items in the source folder for the string and report the path to the result
		findSearchTerm(folder, zipFiles);
		//set up first EPP count entries in displayArea
		displayEntries();
		//edit footer labels and text field
		updateSummary();
		this.currentPage.setVisible(true);
		this.resultSummary.setVisible(true);
		this.nextButton.setEnabled(true);
		this.backButton.setEnabled(true);
		this.searchButton.setEnabled(true);
		setCursor(Cursor.getDefaultCursor());
	}
	
	public void displayEntries() {
		this.displayArea.setText("");
		for(int i = currentIndex; i < currentIndex + this.EPP; i++) {
			if(i == this.results.size()) {
				break;
			} else {
				this.displayArea.append(this.results.get(i));
			}
		}
		this.backButton.setEnabled(!(currentIndex == 0));
		this.nextButton.setEnabled(!(currentIndex + this.EPP >= this.results.size()));
		this.displayArea.setCaretPosition(0);
	}
	
	public void updateSummary() {
		if(!this.threadLock) {
			this.threadLock = true;
			this.currentPage.setText(String.valueOf(currentIndex + 1));
		}
		int temp = Integer.valueOf(currentIndex + EPP);
		if(temp >= results.size()) {
			temp = results.size();
		}
		this.resultSummary.setText("to " + temp + " of " + results.size());
		this.threadLock = false;
	}
	
	public void backPage() {
		currentIndex -= EPP;
		if(currentIndex < 0) {
			currentIndex = 0;
		}
		displayEntries();
		updateSummary();
	}
	
	public void nextPage() {
		currentIndex += EPP;
		displayEntries();
		updateSummary();
	}
	
	//ADD NEW COMPONENTS HERE
	public void updateComponents() {
		int xBuffer = (int) ((this.windowWidth) * 0.01d); //1% window width
		int yBuffer = (int) ((this.windowHeight) * 0.01d); //1% window height
		int xPos = xBuffer, yPos = yBuffer; //1% window width, 1% window height
		double yBorderBuffer = 15d/windowHeight;
		double xBorderBuffer = 15d/windowWidth;
		//set font size
		this.font = new Font("monospaced", Font.PLAIN, (int) (this.windowHeight * 0.025d)); //2.5% window height
		//search input box
		this.searchBox.setBounds(xPos,yPos, (int) (this.windowWidth * 0.8 - xBuffer * 3d - this.windowWidth * xBorderBuffer), (int) (this.windowHeight * 0.05d)); //76% window width, 5% window height
		this.searchBox.setFont(this.font);
		//search button
		this.searchButton.setBounds(xPos + this.searchBox.getWidth() + xBuffer, yPos, (int) (this.windowWidth * 0.2d), (int) (this.windowHeight * 0.05d));
		this.searchButton.setFont(this.font);
		yPos += this.searchButton.getHeight();
		//case sensitivity toggle
		this.toggleCaseBox.setBounds(xPos, yPos, (int) (this.windowWidth - this.windowWidth * xBorderBuffer - xBuffer * 2), (int) (this.windowHeight * 0.05d));
		this.toggleCaseBox.setFont(this.font);
		yPos += this.toggleCaseBox.getHeight();
		//output box
		this.scroller.setBounds(xPos, yPos, (int) (this.windowWidth - this.windowWidth * xBorderBuffer - xBuffer * 2), (int) (this.windowHeight * 0.81d - 40));
		//this.displayArea.setFont(this.font);
		this.displayArea.setLineWrap(true);
		this.displayArea.setWrapStyleWord(true);
		yPos += this.scroller.getHeight() + yBuffer;
		//save button
		this.backButton.setBounds(xPos, yPos, (int) (this.windowWidth * 0.2d), (int) (this.windowHeight * 0.05d));
		this.backButton.setFont(this.font);
		//currentPage text field
		this.currentPage.setBounds(xPos + xBuffer + this.backButton.getWidth(), yPos, (int) (this.windowWidth * 0.15d), (int) (this.windowHeight * 0.05d));
		this.currentPage.setFont(this.font);
		//resultSummary label
		this.resultSummary.setBounds(xPos + xBuffer * 2 + this.backButton.getWidth() + this.currentPage.getWidth(), yPos, (int) (this.windowWidth * 0.4d), (int) (this.windowHeight * 0.05d));
		this.resultSummary.setFont(this.font);
		//help button
		this.nextButton.setBounds(xPos + this.searchBox.getWidth() + xBuffer, yPos, (int) (this.windowWidth * 0.2d), (int) (this.windowHeight * 0.05d));
		this.nextButton.setFont(this.font);
	}
	
	//updates the window dimension fields
	public void updateDimensions() {
		this.windowWidth = getWidth();
		this.windowHeight = getHeight();
	}
	
	public static void main(String args[]) {
		Indexer gui = new Indexer();
		gui.setVisible(true);
	}
	
	public void findSearchTerm(File folder, ArrayList<File> zipFiles) {
	//public ArrayList<String> findSearchTerm(File folder, ArrayList<File> zipFiles) {
		//ArrayList<String> results = new ArrayList<String>();
		//for each zip file archive...
		for(int i = 0; i < zipFiles.size(); i++) {
			try {
				ZipInputStream zipInput = null;
				ZipFile zipFile = new ZipFile(zipFiles.get(i)); //get the file and set it to be read as a zip file
				Enumeration<? extends ZipEntry> entries = zipFile.entries(); //create a way of iterating through the files inside the zip file
				while(entries.hasMoreElements()) {
					ZipEntry zipEntry = entries.nextElement(); //get the next file in the zip file
					if(!zipEntry.isDirectory()) {
						String fileName = zipEntry.getName();  //get the name of the file in the zip file
						if(fileName.endsWith(".txt")) { //if the file is a text file...
							InputStream stream = zipFile.getInputStream(zipEntry);
							Scanner scanner = new Scanner(stream);
							int lineNumber = 0;
							while(scanner.hasNextLine()) {
								lineNumber++;
								//get the String representation of the line
								String line = scanner.nextLine();
								//process search
								boolean allFound = true;
								boolean excludedNotFound = true;
								//check for all search terms
								for(String s : search) {
									//handle case-sensitivity
									if(this.toggleCaseBox.isSelected()) {
										if(!line.contains(s)) {
											allFound = false;
											break;
										}
									} else {
										if(!line.toLowerCase().contains(s.toLowerCase())) {
											allFound = false;
											break;
										}
									}
								}
								//if all search terms are found, check against all omitted terms
								if(allFound) {
									for(String s : omit) {
										//handle case-sensitivity
										if(this.toggleCaseBox.isSelected()) {
											if(line.contains(s)) {
												excludedNotFound = false;
												break;
											}
										} else {
											if(line.toLowerCase().contains(s.toLowerCase())) {
												excludedNotFound = false;
												break;
											}
										}
									}
								}
								if(allFound && excludedNotFound) {
									results.add("----------------------\nZip File: " + zipFiles.get(i).getName() + "\nFile: " + fileName + "\nLine: " + lineNumber + "\n" + line + "\n");
								}
							}
						}
					}
				}
			} catch(ZipException e) {
				System.out.println("ERROR IN READING ZIP FILE");
				System.exit(0);
			} catch(IOException e) {
				System.out.println("IO ERROR");
				e.printStackTrace();
				System.exit(0);
			}
		}
		//return results;
	}
	
	//ADD NEW FEATURES HERE
	public void parseSearch(String searchParam) {
		boolean encapsulate = false;
		while(searchParam.length() != 0) {
			boolean add = true;
			boolean endOfSearch = false;
			//allow for omissions using -
			if(searchParam.charAt(0) == '-') {
				add = false;
				if(searchParam.length() == 1) {
					break;
				} else {
					searchParam = searchParam.substring(1);
				}
			}
			for(int i = 0; i < searchParam.length(); i++) {
				boolean exit = false;
				switch(searchParam.charAt(i)) {
					case '\\':
						//trim out the \, causing the next char to skip getting checked
						if(i == 0) {
							searchParam = searchParam.substring(1);
						} else {
							searchParam = searchParam.substring(0, i) + searchParam.substring(i+1);
						}
					break;
					case '"':
						//toggle encapsulation to switch off exiting after a space is found
						encapsulate = !encapsulate;
						exit = true;
					break;
					case ' ':
						//process entry unless it's within ""
						if(!encapsulate) {
							exit = true;
						}
					break;
				}
				if(i == searchParam.length() - 1 && exit == false) {
					if(add) {
						search.add(searchParam);
					} else {
						omit.add(searchParam);
					}
					searchParam = "";
					break;
				}
				if(exit) {
					if(add) {
						search.add(searchParam.substring(0,i));
					} else {
						omit.add(searchParam.substring(0,i));
					}
					searchParam = searchParam.substring(i + 1);
					break;
				}
			}
		}
		//eliminate empty search and omit entries ""
		for(int i = 0; i < search.size(); i++) {
			if(search.get(i).length() == 0) {
				search.remove(i--);
			}
		}
		for(int i = 0; i < omit.size(); i++) {
			if(omit.get(i).length() == 0) {
				omit.remove(i--);
			}
		}
		/*
		//TEST CODE
		for(String s : search) {
			System.out.println(s);
		}
		System.out.println("----------------------");
		System.out.println("----------------------");
		System.out.println("----------------------");
		System.out.println("----------------------");
		System.out.println("----------------------");
		for(String s : omit) {
			System.out.println(s);
		}
		System.out.println("----------------------");
		System.out.println("----------------------");
		System.out.println("----------------------");
		System.out.println("----------------------");
		*/
	}
	
	public File getSourceFolder() {
		fc.setDialogTitle("Select archive folder location.");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		while(true) {
			if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				break;
			} else {
				continue;
			}
		}
		return fc.getSelectedFile();
	}
}