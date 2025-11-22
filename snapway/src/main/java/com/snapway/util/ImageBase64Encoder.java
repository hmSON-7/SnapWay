package com.snapway.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;

//이 유틸은 이미지 파일을 base64코드로 변환해 String으로 반환하는 코드입니다.
public final class ImageBase64Encoder {

    private ImageBase64Encoder() {}

    public static String encode(String filePath) {
        File file = new File(filePath);

        // 1. 파일 존재 여부 및 타입 체크
        if (!file.exists()) {
            System.out.println("파일이 존재하지 않습니다: " + filePath);
            return null;
        }

        if (!file.canRead()) {
            System.out.println("파일을 읽을 수 있는 권한이 없습니다: " + filePath);
            return null;
        }

        // 2. 실제 파일 읽기시 발생할 예외들
        try (FileInputStream fi = new FileInputStream(file)) {
            byte[] fileBytes = new byte[(int) file.length()];
            int readBytes = fi.read(fileBytes);

            if (readBytes != fileBytes.length) {
                System.out.println("파일을 완전히 읽지 못했습니다: " + filePath);
                return null;
            }

            return Base64.getEncoder().encodeToString(fileBytes);

        } catch (FileNotFoundException e) {
            System.out.println("파일을 찾을 수 없습니다: " + filePath);
            e.printStackTrace();
            return null;

        } catch (IOException e) {
            System.out.println("파일 읽기 중 I/O 오류가 발생했습니다: " + filePath);
            e.printStackTrace();
            return null;

        }
    }
}
