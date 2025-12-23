<script setup>
import { ref, onMounted, watch } from 'vue';
import { VITE_KEY_DATA } from '@/api/key';

// =========================================
// 1. 상태 관리
// =========================================
const sidos = ref([]);
const guguns = ref([]);
const selectedSido = ref('');
const selectedGugun = ref('');
const selectedContentType = ref('');

const map = ref(null);
const markers = ref([]);
const infowindows = ref([]);

// key.js에서 가져온 키 사용
const SERVICE_KEY = VITE_KEY_DATA || '';

// =========================================
// 2. 카카오맵 초기화
// =========================================
onMounted(() => {
  if (window.kakao && window.kakao.maps) {
    initMap();
  } else {
    console.error('카카오맵 스크립트를 찾을 수 없습니다. index.html 설정을 확인하세요.');
  }
  // 키가 있을 때만 요청
  if (SERVICE_KEY) {
      getSido();
  }
});

const initMap = () => {
  const container = document.getElementById('map');
  const options = {
    center: new window.kakao.maps.LatLng(37.566826, 126.9786567),
    level: 7,
  };
  map.value = new window.kakao.maps.Map(container, options);
};

// =========================================
// 3. 데이터 Fetching
// =========================================
const getFetch = async (url, params) => {
  const { serviceKey, ...restParams } = params;
  const queryString = new URLSearchParams(restParams).toString();
  const finalUrl = `${url}?serviceKey=${serviceKey}&${queryString}`;

  try {
    const response = await fetch(finalUrl);

    // [디버깅] 응답 내용 확인
    const textData = await response.text();
    
    if (!response.ok) {
      // 404, 500 등 HTTP 에러 발생 시
      throw new Error(`HTTP Error: ${response.status}`);
    }

    try {
        const data = JSON.parse(textData);
        // 공공데이터포털 에러 코드 확인
        if (data.response?.header?.resultCode && data.response.header.resultCode !== '0000') {
             console.error(`API Error: ${data.response.header.resultMsg}`);
             return [];
        }
        return data.response?.body?.items?.item || [];
    } catch (e) {
        // XML 에러 메시지(SERVICE_KEY_IS_NOT_REGISTERED 등)가 올 경우
        console.error("JSON 파싱 실패 (API 키 에러 가능성):", textData);
        return [];
    }
  } catch (error) {
    console.error('API Fetch Error:', error);
    return [];
  }
};

const getSido = async () => {
  const params = {
    serviceKey: SERVICE_KEY,
    numOfRows: 100,
    pageNo: 1,
    MobileOS: 'ETC',
    MobileApp: 'SnapWay',
    _type: 'json',
  };
  
  // 🔴 [핵심 수정] 주소 문제 해결
  // 1. https://... 로 직접 호출하면 CORS 에러 발생
  // 2. apis.data.go.kr/... (프로토콜 누락)시 localhost 호출 에러 발생
  // 3. 따라서 vite.config.js에 설정된 프록시 별칭 '/api'를 사용해야 함
  const baseUrl = '/api/B551011/KorService2'; 
  
  sidos.value = await getFetch(`${baseUrl}/areaCode2`, params);
};

const getGugun = async (areaCode) => {
  if (!areaCode) {
    guguns.value = [];
    return;
  }
  const params = {
    serviceKey: SERVICE_KEY,
    numOfRows: 100,
    pageNo: 1,
    MobileOS: 'ETC',
    MobileApp: 'SnapWay',
    _type: 'json',
    areaCode: areaCode,
  };
  
  // [수정] 프록시 경로 사용
  const baseUrl = '/api/B551011/KorService2';
  guguns.value = await getFetch(`${baseUrl}/areaCode2`, params);
};

watch(selectedSido, (newVal) => {
  selectedGugun.value = '';
  if (newVal) getGugun(newVal);
});

// =========================================
// 4. 검색 및 마커 표시
// =========================================
const searchAttractions = async () => {
  if (!SERVICE_KEY) {
    alert('서비스 키를 찾을 수 없습니다. src/api/key.js 파일을 확인하세요.');
    return;
  }

  if (!selectedSido.value) {
      alert("지역(시/도)을 선택해주세요.");
      return;
  }

  const params = {
    serviceKey: SERVICE_KEY,
    numOfRows: 100,
    pageNo: 1,
    MobileOS: 'ETC',
    MobileApp: 'SnapWay',
    _type: 'json',
    arrange: 'A',
    areaCode: selectedSido.value,
    sigunguCode: selectedGugun.value,
    contentTypeId: selectedContentType.value,
  };

  // [수정] 프록시 경로 사용
  const baseUrl = '/api/B551011/KorService2';
  
  const items = await getFetch(`${baseUrl}/areaBasedList2`, params);

  if (!items || items.length === 0) {
    alert('검색 결과가 없습니다.');
    return;
  }
  displayMarkers(items);
};

const displayMarkers = (positions) => {
  if (markers.value.length > 0) {
    markers.value.forEach((marker) => marker.setMap(null));
    markers.value = [];
  }
  infowindows.value.forEach((iw) => iw.close());
  infowindows.value = [];

  const bounds = new window.kakao.maps.LatLngBounds();

  positions.forEach((position) => {
    if (!position.mapx || !position.mapy) return;

    const latlng = new window.kakao.maps.LatLng(position.mapy, position.mapx);
    const marker = new window.kakao.maps.Marker({
      map: map.value,
      position: latlng,
      title: position.title,
    });

    markers.value.push(marker);
    bounds.extend(latlng);

    const iwContent = `
      <div class="iw-wrap" style="padding:10px; min-width:200px; max-width:300px; color:#333; font-family:'Pretendard', sans-serif;">
        <div style="font-weight:bold; margin-bottom:5px; font-size:14px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${position.title}</div>
        <div style="display:flex; gap:10px;">
            <div style="flex-shrink:0; width:70px; height:70px; border-radius:8px; overflow:hidden; background:#eee;">
                 <img src="${position.firstimage || position.firstimage2 || 'https://via.placeholder.com/70?text=No+Image'}" 
                      style="width:100%; height:100%; object-fit:cover;" alt="img">
            </div>
            <div style="flex-grow:1; font-size:12px; line-height:1.4;">
                <div style="margin-bottom:2px; color:#666;">${position.addr1 || '주소 정보 없음'}</div>
                <div style="color:#888;">${position.tel || ''}</div>
            </div>
        </div>
      </div>
    `;

    const infowindow = new window.kakao.maps.InfoWindow({
      content: iwContent,
      removable: true,
      zIndex: 10,
    });

    window.kakao.maps.event.addListener(marker, 'click', () => {
      infowindows.value.forEach((iw) => iw.close());
      infowindow.open(map.value, marker);
      infowindows.value.push(infowindow);
    });
  });

  map.value.setBounds(bounds);
};
</script>

<!-- template 및 style은 기존과 동일 -->
<template>
  <div class="map-page">
    <div class="map-container">
      <div class="control-panel">
        <h2 class="panel-title">여행지 탐색</h2>
        <div class="filter-group">
            <div class="select-wrapper">
                <label>지역</label>
                <select v-model="selectedSido" class="custom-select">
                    <option value="">시/도 선택</option>
                    <option v-for="sido in sidos" :key="sido.code" :value="sido.code">{{ sido.name }}</option>
                </select>
            </div>
            <div class="select-wrapper">
                <label>구군</label>
                <select v-model="selectedGugun" class="custom-select">
                    <option value="">구/군 선택</option>
                    <option v-for="gugun in guguns" :key="gugun.code" :value="gugun.code">{{ gugun.name }}</option>
                </select>
            </div>
            <div class="select-wrapper">
                <label>유형</label>
                <select v-model="selectedContentType" class="custom-select">
                    <option value="">모든 유형</option>
                    <option value="12">관광지</option>
                    <option value="14">문화시설</option>
                    <option value="15">축제/공연</option>
                    <option value="25">여행코스</option>
                    <option value="28">레포츠</option>
                    <option value="32">숙박</option>
                    <option value="38">쇼핑</option>
                    <option value="39">음식점</option>
                </select>
            </div>
            <button class="search-btn" @click="searchAttractions">
                검색하기
            </button>
        </div>
      </div>
      <div id="map" class="kakao-map"></div>
    </div>
  </div>
</template>

<style scoped>
/* 기존 스타일 그대로 유지 */
.map-page {
  min-height: calc(100vh - 80px);
  display: flex;
  justify-content: center;
  padding: 20px;
  background: radial-gradient(circle at top left, #0f172a 0, #020617 45%, #000000 100%);
  color: #e5e7eb;
}
.map-container {
  width: 100%;
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.control-panel {
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  padding: 20px 24px;
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}
.panel-title {
    font-size: 1.2rem;
    font-weight: 700;
    margin-bottom: 16px;
    color: #e5f0ff;
}
.filter-group {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    align-items: flex-end;
}
.select-wrapper {
    display: flex;
    flex-direction: column;
    gap: 6px;
    flex: 1;
    min-width: 140px;
}
.select-wrapper label {
    font-size: 0.85rem;
    color: #94a3b8;
    font-weight: 600;
}
.custom-select {
    width: 100%;
    padding: 10px 12px;
    border-radius: 8px;
    border: 1px solid rgba(148, 163, 184, 0.4);
    background: rgba(30, 41, 59, 0.8);
    color: #e5e7eb;
    font-size: 0.95rem;
    outline: none;
    transition: all 0.2s;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2394a3b8'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 10px center;
    background-size: 16px;
}
.custom-select:focus {
    border-color: #38bdf8;
    box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2);
}
.search-btn {
    padding: 10px 24px;
    height: 44px;
    border-radius: 8px;
    border: none;
    background: linear-gradient(135deg, #38bdf8, #2563eb);
    color: white;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
}
.search-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(37, 99, 235, 0.5);
}
.search-btn:active {
    transform: translateY(0);
}
.kakao-map {
    width: 100%;
    height: 600px;
    border-radius: 16px;
    border: 1px solid rgba(148, 163, 184, 0.2);
    overflow: hidden;
    position: relative;
    background-color: #1e293b;
}
@media (max-width: 768px) {
    .filter-group {
        flex-direction: column;
        align-items: stretch;
    }
    .search-btn {
        width: 100%;
        margin-top: 8px;
    }
    .kakao-map {
        height: 400px;
    }
}
</style>
