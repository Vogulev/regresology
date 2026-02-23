package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.MaterialRequest;
import com.vogulev.regreso.dto.response.MaterialListItemResponse;
import com.vogulev.regreso.dto.response.MaterialResponse;

import java.util.List;
import java.util.UUID;

public interface MaterialService {

    List<MaterialListItemResponse> getMaterials(UUID practitionerId, boolean includeArchived);

    MaterialResponse createMaterial(MaterialRequest request, UUID practitionerId);

    MaterialResponse updateMaterial(UUID id, MaterialRequest request, UUID practitionerId);

    void archiveMaterial(UUID id, UUID practitionerId);
}
