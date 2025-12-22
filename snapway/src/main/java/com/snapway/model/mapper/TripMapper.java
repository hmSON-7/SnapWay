package com.snapway.model.mapper;

import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.snapway.model.dto.Trip;
import com.snapway.model.dto.TripRecord;
import com.snapway.model.dto.TripPhoto;

@Mapper
public interface TripMapper {
    
    // --- 1. Trip (여행 폴더) ---
    // 여행 생성 (tripId 반환됨)
    int insertTrip(Trip trip) throws SQLException;
    
    // 내 여행 목록 조회
    List<Trip> selectTripListByMemberId(int memberId) throws SQLException;
    
    // 여행 상세 조회
    Trip selectTripById(int tripId) throws SQLException;
    

    // --- 2. TripRecord (개별 기록/마커) ---
    // 기록 생성 (recordId 반환됨)
    int insertTripRecord(TripRecord tripRecord) throws SQLException;
    
    // 특정 여행의 모든 기록 조회 (지도에 뿌릴 데이터)
    List<TripRecord> selectRecordsByTripId(int tripId) throws SQLException;
    

    // --- 3. TripPhoto (사진 파일) ---
    // 사진 저장
    int insertTripPhoto(TripPhoto tripPhoto) throws SQLException;
    
    // 특정 기록에 포함된 사진 조회
    List<TripPhoto> selectPhotosByRecordId(int recordId) throws SQLException;
}