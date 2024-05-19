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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import us.dit.gestorconsentimientos.service.model.FhirDAO;
import us.dit.gestorconsentimientos.service.model.FhirDTO;
import us.dit.gestorconsentimientos.service.services.kie.KieConsentService;
import us.dit.gestorconsentimientos.service.services.kie.process.ConsentReviewProcessService;
import us.dit.gestorconsentimientos.service.services.mapper.MapToQuestionnaireResponse;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireResponseToQuestionnaire;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireToFormPatient;
import us.dit.gestorconsentimientos.model.RequestedConsent;
import us.dit.gestorconsentimientos.model.ReviewedConsent;

import org.springframework.web.bind.annotation.PostMapping;


/**
 * Controlador que va a atender las operaciones que van destinadas a los recursos que
 * pueden utilizar los facultativos y pacientes, relacionados con el rol de paciente.
 * 
 * @author Javier
 */
@Controller
public class PatientController {

	private static final Logger logger = LogManager.getLogger();

    @Autowired
    ConsentReviewProcessService consentReviewProcessService;

    @Autowired
    KieConsentService kieConsentService;

    private static FhirDAO fhirDAO = new FhirDAO();

	@Autowired
    QuestionnaireResponseToQuestionnaire questionnaireResponseToQuestionnaire;

    private final QuestionnaireToFormPatient questionnaireToFormPatient = new QuestionnaireToFormPatient();

    /**
     * Controlador que gestiona las operaciones GET al recurso "/paciente".
     * Genera el contenido web pertinente al menú para los pacientes.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "patient-menu" plantilla thymeleaf
     */
    @GetMapping("/paciente")
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
    @GetMapping("/paciente/solicitudes")
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
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param id identifica la instancia de proceso "ConsentReview" que tiene asociada el consentimiento
     * @return "patient-request-individual" plantilla thymeleaf
     */
    @GetMapping("/paciente/solicitud")
    @ResponseBody
    public String getPatientRequestById(HttpSession httpSession, @RequestParam Long id) {
        //http://localhost:8090/paciente/solicitud?id=2
        logger.info("IN --- /paciente/solicitud");
        
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
        vars = consentReviewProcessService.initReviewTask(id);
        fhirServer = (String) vars.get("fhirServer");
        requestQuestionnaireResponseId = (Long) vars.get("requestQuestionnaireResponseId");
        requestQuestionnaireResponse = fhirDAO.get(fhirServer,"QuestionnaireResponse", requestQuestionnaireResponseId);

        // Generación del cuestionario que el paciente utiliza para revisar la solicitud de consentimiento
        reviewQuestionnaire = questionnaireResponseToQuestionnaire.map( requestQuestionnaireResponse);
        reviewQuestionnaire.setServer(fhirServer);
        reviewQuestionnaireId = fhirDAO.save(reviewQuestionnaire);

        httpSession.setAttribute("fhirServer", fhirServer);
        httpSession.setAttribute("reviewQuestionnaireId", reviewQuestionnaireId);
        httpSession.setAttribute("processInstanceId", id);

        reviewQuestionnarieForm = questionnaireToFormPatient.map(reviewQuestionnaire);

        logger.info("OUT --- /paciente/solicitud");
        //TODO plantilla Thymeleaf
        //return "patient-request-individual";
        return reviewQuestionnarieForm;
    }

    /**
     * Controlador que gestiona las operaciones POST al recurso "/paciente/consentimiento".
     * Muestra el consentimiento generado.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param request representa la petición HTTP recibida, y contiene la información de esta
     * @return
     */ 
    @PostMapping("/paciente/consentimiento")
    public String postPatientConsent(HttpSession httpSession, HttpServletRequest request) {
        
        logger.info("IN --- POST /paciente/solicitud");

        String fhirServer = (String) httpSession.getAttribute("fhirServer");
        Long reviewQuestionnaireId = (Long) httpSession.getAttribute("reviewQuestionnaireId");
        Long processInstanceId = (Long) httpSession.getAttribute("processInstanceId");
        Map<String, String[]> reviewQuestionnaireFormResponse = null;
        MapToQuestionnaireResponse mapToQuestionnaireResponse = null;
        FhirDTO reviewQuestionnaireResponse = null;
        Long reviewQuestionnaireResponseId = null;
        Boolean review = null;
        Map<String, Object> results = new HashMap<String,Object>();

        // Procesado de la respuesta al cuestionario para aceptar o rechazar la solicitud
        // de consentimiento que se ha revisado.
        
		// Obtenemos los campos rellenados del Meta-Cuestionario en un Map
		reviewQuestionnaireFormResponse = request.getParameterMap();
        mapToQuestionnaireResponse = new MapToQuestionnaireResponse( fhirDAO.get(fhirServer,"Questionnaire", reviewQuestionnaireId));
        reviewQuestionnaireResponse = mapToQuestionnaireResponse.map(reviewQuestionnaireFormResponse);
        reviewQuestionnaireResponse.setServer(fhirServer);
        reviewQuestionnaireResponseId = fhirDAO.save(reviewQuestionnaireResponse);
        
        //TODO Obtener la respuesta del consentimiento... (Prodria no ser necesario cuando se utilice fhir Consent)
        //review = 

        // Finalización de la tarea humana correspondiente al cuestionario
        results.put("reviewQuestionnaireId",reviewQuestionnaireId);
        results.put("reviewQuestionnaireResponseId",reviewQuestionnaireResponseId);
        results.put("review",review);
        consentReviewProcessService.completeReviewTask(processInstanceId, results);

        logger.info("OUT --- POST /paciente/solicitud");
        return "redirect:/paciente/consentimiento/?id="+processInstanceId.toString();
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/paciente/consentimientos".
     * Genera contenido web que muestra una lista con los consentimientos otorgados por 
     * el paciente.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "patient-consent-list" plantilla thymeleaf
     */ 
    @GetMapping("/paciente/consentimientos")
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
    @GetMapping("/paciente/consentimiento")
    public String getPatientConsentById(Model model,@RequestParam Long id) {

        logger.info("IN --- /paciente/consentimiento");

        //TODO

        //TODO plantilla Thymeleaf

        logger.info("OUT --- /paciente/consentimiento");
        return "patient-consent-individual" ;
    }

}
