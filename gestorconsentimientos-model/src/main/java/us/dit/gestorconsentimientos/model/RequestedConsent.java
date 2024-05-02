package us.dit.gestorconsentimientos.model;


/**
 * Clase que representa una solicitud de consentimiento, la cual es generada por un
 * facultativo, y va dirigida a un paciente.
 * 
 * @author Javier 
 */
public class RequestedConsent {

    protected final String fhirServer;
    
    protected final String requestQuestionnaireId;
    
    protected final String requestQuestionnaireResponseId;

    protected final String practitioner;

    protected final String patient;

    public RequestedConsent(String fhirServer, String requestQuestionnaireId, String requestQuestionnaireResponseId,
            String practitioner, String patient) {
        this.fhirServer = fhirServer;
        this.requestQuestionnaireId = requestQuestionnaireId;
        this.requestQuestionnaireResponseId = requestQuestionnaireResponseId;
        this.practitioner = practitioner;
        this.patient = patient;
    }

    public String getFhirServer() {
        return fhirServer;
    }

    public String getRequestQuestionnaireId() {
        return requestQuestionnaireId;
    }

    public String getRequestQuestionnaireResponseId() {
        return requestQuestionnaireResponseId;
    }

    public String getPractitioner() {
        return practitioner;
    }

    public String getPatient() {
        return patient;
    }

    public String toString(){
        return "Requested Consent -- "+ 
            "fhirServer: " + fhirServer + 
            ", requestQuestionnaireId: " + requestQuestionnaireId + 
            ", requestQuestionnaireResponseId: " + requestQuestionnaireResponseId + 
            ", practitioner: " + practitioner + 
            ", patient: " + patient;
    }

}

