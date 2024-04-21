package com.ltimindtree.pdfcompare;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ComparePDFText {

    public static final int SCALE = 4;
    private AffineTransform flipAT;
    private AffineTransform rotateAT;
    private Graphics2D g2d;

    public List<BufferedImage> compareFontOrText(String originalFilePath, String modifiedFilePath, boolean isFont) throws IOException {

        List<BufferedImage> images = new ArrayList<>();
        boolean isDiff = false;
        try (PDDocument doc1 = Loader.loadPDF(new File(originalFilePath));
             PDDocument doc2 = Loader.loadPDF(new File(modifiedFilePath))) {

            PDFRenderer renderer1 = new PDFRenderer(doc1);
            PDFRenderer renderer2 = new PDFRenderer(doc2);

            for (int page = 0; page < Math.min(doc1.getNumberOfPages(), doc2.getNumberOfPages()); page++) {
                BufferedImage image1 = renderer1.renderImage(page, SCALE);
                BufferedImage image2 = renderer2.renderImage(page, SCALE);

                PDPage pdPage = doc2.getPage(page);
                PDRectangle cropBox = pdPage.getCropBox();

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

                Graphics2D g1d = image1.createGraphics();
                g1d.setStroke(new BasicStroke(0.1f));
                g1d.setColor(Color.GREEN);
                g1d.scale(SCALE, SCALE);

                g2d = image2.createGraphics();
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(new Color(255, 255, 153, 130));
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
                        g1d.draw(rect);
                        g2d.fill(s);
                        isDiff = true;
                    }
                }

                g1d.dispose();

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
