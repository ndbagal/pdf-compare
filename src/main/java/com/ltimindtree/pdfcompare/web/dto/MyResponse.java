package com.ltimindtree.pdfcompare.web.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyResponse {
    private String displayName;
    private String filename;
    private boolean isDiff;
    private String message;
    private String url;
}