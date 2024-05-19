package us.dit.gestorconsentimientos.model;

import java.util.Date;

/**
 * Clase que representa una solicitud de consentimiento, la cual es generada por un
 * facultativo, y va dirigida a un paciente.
 * 
 * @author Javier 
 */
public class RequestedConsent {

    protected final String fhirServer;
    
    protected final Long requestQuestionnaireId;
    
    protected final Long requestQuestionnaireResponseId;

    protected final String practitioner;

    protected final String patient;
    
    protected final Date date;

    // Id de la isntancia de proceso "ConsentReview" que el consentimiento tiene asociado
    protected final Long id;

    public RequestedConsent(Long id, String fhirServer, Long requestQuestionnaireId, Long requestQuestionnaireResponseId, Date date,
            String practitioner, String patient) {
        this.id = id;
        this.fhirServer = fhirServer;
        this.requestQuestionnaireId = requestQuestionnaireId;
        this.requestQuestionnaireResponseId = requestQuestionnaireResponseId;
        this.date = date;
        this.practitioner = practitioner;
        this.patient = patient;
    }

    public String getFhirServer() {
        return fhirServer;
    }

    public Long getRequestQuestionnaireId() {
        return requestQuestionnaireId;
    }

    public Long getRequestQuestionnaireResponseId() {
        return requestQuestionnaireResponseId;
    }

    public String getPractitioner() {
        return practitioner;
    }

    public String getPatient() {
        return patient;
    }

    public Date getDate() {
        return date;
    }

    public Long getId() {
        return id;
    }    

    public String toString(){
        return "Requested Consent -- "+ 
            "id: " + id + 
            ", fhirServer: " + fhirServer + 
            ", requestQuestionnaireId: " + requestQuestionnaireId + 
            ", requestQuestionnaireResponseId: " + requestQuestionnaireResponseId + 
            ", date: " + date + 
            ", practitioner: " + practitioner + 
            ", patient: " + patient;
    }

}

