package com.snapway.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

//이 유틸은 이미지 파일을 base64코드로 변환해 String으로 반환하는 코드입니다.
@Slf4j
@Component
public final class ImageBase64Encoder {

	/**
	 * 업로드된 MultipartFile을 바로 Base64 문자열로 변환
	 * 파일 저장 전, 메모리 상에서 AI에게 전송할 때 사용
	 * @param file 업로드된 파일 객체
	 * @return Base64 인코딩된 문자열(실패시 null)
	 */
    public static String encode(MultipartFile file) {
    	if(file == null || file.isEmpty()) {
    		log.warn("Base64 변환 실패 : 파일이 비었거나 null입니다.");
    		return null;
    	}
    	
    	try {
    		byte[] bytes = file.getBytes();
    		return Base64.getEncoder().encodeToString(bytes);
    	} catch(IOException e) {
    		log.error("MultipartFile Base64 변환 중 오류 발생 : {}", file.getOriginalFilename(), e);
    		return null;
    	}
    }
    
    /**
     * 로컬 디스크에 저장된 이미지 파일을 읽어서 Base64 문자열로 변환합니다.
     * (이미 저장된 파일을 다시 분석하거나 전송할 때 사용)
     * * @param filePath 이미지 파일의 절대 경로 (FileUtil.getAbsolutePath()로 얻은 경로 권장)
     * @return Base64 인코딩된 문자열 (실패 시 null)
     */
    public String encode(String filePath) {
    	if(filePath == null || filePath.isEmpty()) {
    		return null;
    	}
    	
    	File file = new File(filePath);
    	if(!file.exists() || !file.isFile()) {
    		log.warn("Base64 변환 실패 : 파일이 존재하지 않거나 디렉토리입니다. 경로 : {}", filePath);
    		return null;
    	}
    	
    	try {
    		Path path = Paths.get(filePath);
    		byte[] fileContent = Files.readAllBytes(path);
    		return Base64.getEncoder().encodeToString(fileContent);
    	} catch(IOException e) {
    		log.error("파일 Base64 변환 중 I/O 오류 발생: {}", filePath, e);
    		return null;
    	}
    }
}
