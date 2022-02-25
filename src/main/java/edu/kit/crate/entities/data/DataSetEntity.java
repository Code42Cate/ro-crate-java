package edu.kit.crate.entities.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.kit.crate.objectmapper.MyObjectMapper;
import java.util.HashSet;
import java.util.Set;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class DataSetEntity extends DataEntity {

  public static final String TYPE = "Dataset";

  public DataSetEntity(ADataSetBuilder<?> entityBuilder) {
    super(entityBuilder);
    if (!entityBuilder.hasPart.isEmpty()) {
      ObjectMapper objectMapper = MyObjectMapper.getMapper();
      ArrayNode withId = objectMapper.createArrayNode();
      ArrayNode node = objectMapper.valueToTree(entityBuilder.hasPart);
      node.forEach(e -> withId.add(objectMapper.createObjectNode().set("@id", e)));
      this.addProperty("hasPart", withId);
    }
    this.addType(TYPE);
  }

  @Override
  public void saveToZip(ZipFile zipFile) throws ZipException {
    if (this.getLocation() != null) {
      zipFile.addFolder(this.getLocation());
    }
  }


  public void addToHasPart(String id) {
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    JsonNode jsonNode = this.getProperty("hasPart");
    ArrayNode node = objectMapper.createArrayNode();
    if (jsonNode == null) {
      node.add(objectMapper.createObjectNode().put("@id", id));
    }
    else if (jsonNode.isArray()) {
      node = (ArrayNode) jsonNode;
      node.add(objectMapper.createObjectNode().put("@id", id));
    } else {
      node.add(jsonNode);
      node.add(objectMapper.createObjectNode().put("@id", id));
    }
    this.addProperty("hasPart", node);
  }

  public boolean hasInHasPart(String id) {
    JsonNode hasPart = this.getProperty("hasPart");
    if (hasPart != null) {
      if (hasPart.isArray()) {
        for (JsonNode node : hasPart) {
          if (node.get("@id").asText().equals(id)) {
            return true;
          }
        }
      } else {
        return hasPart.get("@id").asText().equals(id);
      }
    }
    return false;
  }

  abstract static class ADataSetBuilder<T extends ADataEntityBuilder<T>> extends
      ADataEntityBuilder<T> {

    Set<String> hasPart;


    public ADataSetBuilder() {
      this.hasPart = new HashSet<>();
    }

    public T setHasPart(Set<String> hastPart) {
      this.hasPart = hastPart;
      return self();
    }

    public T addToHasPart(DataEntity dataEntity) {
      this.hasPart.add(dataEntity.getId());
      return self();
    }

    public T addToHasPart(String dataEntity) {
      this.hasPart.add(dataEntity);
      return self();
    }

    @Override
    abstract public DataSetEntity build();
  }

  static final public class DataSetBuilder extends ADataSetBuilder<DataSetBuilder> {

    @Override
    public DataSetBuilder self() {
      return this;
    }

    @Override
    public DataSetEntity build() {
      return new DataSetEntity(this);
    }
  }
}
