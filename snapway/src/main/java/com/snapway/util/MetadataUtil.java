package com.snapway.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.snapway.model.dto.PhotoMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MetadataUtil {
	
	/**
	 * 파일과 추출된 메타데이터를 함께 다루기 위한 래퍼 클래스
	 */
	@Getter
	@Builder
	@AllArgsConstructor
	public static class PhotoWithFile {
		private MultipartFile file;
		private PhotoMetadata metadata;
	}

	/**
	 * 여러 장의 사진을 받아 메타데이터를 추출하고, 촬영 시간 순(오름차순)으로 정렬하여 반환
	 */
	public List<PhotoWithFile> extractAndSort(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return new ArrayList<>();
		}

		return files.stream()
				.map(file -> PhotoWithFile.builder()
						.file(file)
						.metadata(extractMetadata(file))
						.build())
				.sorted(Comparator.comparing(
						pwf -> pwf.getMetadata().getTakenAt(),
						Comparator.nullsLast(Comparator.naturalOrder()) // 날짜 없는 경우 맨 뒤로
				))
				.collect(Collectors.toList());
	}

	/**
	 * MultipartFile로부터 메타데이터(촬영 일시 및 GPS) 추출
	 */
	public PhotoMetadata extractMetadata(MultipartFile multipartFile) {
		try(InputStream inputStream = multipartFile.getInputStream()) {
			Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
			return parseMetadata(metadata);
		} catch (IOException | ImageProcessingException e) {
			log.warn("메타데이터 추출 실패 (파일명: {}): {}", multipartFile.getOriginalFilename(), e.getMessage());
			return new PhotoMetadata();
		}
	}
	
	public PhotoMetadata extractMetadata(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            return parseMetadata(metadata);
        } catch (IOException | ImageProcessingException e) {
            log.warn("메타데이터 추출 실패 (파일경로: {}): {}", file.getPath(), e.getMessage());
            return new PhotoMetadata();
        }
    }
	
	private PhotoMetadata parseMetadata(Metadata metadata) {
		PhotoMetadata result = new PhotoMetadata();
		
		// 1. 촬영 일시 추출(Exif SubIFD 디렉토리 확인)
		ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
		if(exifDirectory != null) {
			Date date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
			if(date != null) {
				result.setTakenAt(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
			}
		}
		
		if(result.getTakenAt() == null) {
			log.debug("촬영 일시 정보를 찾을 수 없습니다.");
		}
		
		// 2. GPS 정보 추출
		GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
		if(gpsDirectory != null) {
			GeoLocation geoLocation = gpsDirectory.getGeoLocation();
			if(geoLocation != null && !geoLocation.isZero()) {
				result.setLatitude(geoLocation.getLatitude());
				result.setLongitude(geoLocation.getLongitude());
			}
		}
		
		return result;
	}
	
}
