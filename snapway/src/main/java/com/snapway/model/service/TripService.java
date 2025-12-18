package com.snapway.model.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.snapway.model.dto.PhotoMetadata;

public interface TripService {
    
    List<PhotoMetadata> extractMetadata(List<MultipartFile> files) throws Exception;

}