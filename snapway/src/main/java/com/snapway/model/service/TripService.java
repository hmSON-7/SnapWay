package com.snapway.model.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.snapway.model.dto.PhotoMetadata;
import com.snapway.model.dto.Trip;

public interface TripService {

    /**
     * AI를 이용해 사진들로부터 여행 기록을 자동 생성하고 저장합니다.
     * * @param memberId 작성자 ID
     * @param title 여행 제목
     * @param files 업로드된 사진 파일 리스트
     * @return 생성된 여행 정보(Trip) - ID 포함
     */
    Trip createAutoTrip(int memberId, String title, List<MultipartFile> files) throws Exception;
    
}