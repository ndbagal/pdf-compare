package com.ltimindtree.pdfcompare;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ComparePDFText {

    public List<BufferedImage> compareFontOrText(String originalFilePath, String modifiedFilePath, boolean isFont) throws IOException {

        List<BufferedImage> images = new ArrayList<>();
        boolean isDiff = false;
        try (PDDocument doc1 = Loader.loadPDF(new File(originalFilePath));
             PDDocument doc2 = Loader.loadPDF(new File(modifiedFilePath))) {

            PDFRenderer renderer1 = new PDFRenderer(doc1);
            PDFRenderer renderer2 = new PDFRenderer(doc2);

            for (int page = 0; page < Math.min(doc1.getNumberOfPages(), doc2.getNumberOfPages()); page++) {
                BufferedImage image1 = renderer1.renderImage(page, 4);
                BufferedImage image2 = renderer2.renderImage(page, 4);

                Graphics2D g2d = image1.createGraphics();
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(Color.GREEN);
                g2d.scale(4, 4);

                Graphics2D g3d = image2.createGraphics();
                g3d.setStroke(new BasicStroke(2));
                g3d.setColor(Color.RED);
                g3d.scale(4, 4);

                List<TextPosition> textPositions1 = getTextPositions(doc1, page + 1);
                List<TextPosition> textPositions2 = getTextPositions(doc2, page + 1);

                for (int i = 0; i < textPositions1.size(); i++) {
                    TextPosition text1 = textPositions1.get(i);
                    TextPosition text2 = textPositions2.get(i);

                    Rectangle2D.Float rect = new Rectangle2D.Float(
                            text1.getXDirAdj(), text1.getYDirAdj(),
                            text1.getWidthDirAdj(), text1.getHeightDir());
                    if (!isEqual(text1, text2, isFont)) {
                        g2d.draw(rect);
                        g3d.draw(rect);
                        isDiff = true;
                    }
                }

                g2d.dispose();

                /*// Save the image with differences highlighted
                String imageFilename = "original_page_" + (page + 1) + "_diff.png";
                File outputfile = new File(imageFilename);
                javax.imageio.ImageIO.write(image1, "png", outputfile);*/

                /*imageFilename = "modified_page_" + (page + 1) + "_diff.png";
                outputfile = new File(imageFilename);
                javax.imageio.ImageIO.write(image2, "png", outputfile);
                System.out.println("Page " + (page + 1) + " comparison saved as: " + imageFilename);*/
                images.add(image2);
            }
        }
        return isDiff ? images : new ArrayList<>();
    }

    private static boolean isEqual(TextPosition pos1, TextPosition pos2, boolean isFont) throws IOException {
        boolean isEqual = pos1.getUnicode().equals(pos2.getUnicode());
        if (isFont) {
            String pos1FName = ((PDCIDFontType2) ((PDType0Font) pos1.getFont()).getDescendantFont()).getTrueTypeFont().getName();
            String pos2FName = ((PDCIDFontType2) ((PDType0Font) pos2.getFont()).getDescendantFont()).getTrueTypeFont().getName();
            isEqual = pos1FName.equals(pos2FName);
        }
        return isEqual;
    }

    private static List<TextPosition> getTextPositions(PDDocument document, int pageNum) throws IOException {
        List<TextPosition> textPositions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper() {

            @Override
            protected void writeString(String string, List<TextPosition> text) throws IOException {
                textPositions.addAll(text);
            }
        };
        stripper.setStartPage(pageNum);
        stripper.setEndPage(pageNum);
        stripper.getText(document);
        return textPositions;
    }

    private static boolean textExists(TextPosition text, List<TextPosition> textPositions) {
        for (TextPosition pos : textPositions) {
            if (pos.getUnicode().equals(text.getUnicode()) &&
                    pos.getXDirAdj() == text.getXDirAdj() &&
                    pos.getYDirAdj() == text.getYDirAdj()) {
                return true;
            }
        }
        return false;
    }
}
