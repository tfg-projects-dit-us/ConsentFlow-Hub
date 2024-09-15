package us.dit.gestorconsentimientos.model;

import java.util.Date;

/**
 * Clase que representa un consentimiento (una solicitud de consentimiento revisado por 
 * el paciente al que va dirigida), el cual es otorgado por un paciente, a un facultativo.
 * Está formado en esencia por una solicitud de consentimiento, con la respuesta pertinente.
 * 
 * @author Javier 
 */
public class ReviewedConsent extends RequestedConsent {

    public ReviewedConsent(Long id, String fhirServer, Long requestQuestionnaireId, Long requestQuestionnaireResponseId, Date date,
            String practitioner, String patient, Long reviewQuestionnaireId, Long reviewQuestionnaireResponseId, Boolean review, Long consentId) {
        super(id, fhirServer, requestQuestionnaireId, requestQuestionnaireResponseId, date, practitioner, patient);
        this.review = review;
        this.reviewQuestionnaireId = reviewQuestionnaireId;
        this.reviewQuestionnaireResponseId = reviewQuestionnaireResponseId;
        this.consentId = consentId;
    }

    protected final Long reviewQuestionnaireId;
    
    protected final Long reviewQuestionnaireResponseId;

    protected final Boolean review;

    protected final Long consentId;

    public Long getReviewQuestionnaireId() {
        return reviewQuestionnaireId;
    }

    public Long getReviewQuestionnaireResponseId() {
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
        ", review: " + review +
        ", consentId: " + consentId ;

    }
 
}