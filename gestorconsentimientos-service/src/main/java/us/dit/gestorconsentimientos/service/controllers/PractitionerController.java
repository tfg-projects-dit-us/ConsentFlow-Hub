package us.dit.gestorconsentimientos.service.controllers;

import java.util.Arrays;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import us.dit.gestorconsentimientos.model.RequestedConsent;
import us.dit.gestorconsentimientos.model.ReviewedConsent;
import us.dit.gestorconsentimientos.service.model.FhirDAO;
import us.dit.gestorconsentimientos.service.model.FhirDTO;
import us.dit.gestorconsentimientos.service.services.kie.KieConsentService;
import us.dit.gestorconsentimientos.service.services.kie.process.ConsentRequestProcessService;
import us.dit.gestorconsentimientos.service.services.mapper.MapToQuestionnaireResponse;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireResponseToConsent;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireToFormPractitioner;


/**
 * Controlador que va a atender las operaciones que van destinadas a los recursos que
 * pueden utilizar los facultativos.
 * 
 * @author Javier
 */
@Controller
public class PractitionerController {

	private static final Logger logger = LogManager.getLogger();

    private static FhirDAO fhirDAO = new FhirDAO();

    @Autowired
    private ConsentRequestProcessService consentRequestProcess;

    @Autowired
    private KieConsentService kieConsentService;

	@Autowired
	private QuestionnaireToFormPractitioner questionnaireToFormPractitionerMapper;

	private Map<String, String[]> deleteFielsPatients(Map<String, String[]> response){
		Map<String, String[]> result = new HashMap<String, String[]>();
		
		for (Map.Entry<String, String[]> entry : response.entrySet()) {
			String key = entry.getKey();
			String[] values = entry.getValue();
			
			if (!(key.equals("patients~string"))) {
				result.put(key, values);
			}
		}		
		return result;
	}

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo".
     * Genera el contenido web pertinente al menú para los facultativo.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "practitioner-menu" plantilla thymeleaf
     */
    @GetMapping("/facultativo")
    public String getPractitionerMenu(Model model) {

        logger.info("IN --- /facultativo");

        Authentication auth = null;
        UserDetails userDetails = null;

        // Obtención del usuario que opera sobre el recurso para toda la sesión
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        model.addAttribute("practitioner", userDetails.getUsername());
        logger.info("+ practitioner: " + userDetails.getUsername());

        logger.info("OUT --- /facultativo");
        return "practitioner-menu";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/solicitar".
     * Genera un formulario web para que el facultativo pueda crear una solicitud de consentimiento
     * que trata un tema específico, y que va dirigida a una serie de pacientes.
     * <br><br>
     * En cuanto a JBPM, va a instanciar el proceso de solicitud de consentimientos, y va a obtener el
     * cuestionario que el facultativo tiene que responder para crear la solicitud de consentimiento (tarea manual).
     * 
     * @param httpSession contiene los atributos de la sesión http
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "practitioner-request-questionnarie" plantilla thymeleaf
     */
    @GetMapping("/facultativo/solicitar")
    //@ResponseBody
    public String getPractitionerRequestQuestionnarie(HttpSession httpSession, Model model) {
        
        logger.info("IN --- /facultativo/solicitar");

        Authentication auth = null;
        UserDetails userDetails = null;
        Map<String,Object> params = new HashMap<String,Object>();
        Long processInstanceId = null;
        Map<String,Object> vars = null;
        String fhirServer = null;
        Long requestQuestionnaireId = null;
        FhirDTO requestQuestionnarie = null;

        // Obtención del usuario que opera sobre el recurso para toda la sesión
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        logger.info("+ practitioner: " + userDetails.getUsername());    

        // Instanciación del proceso de solicitud de consentimiento
        params.put("practitioner",userDetails.getUsername());
        processInstanceId = consentRequestProcess.createProcessInstance(params);
        logger.info("+ processInstanceId: " + processInstanceId);
        httpSession.setAttribute("processInstanceId", processInstanceId);
              
        // Obtención del cuestionario a mostrar
        vars = consentRequestProcess.initRequestTask(processInstanceId);
        fhirServer = (String) vars.get("fhirServer");
        requestQuestionnaireId = (Long) vars.get("requestQuestionnaireId");
        logger.info("+ fhirServer: " + fhirServer);
        logger.info("+ requestQuestionnaireId: " + requestQuestionnaireId);
        httpSession.setAttribute("fhirServer", fhirServer);
        httpSession.setAttribute("requestQuestionnaireId", requestQuestionnaireId);
        requestQuestionnarie = fhirDAO.get(fhirServer, "Questionnaire", requestQuestionnaireId);
        
        ///TODO plantilla Thymeleaf

        logger.info("OUT --- /facultativo/solicitar");
        //return "practitioner-request-questionnarie";
        //return questionnaireToFormPractitionerMapper.map(requestQuestionnarie);
        model.addAttribute("questionnaire", requestQuestionnarie.getResource());
		return "questionnaireForm";
    }


    /**
     * Controlador que gestiona las operaciones POST al recurso "/facultativo/solicitud".
     * Muestra la solicitud de consentimiento que se ha generado.
     * <br><br>
     * Procesa la respuesta al formulario, para generar la solicitud de consentimiento, y los pacientes
     * a los que va dirigida.
     * <br><br>
     * En cuanto a JBPM, va a finalizar la tarea manual, lo que provoca la creación de procesos de revisión
     * de consentimiento para cada uno de los pacientes a los que va dirigida la solicitud.
     * 
     * @param httpSession contiene los atributos de la sesión http
     * @param request representa la petición HTTP recibida, y contiene la información de esta
     * @return 
     */    
    @PostMapping("/facultativo/solicitud")
    public String postPractitionerRequest(HttpSession httpSession, HttpServletRequest request) {

        logger.info("IN --- POST /facultativo/solicitud");

        Long processInstanceId = (Long) httpSession.getAttribute("processInstanceId");
        String fhirServer = (String) httpSession.getAttribute("fhirServer");
        FhirDTO requestQuestionnarie = fhirDAO.get(
            fhirServer, "Questionnaire",(Long) httpSession.getAttribute("requestQuestionnaireId"));
        MapToQuestionnaireResponse mapToQuestionnaireResponseMapper = new MapToQuestionnaireResponse( requestQuestionnarie);
        Map<String, String[]> formResponse = null; 
        List<String> patientList = null;
        FhirDTO qestionnaireResponse = null;
        Long requestQuestionnarieResponseId = null;
        Map <String,Object> results = new HashMap<String,Object>();
        
        // Procesado de la respuesta al cuestionario que asiste en la creación de una solicitud de consentimiento
        formResponse = request.getParameterMap();
        patientList = Arrays.asList(formResponse.get("patients")[0].split(";"));
        qestionnaireResponse = mapToQuestionnaireResponseMapper.map(formResponse);
        qestionnaireResponse.setServer(fhirServer);
        requestQuestionnarieResponseId = fhirDAO.save(qestionnaireResponse);

        // Finalizalización de la tarea humana que corresponde a contestar al cuestionario
        results.put("requestQuestionnaireResponseId",requestQuestionnarieResponseId);
        results.put("patientList",patientList);
        consentRequestProcess.completeRequestTask(processInstanceId, results);
        logger.info("+ requestQuestionnarieResponseId: " + requestQuestionnarieResponseId);
        logger.info("+ patientList: " + patientList);
        
        logger.info("OUT --- POST /facultativo/solicitud");
        
        return "redirect:/facultativo/solicitudes/"+processInstanceId.toString();
    }
     
    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/solicitudes".
     * Genera contenido web que muestra una lista con las solicitudes pendientes de 
     * respuesta, emitidas por el facultativo.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "practitioner-request-list" plantilla thymeleaf
     */        
    @GetMapping("/facultativo/solicitudes")
    public String getPractitionerRequests(Model model) {

        logger.info("IN --- /facultativo/solicitudes");
        
        Authentication auth = null;
        UserDetails userDetails = null;
        List<RequestedConsent> requestConsentList = null;

        // Obtención del usuario que opera sobre el recurso para toda la sesión
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        logger.info("+ practitioner: " + userDetails.getUsername());

        // Obtención de la lista de solicitudes de consentimiento emitidas por el facultativo
        requestConsentList = kieConsentService.getRequestedConsentsByPractitioner(userDetails.getUsername());
        logger.info("Lista de solicitudes de consentimientos emitidas por el facultativo: ");
        for (RequestedConsent reviewedConsent: requestConsentList){
            logger.info(reviewedConsent.toString());    
        }

        model.addAttribute("requestConsentList", requestConsentList);
        
        logger.info("OUT --- /facultativo/solicitudes");
        return "practitioner-request-list";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/solicitud".
     * Genera contenido web que muestra en detalle la solicitud de consentimiento solicitada.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param id identifica la instancia de proceso "ConsentReview" que tiene asociada la solicitud de consentimiento
     * @return "practitioner-request-individual" plantilla thymeleaf
     */
    @GetMapping("/facultativo/solicitudes/{id}")
    public String getPractitionerRequestById(Model model, @PathVariable Long id) {
        
        logger.info("IN --- /facultativo/solicitud");
        
        //TODO

        //TODO plantilla Thymeleaf

        logger.info("OUT --- /facultativo/solicitud");
        return "practitioner-request-individual";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/consentimientos".
     * Genera contenido web que muestra una lista con los consentimientos obtenidos por el 
     * facultativo (solicitudes de consentimiento revisadas por los pacientes).
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "practitioner-consent-list" plantilla thymeleaf
     */ 
    @GetMapping("/facultativo/consentimientos")
    public String getPractitionerConsents(Model model) {

        logger.info("IN --- /facultativo/consentimientos");

        Authentication auth = null;
        UserDetails userDetails = null;
        List<ReviewedConsent> consentList = null;

        // Obtención del usuario que opera sobre el recurso para toda la sesión
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        logger.info("+ practitioner: " + userDetails.getUsername());

        // Obtención de la lista de consentimientos obtenidos por el facultativo
        consentList = kieConsentService.getConsentsByPractitioner(userDetails.getUsername());
        logger.info("Lista de consentimientos obtenidos por el facultativo: ");
        for (ReviewedConsent reviewedConsent: consentList){
            logger.info(reviewedConsent.toString());
        }

        model.addAttribute("consentList", consentList);
       
        logger.info("OUT --- /facultativo/consentimientos");
        return "practitioner-consent-list";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/consentimiento".
     * Genera contenido web que muestra en detalle un consentimiento otorgado por un 
     * paciente, que puede ser aceptados o rechazados.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param id identifica la instancia de proceso "ConsentReview" que tiene asociada el consentimiento
     * @return "practitioner-consent-individual" plantilla thymeleaf
     */
    @GetMapping("/facultativo/consentimientos/{id}")
    public String getPractitionerConsentById(Model model, @PathVariable Long id) {

        logger.info("IN --- /facultativo/consentimiento");
        
        //TODO

        //TODO plantilla Thymeleaf
        
        logger.info("OUT --- /facultativo/consentimiento");
        return "practitioner-consent-individual";
    }
}
