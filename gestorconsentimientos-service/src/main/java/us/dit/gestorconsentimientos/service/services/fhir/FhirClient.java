package us.dit.gestorconsentimientos.service.services.fhir;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
//import jdk.internal.org.jline.utils.Log;

/**
 * Servicio que se encarga de comunicarse con el servidor fhir para la obtención y 
 * persistencia de recursos.
 * 
 * @author Isabel, Javier
 */
@Service
public class FhirClient {
	private static final Logger logger = LogManager.getLogger();
	public Questionnaire getQuestionnaire(String serverBase,String questionnaireId) {
		
		logger.info("IN --- FhirClient>getQuestionnaire");

		FhirContext ctx = null;
		IGenericClient client = null;
		Questionnaire questionnaire = null;

		logger.info("+  Búsqueda del cuestionario '" + questionnaireId+"' en '"+serverBase+"'");

		// Conexión con un servidor compatible con DSTU1
		ctx = FhirContext.forR5();
		client = ctx.newRestfulGenericClient(serverBase);
		questionnaire =
		      client.read().resource(Questionnaire.class).withId(questionnaireId).execute();

		logger.info("+ Resultado de la búsqueda: " + questionnaire.getId());

		logger.info("OUT --- FhirClient>getQuestionnaire");
		return questionnaire;

	}

	public String saveQuestionnaire(String fhirServer,Questionnaire questionnaire) {
		
		logger.info("IN --- FhirClient>saveQuestionnaire");

		FhirContext ctx = null;		
		IGenericClient client = null;
		MethodOutcome outcome = null;
		String questionnaireId;

		ctx = FhirContext.forR5();
		client = ctx.newRestfulGenericClient(fhirServer);
	    
		// Persitencia de la respuesta del cuestionario pasado al practicante, para la ejecución de la Solicitud de Consentimiento al paciente. 
		outcome = client.create()
		      .resource(questionnaire)
		      .prettyPrint()
		      .encodedJson()
		      .execute();

		// The MethodOutcome object will contain information about the
		// response from the server, including the ID of the created
		// resource, the OperationOutcome response, etc. (assuming that
		// any of these things were provided by the server! They may not
		// always be)

	    questionnaireId =outcome.getId().getValueAsString();
	    logger.info("+ ID Respuesta: "+questionnaireId);
		
		logger.info("OUT --- FhirClient>saveQuestionnaire");
		return questionnaireId;	
	}

	public QuestionnaireResponse getQuestionnaireResponse(String serverBase,String questionnaireResponseId) {
		
		logger.info("IN --- FhirClient>getQuestionnaireResponse");

		FhirContext ctx = null;
		IGenericClient client = null;
		QuestionnaireResponse questionnaireResponse = null;

		logger.info("+  Búsqueda de la respuesta de cuestionario '" + questionnaireResponseId+"' en '"+serverBase+"'");

		// Conexión con un servidor compatible con DSTU1
		ctx = FhirContext.forR5();
		client = ctx.newRestfulGenericClient(serverBase);
		questionnaireResponse =
		      client.read().resource(QuestionnaireResponse.class).withId(questionnaireResponseId).execute();

		logger.info("+  Resultado de la búsqueda: " + questionnaireResponse.getId());

		logger.info("OUT --- FhirClient>getQuestionnaireResponse");
		return questionnaireResponse;

	}

	public String saveQuestionnaireResponse(String serverBase,QuestionnaireResponse questionnaireResponse) {
		
		logger.info("IN --- FhirClient>saveQuestionnaireResponse");

		FhirContext ctx = null;		
		IGenericClient client = null;
		MethodOutcome outcome = null;
		String questionnaireResponseId;

		ctx = FhirContext.forR5();
		client = ctx.newRestfulGenericClient(serverBase);
	    
		// Persitencia de la respuesta del cuestionario pasado al practicante, para la ejecución de la Solicitud de Consentimiento al paciente. 
		outcome = client.create()
		      .resource(questionnaireResponse)
		      .prettyPrint()
		      .encodedJson()
		      .execute();

		// The MethodOutcome object will contain information about the
		// response from the server, including the ID of the created
		// resource, the OperationOutcome response, etc. (assuming that
		// any of these things were provided by the server! They may not
		// always be)

	    questionnaireResponseId =outcome.getId().getValueAsString();
	    logger.info("+ ID Respuesta: "+questionnaireResponseId);
		
		logger.info("OUT --- FhirClient>saveQuestionnaireResponse");
		return questionnaireResponseId;	
	}

}
