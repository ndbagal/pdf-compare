package com.ltimindtree.pdfcompare.web.controller;

import com.ltimindtree.pdfcompare.ComparePDFText;
import com.ltimindtree.pdfcompare.service.FileService;
import com.ltimindtree.pdfcompare.service.PdfUtilityService;
import com.ltimindtree.pdfcompare.util.TrackExecutionTime;
import com.ltimindtree.pdfcompare.web.dto.MyResponse;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
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

    @TrackExecutionTime
    @RequestMapping(
            value = "/compare",
            method = RequestMethod.POST,
            consumes = "multipart/form-data"
    )
    public ResponseEntity<List<MyResponse>> comparePdfFiles(
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

                String comparisonType = typeOpt.get();

                switch (comparisonType) {
                    case "image" -> {
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
                    }
                    case "font" -> {
                        // Load the first PDF document
                        try (PDDocument document = new PDDocument()) {
                            ComparePDFText comparePDFText = new ComparePDFText();
                            Map<Integer, List<BufferedImage>> diffImagesMap = comparePDFText.compareFontOrText(
                                    originalFilePath, modifiedFilePath, true
                            );
                            createDifferencePdfFile(
                                    responses, original, originalFilePath, document, diffImagesMap
                            );
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                    case "text" -> {
                        // Load the first PDF document
                        try (PDDocument document = new PDDocument()) {
                            ComparePDFText comparePDFText = new ComparePDFText();
                            Map<Integer, List<BufferedImage>> diffImagesMap = comparePDFText.compareFontOrText(
                                    originalFilePath, modifiedFilePath, false
                            );
                            createDifferencePdfFile(
                                    responses, original, originalFilePath, document, diffImagesMap
                            );
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                    default -> throw new RuntimeException("Method not supported");
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return ResponseEntity.ok(responses);
    }

    private void createDifferencePdfFile(List<MyResponse> responses, MultipartFile original, String originalFilePath, PDDocument document, Map<Integer, List<BufferedImage>> diffImagesMap) throws Exception {
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
            diffImagesMap.forEach((key, diffImages) -> {
                // Calculate the width and height of the page
                BufferedImage image1 = diffImages.get(0);
                BufferedImage image2 = diffImages.get(1);

                float pageWidth = Math.addExact(image1.getWidth(), image2.getWidth());
                float pageHeight = Math.max(image1.getHeight(), image2.getHeight());

                // Create a new page
                PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
                document.addPage(page);

                // Create a content stream for the page
                PDPageContentStream contentStream;
                try {
                    contentStream = new PDPageContentStream(document, page);
                    // Draw the first image on the left side
                    contentStream.drawImage(LosslessFactory.createFromImage(document, image1), 0, 0, image1.getWidth(), image1.getHeight());

                    // Draw the second image next to the first one
                    contentStream.drawImage(LosslessFactory.createFromImage(document, image2), image1.getWidth(), 0, image2.getWidth(), image2.getHeight());

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
