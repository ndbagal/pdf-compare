package com.ltimindtree.pdfcompare.service.impl;

import com.ltimindtree.pdfcompare.service.FileService;
import com.ltimindtree.pdfcompare.service.PdfUtilityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

@Slf4j
@Service
public class PdfUtilityServiceImpl implements PdfUtilityService {

    private final FileService fileService;

    public PdfUtilityServiceImpl(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public Map<Integer, BufferedImage> convertPDFToImage(File file) throws IOException, InterruptedException {
        PDDocument document = Loader.loadPDF(file);
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        Map<Integer, BufferedImage> bufferedImagesMap = new HashMap<>();

        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            // Iterate over each page and convert it to an image
            bufferedImagesMap.put(page, pdfRenderer.renderImageWithDPI(page, 300));
        }
        document.close();
        return bufferedImagesMap;
    }

    @Override
    public Map<Integer, List<BufferedImage>> comparePDFs(
            Map<Integer, BufferedImage> originalBufferedImagesMap,
            Map<Integer, BufferedImage> modifiedBufferedImagesMap
    ) {

        Map<Integer, List<BufferedImage>> diffImagesMap = new HashMap<>();

        if (originalBufferedImagesMap.size() == modifiedBufferedImagesMap.size()) {
            originalBufferedImagesMap.forEach((key, originalImage) -> {
                BufferedImage modifiedImage = modifiedBufferedImagesMap.get(key);

                // Get image dimensions
                int width = originalImage.getWidth();
                int height = originalImage.getHeight();

                // Create difference image
                BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                Graphics2D g2d = diffImage.createGraphics();
                g2d.drawImage(modifiedImage, 0, 0, null);

                // Find differing areas and draw rectangles around them
                List<Rectangle> differingAreas = findDifferingAreas(originalImage, modifiedImage);
                g2d.setColor(Color.RED);
                for (Rectangle rect : differingAreas) {
                    g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                }
                g2d.dispose();

                if (!differingAreas.isEmpty()) {
                    diffImagesMap.put(key, Arrays.asList(originalImage, diffImage));
                }
            });
        }
        return diffImagesMap;
    }

    private static List<Rectangle> findDifferingAreas(BufferedImage originalImage, BufferedImage modifiedImage) {
        List<Rectangle> differingAreas = new ArrayList<>();
        int width = Math.min(originalImage.getWidth(), modifiedImage.getWidth());
        int height = Math.min(originalImage.getHeight(), modifiedImage.getHeight());

        boolean[][] differingPixels = new boolean[width][height];

        // Compare pixel values
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalPixel = originalImage.getRGB(x, y);
                int modifiedPixel = modifiedImage.getRGB(x, y);

                differingPixels[x][y] = originalPixel != modifiedPixel;
            }
        }

        // Find differing areas using a simple connected-components algorithm
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (differingPixels[x][y]) {
                    // Start of a differing area, expand it
                    int endX = x;
                    while (endX < width && differingPixels[endX][y]) {
                        endX++;
                    }

                    int endY = y;
                    outer:
                    while (endY < height) {
                        for (int i = x; i < endX; i++) {
                            if (!differingPixels[i][endY]) {
                                break outer;
                            }
                        }
                        endY++;
                    }

                    differingAreas.add(new Rectangle(x, y, endX - x, endY - y));

                    // Mark this area as visited to avoid processing it again
                    for (int i = x; i < endX; i++) {
                        for (int j = y; j < endY; j++) {
                            differingPixels[i][j] = false;
                        }
                    }
                }
            }
        }

        return differingAreas;
    }

    @Override
    public String createDiffPdf(Map<Integer, BufferedImage> originalBufferedImagesMap, Map<Integer, List<BufferedImage>> diffImagesMap, String filename) {
        try (PDDocument document = new PDDocument()) {
            originalBufferedImagesMap.forEach((key, image) -> {
                List<BufferedImage> diffImages = diffImagesMap.get(key);
                if (Objects.isNull(diffImages)) {
                    /*// Calculate the width and height of the page
                    float pageWidth = image.getWidth();
                    float pageHeight = image.getHeight();

                    // Create a new page
                    PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                    document.addPage(page);

                    // Add the image to the page
                    PDPageContentStream contentStream;
                    try {
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.drawImage(LosslessFactory.createFromImage(document, image), 0, 0);
                        contentStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }*/
                } else {
                    // Calculate the width and height of the page
                    BufferedImage image1 = diffImages.get(0);
                    BufferedImage image2 = diffImages.get(1);

                    float pageWidth = Math.addExact(image1.getWidth(), image2.getWidth());
                    float pageHeight = Math.max(image1.getHeight(), image2.getHeight());

                    // Create a new page
                    PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                    document.addPage(page);

                    // Create a content stream for the page
                    PDPageContentStream contentStream;
                    try {
                        contentStream = new PDPageContentStream(document, page);
                        // Draw the first image on the left side
                        contentStream.drawImage(LosslessFactory.createFromImage(document, image1), 0, 0, image1.getWidth(), image1.getHeight());

                        // Draw the second image next to the first one
                        contentStream.drawImage(LosslessFactory.createFromImage(document, image2), image1.getWidth(), 0, image2.getWidth(), image2.getHeight());

                        // Close the content stream
                        contentStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return fileService.saveFile(document, "diff", filename);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
