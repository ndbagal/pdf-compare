package com.ltimindtree.pdfcompare.service;

import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.List;

public interface DrawPrintTextLocationsService {
    void highlightDifference(String filename, List<TextPosition> diffTextPositions) throws IOException;
}
