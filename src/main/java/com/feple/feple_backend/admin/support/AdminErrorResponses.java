package com.feple.feple_backend.admin.support;

import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.global.exception.ErrorResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/** 크롤/OCR 관리자 컨트롤러들(WebScrapeAdminController, TimetableOcrAdminController,
 * ArtistLineupOcrAdminController)이 서로 다른 하위 패키지(admin.scraper, admin.ocr)에
 * 있어 공개 접근이 필요한 공유 에러 응답 헬퍼. */
public final class AdminErrorResponses {

    private AdminErrorResponses() {}

    public static ResponseEntity<ErrorResponse> badRequest(String error) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, error, ErrorCode.ILLEGAL_ARGUMENT));
    }

    public static ResponseEntity<ErrorResponse> serverError(String error) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, error, ErrorCode.SERVER_ERROR));
    }

    // 포맷 판별에 필요한 헤더 바이트만 읽어 대용량 파일 전체를 메모리에 올리지 않는다 —
    // JPEG/PNG/GIF/BMP/WEBP/HEIC 등 지원 포맷은 시그니처가 모두 처음 수십 바이트 안에 있다.
    private static final int HEADER_PROBE_BYTES = 64;

    // ImageIO는 JVM 기본 리더(JPEG/PNG/GIF/BMP) + webp-imageio 플러그인만 인식해 HEIC/HEIF/AVIF
    // (iPhone 카메라·최신 브라우저가 기본으로 만들어내는 포맷)는 디코딩하지 못한다. 반면 Gemini
    // API는 이 포맷들을 그대로 받아들이므로, ImageIO가 모르는 포맷이어도 ISO-BMFF 컨테이너의
    // ftyp 박스 브랜드를 별도로 확인해 정상적인 업로드를 거부하지 않는다.
    private static final Set<String> ISOBMFF_IMAGE_BRANDS = Set.of(
            "heic", "heix", "heim", "heis", "hevc", "hevx", "hevm", "hevs", // HEIC/HEIF
            "mif1", "msf1", // 범용 HEIF
            "avif", "avis"  // AVIF
    );

    // OCR 컨트롤러들이 업로드받은 파일을 그대로 base64 인코딩해 Gemini API에 전달하므로,
    // 이미지가 아닌 파일(PDF·실행파일 등)이 전송되는 것을 막아야 한다. Content-Type 헤더는
    // 클라이언트가 임의로 조작할 수 있을 뿐 아니라, HEIC처럼 브라우저/OS에 따라 아예
    // application/octet-stream 등 엉뚱한 값으로 오기도 해 신뢰할 수 없다 — 그래서 Content-Type은
    // 보지 않고, ImageResizeService.resizeToJpeg()가 디코딩 단계에서 ImageIO 리더를 못 찾으면
    // 실패 처리하는 것과 같은 방식으로 실제 바이트 시그니처만으로 판단한다(ImageResizeService의
    // validateFile()은 확장자만 검사할 뿐, 바이트 시그니처 검증은 아니다).
    // 파일 읽기 실패(IOException)는 여기서 삼키지 않고 호출부의 try/catch로 그대로 흘려보낸다 —
    // "파일 디코딩·읽기 실패는 500"이라는 프로젝트 컨벤션을 따르기 위함(호출부 참고).
    public static boolean isNotImage(MultipartFile image) throws IOException {
        byte[] header;
        try (InputStream in = image.getInputStream()) {
            header = in.readNBytes(HEADER_PROBE_BYTES);
        }
        if (isIsoBmffImage(header)) {
            return false;
        }
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(header))) {
            return iis == null || !ImageIO.getImageReaders(iis).hasNext();
        } catch (RuntimeException | LinkageError e) {
            // 손상되거나 악의적으로 조작된 바이트가 ImageReaderSpi의 포맷 판별 로직에서 예기치
            // 않은 예외를 유발할 수 있다. 특히 webp-imageio는 네이티브 라이브러리를 로드하는
            // 서드파티 플러그인이라 플랫폼에 따라 UnsatisfiedLinkError(LinkageError) 위험이
            // 있다(macOS arm64 로컬 환경에서 실제로 겪은 이력 있음). 어느 쪽이든 유효한 이미지가
            // 아니라는 신호이므로 서버 오류가 아니라 검증 실패로 처리한다.
            return true;
        }
    }

    private static boolean isIsoBmffImage(byte[] header) {
        if (header.length < 12 || header[4] != 'f' || header[5] != 't' || header[6] != 'y' || header[7] != 'p') {
            return false;
        }
        for (int offset = 8; offset + 4 <= header.length; offset += 4) {
            String brand = new String(header, offset, 4, StandardCharsets.US_ASCII);
            if (ISOBMFF_IMAGE_BRANDS.contains(brand)) return true;
        }
        return false;
    }

    public static ResponseEntity<ErrorResponse> geminiNotConfigured() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE,
                        "Gemini API 키가 설정되지 않았습니다. application-local.yaml에 app.gemini.api-key를 설정하세요.",
                        ErrorCode.SERVICE_UNAVAILABLE));
    }
}
