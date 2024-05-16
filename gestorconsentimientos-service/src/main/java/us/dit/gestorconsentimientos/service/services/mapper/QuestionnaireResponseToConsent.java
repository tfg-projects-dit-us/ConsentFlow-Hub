package us.dit.gestorconsentimientos.service.services.mapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Consent;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Consent.ConsentState;
import org.hl7.fhir.r5.model.Consent.ProvisionDataComponent;
import org.springframework.stereotype.Service;

import us.dit.gestorconsentimientos.service.model.FhirDTO;


/**
 * Mapper que permite la conversión entre un recurso Fhir de tipo QuestionnaireResponse
 * a uno de tipo Consent.
 * 
 * @author Jose Antonio
 */
@Service
public class QuestionnaireResponseToConsent {

	private static final Logger logger = LogManager.getLogger();

	public FhirDTO map(FhirDTO questionnaireResponseDTO) {
				
		logger.info("Mapeo de QuestionnaireResponse a Consent");		
		
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) questionnaireResponseDTO.getResource();
		Consent consent = null;
		Consent.ProvisionComponent provision = null;
		FhirDTO consentDTO = null;

		// Creación del consentimiento en estado activo
		consent = new Consent(ConsentState.ACTIVE);
		provision = new Consent.ProvisionComponent();

		// Establecimiento de la fecha en la que se ha creado el consentimiento (actual)
		consent.setDate(new Date());
		
		// Para cada uno de los componentes que forma la respuesta de cuestionario
		consent = qrItemComponentProcessing(consent, provision, questionnaireResponse.getItem());
		consent.setProvision(provision);

		consentDTO = new FhirDTO(consent);
		return consentDTO;
	}
	

	private Consent qrItemComponentProcessing(Consent consent, Consent.ProvisionComponent provision, List<QuestionnaireResponse.QuestionnaireResponseItemComponent> qrItemComponentList){

		logger.info("qrItemComponentProcessing");
		logger.info(qrItemComponentList);

		for (QuestionnaireResponse.QuestionnaireResponseItemComponent qrItemComponent : qrItemComponentList){
			
			logger.info("qrItemComponent");
			logger.info("Has Item: " + qrItemComponent.hasItem());
			// Grupo de campos
			if (qrItemComponent.hasItem()){
				logger.info("LinkId: " + qrItemComponent.getLinkId());
				qrItemComponentProcessing(consent, provision, qrItemComponent.getItem());
			}

			logger.info("Has Answer: " + qrItemComponent.hasAnswer());
			//Campo de respuesta
			// TODO
			// Se supone que el paciente es la persona que se ve afectada, como la persona que acepta el consentimiento
			if (qrItemComponent.hasAnswer()){
				logger.info("LinkId: " + qrItemComponent.getLinkId());				
				// Se obtiene la primera respuesta, suponiendo que no se está pasando el formulario más de una vez
				
				// Elemento "patients"
				if (qrItemComponent.getLinkId().equals("patients")){
					consent.setGrantor(new ArrayList<Reference>(){{
						add(new Reference().setDisplay(qrItemComponent.getAnswerFirstRep().getValueStringType().getValue())); 
					}});
	
					consent.setSubject(new Reference().setDisplay(qrItemComponent.getAnswerFirstRep().getValueStringType().getValue()));				
				}

				// Elemento "1.3" - periodo
				if (qrItemComponent.getLinkId().equals("1.3")){
					String aux = qrItemComponent.getAnswerFirstRep().getValueStringType().getValue();
					String[] start = aux.split(";")[0].split("-");
					String[] end = aux.split(";")[1].split("-");
					
					Calendar startCalendar = Calendar.getInstance();
					startCalendar.set(Integer.parseInt(start[0]),Integer.parseInt(start[0]),Integer.parseInt(start[0]));
	
					Calendar endCalendar = Calendar.getInstance();
					endCalendar.set(Integer.parseInt(end[0]),Integer.parseInt(end[0]),Integer.parseInt(end[0]));
	
					// Establecimiento del periodo que el consentimiento es válido
					consent.setPeriod(new Period().setStart(startCalendar.getTime()).setEnd(endCalendar.getTime()));
					
				}
	
				// Elemento "1.4"
				if (qrItemComponent.getLinkId().equals("1.4")){
					
					consent.setGrantee(new ArrayList<Reference>(){{
						add(new Reference().setDisplay(qrItemComponent.getAnswerFirstRep().getValueStringType().getValue())); 
					}});
				}
	
				// Elemento "1.5"
				if (qrItemComponent.getLinkId().equals("1.5")){
					
					String[] codeList = qrItemComponent.getAnswerFirstRep().getValueStringType().getValue().split(";");
	
					for (String code :  codeList){
						provision.addAction(new CodeableConcept(new Coding().setCode(code)));
					}
	
				}
	
				//TODO 2.1
	
				//TODO 2.2
	
				// Elemento "2.3"
				if (qrItemComponent.getLinkId().equals("2.3")){
					
					String[] codeList = qrItemComponent.getAnswerFirstRep().getValueStringType().getValue().split(";");
	
					for (String code :  codeList){
						provision.addResourceType(new Coding().setCode(code));
					}
	
				}
	
				// Elemento "2.4"
				if (qrItemComponent.getLinkId().equals("2.4")){
	
					provision.addData(
						new ProvisionDataComponent().setReference(
							new Reference().setDisplay(qrItemComponent.getAnswerFirstRep().getValueStringType().getValue())
							)
						);
	
				}

			}
			
			




		}

		return consent;
	}

}
