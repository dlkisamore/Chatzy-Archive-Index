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
import java.io.FileInputStream;
import java.util.Scanner;
import java.io.InputStream;

public class Indexer {
	static ArrayList<String> search = new ArrayList<String>();
	static ArrayList<String> omit = new ArrayList<String>();
	
	public static void main(String args[]) {
		//get source folder from text file
		File folder = new File(getSourceFolder());
		//get zip files from folder
		ArrayList<File> zipFiles = new ArrayList<File>();
		for(File f : folder.listFiles()) {
			zipFiles.add(f);
		}
		//get search parameters
		String searchParam = getSearchParameters();
		parseSearch(searchParam);
		//search all items in the source folder for the string and report the path to the result
		for(String s : findSearchTerm(folder, zipFiles)) {
			System.out.println(s);
		}
	}
	
	public static ArrayList<String> findSearchTerm(File folder, ArrayList<File> zipFiles) {
		ArrayList<String> results = new ArrayList<String>();
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
									if(!line.contains(s)) {
										allFound = false;
										break;
									}
								}
								//if all search terms are found, check against all omitted terms
								if(allFound) {
									for(String s : omit) {
										if(line.contains(s)) {
											excludedNotFound = false;
											break;
										}
									}
								}
								if(allFound && excludedNotFound) {
									System.out.println("----------------------");
									System.out.println("Zip File: " + zipFiles.get(i).getName());
									System.out.println("File: " + fileName);
									System.out.println("Line: " + lineNumber);
									System.out.println(line);
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
		return results;
	}
	
	public static void parseSearch(String searchParam) {
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
	
	public static String getSearchParameters() {
		String parameters = null;
		System.out.print("Search for: ");
		Scanner scanner = new Scanner(System.in);
		parameters = scanner.nextLine();
		return parameters;
	}
	
	public static String getSourceFolder() {
		String line = null;
		try(BufferedReader br = new BufferedReader(new FileReader("config.txt"))) {
			line = br.readLine();
		} catch(IOException e) {
			System.out.println("Unable to find config.txt");
			System.exit(1);
		}
		return line;
	}
}