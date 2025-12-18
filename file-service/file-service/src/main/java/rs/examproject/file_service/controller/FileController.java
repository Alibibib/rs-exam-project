package rs.examproject.file_service.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.examproject.file_service.dto.FileMetadataResponse;
import rs.examproject.file_service.service.FileStorageService;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileStorageService storageService;

    public FileController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileMetadataResponse upload(@RequestPart("file") MultipartFile file,
                                       @AuthenticationPrincipal Jwt jwt) {
        String uploader = jwt != null ? jwt.getClaimAsString("preferred_username") : "anonymous";
        return storageService.store(file, uploader);
    }

    @GetMapping
    public List<FileMetadataResponse> list() {
        return storageService.list();
    }

    @GetMapping("/{id}")
    public FileMetadataResponse metadata(@PathVariable long id) {
        return storageService.getMetadata(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable long id) {
        var download = storageService.download(id);
        var headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", download.meta().getOriginalFilename());
        headers.setContentLength(download.meta().getSize());
        headers.setContentType(MediaType.parseMediaType(
                download.meta().getContentType() != null
                        ? download.meta().getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(download.stream()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        storageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
