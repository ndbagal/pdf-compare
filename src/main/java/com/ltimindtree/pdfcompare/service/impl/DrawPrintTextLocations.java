/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ltimindtree.pdfcompare.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.interactive.pagenavigation.PDThreadBead;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is an example on how to get some x/y coordinates of text and to show them in a rendered
 * image.
 *
 * @author Ben Litchfield
 * @author Tilman Hausherr
 */
@Slf4j
public class DrawPrintTextLocations extends PDFTextStripper {
    static final int SCALE = 4;
    private final String filename;
    private AffineTransform flipAT;
    private AffineTransform rotateAT;
    private AffineTransform transAT;
    private Graphics2D g2d;
    private List<TextPosition> diffTextPositions;

    /**
     * Instantiate a new PDFTextStripper object.
     *
     * @param document Document to process
     * @param filename File name
     * @throws IOException If there is an error loading the properties.
     */
    public DrawPrintTextLocations(PDDocument document, String filename, List<TextPosition> diffTextPositions) throws IOException {
        this.document = document; // must initialize here, base class initializes too late
        this.filename = filename;
        this.diffTextPositions = diffTextPositions;
    }

    /**
     * This will print the documents data.
     *
     * @param filename filename.
     * @throws IOException If there is an error parsing the document.
     */
    public void highlightDifference(String filename) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(filename))) {
            DrawPrintTextLocations stripper = new DrawPrintTextLocations(document, filename, diffTextPositions);
            stripper.setSortByPosition(true);

            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                stripper.stripPage(page);
                log.info("Processed page {}", page);
            }
        }
    }

    /*@Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement)
            throws IOException {
        super.showGlyph(textRenderingMatrix, font, code, displacement);

        // in cyan:
        // show actual glyph bounds. This must be done here and not in writeString(),
        // because writeString processes only the glyphs with unicode,
        // see e.g. the file in PDFBOX-3274
        Shape cyanShape = calculateGlyphBounds(textRenderingMatrix, font, code);

        if (cyanShape != null) {
            cyanShape = flipAT.createTransformedShape(cyanShape);
            cyanShape = rotateAT.createTransformedShape(cyanShape);
            cyanShape = transAT.createTransformedShape(cyanShape);

            g2d.setColor(Color.CYAN);
            g2d.draw(cyanShape);
        }
    }

    // this calculates the real (except for type 3 fonts) individual glyph bounds
    private Shape calculateGlyphBounds(Matrix textRenderingMatrix, PDFont font, int code) throws IOException {
        GeneralPath path = null;
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        at.concatenate(font.getFontMatrix().createAffineTransform());
        if (font instanceof PDType3Font t3Font) {
            // It is difficult to calculate the real individual glyph bounds for type 3 fonts
            // because these are not vector fonts, the content stream could contain almost anything
            // that is found in page content streams.
            PDType3CharProc charProc = t3Font.getCharProc(code);
            if (charProc != null) {
                BoundingBox fontBBox = t3Font.getBoundingBox();
                PDRectangle glyphBBox = charProc.getGlyphBBox();
                if (glyphBBox != null) {
                    // PDFBOX-3850: glyph bbox could be larger than the font bbox
                    glyphBBox.setLowerLeftX(Math.max(fontBBox.getLowerLeftX(), glyphBBox.getLowerLeftX()));
                    glyphBBox.setLowerLeftY(Math.max(fontBBox.getLowerLeftY(), glyphBBox.getLowerLeftY()));
                    glyphBBox.setUpperRightX(Math.min(fontBBox.getUpperRightX(), glyphBBox.getUpperRightX()));
                    glyphBBox.setUpperRightY(Math.min(fontBBox.getUpperRightY(), glyphBBox.getUpperRightY()));
                    path = glyphBBox.toGeneralPath();
                }
            }
        } else if (font instanceof PDVectorFont vectorFont) {
            path = vectorFont.getPath(code);

            if (font instanceof PDTrueTypeFont ttFont) {
                int unitsPerEm = ttFont.getTrueTypeFont().getHeader().getUnitsPerEm();
                at.scale(1000d / unitsPerEm, 1000d / unitsPerEm);
            }
            if (font instanceof PDType0Font t0font) {
                if (t0font.getDescendantFont() instanceof PDCIDFontType2) {
                    int unitsPerEm = ((PDCIDFontType2) t0font.getDescendantFont()).getTrueTypeFont().getHeader().getUnitsPerEm();
                    at.scale(1000d / unitsPerEm, 1000d / unitsPerEm);
                }
            }
        } else {
            // shouldn't happen, please open issue in JIRA
            System.out.println("Unknown font class: " + font.getClass());
        }
        if (path == null) {
            return null;
        }
        return at.createTransformedShape(path.getBounds2D());
    }*/

    private void stripPage(int page) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        BufferedImage image = pdfRenderer.renderImage(page, SCALE);
        PDPage pdPage = document.getPage(page);
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

        // cropbox
        transAT = AffineTransform.getTranslateInstance(-cropBox.getLowerLeftX(), cropBox.getLowerLeftY());

        g2d = image.createGraphics();
        g2d.setStroke(new BasicStroke(0.1f));
        g2d.scale(SCALE, SCALE);

        setStartPage(page + 1);
        setEndPage(page + 1);

        Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        writeText(document, dummy);

        // beads in green
        g2d.setStroke(new BasicStroke(0.4f));
        List<PDThreadBead> pageArticles = pdPage.getThreadBeads();
        for (PDThreadBead bead : pageArticles) {
            if (bead == null) {
                continue;
            }
            PDRectangle r = bead.getRectangle();
            Shape s = r.toGeneralPath().createTransformedShape(transAT);
            s = flipAT.createTransformedShape(s);
            s = rotateAT.createTransformedShape(s);
            g2d.setColor(Color.green);
            g2d.draw(s);
        }

        g2d.dispose();

        String imageFilename = filename;
        int pt = imageFilename.lastIndexOf('.');
        imageFilename = imageFilename.substring(0, pt) + "-marked-" + (page + 1) + ".png";
        ImageIO.write(image, "png", new File(imageFilename));
    }

    /**
     * Override the default functionality of PDFTextStripper.
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        textPositions = textPositions
                .stream()
                .filter(textPosition -> containsTextPosition(diffTextPositions, textPosition))
                .collect(Collectors.toList());

        for (TextPosition text : textPositions) {
            System.out.println("String[" + text.getXDirAdj() + ","
                    + text.getYDirAdj() + " fs=" + text.getFontSize() + " xscale="
                    + text.getXScale() + " height=" + text.getHeightDir() + " space="
                    + text.getWidthOfSpace() + " width="
                    + text.getWidthDirAdj() + "]" + text.getUnicode());

            // glyph space -> user space
            // note: text.getTextMatrix() is *not* the Text Matrix, it's the Text Rendering Matrix
            AffineTransform at = text.getTextMatrix().createAffineTransform();

            // in red:
            // show rectangles with the "height" (not a real height, but used for text extraction
            // heuristics, it is 1/2 of the bounding box height and starts at y=0)
            Rectangle2D.Float rect = new Rectangle2D.Float(0, 0,
                    text.getWidthDirAdj() / text.getTextMatrix().getScalingFactorX(),
                    text.getHeightDir() / text.getTextMatrix().getScalingFactorY());
            Shape s = at.createTransformedShape(rect);
            /*s = flipAT.createTransformedShape(s);
            s = rotateAT.createTransformedShape(s);
            g2d.setColor(Color.red);
            g2d.draw(s);*/

            // in blue:
            // show rectangle with the real vertical bounds, based on the font bounding box y values
            // usually, the height is identical to what you see when marking text in Adobe Reader
            PDFont font = text.getFont();
            BoundingBox bbox = font.getBoundingBox();

            // advance width, bbox height (glyph space)
            float xAdvance = font.getWidth(text.getCharacterCodes()[0]); // todo: should iterate all chars
            rect = new Rectangle2D.Float(0, bbox.getLowerLeftY(), xAdvance, bbox.getHeight());

            if (font instanceof PDType3Font) {
                // bbox and font matrix are unscaled
                at.concatenate(font.getFontMatrix().createAffineTransform());
            } else {
                // bbox and font matrix are already scaled to 1000
                at.scale(1 / 1000f, 1 / 1000f);
            }
            s = at.createTransformedShape(rect);
            s = flipAT.createTransformedShape(s);
            s = rotateAT.createTransformedShape(s);

            g2d.setColor(Color.blue);
            g2d.draw(s);
        }
    }

    public static boolean containsTextPosition(List<TextPosition> list1, TextPosition textPosition) {
        for (TextPosition tp : list1) {
            if (areEqual(tp, textPosition)) {
                return true;
            }
        }
        return false;
    }

    public static boolean areEqual(TextPosition textPosition1, TextPosition textPosition2) {
        // Check if characters are equal
        boolean charactersEqual = textPosition1.getUnicode().equals(textPosition2.getUnicode());

        // Check if font sizes are equal
        boolean fontSizesEqual = Math.abs(textPosition1.getFontSize() - textPosition2.getFontSize()) < 0.001;

        // Check if positions are equal (within a small tolerance)
        boolean positionsEqual = Math.abs(textPosition1.getX() - textPosition2.getX()) < 0.001 &&
                Math.abs(textPosition1.getY() - textPosition2.getY()) < 0.001;

        // Return true if all properties are equal
        return charactersEqual && fontSizesEqual && positionsEqual;
    }
}