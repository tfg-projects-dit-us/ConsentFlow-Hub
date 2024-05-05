package us.dit.gestorconsentimientos.service.model;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * Clase DAO que permite la obtención y persistencia de recursos FHIR. Esta se encarga
 * de realizar la conexión con el servidor.
 * 
 * @author Isabel, Javier
 */
public class FhirDAO {


    /**
     * Método para obtener un recruso FHIR de un servidor, a partir de su ID.
     * 
     * @param server servidor fhir en el que se encuentra el recurso FHIR
     * @param resourceType tipo de recurso FHIR que se maneja
     * @param id identificador del recurso FHIR a obtener
     * @return DTO del recurso fhir que se obtiene
     */
    public FhirDTO get(String server, String resourceType, long id) {

        FhirContext ctx = null;
		IGenericClient client = null;

		ctx = FhirContext.forR5();
		client = ctx.newRestfulGenericClient(server);       

        return new FhirDTO(server,client.read().resource(resourceType).withId(id).execute());
    }


    /**
     * Método para persistir un recurso FHIR.
     * 
     * @param dto DTO del recurso fhir que se quiere almacenar
     * @return ID del recurso FHIR que se ha persistido
     */
    public Long save(FhirDTO dto) {

		FhirContext ctx = null;		
		IGenericClient client = null;
		MethodOutcome outcome = null;
		Long id;

		ctx = FhirContext.forR5();
		client = ctx.newRestfulGenericClient(dto.getServer());
	    
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
