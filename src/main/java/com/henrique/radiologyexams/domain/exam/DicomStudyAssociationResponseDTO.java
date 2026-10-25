package com.henrique.radiologyexams.domain.exam;


import java.time.Instant;

public record DicomStudyAssociationResponseDTO(
        String orthancStudyId,
        String studyInstanceUid,
        Instant dicomReceivedAt
) {
}
