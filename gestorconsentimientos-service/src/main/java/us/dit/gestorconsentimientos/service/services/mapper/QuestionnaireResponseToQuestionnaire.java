package us.dit.gestorconsentimientos.service.services.mapper;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.StringType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;


/**
 * Mapper que permite la conversión entre un recurso Fhir de tipo QuestionnaireResponse
 * a uno de tipo Questionnaire.
 * 
 * @author Jose Antonio
 */
@Service
public class QuestionnaireResponseToQuestionnaire implements IMapper<QuestionnaireResponse, Questionnaire> {

	private static final Logger logger = LogManager.getLogger();

	private FhirContext ctx = FhirContext.forR5();

	@Value("${fhirserver.location}")
	private String fhirServer;

	private IGenericClient client;

	@Override
	public Questionnaire map(QuestionnaireResponse in) {
		Questionnaire questionnaire = null;
		
		logger.info(fhirServer);
		this.client = ctx.newRestfulGenericClient(fhirServer);
		String id = getQuestionnaire(in);

		questionnaire = client.read().resource(Questionnaire.class).withId(id).execute();
		
		return questionnaire;
	}
	
	private String getQuestionnaire(QuestionnaireResponse response) {
		String id = null;
		Questionnaire questionnaire = new Questionnaire();
		Questionnaire metaQuestionnaire = client.read().resource(Questionnaire.class).withUrl(response.getQuestionnaire()).execute();
		
		for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : response.getItem().get(0).getItem()) {
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
		
		id = createQuest(questionnaire);
		
		return id;
	}
	
	private String createQuest(Questionnaire questionnaire) {
		String id = null;
		
		MethodOutcome outcome = client.create().resource(questionnaire).execute();
		id = outcome.getId().getIdPart();
		
		return id;
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
