# 🚀 Interactive Geo-Spatial Map UDC (Leaflet-based)

이 프로젝트는 **Leaflet.js**를 기반으로 구축된 eXBuilder6 전용 **UDC(User Defined Control)**입니다. 공공데이터 및 내부 API의 GeoJSON 데이터를 활용하여 동적인 행정구역 시각화, 단계별 드릴다운, 그리고 실시간 지도 드로잉 기능을 제공합니다.

## 🛠 Tech Stack
* **Library:** Leaflet.js (v1.9.4)
* **Plugins:** Leaflet.markercluster (마커 그룹화 및 성능 최적화)
* **Data Format:** GeoJSON (행정구역 경계), JSON (비즈니스 데이터)
* **Platform:** eXBuilder6 UI Framework

## 🌟 Key Technical Features

### 1. 비동기 리소스 체이닝 (Asynchronous Resource Loading)
단순한 스크립트 태그 삽입 대신 `cpr.core.ResourceLoader`를 사용하여 의존성 있는 라이브러리들을 순차적으로 로드합니다.
* **프로세스:** Leaflet 코어 로드 ➔ MarkerCluster 플러그인 로드 ➔ CSS 스타일 로드 ➔ 지도 초기화.
* **장점:** 필요한 시점에만 리소스를 로드하여 초기 로딩 속도를 개선하고 라이브러리 간 실행 순서를 보장합니다.

### 2. 고수준 드릴다운 로직 (Multi-level Drill-down)
대한민국의 행정 체계에 맞춘 **시도 ➔ 시군구 ➔ 읍면동** 3단계 드릴다운을 구현했습니다.
* **상태 유지:** `mapConfig` 컴포넌트에 현재의 관리 레벨(`adminLevel`)과 부모 코드 정보를 저장하여 데이터 일관성을 유지합니다.
* **내비게이션:** 하위 레벨 진입 시 `addBackButton` 함수를 통해 동적으로 '뒤로가기' 컨트롤을 생성하여 사용자 경험을 향상시킵니다.
* **최적화:** `fitBounds`를 사용하여 지역 이동 시 해당 구역의 경계값에 맞춰 지도의 줌 레벨과 중심점을 자동 조정합니다.

### 3. 단계구분도 시각화 (Choropleth Mapping)
비즈니스 수치 데이터(`dataValues`)를 기반으로 지도 영역의 색상을 동적으로 할당합니다.
* **색상 알고리즘:** `minValue`와 `maxValue`를 계산하여 5단계 색상 스키마(Blue, Red, Green, Purple)에 매핑하는 선형 보간 로직을 적용했습니다.
* **인터랙션:** 마우스 오버 시 `hoverColors`를 적용하고, `bringToFront()`를 호출하여 선택된 영역의 경계선을 강조합니다.

### 4. 인터랙티브 드로잉 및 편집 시스템
지도 위에 직접 객체를 생성하고 편집할 수 있는 사용자 도구 모음을 포함합니다.
* **Drawing Mode:** `drawState` 전역 객체로 상태를 관리하며 마커, 경로(Polyline), 커스텀 이미지, 정보 패널, 스파크 차트 기능을 지원합니다.
* **Event Interception:** 드로잉 모드 활성화 시 기존 지역 클릭 이벤트를 차단하고 `handleMapClick`으로 제어권을 전환하여 정교한 객체 생성이 가능합니다.
* **Custom UI:** `L.divIcon`과 SVG를 결합하여 실시간 데이터 추이를 보여주는 **스파크 차트(Sparkline)**를 지도 좌표 위에 렌더링합니다.

### 5. 실시간 스타일 에디터 (Popup Editor)
사용자가 팝업창을 통해 직접 지도 요소의 스타일을 변경할 수 있는 기능을 제공합니다.
* **편집 항목:** 지역 색상, 툴팁 배경 및 글자색, 툴팁 텍스트 내용 등.
* **데이터 바인딩:** 변경된 값은 `Feature`의 `_state` 객체에 즉시 반영되며, `layer.bindTooltip`을 재실행하여 실시간으로 반영됩니다.

## 🏗 Logical Flow
1. **Load:** `onShl1Load`에서 라이브러리 로드 및 API 엔드포인트 설정.
2. **Fetch:** `loadMapData`를 호출하여 GeoJSON 및 수치 데이터 수신.
3. **Render:** `renderGeoJson`에서 스타일 적용 및 이벤트(Click, Hover) 바인딩.
4. **Interact:** 사용자의 드릴다운 클릭 혹은 툴바를 통한 객체 생성 처리.

---
**Tip:** 이 UDC를 활용하면 별도의 GIS 엔진 없이도 강력한 데이터 시각화 대시보드를 구축할 수 있습니다.
