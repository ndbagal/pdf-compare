package com.ltimindtree.pdfcompare.service.impl;

import com.ltimindtree.pdfcompare.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDGamma;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@Slf4j
public class PDFDifferenceHighlighter extends PDFTextStripper {

    private final FileService fileService = new FileServiceImpl();

    public PDFDifferenceHighlighter() {
        super();
    }

    public void compareFont(String originalFilePath, String modifiedFilePath) {
        try (PDDocument originalDoc = Loader.loadPDF(new File(originalFilePath));
             PDDocument modifiedDoc = Loader.loadPDF(new File(modifiedFilePath))) {

            PDFDifferenceHighlighter highlighter = new PDFDifferenceHighlighter();

            // Iterate over each page
            int pageCount = Math.max(originalDoc.getNumberOfPages(), modifiedDoc.getNumberOfPages());
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                highlighter.setStartPage(pageIndex + 1);
                highlighter.setEndPage(pageIndex + 1);

                // Extract text and positions from both documents for the current page
                String text1 = highlighter.getText(originalDoc);
                List<List<TextPosition>> positions1 = highlighter.getCharactersByArticle();

                highlighter = new PDFDifferenceHighlighter();
                highlighter.setStartPage(pageIndex + 1);
                highlighter.setEndPage(pageIndex + 1);
                String text2 = highlighter.getText(modifiedDoc);
                List<List<TextPosition>> positions2 = highlighter.getCharactersByArticle();

                // Compare text and positions to identify differences
                // For example, you can use a text diffing algorithm

                // Apply differences to the first document
                highlightDifferences(
                        originalDoc,
                        positions1.stream().flatMap(Collection::stream).toList(),
                        positions2.stream().flatMap(Collection::stream).toList()
                );
            }

            // Save the modified document
//            originalDoc.save("highlighted_document.pdf");
            fileService.saveFile(originalDoc, "diff", "fontdiff");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        // Override to capture text positions
        super.processTextPosition(text);
    }

    private static void highlightDifferences(PDDocument document, List<TextPosition> positions1, List<TextPosition> positions2) throws IOException {
        // Iterate over the positions and draw rectangles or annotations
        try (PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0), PDPageContentStream.AppendMode.APPEND, false)) {
            for (int i = 0; i < positions1.size(); i++) {
                TextPosition pos1 = positions1.get(i);
                TextPosition pos2 = positions2.get(i);

                String pos1FName = ((PDCIDFontType2) ((PDType0Font) pos1.getFont()).getDescendantFont()).getTrueTypeFont().getName();
                String pos2FName = ((PDCIDFontType2) ((PDType0Font) pos2.getFont()).getDescendantFont()).getTrueTypeFont().getName();

                if (!pos1FName.equals(pos2FName)) {
                    float x = pos1.getXDirAdj();
                    float y = pos1.getYDirAdj();
                    float width = pos1.getWidthDirAdj();
                    float height = pos1.getHeightDir();

                    PDAnnotationHighlight highlight = new PDAnnotationHighlight();
                    PDRectangle position = new PDRectangle(x, y, width, height);
                    highlight.setRectangle(position);
                    highlight.setConstantOpacity((float) 0.3); // Set opacity
                    highlight.setColor(new PDColor(new float[]{1, 1, 0}, PDDeviceRGB.INSTANCE)); // Set highlight color
                    highlight.setContents(pos1.getUnicode());

                    // Add annotation to the page
                    document.getPage(0).getAnnotations().add(highlight);
                }
            }
        }
    }
}
