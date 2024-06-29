package us.dit.gestorconsentimientos.service.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import us.dit.gestorconsentimientos.service.model.FhirDAO;
import us.dit.gestorconsentimientos.service.model.FhirDTO;
import us.dit.gestorconsentimientos.service.services.kie.KieConsentService;
import us.dit.gestorconsentimientos.service.services.kie.process.ConsentReviewProcessService;
import us.dit.gestorconsentimientos.service.services.mapper.MapToQuestionnaireResponse;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireResponseToConsent;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireResponseToQuestionnaire;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireResponseToViewForm;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireToFormPatient;
import us.dit.gestorconsentimientos.model.RequestedConsent;
import us.dit.gestorconsentimientos.model.ReviewedConsent;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * Controlador que va a atender las operaciones que van destinadas a los recursos que
 * pueden utilizar los facultativos y pacientes, relacionados con el rol de paciente.
 * 
 * @author Javier
 */
@Controller
@RequestMapping("/paciente")
public class PatientController {

	private static final Logger logger = LogManager.getLogger();
    
    private static FhirDAO fhirDAO = new FhirDAO();
    
    private final QuestionnaireToFormPatient questionnaireToFormPatient = new QuestionnaireToFormPatient();
    
    @Autowired
    ConsentReviewProcessService consentReviewProcessService;

    @Autowired
    KieConsentService kieConsentService;

	@Autowired
    QuestionnaireResponseToQuestionnaire questionnaireResponseToQuestionnaire;

	@Autowired
	private QuestionnaireResponseToConsent qrToConsent;

    /**
     * Controlador que gestiona las operaciones GET al recurso "/paciente".
     * Genera el contenido web pertinente al menú para los pacientes.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "patient-menu" plantilla thymeleaf
     */
    @GetMapping
    public String getPatientMenu(Model model) {

        logger.info("IN /paciente");

        Authentication auth = null;
        UserDetails userDetails = null;

        // Obtención del usuario que opera sobre el recurso para toda la sesión
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        model.addAttribute("patient", userDetails.getUsername());
        logger.info("patient: " + userDetails.getUsername());

        logger.info("OUT /paciente");
        return "patient-menu";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/paciente/solicitudes".
     * Genera contenido web que muestra una lista con las solicitudes de consentimiento,
     * recibidas por el paciente, y que están pendientes de ser revisadas.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "patient-request-list" plantilla thymeleaf
     */
    @GetMapping("/solicitudes")
    public String getPatientRequests(Model model) {
        
        logger.info("IN --- /paciente/solicitudes");

        Authentication auth = null;
        UserDetails userDetails = null;
        List<RequestedConsent> requestConsentList = null;

        // Obtención del usuario que opera sobre el recurso
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        logger.info("+ patient: " + userDetails.getUsername());

        // Obtención de la lista de solicitudes pendientes
        requestConsentList = kieConsentService.getRequestedConsentsByPatient(userDetails.getUsername());
        logger.info("Lista de solicitudes de consentimientos emitidas para el paciente: ");
        for (RequestedConsent requestConsent: requestConsentList){
            logger.info(requestConsent.toString());  
        }

        model.addAttribute("requestConsentList", requestConsentList);

        logger.info("OUT --- /paciente/solicitudes");
        return "patient-request-list";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/paciente/solicitud".
     * Genera contenido web que muestra un formulario para revisar la solicitud de
     * consentimiento, y otorgarlo o rechazarlo.
     * <br><br>
     * Los parámetros que trae el WI de la tarea humana que se inicia son: 
     * + fhirServer
     * + requestQuestionnaireResponseId
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param id identifica la instancia de proceso "ConsentReview" que tiene asociada el consentimiento
     * @return "patient-request-individual" plantilla thymeleaf
     */
    @GetMapping("/solicitudes/{id}")
    @ResponseBody
    public String getPatientRequestById(HttpSession httpSession, @PathVariable Long id) {
        //http://localhost:8090/paciente/solicitudes/2
        logger.info("IN --- /paciente/solicitudes/"+Long.toString(id));
        
        // El ID de la isntancia del proceso que corresponde a la solicitud de consentimiento
        // a rellenar es el ID utilizado para identificar la tarea de revisión para el 
        // paciente, en la BA.
        
        Map<String,Object> vars = null;
        String fhirServer = null;
        Long requestQuestionnaireResponseId = null;
        FhirDTO requestQuestionnaireResponse = null;
        FhirDTO reviewQuestionnaire = null;
        Long reviewQuestionnaireId = null;
        String reviewQuestionnarieForm = null;

        // Obtención de la solicitud de consentimiento creada por el facultativo
        vars = consentReviewProcessService.initReviewTask(id); // Sólo funcionará la primera vez
        fhirServer = (String) vars.get("fhirServer");
        requestQuestionnaireResponseId = (Long) vars.get("requestQuestionnaireResponseId");
        requestQuestionnaireResponse = fhirDAO.get(fhirServer,"QuestionnaireResponse", requestQuestionnaireResponseId);

        // Generación del cuestionario que el paciente utiliza para revisar la solicitud de consentimiento
        reviewQuestionnaire = questionnaireResponseToQuestionnaire.map( requestQuestionnaireResponse);
        reviewQuestionnaire.setServer(fhirServer);
        reviewQuestionnaireId = fhirDAO.save(reviewQuestionnaire);

        httpSession.setAttribute("fhirServer", fhirServer);
        httpSession.setAttribute("reviewQuestionnaireId", reviewQuestionnaireId);
        httpSession.setAttribute("requestQuestionnaireResponseId", requestQuestionnaireResponseId);
        httpSession.setAttribute("processInstanceId", id);

        reviewQuestionnarieForm = questionnaireToFormPatient.map(reviewQuestionnaire);

        logger.info("OUT --- /paciente/solicitudes/"+Long.toString(id));
        //TODO plantilla Thymeleaf que muestre un formulario para aceptar o rechazar un una solicitud de consentimiento, que será necesario mostrar, a partir de un recurso FHIR de tipo QuestionnaireResponse
        //return "patient-request-individual";
        return reviewQuestionnarieForm;
    }

    /**
     * Controlador que gestiona las operaciones POST al recurso "/paciente/consentimiento".
     * Muestra el consentimiento generado.
     * <br><br>
     * Los parámetros de salida de la tarea humana que se completa son: 
     * + reviewQuestionnaireId
     * + reviewQuestionnaireResponseId
     * + review
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param request representa la petición HTTP recibida, y contiene la información de esta
     * @return
     */ 
    @PostMapping("/consentimiento")
    public String postPatientConsent(HttpSession httpSession, HttpServletRequest request) {
        
        logger.info("IN --- POST /paciente/consentimiento");

        String fhirServer = (String) httpSession.getAttribute("fhirServer");
        Long reviewQuestionnaireId = (Long) httpSession.getAttribute("reviewQuestionnaireId");
        Long requestQuestionnaireResponseId = (Long) httpSession.getAttribute("requestQuestionnaireResponseId");
        Long processInstanceId = (Long) httpSession.getAttribute("processInstanceId");
        Map<String, String[]> reviewQuestionnaireFormResponse = null;
        MapToQuestionnaireResponse mapToQuestionnaireResponse = null;
        FhirDTO reviewQuestionnaireResponse = null;
        Long reviewQuestionnaireResponseId = null;
        Boolean review = null;
        Map<String, Object> results = new HashMap<String,Object>();
        String redirect = "redirect:/paciente/solicitudes/";

        // Procesado de la respuesta al cuestionario para aceptar o rechazar la solicitud
        // de consentimiento que se ha revisado.
        
		// Obtenemos los campos rellenados del Meta-Cuestionario en un Map
		reviewQuestionnaireFormResponse = request.getParameterMap();
        String[] value_review = reviewQuestionnaireFormResponse.get("3");// Identificador del campo de la respuesta del formulario HTML utilizado para aceptar el consentimiento

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value_review.length; i++) {
            sb.append(value_review[i]);
            if (i < value_review.length - 1) {
                sb.append(";");
            }
        }
        review = Boolean.parseBoolean(sb.toString());

        if (review == Boolean.TRUE) {
            logger.info("La solicitud de consentimiento ha sido aceptada");
            mapToQuestionnaireResponse = new MapToQuestionnaireResponse( fhirDAO.get(fhirServer,"Questionnaire", reviewQuestionnaireId));
            reviewQuestionnaireResponse = mapToQuestionnaireResponse.map(reviewQuestionnaireFormResponse);
            reviewQuestionnaireResponse.setServer(fhirServer);
            reviewQuestionnaireResponseId = fhirDAO.save(reviewQuestionnaireResponse);
    
            // Generación de un recurso Consent
            FhirDTO consent = qrToConsent.map(fhirDAO.get(fhirServer,"QuestionnaireResponse", requestQuestionnaireResponseId));
            consent.setServer(fhirServer);
            Long consentId = fhirDAO.save(consent);
            logger.info("Consent ID: " + consentId);
    
            // Finalización de la tarea humana correspondiente al cuestionario
            results.put("reviewQuestionnaireId",reviewQuestionnaireId);
            results.put("reviewQuestionnaireResponseId",reviewQuestionnaireResponseId);
            results.put("review",review);
            consentReviewProcessService.completeReviewTask(processInstanceId, results);
            redirect = "redirect:/paciente/consentimientos/";
        }else{
            logger.info("La solicitud de consentimiento ha sido rechazada");
        }

        logger.info("OUT --- POST /paciente/consentimiento");
        return redirect;
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/paciente/consentimientos".
     * Genera contenido web que muestra una lista con los consentimientos otorgados por 
     * el paciente.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "patient-consent-list" plantilla thymeleaf
     */ 
    @GetMapping("/consentimientos")
    public String getPatientConsents(Model model) {
        
        logger.info("IN --- /paciente/consentimientos");

        Authentication auth = null;
        UserDetails userDetails = null;
        List<ReviewedConsent> consentList = null;

        // Obtención del usuario que opera sobre el recurso
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        logger.info("+ patient: " + userDetails.getUsername());

        // Obtención de la lista de consentimientos
        consentList = kieConsentService.getConsentsByPatient(userDetails.getUsername());
        logger.info("Lista de consentimientos otorgados por el paciente: ");
        for (RequestedConsent ReviewedConsent: consentList){
            logger.info(ReviewedConsent.toString());  
        }

        model.addAttribute("consentList", consentList);

        logger.info("OUT --- /paciente/consentimientos");
        return "patient-consent-list";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/paciente/consentimiento".
     * Genera contenido web que muestra en detalle un consentimiento ya revisado por el paciente.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param id identifica la instancia de proceso "ConsentReview" que tiene asociada el consentimiento
     * @return "patient-consent-individual" plantilla thymeleaf
     */    
    @GetMapping("/consentimientos/{id}")
    @ResponseBody
    public String getPatientConsentById(HttpSession httpSession, @PathVariable Long id) {

        logger.info("IN --- /paciente/consentimientos/"+Long.toString(id));

        //TODO Hay que hacer el cambio en RequestedConsent (definición de proceso, y de la entidad del modelo de datos) y añadir el id del recurso Fhir Consent, y dejar de utilizar por tanto QuestionnaireResponse para representar Consent (aunque se siga manteniendo)

        ReviewedConsent reviewedConsent = null;
        Long questionnaireResponseId = null;
        FhirDTO questionnaireResponse = null;
        String fhirServer = (String) httpSession.getAttribute("fhirServer");
        QuestionnaireResponseToViewForm questionnaireResponseToViewForm = new QuestionnaireResponseToViewForm();
        String result = null;

        // Obtención del ID del questionnaireResponse que es la respuesta al cuestionario con el que un facultativo ha creado una solicitud de consentimiento.
        reviewedConsent = kieConsentService.getReviewedConsentByConsentReviewInstanceId(id);

        if (reviewedConsent != null){
            questionnaireResponseId = reviewedConsent.getRequestQuestionnaireResponseId();
            questionnaireResponse = fhirDAO.get(fhirServer, "QuestionnaireResponse", questionnaireResponseId);
        
            result = questionnaireResponseToViewForm.map(questionnaireResponse);
        }else{
            // TODO Poner plantilla de error cuando se implemente la plantilla para la respuesta correcta en lugar de utilizar el mapper
            result = "ERROR";
        }

        
        logger.info("OUT --- /paciente/consentimientos/"+Long.toString(id));
        //TODO plantilla Thymeleaf que muestre un consentimiento, a partir de un recurso FHIR de tipo Consent
        //return "patient-consent-individual" ;

        return result;
    }

}
