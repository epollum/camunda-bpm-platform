/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.model.xml.validation;

import static org.assertj.core.api.Assertions.*;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.testmodel.TestModelParser;
import org.camunda.bpm.model.xml.testmodel.instance.Bird;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Daniel Meyer
 *
 */
public class ModelValidationTest {

  protected ModelInstance modelInstance;

  @Before
  public void parseModel() {
    TestModelParser modelParser = new TestModelParser();
    String testXml = "org/camunda/bpm/model/xml/testmodel/instance/UnknownAnimalTest.xml";
    InputStream testXmlAsStream = this.getClass().getClassLoader().getResourceAsStream(testXml);

    modelInstance = modelParser.parseModelFromStream(testXmlAsStream);
  }

  @Test
  public void shouldValidateWithEmptyList() {
    List<ModelElementValidator<?>> validators = new ArrayList<ModelElementValidator<?>>();

    ValidationResults results = modelInstance.validate(validators);

    assertThat(results).isNotNull();
    assertThat(results.hasErrors()).isFalse();
  }

  @Test
  public void shouldCollectWarnings() {
    List<ModelElementValidator<?>> validators = new ArrayList<ModelElementValidator<?>>();

    validators.add(new IsAdultWarner());

    ValidationResults results = modelInstance.validate(validators);

    assertThat(results).isNotNull();
    assertThat(results.hasErrors()).isFalse();
    assertThat(results.getErrorCount()).isEqualTo(0);
    assertThat(results.getWarinigCount()).isEqualTo(7);
  }

  @Test
  public void shouldCollectErrors() {
    List<ModelElementValidator<?>> validators = new ArrayList<ModelElementValidator<?>>();

    validators.add(new IllegalBirdValidator("tweety"));

    ValidationResults results = modelInstance.validate(validators);

    assertThat(results).isNotNull();
    assertThat(results.hasErrors()).isTrue();
    assertThat(results.getErrorCount()).isEqualTo(1);
    assertThat(results.getWarinigCount()).isEqualTo(0);
  }

  @Test
  public void shouldWriteResults() {
    List<ModelElementValidator<?>> validators = new ArrayList<ModelElementValidator<?>>();

    validators.add(new IllegalBirdValidator("tweety"));

    ValidationResults results = modelInstance.validate(validators);

    StringWriter stringWriter = new StringWriter();
    results.write(stringWriter, new TestResultFormatter());

    assertThat(stringWriter.toString()).isEqualTo("tweety\n\tERROR (20): Bird tweety is illegal\n");
  }

  @Test
  public void shouldReturnResults() {
    List<ModelElementValidator<?>> validators = new ArrayList<ModelElementValidator<?>>();

    validators.add(new IllegalBirdValidator("tweety"));
    validators.add(new IsAdultWarner());

    ValidationResults results = modelInstance.validate(validators);

    assertThat(results.getErrorCount()).isEqualTo(1);
    assertThat(results.getWarinigCount()).isEqualTo(7);

    Map<ModelElementInstance, List<ValidationResult>> resultsByElement = results.getResults();
    assertThat(resultsByElement.size()).isEqualTo(7);

    for (Entry<ModelElementInstance, List<ValidationResult>> resultEntry : resultsByElement.entrySet()) {
      Bird element = (Bird) resultEntry.getKey();
      List<ValidationResult> validationResults = resultEntry.getValue();
      assertThat(element).isNotNull();
      assertThat(validationResults).isNotNull();

      if(element.getId().equals("tweety")) {
        assertThat(validationResults.size()).isEqualTo(2);
        ValidationResult error = validationResults.remove(0);
        assertThat(error.getType()).isEqualTo(ValidationResultType.ERROR);
        assertThat(error.getCode()).isEqualTo(20);
        assertThat(error.getMessage()).isEqualTo("Bird tweety is illegal");
        assertThat(error.getElement()).isEqualTo(element);
      }
      else {
        assertThat(validationResults.size()).isEqualTo(1);
      }

      ValidationResult warning = validationResults.get(0);
      assertThat(warning.getType()).isEqualTo(ValidationResultType.WARNING);
      assertThat(warning.getCode()).isEqualTo(10);
      assertThat(warning.getMessage()).isEqualTo("Is not an adult");
      assertThat(warning.getElement()).isEqualTo(element);
    }
  }
}
