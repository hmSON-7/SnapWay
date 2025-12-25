package com.snapway.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ImageBase64Encoder {

    // GMS 프록시 통과를 위한 초경량 설정
    // 512px이면 AI가 인식하기에 충분하면서 용량은 1/4로 줄어듭니다.
    private static final int MAX_WIDTH = 512;
    private static final int MAX_HEIGHT = 512;
    
    // 압축 품질 (0.5 = 50%)
    private static final float JPEG_QUALITY = 0.5f;

    public String encode(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Base64 변환 실패: 파일이 비어있거나 null입니다.");
            return null;
        }
        
        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                log.warn("이미지 파일이 아니거나 지원하지 않는 포맷입니다: {}", file.getOriginalFilename());
                return null;
            }

            return resizeAndEncode(originalImage, file.getOriginalFilename());
        } catch (IOException e) {
            log.error("MultipartFile Base64 변환 중 오류 발생: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    public String encode(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            log.warn("Base64 변환 실패: 파일이 존재하지 않거나 디렉토리입니다. 경로: {}", filePath);
            return null;
        }

        try {
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) {
                return null;
            }
            return resizeAndEncode(originalImage, file.getName());
        } catch (IOException e) {
            log.error("파일 Base64 변환 중 I/O 오류 발생: {}", filePath, e);
            return null;
        }
    }

    private String resizeAndEncode(BufferedImage originalImage, String fileName) throws IOException {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // 리사이징 (512px 제한)
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            double aspectRatio = (double) originalWidth / originalHeight;

            if (originalWidth > originalHeight) {
                newWidth = MAX_WIDTH;
                newHeight = (int) (MAX_WIDTH / aspectRatio);
            } else {
                newHeight = MAX_HEIGHT;
                newWidth = (int) (MAX_HEIGHT * aspectRatio);
            }
        }

        // 이미지 리사이징 (TYPE_INT_RGB로 투명도 제거)
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // JPEG 압축
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        
        if (!writers.hasNext()) throw new IllegalStateException("No writers found for jpg");
        ImageWriter writer = writers.next();
        
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(resizedImage, null, null), param);
        } finally {
            writer.dispose();
        }
        
        byte[] imageBytes = baos.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        
        // 로그로 용량 확인 (KB 단위)
        log.info("이미지 초경량화 완료: {} ({}x{} -> {}x{}), 최종용량: {}KB", 
                fileName, originalWidth, originalHeight, newWidth, newHeight, imageBytes.length / 1024);
        
        return base64;
    }
}
