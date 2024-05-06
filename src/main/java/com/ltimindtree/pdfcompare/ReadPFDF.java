package com.ltimindtree.pdfcompare;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
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
			log.info("Successfully wrote to the file.");
		} catch (IOException e) {
			log.error("An error occurred.", e);
		}
		document.close();
	}
}
