package com.ltimindtree.pdfcompare;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class ImageComparator {
    static String USER_HOME_DIR = System.getProperty("user.home");

    public static void main(String[] args) throws IOException {
        PDFToImageConverter pdftoImageConverter = new PDFToImageConverter();
        pdftoImageConverter.convertPdfToImage();

        long count;
        try (Stream<Path> files = Files.list(Paths.get(USER_HOME_DIR + "/Downloads/ConvertToImageANDProcessingFolder/Original/"))) {
            count = files.count();
            log.info("Number of files {}", count);
        }

        for (int i = 1; i < count; i++) {
            String imagePath1 = USER_HOME_DIR + "/Downloads/ConvertToImageANDProcessingFolder/Original/page_" + i + ".png";
            String imagePath2 = USER_HOME_DIR + "//Downloads/ConvertToImageANDProcessingFolder/Modified/page_" + i + ".png";
            String diffImagePath = USER_HOME_DIR + "/Downloads/ConvertToImageANDProcessingFolder/difference" + i + ".png";

            try {
                // Load images
                BufferedImage image1 = ImageIO.read(new File(imagePath1));
                BufferedImage image2 = ImageIO.read(new File(imagePath2));

                // Get image dimensions
                int width = image1.getWidth();
                int height = image1.getHeight();

                // Create difference image
                BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                // Compare pixel values
                boolean imagesEqual = true;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel1 = image1.getRGB(x, y);
                        int pixel2 = image2.getRGB(x, y);

                        if (pixel1 != pixel2) {
                            diffImage.setRGB(x, y, Color.RED.getRGB());
                            imagesEqual = false;

                        } else {
                            diffImage.setRGB(x, y, pixel1);
                        }
                    }
                }

                if (imagesEqual) {
                    log.info("Images are identical.{}", i);
                } else {
                    // Save difference image
                    File outputImage = new File(diffImagePath);
                    ImageIO.write(diffImage, "png", outputImage);
                    log.info("Images are different. Difference image saved as {}", diffImagePath);
                }
            } catch (Exception e) {
                log.error("Exception occurred : ", e);
            }
        }

        List<String> arr = new ArrayList<>();
        arr.add("Original");
        arr.add("Modified");

        for (String s : arr) {
            String pdfFileNameToRead = USER_HOME_DIR + "/Downloads/ConvertedImageFolder/" + s + ".pdf";
            ReadPFDF r = new ReadPFDF();
            r.ReadPDFfile(pdfFileNameToRead, s);
        }

        TextFileComparision t = new TextFileComparision();
        t.CompareTextFiles();
    }
}
