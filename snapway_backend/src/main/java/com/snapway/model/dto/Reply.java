package com.snapway.model.dto;

import java.util.Date;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Reply {
	private int replyId;
	private int articleId;
	private int replierId;
	private String replierName;
	private String content;
	private Date repliedAt;
}
