package com.ltimindtree.pdfcompare.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PdfUtilityService {

    Map<Integer, BufferedImage> convertPDFToImage(File file) throws IOException, InterruptedException;

    Map<Integer, List<BufferedImage>> comparePDFs(Map<Integer, BufferedImage> originalBufferedImagesMap, Map<Integer, BufferedImage> modifiedBufferedImagesMap) throws IOException, InterruptedException;

    String createDiffPdf(Map<Integer, BufferedImage> originalBufferedImagesMap, Map<Integer, List<BufferedImage>> diffImagesMap, String filename);
}
