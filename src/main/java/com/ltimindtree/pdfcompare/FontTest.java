package com.ltimindtree.pdfcompare;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class FontTest {

	public static void main(String[] args) throws IOException {
		PDDocument pdfDocument = Loader.loadPDF(new File("C:\\Users\\722505\\Downloads\\ConvertedImageFolder\\font.pdf"));

		PDFTextStripper pdfStripper = new PDFTextStripper() {
		    @Override
		    protected void processTextPosition(TextPosition text) {
		       // System.out.println("Text `" + text.getUnicode() + "` with font `" + text.getFont().getName() + "`");
		    }
		};

		// force parse
		pdfStripper.getText(pdfDocument);
		FontTest fonttest= new FontTest();
		fonttest.ComparePDFFiles();
	}
	
	public void ComparePDFFiles() {
	
	// Paths to the PDF files
    String pdfFilePath1 = "C:\\Users\\722505\\Downloads\\ConvertedImageFolder\\font.pdf";
    String pdfFilePath2 = "C:\\Users\\722505\\Downloads\\ConvertedImageFolder\\font1.pdf";
    try {
        // Load the first PDF document
        PDDocument pdfDocument1 = Loader.loadPDF(new File(pdfFilePath1));
        // Extract text with font information from the first PDF
        Map<String, String> fontMap1 = extractTextWithFont(pdfDocument1);

        // Load the second PDF document
        PDDocument pdfDocument2 = Loader.loadPDF(new File(pdfFilePath2));
        // Extract text with font information from the second PDF
        Map<String, String> fontMap2 = extractTextWithFont(pdfDocument2);

        // Compare fonts between the two documents
        compareFonts(fontMap1, fontMap2);

        // Close the PDF documents
        pdfDocument1.close();
        pdfDocument2.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
	// Method to extract text with font information from a PDF document
    private static Map<String, String> extractTextWithFont(PDDocument pdfDocument) throws IOException {
        Map<String, String> fontMap = new HashMap<>();
        PDFTextStripper pdfStripper = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
                fontMap.put(text.getUnicode(), text.getFont().getName());
            }
        };

        // Force parse to extract text
        pdfStripper.getText(pdfDocument);
        return fontMap;
    }

    // Method to compare fonts between two documents
    private static void compareFonts(Map<String, String> fontMap1, Map<String, String> fontMap2) {
        for (Map.Entry<String, String> entry : fontMap1.entrySet()) {
            String text = entry.getKey();
            String font1 = entry.getValue();
            String font2 = fontMap2.get(text);

            if (font2 != null && !font1.equals(font2)) {
                System.out.println("Text: " + text);
                System.out.println("Font in File 1: " + font1);
                System.out.println("Font in File 2: " + font2);
                System.out.println("--------------");
            }
        }
    }
	
	
	
}


