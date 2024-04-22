package com.ltimindtree.pdfcompare.web.controller;

import com.ltimindtree.pdfcompare.ComparePDFText;
import com.ltimindtree.pdfcompare.service.FileService;
import com.ltimindtree.pdfcompare.service.PdfUtilityService;
import com.ltimindtree.pdfcompare.service.impl.DrawPrintTextLocations;
import com.ltimindtree.pdfcompare.util.TrackExecutionTime;
import com.ltimindtree.pdfcompare.web.dto.MyResponse;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
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
import java.util.List;

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

    @TrackExecutionTime
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
                String modifiedFilePath;
                String originalFilePath = fileService.uploadFile(original, "original");
                if (originalFiles.size() == 1) {
                    modifiedFilePath = fileService.uploadFile(
                            modifiedFiles.get(0),
                            "modified"
                    );
                } else {
                    Optional<MultipartFile> optionalMultipartFile = modifiedFiles
                            .stream()
                            .filter(multipartFile -> Objects.equals(multipartFile.getOriginalFilename(), original.getOriginalFilename()))
                            .findFirst();
                    if (optionalMultipartFile.isPresent()) {
                        modifiedFilePath = fileService.uploadFile(
                                optionalMultipartFile.get(),
                                "modified"
                        );
                    } else {
                        throw new IllegalArgumentException("No modified file present with name " + original.getOriginalFilename());
                    }
                }

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
                                        .displayName(original.getOriginalFilename())
                                        .filename("")
                                        .isDiff(false)
                                        .message("Files are identical.")
                                        .build()
                        );
                    } else {
                        String filename = originalFile.getName();
                        String diffPdfPath = pdfUtilityService.createDiffPdf(originalBufferedImagesMap, diffImagesMap, filename.substring(0, filename.lastIndexOf('.')));

                        // file response
                        responses.add(
                                MyResponse.builder()
                                        .displayName(original.getOriginalFilename())
                                        .filename(filename)
                                        .url(diffPdfPath)
                                        .isDiff(true)
                                        .message("Files are not identical.")
                                        .build()
                        );
                    }
                } else if (typeOpt.get().equals("font")) {
                    // Load the first PDF document
                    try (PDDocument document = new PDDocument()) {
                        ComparePDFText comparePDFText = new ComparePDFText();
                        List<BufferedImage> bufferedImages = comparePDFText.compareFontOrText(
                                originalFilePath, modifiedFilePath, true
                        );
                        if (bufferedImages.isEmpty()) {
                            responses.add(
                                    MyResponse.builder()
                                            .displayName(original.getOriginalFilename())
                                            .filename("")
                                            .isDiff(false)
                                            .message("Files are identical.")
                                            .build()
                            );
                        } else {
                            bufferedImages.forEach(bufferedImage -> {
                                float pageWidth = bufferedImage.getWidth();
                                float pageHeight = bufferedImage.getHeight();

                                // Create a new page
                                PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                                document.addPage(page);

                                // Create a content stream for the page
                                PDPageContentStream contentStream;
                                try {
                                    contentStream = new PDPageContentStream(document, page);
                                    // Draw the first image on the left side
                                    contentStream.drawImage(LosslessFactory.createFromImage(document, bufferedImage), 0, 0);

                                    // Close the content stream
                                    contentStream.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            String filename = new File(originalFilePath).getName();
                            String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
                            String diffPdfPath = fileService.saveFile(document, "diff", filenameWithoutExtension);
                            // file response
                            responses.add(
                                    MyResponse.builder()
                                            .displayName(original.getOriginalFilename())
                                            .filename(filename)
                                            .url(diffPdfPath)
                                            .isDiff(true)
                                            .message("Files are not identical.")
                                            .build()
                            );
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                } else if (typeOpt.get().equals("text")) {
                    // Load the first PDF document
                    try (PDDocument document = new PDDocument()) {
                        ComparePDFText comparePDFText = new ComparePDFText();
                        List<BufferedImage> bufferedImages = comparePDFText.compareFontOrText(
                                originalFilePath, modifiedFilePath, false
                        );
                        if (bufferedImages.isEmpty()) {
                            responses.add(
                                    MyResponse.builder()
                                            .displayName(original.getOriginalFilename())
                                            .filename("")
                                            .isDiff(false)
                                            .message("Files are identical.")
                                            .build()
                            );
                        } else {
                            bufferedImages.forEach(bufferedImage -> {
                                float pageWidth = bufferedImage.getWidth();
                                float pageHeight = bufferedImage.getHeight();

                                // Create a new page
                                PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                                document.addPage(page);

                                // Create a content stream for the page
                                PDPageContentStream contentStream;
                                try {
                                    contentStream = new PDPageContentStream(document, page);
                                    // Draw the first image on the left side
                                    contentStream.drawImage(LosslessFactory.createFromImage(document, bufferedImage), 0, 0);

                                    // Close the content stream
                                    contentStream.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            String filename = new File(originalFilePath).getName();
                            String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
                            String diffPdfPath = fileService.saveFile(document, "diff", filenameWithoutExtension);
                            // file response
                            responses.add(
                                    MyResponse.builder()
                                            .displayName(original.getOriginalFilename())
                                            .filename(filename)
                                            .url(diffPdfPath)
                                            .isDiff(true)
                                            .message("Files are not identical.")
                                            .build()
                            );
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                } else {
                    throw new RuntimeException("Method not supported");
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return ResponseEntity.ok(responses);
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

    // Method to extract text with font information from a PDF document
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

    // Method to compare fonts between two documents
    private List<TextPosition> compareFonts(List<TextPosition> originalTextPositions, List<TextPosition> modifiedTextPositions) throws IOException {
        List<TextPosition> diffTextPositions = new ArrayList<>();
        for (int i = 0; i < originalTextPositions.size(); i++) {
            TextPosition originalTextPos = originalTextPositions.get(i);
            TextPosition modifiedTextPos = modifiedTextPositions.get(i);

            String pos1FName = ((PDCIDFontType2) ((PDType0Font) originalTextPos.getFont()).getDescendantFont()).getTrueTypeFont().getName();
            String pos2FName = ((PDCIDFontType2) ((PDType0Font) modifiedTextPos.getFont()).getDescendantFont()).getTrueTypeFont().getName();

            if (!pos1FName.equals(pos2FName)) {
                diffTextPositions.add(originalTextPos);
            }
        }
        return diffTextPositions;
    }
}
