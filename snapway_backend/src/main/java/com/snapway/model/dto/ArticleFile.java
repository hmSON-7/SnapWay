package com.snapway.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ArticleFile {
	private long fileId;
	private long articleId;
	private String filePath;
	private String originalName;
	private long fileSize;
	private LocalDateTime uploadedAt;
	private int positionIdx;
}
