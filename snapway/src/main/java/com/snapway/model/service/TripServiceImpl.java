package com.snapway.model.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.snapway.model.dto.PhotoMetadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

	/**
     * 업로드된 사진들로부터 메타데이터(위치, 시간) 추출
     * @param files 업로드된 파일 리스트
     * @return 추출된 메타데이터 리스트
     */
    @Override
    public List<PhotoMetadata> extractMetadata(List<MultipartFile> files) throws Exception {
        List<PhotoMetadata> metadataList = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try (InputStream inputStream = file.getInputStream()) {
                // 1. 파일 스트림에서 메타데이터 읽기
                Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
                
                // 2. 초기값 설정
                Double latitude = null;
                Double longitude = null;
                LocalDateTime dateTaken = LocalDateTime.now(); // 없을 경우 현재 시간
                boolean hasLocation = false;

                // 3. GPS 정보 추출
                GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                if (gpsDirectory != null) {
                    GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                    if (geoLocation != null) {
                        latitude = geoLocation.getLatitude();
                        longitude = geoLocation.getLongitude();
                        hasLocation = true;
                    }
                }

                // 4. 촬영 시간 추출 (ExifSubIFDDirectory)
                ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                if (exifDirectory != null) {
                    Date date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                    if (date != null) {
                        dateTaken = date.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
                    }
                }

                // 5. 결과 리스트에 추가
                PhotoMetadata meta = PhotoMetadata.builder()
                        .originalFileName(file.getOriginalFilename())
                        .latitude(latitude)
                        .longitude(longitude)
                        .dateTaken(dateTaken)
                        .hasLocation(hasLocation)
                        .build();
                
                metadataList.add(meta);
                
                log.debug("Extracted Metadata - File: {}, Lat: {}, Lon: {}, Date: {}", 
                        file.getOriginalFilename(), latitude, longitude, dateTaken);

            } catch (IOException | com.drew.imaging.ImageProcessingException e) {
                log.error("메타데이터 추출 실패: {}", file.getOriginalFilename(), e);
                // 실패하더라도 에러를 던지지 않고 기본값(위치 없음)으로 리스트에 포함
                metadataList.add(PhotoMetadata.builder()
                        .originalFileName(file.getOriginalFilename())
                        .dateTaken(LocalDateTime.now())
                        .hasLocation(false)
                        .build());
            }
        }

        return metadataList;
    }
}