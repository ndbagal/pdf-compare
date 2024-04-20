package com.ltimindtree.pdfcompare.web.controller;

import com.ltimindtree.pdfcompare.service.FileService;
import com.ltimindtree.pdfcompare.service.PdfUtilityService;
import com.ltimindtree.pdfcompare.service.impl.PDFDifferenceHighlighter;
import com.ltimindtree.pdfcompare.web.dto.MyResponse;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/files")
public class FileController {

    private final PdfUtilityService pdfUtilityService;
    private final FileService fileService;

    public FileController(PdfUtilityService pdfUtilityService, FileService fileService) {
        this.pdfUtilityService = pdfUtilityService;
        this.fileService = fileService;
    }

    @RequestMapping(value = "/compare", method = RequestMethod.POST, consumes = "multipart/form-data")
    public ResponseEntity<List<MyResponse>> comparePDFs(
            @RequestParam("originalFiles") List<MultipartFile> originalFiles,
            @RequestParam("modifiedFiles") List<MultipartFile> modifiedFiles,
            @PathParam("type") Optional<String> typeOpt
    ) {
        List<MyResponse> responses = new ArrayList<>();
        if (typeOpt.isEmpty()) typeOpt = Optional.of("image");
        for (MultipartFile original : originalFiles) {
            try {
                String originalFilePath = fileService.uploadFile(original, "original");
                String modifiedFilePath = fileService.uploadFile(
                        modifiedFiles
                                .stream()
                                .filter(multipartFile -> Objects.equals(multipartFile.getOriginalFilename(), original.getOriginalFilename()))
                                .findFirst().orElseThrow(),
                        "modified"
                );

                if (typeOpt.get().equals("image")) {
                    // Convert PDF files to images
                    File originalFile = new File(originalFilePath);
                    Map<Integer, BufferedImage> originalBufferedImagesMap = pdfUtilityService.convertPDFToImage(originalFile);
                    File modifiedFile = new File(modifiedFilePath);
                    Map<Integer, BufferedImage> modifiedBufferedImagesMap = pdfUtilityService.convertPDFToImage(modifiedFile);

                    Map<Integer, List<BufferedImage>> diffImagesMap = pdfUtilityService.comparePDFs(
                            originalBufferedImagesMap,
                            modifiedBufferedImagesMap
                    );

                    if (diffImagesMap.isEmpty()) {
                        responses.add(
                                MyResponse.builder()
                                        .filename("")
                                        .isDiff(false)
                                        .message("Files are identical.")
                                        .build()
                        );
                    } else {
                        String filename = originalFile.getName();
                        String diffPdfPath = pdfUtilityService.createDiffPdf(originalBufferedImagesMap, diffImagesMap, filename.substring(0, filename.lastIndexOf('.')));

                        // Prepare file response
                        File file = new File(diffPdfPath);
                        byte[] fileContent = Files.readAllBytes(file.toPath());

                        // file response
                        responses.add(
                                MyResponse.builder()
                                        .filename(filename)
                                        .isDiff(true)
                                        .message("Files are not identical.")
                                        .build()
                        );
                    }
                } else if (typeOpt.get().equals("font")) {
                        // Load the first PDF document
                        PDFDifferenceHighlighter pdfDifferenceHighlighter = new PDFDifferenceHighlighter();
                        pdfDifferenceHighlighter.compareFont(originalFilePath, modifiedFilePath);
                        return null;
                } else {
                    return null;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return ResponseEntity.ok(responses);
    }

    private List<TextPosition> extractTextWithFont(PDDocument pdfDocument) throws IOException {
        List<TextPosition> textPositions = new ArrayList<>();
        PDFTextStripper pdfStripper = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition textPosition) {
                textPositions.add(textPosition);
            }
        };

        // Force parse to extract text
        pdfStripper.getText(pdfDocument);
        return textPositions;
    }

    private boolean compareFonts(
            List<TextPosition> originalTextPositions, List<TextPosition> modifiedTextPositions
    ) {
        boolean isDiff = false;
        for (int i = 0; i < originalTextPositions.size(); i++) {
            TextPosition originalTextPosition = originalTextPositions.get(i);
            TextPosition modifiedTextPosition = modifiedTextPositions.get(i);

            String originalFont = originalTextPosition.getFont().getName();
            String modifiedFont = modifiedTextPosition.getFont().getName();

            if (modifiedFont != null && !originalFont.equals(modifiedFont)) {
                float x = Math.min(originalTextPosition.getXDirAdj(), modifiedTextPosition.getXDirAdj());
                float y = Math.min(originalTextPosition.getYDirAdj(), modifiedTextPosition.getYDirAdj());
                float width = Math.abs(originalTextPosition.getWidthDirAdj() - modifiedTextPosition.getWidthDirAdj());
                float height = Math.abs(originalTextPosition.getHeightDir() - modifiedTextPosition.getHeightDir());
                isDiff = true;
            }
        }
        return isDiff;
    }

    @GetMapping
    public ResponseEntity<byte[]> getFile(@PathParam("filename") String filename) throws IOException {
        Path filePath = fileService.getFile(filename).toPath();
        // Set headers for the file response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("diffFile", filePath.toFile().getName());
        return new ResponseEntity<>(Files.readAllBytes(filePath), headers, HttpStatus.OK);
    }
}
