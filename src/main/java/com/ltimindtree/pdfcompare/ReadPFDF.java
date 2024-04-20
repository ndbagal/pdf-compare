package com.ltimindtree.pdfcompare;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class ReadPFDF {
	
 
	public void ReadPDFfile(String pdfFileNametoRead, String textFileName) throws IOException {
		
		File file = new File(pdfFileNametoRead);
		PDDocument document = Loader.loadPDF(file);

		// Instantiate PDFTextStripper class
		PDFTextStripper pdfStripper = new PDFTextStripper();

		// Retrieving text from PDF document
		String text = pdfStripper.getText(document);
		System.out.println(text);

		String home = System.getProperty("user.home");
		try {
			FileWriter myWriter = new FileWriter(home + "/Downloads/" + textFileName + "text.txt");
			myWriter.write(text);
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		document.close();
	}
}
