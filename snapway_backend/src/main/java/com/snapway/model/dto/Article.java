package com.snapway.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Article {

	private long articleId;
	private String title;
	private String tags;
	private int authorId;
	private String authorName;

	private String category;

	private String content;
	private int likes;
	private int hits;
	private LocalDateTime uploadedAt;
}
