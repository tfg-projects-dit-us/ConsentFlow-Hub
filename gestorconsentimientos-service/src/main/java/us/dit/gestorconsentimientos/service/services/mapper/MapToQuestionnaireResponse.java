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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseStatus;

import us.dit.gestorconsentimientos.service.model.FhirDTO;
import us.dit.gestorconsentimientos.service.model.FhirDAO;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;


/**
 * Mapper que permite la conversión entre la respuesta de un cuestionario HTML, a un 
 * recurso Fhir de tipo QuestionnaireResponse.
 * 
 * @author Jose Antonio García Linares
 */
public class MapToQuestionnaireResponse implements IMapper<Map<String, String[]>, FhirDTO> {

	private Questionnaire questionnaire;
	private String source;
	private FhirDAO fhirDAO = new FhirDAO();
	private String server = null;
	private String role = null;
	private Long processInstanceId = null;

	private static final Logger logger = LogManager.getLogger();

	/*
	 * Constructor que toma el rol de la persona que responde, siendo este rol el recurso fhir patient o practitioner.
	 */
	public MapToQuestionnaireResponse(FhirDTO questionnaire, String source, String role, Long processInstanceId) {
		this.questionnaire =  (Questionnaire) questionnaire.getResource();
		this.source =  source;
		this.server = questionnaire.getServer();
		this.role = role;
		this.processInstanceId = processInstanceId;
	}

	@Override
	public FhirDTO map(Map<String, String[]> in) {
		QuestionnaireResponse response = new QuestionnaireResponse();

		// Simplificar Map, borrar los campos opcionales que no se hayan rellenado
		in = simplifyMap(in);

		response.setQuestionnaire(questionnaire.getId());
		response.setStatus(QuestionnaireResponseStatus.COMPLETED);

		Extension questionnaire_tipo_traza = questionnaire.getExtensionByUrl("Tipo_Traza_Proceso_Solicitud_Consentimiento");

		logger.info("Mapper Map - QuestionnaireResponse: " + questionnaire_tipo_traza.getValue().toString());

		if ("ConsentRequestQuestionnaire".equals(questionnaire_tipo_traza.getValue().toString())) {
			response.setSource(new Reference(this.role + "/" + fhirDAO.searchPatientOrPractitionerIdByName(this.server, source, this.role)));	
			// No funciona cuando hay más de un paciente....
			//response.setSubject(new Reference("Patient" + "/" + fhirDAO.searchPatientOrPractitionerIdByName(this.server, getParameter(in.get("patients")), "Patient")));

			Extension extension_tipo_traza = new Extension();
			extension_tipo_traza.setUrlElement(new UriType("Tipo_Traza_Proceso_Solicitud_Consentimiento"));
			extension_tipo_traza.setValue(new StringType("ConsentRequest"));
	
			Extension extension_process_instance_id = new Extension();
			extension_process_instance_id.setUrlElement(new UriType("Id_process_instance"));
			extension_process_instance_id.setValue(new StringType(this.processInstanceId.toString()));
	
			ArrayList<org.hl7.fhir.r5.model.Extension> extensions = new ArrayList<Extension>();
			extensions.add(extension_tipo_traza);
			extensions.add(extension_process_instance_id);
	
			response.setExtension(extensions);

		}

		if ("ConsentRevisionQuestionnaire".equals(questionnaire_tipo_traza.getValue().toString())) {
			response.setSource(new Reference(this.role + "/" + fhirDAO.searchPatientOrPractitionerIdByName(this.server, source, this.role)));
			response.setSubject(new Reference(this.role + "/" + fhirDAO.searchPatientOrPractitionerIdByName(this.server, source, this.role)));
	
			Extension extension_tipo_traza = new Extension();
			extension_tipo_traza.setUrlElement(new UriType("Tipo_Traza_Proceso_Solicitud_Consentimiento"));
			extension_tipo_traza.setValue(new StringType("ConsentRevision"));
	
			ArrayList<org.hl7.fhir.r5.model.Extension> extensions = new ArrayList<Extension>();
			extensions.add(extension_tipo_traza);
			extensions.add(questionnaire.getExtensionByUrl("Id_process_instance"));
	
			response.setExtension(extensions);

		}		


		for (Questionnaire.QuestionnaireItemComponent item : questionnaire.getItem()) {
			switch (item.getType()) {
			case BOOLEAN:
				response.addItem()
					.setLinkId(item.getLinkId())
					.setText(item.getText())
					.addAnswer()
						.setValue(new BooleanType(getParameter(in.get(item.getLinkId()))));
				break;
			case GROUP:
				responseGroup(response, item, item.getItem(), in);
				break;
			default:
				throw new UnsupportedOperationException(
						"Tipo de componente no soportado: " + item.getType().getDisplay());
			}
		}

		return new FhirDTO(response);
	}

	private void responseGroup(
		QuestionnaireResponse response, 
		QuestionnaireItemComponent item,
		List<QuestionnaireItemComponent> items, 
		Map<String, String[]> results
		) {
		
			List<QuestionnaireResponseItemComponent> example = new ArrayList<QuestionnaireResponse.QuestionnaireResponseItemComponent>();

		for (Questionnaire.QuestionnaireItemComponent it : items) {
			QuestionnaireResponseItemComponent t = new QuestionnaireResponseItemComponent();
			
			if (results.containsKey(it.getLinkId())) {
				String[] values = results.get(it.getLinkId());
				
				switch (it.getType()) {
				case BOOLEAN:
					t.setLinkId(it.getLinkId()).setText(it.getText()).addAnswer()
							.setValue(new BooleanType(getParameter(values)));
					example.add(t);
					break;
				case STRING:
					t.setLinkId(it.getLinkId()).setText(it.getText()).addAnswer()
							.setValue(new StringType(getParameter(values)));
					example.add(t);
					break;
				case CODING:
					t.setLinkId(it.getLinkId()).setText(it.getText()).addAnswer()
							.setValue(new StringType(getParameter(values)));
					example.add(t);
					break;
				case DATE:
					t.setLinkId(it.getLinkId()).setText(it.getText()).addAnswer()
							.setValue(new StringType(getParameter(values)));
					example.add(t);
					break;
				default:
					throw new UnsupportedOperationException(
							"Tipo de componente no soportado: " + it.getType().getDisplay());
				}
			}
		}

		response.addItem().setLinkId(item.getLinkId()).setText(item.getText()).setItem(example);
	}

	private String getParameter(String[] values) {
		String result = null;

		if (values.length == 1) {
			result = values[0];
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < values.length; i++) {
				sb.append(values[i]);
				if (i < values.length - 1) {
					sb.append(";");
				}
			}
			result = sb.toString();
		}

		return result;
	}

	private Map<String, String[]> simplifyMap(Map<String, String[]> in) {
		Map<String, String[]> map = new HashMap<String, String[]>();

		for (Map.Entry<String, String[]> entry : in.entrySet()) {
			String key = entry.getKey();
			String[] values = entry.getValue();

			if (!(values[0].isEmpty())) {
				map.put(key, values);
			}
		}

		return map;
	}

}
