package com.ltimindtree.pdfcompare;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class ComparePDFText {

    public static final int SCALE = 4;
    private AffineTransform flipAT;
    private AffineTransform rotateAT;
    private Graphics2D g2d;

    public Map<Integer, List<BufferedImage>> compareFontOrText(String originalFilePath, String modifiedFilePath, boolean isFont) throws IOException {

        Map<Integer, List<BufferedImage>> diffImagesMap = new HashMap<>();
        try (PDDocument doc1 = Loader.loadPDF(new File(originalFilePath));
             PDDocument doc2 = Loader.loadPDF(new File(modifiedFilePath))) {

            PDFRenderer renderer1 = new PDFRenderer(doc1);
            PDFRenderer renderer2 = new PDFRenderer(doc2);

            for (int page = 0; page < Math.min(doc1.getNumberOfPages(), doc2.getNumberOfPages()); page++) {
                boolean isDiff = false;
                BufferedImage image1 = renderer1.renderImage(page, SCALE);
                BufferedImage image2 = renderer2.renderImage(page, SCALE);

                PDPage pdPage = doc2.getPage(page);

                // flip y-axis
                flipAT = new AffineTransform();
                flipAT.translate(0, pdPage.getBBox().getHeight());
                flipAT.scale(1, -1);

                // page may be rotated
                rotateAT = new AffineTransform();
                int rotation = pdPage.getRotation();
                if (rotation != 0) {
                    PDRectangle mediaBox = pdPage.getMediaBox();
                    switch (rotation) {
                        case 90:
                            rotateAT.translate(mediaBox.getHeight(), 0);
                            break;
                        case 270:
                            rotateAT.translate(0, mediaBox.getWidth());
                            break;
                        case 180:
                            rotateAT.translate(mediaBox.getWidth(), mediaBox.getHeight());
                            break;
                        default:
                            break;
                    }
                    rotateAT.rotate(Math.toRadians(rotation));
                }

                g2d = image2.createGraphics();
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(new Color(255, 0, 0, 51));
                g2d.scale(SCALE, SCALE);

                List<TextPosition> textPositions1 = getTextPositions(doc1, page + 1);
                List<TextPosition> textPositions2 = getTextPositions(doc2, page + 1);

                for (int i = 0; i < textPositions1.size(); i++) {
                    TextPosition text1 = textPositions1.get(i);
                    TextPosition text2 = textPositions2.get(i);

                    AffineTransform at = text2.getTextMatrix().createAffineTransform();
                    PDFont font = text2.getFont();
                    BoundingBox bbox = font.getBoundingBox();

                    float xAdvance = font.getWidth(text2.getCharacterCodes()[0]); // todo: should iterate all chars
                    Rectangle2D.Float rect = new Rectangle2D.Float(0, bbox.getLowerLeftY(), xAdvance, bbox.getHeight());
                    if (font instanceof PDType3Font) {
                        // bbox and font matrix are unscaled
                        at.concatenate(font.getFontMatrix().createAffineTransform());
                    } else {
                        // bbox and font matrix are already scaled to 1000
                        at.scale(1 / 1000f, 1 / 1000f);
                    }

                    Shape s = at.createTransformedShape(rect);
                    s = flipAT.createTransformedShape(s);
                    s = rotateAT.createTransformedShape(s);

                    if (!isEqual(text1, text2, isFont)) {
                        g2d.fill(s);
                        isDiff = true;
                    }
                }

                g2d.dispose();
                if (isDiff) {
                    diffImagesMap.put(page + 1, Arrays.asList(image1, image2));
                }
            }
        }
        return diffImagesMap.isEmpty() ? new HashMap<>() : diffImagesMap;
    }

    private static boolean isEqual(TextPosition pos1, TextPosition pos2, boolean isFont) {
        boolean isEqual = pos1.getUnicode().equals(pos2.getUnicode());
        if (isFont) {
            String pos1FName = getFontName(pos1.getFont());
            String pos2FName = getFontName(pos2.getFont());
            assert pos1FName != null;
            isEqual = pos1FName.equals(pos2FName);
        }
        return isEqual;
    }

    private static String getFontName(PDFont pos1Font) {
        if (pos1Font instanceof PDType3Font t3Font) {
            return t3Font.getName();
        } else if (pos1Font instanceof PDVectorFont) {
            if (pos1Font instanceof PDTrueTypeFont ttFont) {
                return ttFont.getName();
            }
            if (pos1Font instanceof PDType0Font t0font) {
                return t0font.getDescendantFont().getName();
            }
        }
        return null;
    }

    private static List<TextPosition> getTextPositions(PDDocument document, int pageNum) throws IOException {
        List<TextPosition> textPositions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper() {

            @Override
            protected void writeString(String string, List<TextPosition> text) {
                textPositions.addAll(text);
            }
        };
        stripper.setStartPage(pageNum);
        stripper.setEndPage(pageNum);
        stripper.getText(document);
        return textPositions;
    }
}
