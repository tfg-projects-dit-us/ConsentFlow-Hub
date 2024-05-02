package us.dit.gestorconsentimientos.model;


/**
 * Clase que representa un consentimiento (una solicitud de consentimiento revisado por 
 * el paciente al que va dirigida), el cual es otorgado por un paciente, a un facultativo.
 * Está formado en esencia por una solicitud de consentimiento, con la respuesta pertinente.
 * 
 * @author Javier 
 */
public class ReviewedConsent extends RequestedConsent {

    public ReviewedConsent(String fhirServer, String requestQuestionnaireId, String requestQuestionnaireResponseId,
            String practitioner, String patient, String reviewQuestionnaireId, String reviewQuestionnaireResponseId, Boolean review) {
        super(fhirServer, requestQuestionnaireId, requestQuestionnaireResponseId, practitioner, patient);
        this.review = review;
        this.reviewQuestionnaireId = reviewQuestionnaireId;
        this.reviewQuestionnaireResponseId = reviewQuestionnaireResponseId;
    }

    protected final String reviewQuestionnaireId;
    
    protected final String reviewQuestionnaireResponseId;

    protected final Boolean review;

    public String getReviewQuestionnaireId() {
        return reviewQuestionnaireId;
    }

    public String getReviewQuestionnaireResponseId() {
        return reviewQuestionnaireResponseId;
    }

    public Boolean getReview() {
        return review;
    }

    public String toString(){

        return "Consent -- "+ 
        "fhirServer: " + fhirServer + 
        ", requestQuestionnaireId: " + requestQuestionnaireId + 
        ", requestQuestionnaireResponseId: " + requestQuestionnaireResponseId + 
        ", practitioner: " + practitioner + 
        ", patient: " + patient + 
        ", reviewQuestionnaireId: " + reviewQuestionnaireId + 
        ", reviewQuestionnaireResponseId: " + reviewQuestionnaireResponseId + 
        ", review: " + review ;           
    }
 
}