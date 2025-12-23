package com.snapway.model.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import com.snapway.model.dto.Trip;
import com.snapway.model.dto.TripHashtag;
import com.snapway.model.dto.TripRecord;
import com.snapway.model.dto.TripPhoto;

@Mapper
public interface TripMapper {
    
    // --- 1. Trip (여행 폴더) ---
    // 여행 생성
    int insertTrip(Trip trip) throws SQLException;

    // 여행 정보 수정 (제목, 날짜, 공개여부 등)
    int updateTrip(Trip trip) throws SQLException;
    
    // 여행 삭제
    int deleteTrip(int tripId) throws SQLException;
    
    // 내 여행 목록 조회
    List<Trip> selectTripListByMemberId(int memberId) throws SQLException;
    
    // 여행 상세 조회
    Trip selectTripById(int tripId) throws SQLException;
    
    // [메인 피드] 모든 공개 여행 목록 조회 (검색 기능 포함 가능)
    // Map 파라미터 예시: { "keyword": "부산", "sort": "date" }
    List<Trip> selectAllPublicTrips(Map<String, Object> params) throws SQLException;
    
    // 해시태그 저장 메서드
    int insertTripHashtag(TripHashtag tripHashtag) throws SQLException;


    // --- 2. TripRecord (개별 기록/마커) ---
    // 기록 생성
    int insertTripRecord(TripRecord tripRecord) throws SQLException;
    
    // 기록 수정 (내용, 장소 등)
    int updateTripRecord(TripRecord tripRecord) throws SQLException;
    
    // 기록 삭제
    int deleteTripRecord(int recordId) throws SQLException;
    
    // 특정 여행의 모든 기록 조회
    List<TripRecord> selectRecordsByTripId(int tripId) throws SQLException;
    
    // 특정 기록 단건 조회 (수정 폼 불러오기 용)
    TripRecord selectRecordById(int recordId) throws SQLException;
    

    // --- 3. TripPhoto (사진 파일) ---
    // 사진 저장
    int insertTripPhoto(TripPhoto tripPhoto) throws SQLException;
    
    // 사진 삭제 (개별 삭제)
    int deleteTripPhoto(int photoCode) throws SQLException;
    
    // 특정 기록의 사진 모두 삭제 (기록 삭제 시 연쇄 삭제용)
    int deletePhotosByRecordId(int recordId) throws SQLException;
    
    // 특정 기록에 포함된 사진 조회
    List<TripPhoto> selectPhotosByRecordId(int recordId) throws SQLException;
}