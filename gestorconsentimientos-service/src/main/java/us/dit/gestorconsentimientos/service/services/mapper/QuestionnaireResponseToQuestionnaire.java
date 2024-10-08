/**
*  This file is part of ConsentFlow Hub: a flexible solution for the eficiente management of consents in healthcare systems.
*  Copyright (C) 2024  Universidad de Sevilla/Departamento de Ingeniería Telemática
*
*  ConsentFlow Hub is free software: you can redistribute it and/or
*  modify it under the terms of the GNU General Public License as published
*  by the Free Software Foundation, either version 3 of the License, or (at
*  your option) any later version.
*
*  ConsentFlow Hub is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
*  Public License for more details.
*
*  You should have received a copy of the GNU General Public License along
*  with ConsentFlow Hub. If not, see <https://www.gnu.org/licenses/>.
**/
package us.dit.gestorconsentimientos.service.services.mapper;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Extension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import us.dit.gestorconsentimientos.service.model.FhirDTO;


/**
 * Mapper que permite la conversión entre un recurso Fhir de tipo QuestionnaireResponse
 * a uno de tipo Questionnaire.
 * 
 * @author Jose Antonio García Linares
 */
@Service
public class QuestionnaireResponseToQuestionnaire implements IMapper<FhirDTO, FhirDTO> {

	private static final Logger logger = LogManager.getLogger();

	private FhirContext ctx = FhirContext.forR5();

	@Value("${fhirserver.location}")
	private String fhirServer;

	private IGenericClient client;

	@Override
	public FhirDTO map(FhirDTO in) {
		Questionnaire questionnaire = null;
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) in.getResource();

		this.client = ctx.newRestfulGenericClient(fhirServer);
		questionnaire = getQuestionnaire(questionnaireResponse);
		questionnaire.setPublisher(questionnaireResponse.getSource().getId());
		
		ArrayList<CanonicalType> canonicalTypes = new ArrayList<CanonicalType>();
		canonicalTypes.add(new CanonicalType(questionnaireResponse.getId()));
		questionnaire.setDerivedFrom(canonicalTypes);

		Extension extension_tipo_traza = new Extension();
		extension_tipo_traza.setUrlElement(new UriType("Tipo_Traza_Proceso_Solicitud_Consentimiento"));
		extension_tipo_traza.setValue(new StringType("ConsentRevisionQuestionnaire"));

		ArrayList<org.hl7.fhir.r5.model.Extension> extensions = new ArrayList<Extension>();
		extensions.add(extension_tipo_traza);
		extensions.add(questionnaireResponse.getExtensionByUrl("Id_process_instance"));

		questionnaire.setExtension(extensions);
		
		return new FhirDTO(questionnaire);
	}
	
	private Questionnaire getQuestionnaire(QuestionnaireResponse response) {
		
		Questionnaire questionnaire = new Questionnaire();
		Questionnaire metaQuestionnaire = client.read().resource(Questionnaire.class).withUrl(response.getQuestionnaire()).execute();
		
		for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : response.getItem().get(0).getItem()) {
			
			// Este linkId identifica al título del cuestionario
			if (item.getLinkId().equals("1.1")) {
				questionnaire.setTitle(item.getAnswer().get(0).getValue().toString());
			}
		}
		
		questionnaire.setStatus(PublicationStatus.ACTIVE);
		
		for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : response.getItem()) {
			Questionnaire.QuestionnaireItemComponent it = null;
			
			for (Questionnaire.QuestionnaireItemComponent metaItem : metaQuestionnaire.getItem()) {
				if (metaItem.getLinkId().equals(item.getLinkId())) {
					it = metaItem;
				}
			}
			
			switch (it.getType()) {
				case BOOLEAN:
					questionnaire.addItem()
						.setLinkId(item.getLinkId())
						.setText(item.getText())
						.setType(it.getType())
						.addAnswerOption().setValue(new StringType(Boolean.toString(item.getAnswer().get(0).getValueBooleanType().booleanValue())));
					break;
				case INTEGER:
					questionnaire.addItem()
						.setLinkId(item.getLinkId())
						.setText(item.getText())
						.setType(it.getType())
						.addAnswerOption().setValue(item.getAnswer().get(0).getValue());
					break;
				case STRING:
					questionnaire.addItem()
						.setLinkId(item.getLinkId())
						.setText(item.getText())
						.setType(it.getType())
						.addAnswerOption().setValue(item.getAnswer().get(0).getValue());
					break;
				case CODING:
					questionnaire.addItem()
						.setLinkId(item.getLinkId())
						.setText(item.getText())
						.setType(it.getType())
						.setAnswerOption(it.getAnswerOption())
						.addAnswerOption().setValue(item.getAnswer().get(0).getValue());
					break;
				case DATE:
					questionnaire.addItem()
						.setLinkId(item.getLinkId())
						.setText(item.getText())
						.setType(it.getType())
						.addAnswerOption().setValue(item.getAnswer().get(0).getValue());
					break;
				case GROUP:
					addGroup(questionnaire, item, it);
					break;
				default:
					throw new UnsupportedOperationException("Tipo de componente no soportado: " + it.getType().getDisplay());
			}
		}
		
		questionnaire.addItem()
			.setLinkId(Integer.toString(metaQuestionnaire.getItem().size()+1))
			.setText("Está de acuerdo y autoriza su consentimiento")
			.setType(QuestionnaireItemType.BOOLEAN)
			.setRequired(true);
		
		
		return questionnaire;
	}
	
	private void addGroup(Questionnaire questionnaire, QuestionnaireResponse.QuestionnaireResponseItemComponent item, Questionnaire.QuestionnaireItemComponent it) {
		List<QuestionnaireItemComponent> example = new ArrayList<Questionnaire.QuestionnaireItemComponent>();
		
		for (QuestionnaireResponse.QuestionnaireResponseItemComponent item1 : item.getItem()) {
			Questionnaire.QuestionnaireItemComponent it1 = null;
			
			if (!(item1.getLinkId().equals("1.1"))) {    // Sentencia para que no se muestre el campo donde se pregunto el titulo del cuestionario
				for (Questionnaire.QuestionnaireItemComponent metaItem : it.getItem()) {
					if (metaItem.getLinkId().equals(item1.getLinkId())) {
						it1 = metaItem;
					}
				}
				
				QuestionnaireItemComponent t = new QuestionnaireItemComponent();
				switch (it1.getType()) {
					case BOOLEAN:
						t.setLinkId(item1.getLinkId())
							.setText(item1.getText())
							.setType(it1.getType())
							.addAnswerOption().setValue(new StringType(Boolean.toString(item1.getAnswer().get(0).getValueBooleanType().booleanValue())));
						example.add(t);
						break;
					case INTEGER:
						t.setLinkId(item1.getLinkId())
							.setText(item1.getText())
							.setType(it1.getType())
							.addAnswerOption().setValue(item1.getAnswer().get(0).getValue());
						example.add(t);
						break;
					case STRING:
						t.setLinkId(item1.getLinkId())
							.setText(item1.getText())
							.setType(it1.getType())
							.addAnswerOption().setValue(item1.getAnswer().get(0).getValue());
						example.add(t);
						break;
					case CODING:
						t.setLinkId(item1.getLinkId())
							.setText(item1.getText())
							.setType(it1.getType())
							.setAnswerOption(it1.getAnswerOption())
							.addAnswerOption().setValue(item1.getAnswer().get(0).getValue());
						example.add(t);
						break;
					case DATE:
						t.setLinkId(item1.getLinkId())
							.setText(item1.getText())
							.setType(it1.getType())
							.addAnswerOption().setValue(item1.getAnswer().get(0).getValue());
						example.add(t);
						break;
					case GROUP:
						addGroup(questionnaire, item1, it1);
						break;
					default:
						throw new UnsupportedOperationException("Tipo de componente no soportado: " + it.getType().getDisplay());
				}
			}
		}
		
		questionnaire.addItem()
			.setLinkId(item.getLinkId())
			.setText(item.getText())
			.setType(it.getType())
			.setItem(example);
	}

}
