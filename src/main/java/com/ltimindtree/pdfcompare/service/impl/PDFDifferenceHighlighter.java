package com.ltimindtree.pdfcompare.service.impl;

import com.ltimindtree.pdfcompare.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Slf4j
public class PDFDifferenceHighlighter extends PDFTextStripper {

    private final FileService fileService = new FileServiceImpl();

    public PDFDifferenceHighlighter() throws IOException {
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
        for (int i = 0; i < positions1.size(); i++) {
            TextPosition pos1 = positions1.get(i);
            TextPosition pos2 = positions2.get(i);

            String pos1FName = ((PDCIDFontType2) ((PDType0Font) pos1.getFont()).getDescendantFont()).getTrueTypeFont().getName();
            String pos2FName = ((PDCIDFontType2) ((PDType0Font) pos2.getFont()).getDescendantFont()).getTrueTypeFont().getName();

            if (!pos1FName.equals(pos2FName)) {
                try (PDPageContentStream contentStream = new PDPageContentStream(document, document.getPage(0), PDPageContentStream.AppendMode.APPEND, false)) {
                    PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setStrokingAlphaConstant(0.6f);
                    gs.setNonStrokingAlphaConstant(0.6f);

                    contentStream.setLineWidth(pos1.getFontSize());
                    contentStream.setStrokingColor(0f, 0f, 1f, 0f); // Yellow color
                    contentStream.setGraphicsStateParameters(gs); // 60% opacity (transparency)

                    // Calculate rectangle coordinates based on position information
                    float x = pos1.getXDirAdj();
                    float y = pos1.getYDirAdj();
                    float width = Math.abs(pos1.getWidthDirAdj());
                    float height = Math.abs(pos1.getHeightDir());

                    log.info("x: {}, y: {}, width: {}, height: {}", x, y, width, height);
                    log.info("POS1: x: {}, y: {}, width: {}, height: {}", pos1.getXDirAdj(), pos1.getYDirAdj(), pos1.getWidthDirAdj(), pos1.getHeightDir());
                    log.info("POS2: x: {}, y: {}, width: {}, height: {}", pos2.getXDirAdj(), pos2.getYDirAdj(), pos2.getWidthDirAdj(), pos2.getHeightDir());
                    contentStream.addRect(x, y, width, height);
                    contentStream.stroke();
                }
            }
        }
    }
}
