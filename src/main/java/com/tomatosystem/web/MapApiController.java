package com.tomatosystem.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/map")
@CrossOrigin(origins = "*")
public class MapApiController {

    /* ==================== 기본 설정 ==================== */

    private static final String API_KEY = "";
    private static final String VWORLD_WFS_URL = "https://api.vworld.kr/req/wfs";
    private static final String DOMAIN = "http://localhost:8080/ui/sample-map.clx";

    private static final String OUTPUT_FORMAT = "application/json";
    private static final String SRS_NAME = "EPSG:4326";
    private static final String WFS_VERSION = "1.1.0";

    private static final String LAYER_SIDO = "lt_c_adsido_info";
    private static final String LAYER_SIGUNGU = "lt_c_adsigg_info";
    private static final String LAYER_DONG = "lt_c_ademd_info";

    private static final String JSON_OUTPUT_PATH =
            "C:\\eb6-work\\workspace\\mapChart-sample\\clx-src\\data\\";

    private static final String SIDO_JSON_FILENAME = "sido_data.json";
    private static final String SIGUNGU_JSON_FILENAME = "sigungu_data.json";
    private static final String DONG_JSON_FILENAME = "dong_data.json";

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    /* ==================== 시도 ==================== */

    @GetMapping("/sido.do")
    public ResponseEntity<?> getSidoGeoJson() {
        try {
            String requestUrl = buildWfsUrl(LAYER_SIDO, null);
            logRequest("시도", requestUrl);

            String response = callApi(requestUrl);
            validateResponse(response);

            saveJsonToFile(response, SIDO_JSON_FILENAME);
            return buildSuccessResponse(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "시도 조회 실패: " + e.getMessage());
        }
    }

    /* ==================== 시군구 (시도 기준 필터) ==================== */

    @GetMapping("/sigungu.do")
    public ResponseEntity<?> getSigunguGeoJson(@RequestParam String sido) {
        try {
            // ex) 11
            String cleanSido = sido.replaceAll("[^0-9]", "");
            String filter = buildLikeFilter("sig_cd", cleanSido + "*");

            String requestUrl = buildWfsUrl(LAYER_SIGUNGU, filter);
            logRequest("시군구", requestUrl);

            String response = callApi(requestUrl);
            validateResponse(response);

            saveJsonToFile(response, SIGUNGU_JSON_FILENAME);
            return buildSuccessResponse(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "시군구 조회 실패: " + e.getMessage());
        }
    }

    /* ==================== 동 (시군구 / 동 필터) ==================== */

    @GetMapping("/dong.do")
    public ResponseEntity<?> getDongGeoJson(
            @RequestParam String sigungu,
            @RequestParam(required = false) String dong) {

        try {
            String cleanSigungu = sigungu.replaceAll("[^0-9]", "");
            String filter;

            if (dong != null && !dong.isEmpty()) {
                // 특정 동 (ex: 11500510)
                String cleanDong = dong.replaceAll("[^0-9]", "");
                filter = buildEqualFilter("emd_cd", cleanDong);
            } else {
                // 시군구에 속한 동 전체
                filter = buildLikeFilter("emd_cd", cleanSigungu + "*");
            }

            String requestUrl = buildWfsUrl(LAYER_DONG, filter);
            logRequest("읍면동", requestUrl);

            String response = callApi(requestUrl);
            validateResponse(response);

            saveJsonToFile(response, DONG_JSON_FILENAME);
            return buildSuccessResponse(response);

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "동 조회 실패: " + e.getMessage());
        }
    }

    /* ==================== WFS URL 생성 ==================== */

    private String buildWfsUrl(String layerName, String ogcFilter) throws Exception {
        StringBuilder url = new StringBuilder();
        url.append(VWORLD_WFS_URL);
        url.append("?key=").append(API_KEY);
        url.append("&domain=").append(URLEncoder.encode(DOMAIN, "UTF-8"));
        url.append("&service=WFS");
        url.append("&version=").append(WFS_VERSION);
        url.append("&request=GetFeature");
        url.append("&typeName=").append(layerName);
        url.append("&outputFormat=").append(OUTPUT_FORMAT);
        url.append("&srsName=").append(SRS_NAME);

        if (ogcFilter != null && !ogcFilter.isEmpty()) {
            url.append("&FILTER=").append(URLEncoder.encode(ogcFilter, "UTF-8"));
        }

        return url.toString();
    }

    /* ==================== OGC FILTER 생성 ==================== */

    private String buildLikeFilter(String property, String valueWithWildcard) {
        return "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">" +
               "<ogc:PropertyIsLike wildCard=\"*\" singleChar=\"_\" escapeChar=\"\\\">" +
               "<ogc:PropertyName>" + property + "</ogc:PropertyName>" +
               "<ogc:Literal>" + valueWithWildcard + "</ogc:Literal>" +
               "</ogc:PropertyIsLike>" +
               "</ogc:Filter>";
    }

    private String buildEqualFilter(String property, String value) {
        return "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">" +
               "<ogc:PropertyIsEqualTo>" +
               "<ogc:PropertyName>" + property + "</ogc:PropertyName>" +
               "<ogc:Literal>" + value + "</ogc:Literal>" +
               "</ogc:PropertyIsEqualTo>" +
               "</ogc:Filter>";
    }

    /* ==================== API 호출 ==================== */

    private String callApi(String requestUrl) throws Exception {
        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        int code = conn.getResponseCode();
        BufferedReader br = (code < 400)
                ? new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                : new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        conn.disconnect();
        return sb.toString();
    }

    /* ==================== 응답 검증 ==================== */

    private void validateResponse(String response) throws Exception {
        if (response.startsWith("<") || response.contains("<!DOCTYPE")) {
            throw new Exception("VWorld API 오류 또는 HTML 응답");
        }
        new JSONObject(response); // JSON 유효성 체크
    }

    /* ==================== 파일 저장 ==================== */

    private void saveJsonToFile(String json, String filename) throws Exception {
        File dir = new File(JSON_OUTPUT_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Files.write(
                Paths.get(JSON_OUTPUT_PATH + filename),
                new JSONObject(json).toString(2).getBytes(StandardCharsets.UTF_8)
        );
    }

    /* ==================== 공통 Response ==================== */

    private ResponseEntity<?> buildSuccessResponse(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private ResponseEntity<?> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"" + message + "\"}");
    }

    private void logRequest(String type, String url) {
        System.out.println("[" + type + "] 요청 URL");
        System.out.println(url);
    }
}
