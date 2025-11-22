package com.snapway.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// 유틸 클래스이기 때문에 상속하지 말고 사용해 주세요
public final class RestfulClient {

	// 유틸 클래스이기 때문에 인스턴스를 생성하지 말고 사용해 주세요
    private RestfulClient() {}

    /**
     * 해당 엔드포인트로 요청을 전송합니다.
     *
     * @param apiKey:	사용할 api에 맞는 키
     * @param endPoint:	사용할 api에 맞는 엔드포인트
     * @param method:	HTTP 메서드 (예: "GET", "POST", "PUT" ...)
     * @param jsonBody:	jsonBody는 request가 json문자열 형태로 변환된 값입니다. 
     * @return 서버가 보내온 response를 utf-8로 인코딩하고 다시 json문자열 형태로 가공해서 return한 값입니다.
     * 			return값은 json문자열 형태입니다. 객체로 변환하고 싶다면 return값을 반드시 각 객체에 맞게 파싱해서 사용해야합니다!!!
     */
    public static String sendRequest(String apiKey, String endPoint, String method, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            // 1. URL & 연결 생성
            URL url = new URL(endPoint);
            conn = (HttpURLConnection) url.openConnection();

            // 2. HTTP 메서드 및 옵션 설정
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            
            // 3. 헤더 설정
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            
            // 4. request에 바디가 존재하는지 판단
            boolean hasRequestBody =
                    jsonBody != null && !jsonBody.isEmpty()
                    && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method));
            if (hasRequestBody) {
                conn.setDoOutput(true);	// request body 존재
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input);
                }
            } else {
                conn.setDoOutput(false); // request body 존재
            }

            // 5. 응답 코드 및 스트림 선택
            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            // 6. 응답 바디를 문자열로 읽어서 반환
            return readAll(is);

        } catch (IOException e) {
            System.out.println("REST 요청 중 통신 오류가 발생했습니다.");
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // InputStream 전체를 문자열로 읽는 유틸 메서드
    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br =
                     new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
