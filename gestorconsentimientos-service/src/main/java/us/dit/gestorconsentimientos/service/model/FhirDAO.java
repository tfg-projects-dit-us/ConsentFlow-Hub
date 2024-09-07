package us.dit.gestorconsentimientos.service.model;


import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;

import org.springframework.beans.factory.annotation.Value;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;

/**
 * Clase DAO que permite la obtención y persistencia de recursos FHIR. Esta se encarga
 * de realizar la conexión con el servidor.
 * 
 * @author Isabel, Javier
 */
public class FhirDAO {
    
    private static final Logger logger = LogManager.getLogger();

    @Value("${fhir.questionnaire.request.id}")
    private String requestConsentQuestionnaireId;

    private FhirContext ctx = null;
    private IGenericClient client = null;

    public FhirDAO(){
        this.ctx = FhirContext.forR5();
    }


    /**
     * Método para obtener un recruso FHIR de un servidor, a partir de su ID.
     * 
     * @param server servidor fhir en el que se encuentra el recurso FHIR
     * @param resourceType tipo de recurso FHIR que se maneja
     * @param id identificador del recurso FHIR a obtener
     * @return DTO del recurso fhir que se obtiene
     */
    public FhirDTO get(String server, String resourceType, long id) {

		IGenericClient client = null;

        client = this.ctx.newRestfulGenericClient(server);

        return new FhirDTO(server,client.read().resource(resourceType).withId(id).execute());
    }

    /**
     * Método para obtener un recruso FHIR de un servidor, a partir de su ID.
     * 
     * @param server servidor fhir en el que se encuentra el recurso FHIR
     * @param resourceType tipo de recurso FHIR que se maneja
     * @param id identificador del recurso FHIR a obtener
     * @return DTO del recurso fhir que se obtiene
     */
    public FhirDTO get(String server,String url) {

		client = this.ctx.newRestfulGenericClient(server);

        // TODO Extraer el resource Type de la URL....
        return new FhirDTO(server,client.read().resource(Questionnaire.class).withUrl(url).execute());
    }

    public String searchPatientOrPractitionerIdByName(String server, String name, String role) {

        String authorId = null;
        
        // context - create this once, as it's an expensive operation
        // see http://hapifhir.io/doc_intro.html
        
        logger.info("searchPatientOrPractitionerIdByName rol=" + role + " name=" + name);
        

        // increase timeouts since the server might be powered down
        // see http://hapifhir.io/doc_rest_client_http_config.html
        ctx.getRestfulClientFactory().setConnectTimeout(60 * 1000);
        ctx.getRestfulClientFactory().setSocketTimeout(60 * 1000);

        // create the RESTful client to work with our FHIR server
        // see http://hapifhir.io/doc_rest_client.html
        client = this.ctx.newRestfulGenericClient(server);

        try {

            // Realizar la búsqueda del autor (por ejemplo, un Practitioner)
            Bundle response_patients = client.search()
                .forResource(role)
                .where(new StringClientParam("name").matches().value(name))
                .returnBundle(Bundle.class)
                .execute();

            logger.info("Encontrados " + response_patients.getTotal() + " " + role + " llamados " + name);

            // Obtener el ID del primer Practitioner encontrado
            if (response_patients.getTotal() !=0 ) {
                authorId = response_patients.getEntry().get(0).getResource().getIdElement().getIdPart();
                logger.info("El ID del " + role + " llamado " + name + " es " + authorId);
            }

        } catch (Exception e) {
            System.out.println("An error occurred trying to search:");
            e.printStackTrace();
        }

        return authorId;
    }

    public List<FhirDTO> searchConsentRequestByPersonAndExtensionTraza(
        String server,
        String extension_value,
        String name, 
        String role
        ) {

        List<FhirDTO> resourcesList = null;
        
        logger.info("searchConsentRequestByPersonAndExtensionTraza rol=" + role + " name=" + name + " tipo de traza=" + extension_value);
        
        // context - create this once, as it's an expensive operation
        // see http://hapifhir.io/doc_intro.html

        // increase timeouts since the server might be powered down
        // see http://hapifhir.io/doc_rest_client_http_config.html
        ctx.getRestfulClientFactory().setConnectTimeout(60 * 1000);
        ctx.getRestfulClientFactory().setSocketTimeout(60 * 1000);

        // create the RESTful client to work with our FHIR server
        // see http://hapifhir.io/doc_rest_client.html
        client = this.ctx.newRestfulGenericClient(server);

        try {

            // Realizar la búsqueda del autor (por ejemplo, un Practitioner)
            Bundle response_patients = client.search()
                .forResource(role)
                .where(new StringClientParam("name").matches().value(name))
                .returnBundle(Bundle.class)
                .execute();


            // Obtener el ID del primer Practitioner encontrado
            String authorId = response_patients.getEntry().get(0).getResource().getIdElement().getIdPart();
            logger.info("El ID del " + role + " llamado " + name + " es " + authorId);

            Bundle response_questionnaireResponses = client.search()
                    .forResource(QuestionnaireResponse.class)
                    .where(QuestionnaireResponse.QUESTIONNAIRE.hasId(requestConsentQuestionnaireId))
                    .where(new ReferenceClientParam("source").hasId(authorId))
                    .returnBundle(Bundle.class)
                    .execute();
            
            logger.info("Encontrados " + response_questionnaireResponses.getTotal() + " rellenados por el " + role + " " + name);
            
            // Se filtra la lista de los recursos obtenidos, para quedarnos solo con los QuestionnaireResponse que representan una solicitud de consentimiento
            resourcesList = response_questionnaireResponses.getEntry().stream()
            .filter(entry -> {
                QuestionnaireResponse resource = (QuestionnaireResponse) entry.getResource();
                if (resource.hasExtension("Tipo_Traza_Proceso_Solicitud_Consentimiento")){
                    if (resource.getExtensionByUrl("Tipo_Traza_Proceso_Solicitud_Consentimiento").getValue().toString().equals(extension_value)){
                        return true;
                    }
                    return false;
                }else{
                    return false;
                }
            })
            .map(entry -> new FhirDTO(server, (QuestionnaireResponse) entry.getResource()))
            .collect(Collectors.toList());

            logger.info(resourcesList.size() + " son " + extension_value);
            
            for (FhirDTO resource: resourcesList){
                QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) resource.getResource();
                System.out.println(questionnaireResponse.getId());
                System.out.println(questionnaireResponse.getAuthor());
            }

        } catch (Exception e) {
            System.out.println("An error occurred trying to search:");
            e.printStackTrace();
        }

        return resourcesList;
    }


    /**
     * Método para persistir un recurso FHIR.
     * 
     * @param dto DTO del recurso fhir que se quiere almacenar
     * @return ID del recurso FHIR que se ha persistido
     */
    public Long save(FhirDTO dto) {

		MethodOutcome outcome = null;
		Long id;

		client = this.ctx.newRestfulGenericClient(dto.getServer());
	    
		// Persitencia de la respuesta del cuestionario pasado al practicante, para la
        // ejecución de la Solicitud de Consentimiento al paciente. 
		outcome = client.create()
		      .resource(dto.getResource())
		      .prettyPrint()
		      .encodedJson()
		      .execute();

		// The MethodOutcome object will contain information about the
		// response from the server, including the ID of the created
		// resource, the OperationOutcome response, etc. (assuming that
		// any of these things were provided by the server! They may not
		// always be)

	    id = outcome.getId().getIdPartAsLong();
        
        return id;

    }

}
