package com.ltimindtree.pdfcompare;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class TextFileComparison {

	//public void CompareTextFiles(String[] urls) {
		public void CompareTextFiles() {
		BufferedWriter writer;
		
		
		
		String home = System.getProperty("user.home");
		//for (String url : urls) {
		//	int lenghtofurl = url.length();
		//	String filename = String.format("PDF%03d", lenghtofurl);
	
			String filename = "diff_";
			String filePath1 = home + "/Downloads/" + "Originaltext.txt";
			String filePath2 = home + "/Downloads/" + "Modifiedtext.txt";

			try {
				Set<String> words1 = getWords(filePath1);
				Set<String> words2 = getWords(filePath2);

				// Find words unique to each file
				Set<String> uniqueToFirstFile = new HashSet<>(words1);
				uniqueToFirstFile.removeAll(words2);

				Set<String> uniqueToSecondFile = new HashSet<>(words2);
				uniqueToSecondFile.removeAll(words1);
				String filePath = home + "/Downloads/" + filename + "uniqueText.txt";
				writer = new BufferedWriter(new FileWriter(filePath));
								
				// Display the results
				System.out.println("Words unique to " + filePath1 + ":");
				displayWords(uniqueToFirstFile);
				writer.write("Words unique to " + filePath1 + ":" +uniqueToFirstFile + "\n");
			//	writer.close();
				
				System.out.println("\nWords unique to " + filePath2 + ":");
				displayWords(uniqueToSecondFile);
				writer.write("Words unique to " + filePath2 + ":" +uniqueToSecondFile + "\n");
				writer.close();
			} catch (IOException e) {
				log.error("Exception occurred.", e);
			}
		}
//	}

	private static Set<String> getWords(String filePath) throws IOException {
		Set<String> words = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Split the line into words using space as a delimiter
				String[] lineWords = line.split("\\s+");
				for (String word : lineWords) {
					// Add the lowercase version of the word to the set
					words.add(word.toLowerCase());
				}
			}
		}
		return words;
	}

	private static void displayWords(Set<String> words) {
		for (String word : words) {
			System.out.println(word);
		}
	}

}
