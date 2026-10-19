package com.henrique.radiologyexams.exceptions;


public class DicomStudyRequiredException extends RuntimeException {

    public DicomStudyRequiredException() {

        super("Exam must have an associated dicom study before being marked as performed.");
    }
}
