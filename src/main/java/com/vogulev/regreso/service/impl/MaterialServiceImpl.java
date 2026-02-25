package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.dto.request.MaterialRequest;
import com.vogulev.regreso.dto.response.MaterialListItemResponse;
import com.vogulev.regreso.dto.response.MaterialResponse;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.entity.PractitionerMaterial;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.mapper.MaterialMapper;
import com.vogulev.regreso.repository.MaterialRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final PractitionerRepository practitionerRepository;
    private final MaterialMapper materialMapper;

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
    public MaterialResponse createMaterial(MaterialRequest request, UUID practitionerId) {
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Практик не найден"));

        PractitionerMaterial material = PractitionerMaterial.builder()
                .practitioner(practitioner)
                .title(request.getTitle())
                .description(request.getDescription())
                .materialType(request.getMaterialType())
                .content(request.getContent())
                .build();

        return materialMapper.toResponse(materialRepository.save(material));
    }

    @Override
    public MaterialResponse updateMaterial(UUID id, MaterialRequest request, UUID practitionerId) {
        PractitionerMaterial material = findOrThrow(id, practitionerId);

        material.setTitle(request.getTitle());
        material.setDescription(request.getDescription());
        material.setMaterialType(request.getMaterialType());
        material.setContent(request.getContent());

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
}
