package com.ltimindtree.pdfcompare.service.impl;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PdfUtilityServiceImpl implements PdfUtilityService {

    private final FileService fileService;

    public PdfUtilityServiceImpl(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public Map<Integer, BufferedImage> convertPDFToImage(File file) throws IOException {
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
            Map<Integer, BufferedImage> expectedBufferedImagesMap,
            Map<Integer, BufferedImage> actualBufferedImagesMap
    ) throws InterruptedException {

        Map<Integer, List<BufferedImage>> diffImagesMap = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(5);

        if (expectedBufferedImagesMap.size() == actualBufferedImagesMap.size()) {
            expectedBufferedImagesMap.forEach((key, expectedImage) -> {
                BufferedImage actualImage = actualBufferedImagesMap.get(key);

                executor.submit(() -> {
                    long startTime = System.nanoTime();
                    log.info("Comparing PDF {}", key);
                    this.compareImage(key, expectedImage, actualImage, diffImagesMap);
                    long endTime = System.nanoTime();
                    log.info("Completed compare PDF {}, took {} ms", key, (endTime - startTime)/1000000);
                });
            });
        }

        // Shutdown ExecutorService
        executor.shutdown();

        // Wait for all tasks to complete
        boolean awaitTermination = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        if (!awaitTermination) {
            throw new InterruptedException();
        }
        return diffImagesMap;
    }

    private void compareImage(Integer key, BufferedImage expectedImage, BufferedImage actualImage, Map<Integer, List<BufferedImage>> diffImagesMap) {
        //Create ImageComparison object with result destination and compare the images.
        ImageComparison imageComparison = new ImageComparison(expectedImage, actualImage);
        imageComparison.setRectangleLineWidth(1);
        imageComparison.setDifferenceRectangleColor(new Color(255, 0, 0, 51));
        imageComparison.setDifferenceRectangleFilling(true, 20.0);

        ImageComparisonResult imageComparisonResult = imageComparison.compareImages();

        if (!imageComparisonResult.getImageComparisonState().equals(ImageComparisonState.MATCH)) {
            diffImagesMap.put(key, Arrays.asList(expectedImage, imageComparisonResult.getResult()));
        }
    }

    @Override
    public String createDiffPdf(Map<Integer, BufferedImage> originalBufferedImagesMap, Map<Integer, List<BufferedImage>> diffImagesMap, String filename) {
        try (PDDocument document = new PDDocument()) {
            originalBufferedImagesMap.forEach((key, image) -> {
                List<BufferedImage> diffImages = diffImagesMap.get(key);
                if (Objects.isNull(diffImages)) {
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
