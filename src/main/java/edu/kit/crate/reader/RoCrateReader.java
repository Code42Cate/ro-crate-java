package edu.kit.crate.reader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.kit.crate.RoCrate;
import edu.kit.crate.context.CrateMetadataContext;
import edu.kit.crate.context.RoCrateMetadataContext;
import edu.kit.crate.entities.contextual.ContextualEntity;
import edu.kit.crate.entities.data.DataEntity;
import edu.kit.crate.entities.data.RootDataEntity;
import edu.kit.crate.validation.JsonSchemaValidation;
import edu.kit.crate.validation.Validator;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * The reader used for reading crates from the outside into the library.
 * The class has a field using a strategy to support different ways of importing the crates.
 * (from zip, folder, etc.)
 */
public class RoCrateReader {

  public static final String CONFORMS_TO_BASE_URL = "https://w3id.org/ro/crate/";
  private final ReaderStrategy reader;
  private RoCrate crate;

  public RoCrateReader(ReaderStrategy reader) {
    this.reader = reader;
  }

  /**
   * This function will read the location (using one of the specified strategies) and then
   * build the relation between the entities.
   *
   * @param location the location of the ro-crate to be read
   * @return the read RO-crate
   */
  public RoCrate readCrate(String location) {
    crate = new RoCrate();
    // get the ro-crate-medata.json
    ObjectNode metadataJson = reader.readMetadataJson(location);
    // get the content of the crate
    File files = reader.readContent(location);

    // this set will contain the files that are associated with entities
    HashSet<String> usedFiles = new HashSet<>();
    usedFiles.add(new File(location).toPath().resolve("ro-crate-metadata.json").toFile().getPath());
    usedFiles.add(new File(location).toPath().resolve("ro-crate-preview.html").toFile().getPath());
    usedFiles.add(new File(location).toPath().resolve("ro-crate-preview_files").toFile().getPath());

    JsonNode context = metadataJson.get("@context");

    CrateMetadataContext crateContext = new RoCrateMetadataContext(context);
    this.crate.setMetadataContext(crateContext);
    JsonNode graph = metadataJson.get("@graph");

    if (graph.isArray()) {

      graph = setRootEntities((ArrayNode) graph);
      for (JsonNode node : graph) {
        // if the id is in the root has part we should add this entity as data entity
        if (this.crate.getRootDataEntity().hasInHasPart(node.get("@id").asText())) {
          File loc = checkFolderHasFile(node.get("@id").asText(), files);
          if (loc != null) {
            usedFiles.add(loc.getPath());
          }
          // data entity
          DataEntity dataEntity = new DataEntity.DataEntityBuilder()
              .setAll(node.deepCopy())
              .setSource(loc)
              .build();
          this.crate.addDataEntity(dataEntity, false);
        } else {
          // contextual entity
          this.crate.addContextualEntity(
              new ContextualEntity.ContextualEntityBuilder().setAll(node.deepCopy()).build());
        }
      }
    }
    var itr = files.listFiles();
    List<File> list = new ArrayList<>();
    for (var f : itr) {
      if (!usedFiles.contains(f.getPath())) {
        list.add(f);
      }
    }
    this.crate.setUntrackedFiles(list);
    Validator defaultValidation = new Validator(new JsonSchemaValidation());
    defaultValidation.validate(this.crate);
    return this.crate;
  }

  private File checkFolderHasFile(String id, File file) {
    Path path = file.toPath().resolve(id);
    if (path.toFile().exists()) {
      return path.toFile();
    }
    return null;
  }

  // gets the entities that every crate should have
  // we will need the root dataset to distinguish between data entities and contextual entities
  private ArrayNode setRootEntities(ArrayNode graph) {

    // for now, we make an empty ArrayNode and put all the entities
    // that still need to be processed there
    var graphCopy = graph.deepCopy();
    // use the algorithm described here: https://www.researchobject.org/ro-crate/1.1/root-data-entity.html#finding-the-root-data-entity
    for (int i = 0; i < graph.size(); i++) {
      JsonNode node = graph.get(i);
      JsonNode type = node.get("conformsTo");
      if (type != null) {

        // according to the RO crate 1.1 spec, the following SHOULD evaluate as true
        boolean validConformsTo = type.isObject() && type.get("@id").asText().startsWith(CONFORMS_TO_BASE_URL);

        // in some crates, especially in nearly all workflow hub crates, conformsTo is an array instead of an object
        // although this is not 100% matching the spec, we accept it if one of the array elements has the correct id that the spec expects
        // relevant issue: https://github.com/kit-data-manager/ro-crate-java/issues/1
        if (type.isArray()) {
          for (var e: type) {
            if (e.get("@id").asText().startsWith(CONFORMS_TO_BASE_URL)) {
              validConformsTo = true;
              break;
            }
          }
        }

        if (validConformsTo) {
          this.crate.setJsonDescriptor(
              new ContextualEntity.ContextualEntityBuilder().setAll(node.deepCopy()).build());
          graphCopy.remove(i);
          String id = node.get("about").get("@id").asText();
          for (int j = 0; j < graphCopy.size(); j++) {
            ObjectNode secondIteration = graphCopy.get(j).deepCopy();
            if (secondIteration.get("@id").asText().equals(id)) {
              // root data entity
              JsonNode hasPartNode = secondIteration.get("hasPart");
              Set<String> hasPartSet = new HashSet<>();
              if (hasPartNode != null) {
                if (hasPartNode.isArray()) {
                  for (var e : hasPartNode) {
                    hasPartSet.add(e.get("@id").asText());
                  }
                } else if (hasPartNode.isObject()) {
                  hasPartSet.add(hasPartNode.get("@id").asText());
                }
              }
              secondIteration.remove("hasPart");
              this.crate.setRootDataEntity(
                  new RootDataEntity.RootDataEntityBuilder()
                      .setAll(secondIteration.deepCopy())
                      .setHasPart(hasPartSet)
                      .build()
              );
              graphCopy.remove(j);
              break;
            }
          }
        }
      }
    }
    return graphCopy;
  }
}

