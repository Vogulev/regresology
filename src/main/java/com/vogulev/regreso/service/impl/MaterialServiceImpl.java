package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.dto.request.MaterialRequest;
import com.vogulev.regreso.dto.response.MaterialListItemResponse;
import com.vogulev.regreso.dto.response.MaterialResponse;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.PractitionerMaterial;
import com.vogulev.regreso.exception.BusinessException;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.mapper.MaterialMapper;
import com.vogulev.regreso.repository.MaterialRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.service.FileStorageService;
import com.vogulev.regreso.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final PractitionerRepository practitionerRepository;
    private final MaterialMapper materialMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<MaterialListItemResponse> getMaterials(UUID practitionerId, boolean includeArchived) {
        List<PractitionerMaterial> materials = includeArchived
                ? materialRepository.findByPractitionerIdOrderByCreatedAtDesc(practitionerId)
                : materialRepository.findByPractitionerIdAndIsArchivedFalseOrderByCreatedAtDesc(practitionerId);
        return materials.stream()
                .map(materialMapper::toListItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MaterialResponse createMaterial(MaterialRequest request, MultipartFile file, UUID practitionerId) throws IOException {
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));
        validateFile(file);

        PractitionerMaterial material = PractitionerMaterial.builder()
                .practitioner(practitioner)
                .title(request.getTitle())
                .description(request.getDescription())
                .materialType(request.getMaterialType())
                .content(request.getContent())
                .build();
        attachFile(material, file);

        return materialMapper.toResponse(materialRepository.save(material));
    }

    @Override
    public MaterialResponse updateMaterial(UUID id, MaterialRequest request, MultipartFile file, UUID practitionerId) throws IOException {
        PractitionerMaterial material = findOrThrow(id, practitionerId);
        validateFile(file);
        String oldFileUrl = material.getFileUrl();

        material.setTitle(request.getTitle());
        material.setDescription(request.getDescription());
        material.setMaterialType(request.getMaterialType());
        material.setContent(request.getContent());
        if (file != null && !file.isEmpty()) {
            attachFile(material, file);
            if (oldFileUrl != null && !oldFileUrl.isBlank()) {
                fileStorageService.delete(oldFileUrl);
            }
        }

        return materialMapper.toResponse(material);
    }

    @Override
    public void archiveMaterial(UUID id, UUID practitionerId) {
        PractitionerMaterial material = findOrThrow(id, practitionerId);
        material.setIsArchived(true);
    }

    @Override
    public void unarchiveMaterial(UUID id, UUID practitionerId) {
        PractitionerMaterial material = findOrThrow(id, practitionerId);
        material.setIsArchived(false);
    }

    private PractitionerMaterial findOrThrow(UUID id, UUID practitionerId) {
        return materialRepository.findByIdAndPractitionerId(id, practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден"));
    }

    private void attachFile(PractitionerMaterial material, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }

        material.setFileUrl(fileStorageService.store(file, "materials"));
        material.setFileName(resolveFileName(file));
        material.setMimeType(resolveMimeType(file));
        material.setFileSizeBytes(file.getSize());
    }

    private void validateFile(MultipartFile file) {
        if (file != null && file.isEmpty()) {
            throw new BusinessException("Файл материала пустой");
        }
    }

    private String resolveMimeType(MultipartFile file) {
        return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
    }

    private String resolveFileName(MultipartFile file) {
        if (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) {
            return file.getOriginalFilename();
        }
        return "material-file." + resolveExtension(file);
    }

    private String resolveExtension(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
