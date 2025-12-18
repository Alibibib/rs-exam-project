package rs.examproject.file_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.examproject.file_service.model.FileMetadata;

import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findByObjectKey(String objectKey);
}
