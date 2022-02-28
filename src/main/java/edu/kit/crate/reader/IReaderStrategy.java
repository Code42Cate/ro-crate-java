package edu.kit.crate.reader;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;

/**
 * @author Nikola Tzotchev on 9.2.2022 г.
 * @version 1
 */
public interface IReaderStrategy {
  ObjectNode readMetadataJson(String location);
  File readContent(String location);
}
