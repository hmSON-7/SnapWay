package com.snapway.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Article {

	private long articleId;
	private String title;
	private String tags;
	private String authorId;
	private String content;
	private int likes;
	private LocalDateTime uploadedAt;
	private String category;
	private int hits;
}
