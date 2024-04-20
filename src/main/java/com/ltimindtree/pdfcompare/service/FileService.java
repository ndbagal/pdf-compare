package com.ltimindtree.pdfcompare.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileService {

    String uploadFile(MultipartFile file, String tag) throws Exception;

    String saveFile(PDDocument document, String tag, String filename) throws Exception;

    File getFile(String filename);
}
