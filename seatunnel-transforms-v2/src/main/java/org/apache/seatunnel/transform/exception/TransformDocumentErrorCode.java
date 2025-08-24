package org.apache.seatunnel.transform.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.seatunnel.common.exception.SeaTunnelErrorCode;

@Getter
@RequiredArgsConstructor
public enum TransformDocumentErrorCode implements SeaTunnelErrorCode {

    DOCUMENT_PARSING_ERROR("TRANSFORM_DOCUMENT-01", "Error occurred while parsing the document."),
    ;

    private final String code;
    private final String description;
}
