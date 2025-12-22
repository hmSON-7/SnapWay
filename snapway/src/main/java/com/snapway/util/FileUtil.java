package com.snapway.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 파일 시스템을 이용한 파일 저장 유틸리티
 * 개발 환경: 프로젝트 내부 resources/static/img 경로 사용
 * 배포 환경: 외부 절대 경로 사용 가능 (설정 파일 제어)
 */
@Slf4j
@Component
public class FileUtil {

    // application.properties에 'file.upload-dir' 설정이 있으면 그 경로를 사용(배포용)
    // 없으면 비워둠(개발용 자동 경로 탐색)
    // 예: file.upload-dir=C:/server/uploads/
    @Value("${file.upload-dir:}")
    private String configuredUploadPath;

    private Path rootLocation;
    private final String STATIC_IMG_PATH = "img"; // 웹 접근시 URL prefix

    @PostConstruct
    public void init() {
        if (configuredUploadPath != null && !configuredUploadPath.isEmpty()) {
            // [배포 환경] 설정 파일에 지정된 외부 경로 사용
            this.rootLocation = Paths.get(configuredUploadPath);
            log.info("배포 환경 파일 저장 경로 설정됨: {}", this.rootLocation.toAbsolutePath());
        } else {
            // [개발 환경] 프로젝트 내부 src/main/resources/static/img 자동 탐색
            // 주의: 이클립스/IntelliJ에서 실행 시 target 폴더와 싱크가 맞아야 바로 보일 수 있음
            String projectPath = System.getProperty("user.dir");
            // Maven 프로젝트 표준 구조 기준
            this.rootLocation = Paths.get(projectPath, "src", "main", "resources", "static", STATIC_IMG_PATH);
            log.info("개발 환경 파일 저장 경로 설정됨: {}", this.rootLocation.toAbsolutePath());
        }

        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 저장소 초기화 실패!", e);
        }
    }

    /**
     * 파일을 구조화된 경로에 저장합니다.
     * 저장 경로: root/{username}/{category}/{subId}/uuid_filename
     * * @param file 업로드할 파일
     * @param username 사용자 ID
     * @param category 카테고리 (article, trip 등)
     * @param subId 게시글ID 또는 여행ID
     * @return 웹에서 접근 가능한 상대 경로 문자열 (예: /img/user1/trip/10/abc.jpg)
     */
    public String saveFile(MultipartFile file, String username, String category, String subId) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("빈 파일은 저장할 수 없습니다.");
        }

        // 1. 저장할 세부 디렉토리 생성
        Path targetDir = this.rootLocation.resolve(Paths.get(username, category, subId));
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // 2. 파일명 중복 방지 처리
        String originalFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String extension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String savedFilename = uuid + extension;
        Path destinationFile = targetDir.resolve(savedFilename);

        // 3. 파일 저장 (덮어쓰기 허용)
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // 4. DB에 저장할 웹 접근 경로 반환
        // 윈도우의 백슬래시(\)를 웹 표준 슬래시(/)로 변환
        String webPath = "/" + STATIC_IMG_PATH + "/" + username + "/" + category + "/" + subId + "/" + savedFilename;
        return webPath.replace("\\", "/");
    }

    /**
     * 저장된 파일을 Resource 객체로 조회합니다. (다운로드 등에 사용)
     * @param webPath DB에 저장된 웹 경로 (예: /img/user1/trip/1/abc.jpg)
     */
    public Resource loadFileAsResource(String webPath) {
        try {
            // 웹 경로에서 실제 파일 시스템 경로로 변환
            // /img/user1... -> user1...
            String relativePath = webPath.replace("/" + STATIC_IMG_PATH + "/", "");
            Path file = rootLocation.resolve(relativePath).normalize();
            
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 찾을 수 없거나 읽을 수 없습니다: " + webPath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일 경로가 올바르지 않습니다: " + webPath, e);
        }
    }
    
    /**
     * 실제 파일 시스템의 절대 경로를 반환합니다. (AI 분석 등을 위해 필요)
     * @param webPath DB에 저장된 웹 경로
     */
    public String getAbsolutePath(String webPath) {
        if (webPath == null) return null;
        String relativePath = webPath.replace("/" + STATIC_IMG_PATH + "/", "");
        return rootLocation.resolve(relativePath).normalize().toString();
    }

    /**
     * 파일 하나를 삭제합니다.
     * @param webPath DB에 저장된 웹 경로
     */
    public void deleteFile(String webPath) {
        if (webPath == null) return;
        try {
            String relativePath = webPath.replace("/" + STATIC_IMG_PATH + "/", "");
            Path file = rootLocation.resolve(relativePath);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", webPath, e);
        }
    }

    /**
     * 특정 디렉토리(여행 기록 전체 등)를 삭제합니다.
     * @param username 사용자 ID
     * @param category 카테고리
     * @param subId ID
     */
    public void deleteDirectory(String username, String category, String subId) {
        Path targetDir = this.rootLocation.resolve(Paths.get(username, category, subId));
        try {
            FileSystemUtils.deleteRecursively(targetDir); // 스프링 유틸 사용하여 하위 파일까지 삭제
            log.info("디렉토리 삭제 완료: {}", targetDir);
        } catch (IOException e) {
            log.error("디렉토리 삭제 실패: {}", targetDir, e);
        }
    }
}