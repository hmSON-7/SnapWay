package com.snapway.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripHashtag {
	
	private Long id;
	private int tripId;
	private TravelStyle style;
	
}
