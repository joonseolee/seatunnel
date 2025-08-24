package org.apache.seatunnel.transform.nlpmodel.parser;

import java.io.File;

public interface StructuredDataParser {

    Object parse(File file) throws Exception;
}
