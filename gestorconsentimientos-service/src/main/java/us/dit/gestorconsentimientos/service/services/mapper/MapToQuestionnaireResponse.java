package us.dit.gestorconsentimientos.service.services.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @author Jose Antonio
 */
public class MapToQuestionnaireResponse implements IMapper<Map<String, String[]>, FhirDTO> {

	private Questionnaire questionnaire;
	private String source;
	private FhirDAO fhirDAO = new FhirDAO();
	private String server = null;
	private String role = null;
	private String tipo_traza = null;


	/*
	 * Constructor que toma el rol de la persona que responde, siendo este rol el recurso fhir patient o practitioner.
	 */
	public MapToQuestionnaireResponse(FhirDTO questionnaire, String source, String role, String tipo_traza) {
		this.questionnaire =  (Questionnaire) questionnaire.getResource();
		this.source =  source;
		this.server = questionnaire.getServer();
		this.role = role;
		this.tipo_traza = tipo_traza;
	}

	@Override
	public FhirDTO map(Map<String, String[]> in) {
		QuestionnaireResponse response = new QuestionnaireResponse();

		// Simplificar Map, borrar los campos opcionales que no se hayan rellenado
		in = simplifyMap(in);

		response.setStatus(QuestionnaireResponseStatus.COMPLETED);
		response.setSource(new Reference(this.role + "/" + fhirDAO.searchPatientOrPractitionerIdByName(this.server, source, this.role)));
		response.setQuestionnaire(questionnaire.getId());

		Extension extension_tipo_traza = new Extension();
		extension_tipo_traza.setUrlElement(new UriType("Tipo_Traza_Proceso_Solicitud_Consentimiento"));
		extension_tipo_traza.setValue(new StringType(tipo_traza));
		ArrayList<org.hl7.fhir.r5.model.Extension> extensions = new ArrayList<Extension>();
		extensions.add(extension_tipo_traza);

		response.setExtension(extensions);
				

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
