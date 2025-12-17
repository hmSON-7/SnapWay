package com.snapway.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ArticleFile {
	private long fileId;
	private long articleId;
	private String filePath;
	private String originFileName;
	private long fieSize;
	private LocalDateTime uploadedAt;
}
