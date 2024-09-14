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
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.Consent.ConsentState;
import org.hl7.fhir.r5.model.Consent.ProvisionDataComponent;
import org.hl7.fhir.r5.model.Extension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import us.dit.gestorconsentimientos.service.model.FhirDAO;
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

	private FhirDAO fhirDAO = new FhirDAO();

	@Value("${fhirserver.location}")
	private String fhirServer;

	public FhirDTO map(FhirDTO questionnaireResponseDTO) {
				
		logger.info("Mapeo de QuestionnaireResponse a Consent");		
		
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) questionnaireResponseDTO.getResource();
		Questionnaire questionnaire = (Questionnaire) fhirDAO.get(fhirServer,"QuestionnaireResponse",questionnaireResponse.getIdElement().getIdPartAsLong()).getResource();		
		Consent consent = null;
		Consent.ProvisionComponent provision = null;
		FhirDTO consentDTO = null;

		// Creación del consentimiento en estado activo
		consent = new Consent(ConsentState.ACTIVE);
		provision = new Consent.ProvisionComponent();
		
		// Paciente al que aplica
		consent.setSubject(questionnaireResponse.getSubject());
		
		// QuestionnaireResponse en el que se basa
		consent.addSourceReference(new Reference(questionnaireResponse.getIdElement()));
		
		// Facultativo que lo ha generado
		List<Reference> referencias = new ArrayList<Reference>();
		referencias.add(new Reference(questionnaire.getPublisher()));
		consent.setGrantee(referencias);

		// Extension con lo que representa y el process ID al que está relacionado
		Extension extension_tipo_traza = new Extension();
		extension_tipo_traza.setUrlElement(new UriType("Tipo_Traza_Proceso_Solicitud_Consentimiento"));
		extension_tipo_traza.setValue(new StringType("Consent"));

		ArrayList<org.hl7.fhir.r5.model.Extension> extensions = new ArrayList<Extension>();
		extensions.add(extension_tipo_traza);
		extensions.add(questionnaireResponse.getExtensionByUrl("Id_process_instance"));

		consent.setExtension(extensions);


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

		// Los identificadores LinkId están definidos en el recurso Fhir Questionnaire el cual se rellena para obtener el QuestionnaireResponse, y a partir del cual se genera el recurso Consent.
		// Los identificadores LinkId que se tratan aquí son los del QuestionnaireResponse, los cuales han podido ser modificados del Questionnaire al que responde, según cómo se haya construido a partir de un formulario HTML concreto...
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
			// FIXME Asegurar que el mapeo que se lleva a cabo de las respuestas del cuestionario al consentimiento es semánticamente correcto
			// Se supone que el paciente es la persona que se ve afectada, como la persona que acepta el consentimiento
			if (qrItemComponent.hasAnswer()){
				logger.info("LinkId: " + qrItemComponent.getLinkId());				
				// Se obtiene la primera respuesta, suponiendo que no se está pasando el formulario más de una vez
				
				// Elemento "patients" - los pacientes a los que se les solicita el consentimiento
				if (qrItemComponent.getLinkId().equals("patients")){
					consent.setGrantor(new ArrayList<Reference>(){{
						add(new Reference().setDisplay(qrItemComponent.getAnswerFirstRep().getValueStringType().getValue())); 
					}});
	
					consent.setSubject(new Reference().setDisplay(qrItemComponent.getAnswerFirstRep().getValueStringType().getValue()));				
				}

				// Elemento "1.3" - el periodo de tiempo que va a estar activo el consentimiento (fecha inicial como la del momento de la creación y la final que viene del QuestionnaireResponse)
				if (qrItemComponent.getLinkId().equals("1.3")){
					String[] end = qrItemComponent.getAnswerFirstRep().getValueStringType().getValue().split("-");
					
					Calendar startCalendar = Calendar.getInstance();
	
					Calendar endCalendar = Calendar.getInstance();
					endCalendar.set(Integer.parseInt(end[0]),Integer.parseInt(end[0]),Integer.parseInt(end[0]));
	
					// Establecimiento del periodo que el consentimiento es válido
					consent.setPeriod(new Period().setStart(startCalendar.getTime()).setEnd(endCalendar.getTime()));
					
				}
	
				// Elemento "1.3" - el periodo de tiempo que va a estar activo el consentimiento (fecha inicial y final, ambas del QuestionnaireResponse)
				// No es la implementación que hay en la actualidad, depende del formulario HTML que se utilice para recoger esta información, puesto que permitirá almacenar una u otra cosa.
				//if (qrItemComponent.getLinkId().equals("1.3~date")){
				//	String aux = qrItemComponent.getAnswerFirstRep().getValueStringType().getValue();
				//	String[] start = aux.split(";")[0].split("-");
				//	String[] end = aux.split(";")[1].split("-");
				//	
				//	Calendar startCalendar = Calendar.getInstance();
				//	startCalendar.set(Integer.parseInt(start[0]),Integer.parseInt(start[0]),Integer.parseInt(start[0]));
				//
				//	Calendar endCalendar = Calendar.getInstance();
				//	endCalendar.set(Integer.parseInt(end[0]),Integer.parseInt(end[0]),Integer.parseInt(end[0]));
				//
				//	// Establecimiento del periodo que el consentimiento es válido
				//	consent.setPeriod(new Period().setStart(startCalendar.getTime()).setEnd(endCalendar.getTime()));
				//	
				//}

				// Elemento "1.4" - Facultativo que va a obtener el consentimiento
				if (qrItemComponent.getLinkId().equals("1.4")){
					
					consent.setGrantee(new ArrayList<Reference>(){{
						add(new Reference().setDisplay(qrItemComponent.getAnswerFirstRep().getValueStringType().getValue())); 
					}});
				}
	
				// Elemento "1.5" - uso que se le va a dar a la información a la que se consigue acceso
				if (qrItemComponent.getLinkId().equals("1.5")){
					
					String[] codeList = qrItemComponent.getAnswerFirstRep().getValueStringType().getValue().split(";");
	
					for (String code :  codeList){
						provision.addAction(new CodeableConcept(new Coding().setCode(code)));
					}
	
				}
	
				//TODO elmento del cuestionario "2.1~date" - periodo temporal del que se necesita la información
	
				//TODO elemento del cuestionario "2.2~string" - campo opcional que indica el tipo de información al que se da acceso
	
				// Elemento "2.3~select" - campo opcional que indica el tipo de recurso Fhir que se solicita
				if (qrItemComponent.getLinkId().equals("2.3")){
					
					String[] codeList = qrItemComponent.getAnswerFirstRep().getValueStringType().getValue().split(";");
	
					for (String code :  codeList){
						provision.addResourceType(new Coding().setCode(code));
					}
	
				}
	
				// Elemento "2.4~string" - campo opcional que indica el identificador específico del recurso
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
