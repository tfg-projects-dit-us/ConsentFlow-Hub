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
package us.dit.gestorconsentimientos.service.model;


import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Consent;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Practitioner;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.springframework.beans.factory.annotation.Value;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import us.dit.gestorconsentimientos.model.RequestedConsent;
import us.dit.gestorconsentimientos.model.ReviewedConsent;

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
        // context - create this once, as it's an expensive operation
        // see http://hapifhir.io/doc_intro.html
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
     * @param url url completa del recurso o tipo de recurso con id
     * @return DTO del recurso fhir que se obtiene
     */
    public FhirDTO get(String server,String url) {

		client = this.ctx.newRestfulGenericClient(server);
        IdType id = new IdType(url);
        System.out.println(id.getResourceType());
        System.out.println(id.getIdBase());
        // TODO Extraer el resource Type de la URL....
        logger.info("LOG");
        logger.info(id.getIdBase().toString());
        return new FhirDTO(server,client.read().resource(id.getIdBase().toString()).withId(id.getId()).execute());
    }

    public String searchPatientOrPractitionerIdByName(String server, String name, String role) {

        String authorId = null;
        
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

    public List<RequestedConsent> searchConsentRequestByPerson(
        String server,
        String role,
        String name 
        ) {

        List<RequestedConsent> resourcesList = null;
        String extension_value = "ConsentRequest";
        logger.info("searchConsentRequestByPerson rol=" + role + " name=" + name);

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

            Bundle response_questionnaireResponses = null;
            if (role.equals("Practitioner")){
                response_questionnaireResponses = client.search()
                        .forResource(QuestionnaireResponse.class)
                        .where(QuestionnaireResponse.QUESTIONNAIRE.hasId(requestConsentQuestionnaireId))
                        .where(new ReferenceClientParam("source").hasId(authorId))
                        .returnBundle(Bundle.class)
                        .execute();
            }

            if (role.equals("Patient")){
                response_questionnaireResponses = client.search()
                        .forResource(QuestionnaireResponse.class)
                        .where(QuestionnaireResponse.QUESTIONNAIRE.hasId(requestConsentQuestionnaireId))
                        .where(new ReferenceClientParam("subject").hasId(authorId))
                        .returnBundle(Bundle.class)
                        .execute();
            }

            
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
            .map(entry -> {
                QuestionnaireResponse resource = (QuestionnaireResponse) entry.getResource();
                IdType patient_reference = (IdType) resource.getSubject().getReferenceElement();
                IdType practitioner_reference = (IdType) resource.getSource().getReferenceElement();
                Patient patient = (Patient) get(server, "Patient", patient_reference.getIdPartAsLong()).getResource();
                Practitioner practitioner = (Practitioner) get(server, "Practitioner", practitioner_reference.getIdPartAsLong()).getResource();
                IdType id = new IdType(new UriType(resource.getQuestionnaire()));
                return new RequestedConsent(
                    Long.parseLong(resource.getExtensionByUrl("Id_process_instance").getValue().toString()),
                    server,
                    Long.parseLong(id.getIdPart()),
                    Long.parseLong(new IdType(resource.getId()).getIdPart()),
                    resource.getMeta().getLastUpdated(),
                    patient.getName().get(0).getText(),
                    practitioner.getName().get(0).getText()
                    );
            })
            .collect(Collectors.toList());
            logger.info(resourcesList.size() + " son " + extension_value);
            
            for (RequestedConsent resource: resourcesList){
                System.out.println(resource.toString());
            }

        } catch (Exception e) {
            System.out.println("An error occurred trying to search:");
            e.printStackTrace();
        }

        return resourcesList;
    }

    public List<ReviewedConsent> searchConsentReviewByPerson(
        String server,
        String role,
        String name 
        ) {

        List<ReviewedConsent> resourcesList = null;
        String extension_value = "Consent";
        logger.info("searchConsentReviewByPerson rol=" + role + " name=" + name);

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

            Bundle response = null;
            if (role.equals("Practitioner")){
                response = client.search()
                        .forResource(Consent.class)
                        //.where(Consent.GRANTEE.hasId(authorId))
                        //.where(new StringClientParam("grantee.display").matches().value(name))
                        .where(new StringClientParam("_has:grantee:display").matches().value(name))
                        .returnBundle(Bundle.class)
                        .execute();
            }

            if (role.equals("Patient")){
                response = client.search()
                        .forResource(Consent.class)
                        //.where(new StringClientParam("_has:subject:display").matches().value(name))
                        //.where(new StringClientParam("subject.display").matches().value(name))
                        .returnBundle(Bundle.class)
                        .execute();
            }


            logger.info("Encontrados " + response.getTotal() + " rellenados por el " + role + " " + name);
            
            // Se filtra la lista de los recursos obtenidos, para quedarnos solo con los QuestionnaireResponse que representan una solicitud de consentimiento
            resourcesList = response.getEntry().stream()
            .filter(entry -> {
                Consent resource = (Consent) entry.getResource();
                if (resource.hasExtension("Tipo_Traza_Proceso_Solicitud_Consentimiento")){
                    if (resource.getExtensionByUrl("Tipo_Traza_Proceso_Solicitud_Consentimiento").getValue().toString().equals(extension_value)){
                       
                        return true;
                    }
                    return false;
                }else{
                    return false;
                }
            })
            .map( (BundleEntryComponent entry) -> {
                Consent resource = (Consent) entry.getResource();
                
                System.out.println(resource.getSubject().getDisplay());
                System.out.println(resource.getGrantee().get(0).getDisplay());

                IdType consentRevisionQuestionnaireResponseId = new IdType(new UriType(resource.getSourceReference().get(0).getId()));
                QuestionnaireResponse consentRevisionQuestionnaireResponse = (QuestionnaireResponse) get(server, "QuestionnaireResponse", consentRevisionQuestionnaireResponseId.getIdPartAsLong()).getResource();

                IdType consentRevisionQuestionnaireId = new IdType(new UriType(consentRevisionQuestionnaireResponse.getQuestionnaire()));
                Questionnaire consentRevisionQuestionnaire = (Questionnaire) get(server, "Questionnaire", consentRevisionQuestionnaireId.getIdPartAsLong()).getResource();

                IdType consentRequestQuestionnaireResponseId = new IdType(new UriType(consentRevisionQuestionnaire.getDerivedFrom().get(0).getId()));
                QuestionnaireResponse consentRequestQuestionnaireResponse = (QuestionnaireResponse) get(server, "QuestionnaireResponse", consentRequestQuestionnaireResponseId.getIdPartAsLong()).getResource();

                IdType consentRequestQuestionnaireId = new IdType(new UriType(consentRequestQuestionnaireResponse.getQuestionnaire()));

                

                return new ReviewedConsent(
                    Long.parseLong(resource.getExtensionByUrl("Id_process_instance").getValue().toString()),
                    server,
                    consentRequestQuestionnaireId.getIdPartAsLong(),
                    consentRequestQuestionnaireResponseId.getIdPartAsLong(),
                    resource.getDate(),
                    resource.getSubject().getDisplay(),
                    resource.getGrantee().get(0).getDisplay(),
                    consentRevisionQuestionnaireId.getIdPartAsLong(),
                    consentRevisionQuestionnaireResponseId.getIdPartAsLong(),
                    true,
                    Long.parseLong(resource.getIdPart())
                    );

            })
            .collect(Collectors.toList());
            logger.info(resourcesList.size() + " son " + extension_value);
            
            for (ReviewedConsent resource: resourcesList){
                System.out.println(resource.toString());
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
