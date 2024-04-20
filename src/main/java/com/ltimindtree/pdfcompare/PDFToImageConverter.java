package com.ltimindtree.pdfcompare;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PDFToImageConverter {
    static String USER_HOME_DIR = System.getProperty("user.home");

    public void convertPdfToImage() {
        // Create folder in downloads folder of system
        String processingFolderPath = USER_HOME_DIR + "/Downloads/ConvertToImageANDProcessingFolder";
        ensureFolderExists(processingFolderPath);

        List<String> arr = new ArrayList<>();
        arr.add("Original");
        arr.add("Modified");

        for (String s : arr) {
            log.info("Converting the PDF file to Images -->  {}", s);
            String sourcePdfFilePath = USER_HOME_DIR + "/Downloads/ConvertedImageFolder/" + s + ".pdf";
            new File(USER_HOME_DIR + "/Downloads/ConvertToImageANDProcessingFolder/" + s).mkdirs();
            String outputFolderPathForImage = USER_HOME_DIR + "/Downloads/ConvertToImageANDProcessingFolder/" + s + "/";

            try {
                PDDocument document = Loader.loadPDF(new File(sourcePdfFilePath));
                PDFRenderer pdfRenderer = new PDFRenderer(document);

                // Iterate over each page and convert it to an image
                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);

                    // Output the image to a file
                    File outputImageFile = new File(outputFolderPathForImage + "page_" + (page + 1) + ".png");
                    ImageIO.write(image, "png", outputImageFile);
                }

                document.close();
                log.info("Conversion completed successfully.");
            } catch (IOException e) {
                log.error("Exception while converting the PDF file", e);
            }
        }
    }

    private void ensureFolderExists(String processingFolderPath) {
        File processingFolder = new File(processingFolderPath);

        if (!processingFolder.exists()) {
            boolean isDirCreated = processingFolder.mkdir();
            if (!isDirCreated) {
                log.error("Could not create folder {}", processingFolderPath);
            }
        } else {
            log.info("Processing Folder already exists - {}", processingFolderPath);
        }
    }
}
