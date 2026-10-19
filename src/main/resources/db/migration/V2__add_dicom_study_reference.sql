
ALTER TABLE radiology_exams
    ADD COLUMN orthanc_study_id VARCHAR(64),
    ADD COLUMN study_instance_uid VARCHAR(64),
    ADD COLUMN dicom_received_at TIMESTAMP WITH TIME ZONE;


ALTER TABLE radiology_exams
    ADD CONSTRAINT uk_radiology_exams_orthanc_study_id
        UNIQUE (orthanc_study_id),
    ADD CONSTRAINT uk_radiology_exams_study_instance_uid
        UNIQUE (study_instance_uid),

    ADD CONSTRAINT ck_radiology_exams_dicom_reference_complete CHECK (
        (
            orthanc_study_id IS NULL
            AND study_instance_uid IS NULL
            AND dicom_received_at IS NULL
        )
        OR
        (
            orthanc_study_id IS NOT NULL
            AND study_instance_uid IS NOT NULL
            AND dicom_received_at IS NOT NULL
        )
    );
