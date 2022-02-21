package edu.kit.rocrate.entities.contextual;

import edu.kit.crate.entities.contextual.OrganizationEntity;
import java.io.IOException;

import edu.kit.rocrate.HelpFunctions;
import org.junit.jupiter.api.Test;

/**
 * @author Nikola Tzotchev on 5.2.2022 г.
 * @version 1
 */
public class OrganizationEntityTest {

  @Test
  void testSerialization() throws IOException {
    OrganizationEntity organization = new OrganizationEntity.OrganizationEntityBuilder()
        .setId("https://ror.org/03f0f6041")
        .setAddress("set")
        .setEmail("Sydney@sy.kit")
        .setTelephone("0665445")
        .setLocationId("#djfffff")
        .addProperty("url", "https://ror.org/03f0f6041")
        .addProperty("name", "University of Technology Sydney")
        .build();

    HelpFunctions.compareEntityWithFile(organization, "/json/entities/contextual/organization.json");
  }
}
