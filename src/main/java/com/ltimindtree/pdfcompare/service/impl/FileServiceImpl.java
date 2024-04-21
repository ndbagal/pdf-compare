package com.ltimindtree.pdfcompare.service.impl;

import com.ltimindtree.pdfcompare.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    private static final String PROCESSING_FOLDER_PATH = System.getProperty("user.home") + "\\comparePDF";

    @Override
    public String uploadFile(MultipartFile file, String tag) throws Exception {
        // Check if the directory exists, if not, create it
        String uploadDir = PROCESSING_FOLDER_PATH + "\\" + tag;
        ensureFolderExists(uploadDir);

        // Modify the file name to add "_original" before the extension
        String originalFileName = file.getOriginalFilename();
        assert originalFileName != null;
        String modifiedFileName = new Date().getTime() + "_" + originalFileName;

        // Save the uploaded file with the modified file name to the specified directory
        Path filePath = Paths.get(uploadDir, modifiedFileName);
        Files.write(filePath, file.getBytes());
        return filePath.toString();
    }

    @Override
    public String saveFile(PDDocument document, String tag, String filename) throws Exception {
        String uploadDir = PROCESSING_FOLDER_PATH + "\\" + tag;
        ensureFolderExists(uploadDir);

        // Save the uploaded file with the modified file name to the specified directory
        String fileName = uploadDir + "\\" + (Objects.isNull(filename) ? tag : filename) + ".pdf";
        document.save(fileName);
        return fileName;
    }

    @Override
    public File getFile(String filename) {
        String diffDir = PROCESSING_FOLDER_PATH + "\\" + "diff";
        return new File(diffDir + "\\" + filename);
    }

    private void ensureFolderExists(String processingFolderPath) throws Exception {
        File processingFolder = new File(processingFolderPath);

        if (!processingFolder.exists()) {
            boolean isDirCreated = processingFolder.mkdir();
            if (!isDirCreated) {
                throw new Exception("Could not create folder " + processingFolderPath);
            }
        } else {
            log.info("Processing Folder already exists - {}", processingFolderPath);
        }
    }
}
