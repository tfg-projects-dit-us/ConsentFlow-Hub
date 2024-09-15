package us.dit.gestorconsentimientos.service.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import us.dit.gestorconsentimientos.model.RequestedConsent;
import us.dit.gestorconsentimientos.model.ReviewedConsent;
import us.dit.gestorconsentimientos.service.model.FhirDAO;
import us.dit.gestorconsentimientos.service.model.FhirDTO;
import us.dit.gestorconsentimientos.service.services.kie.KieConsentService;
import us.dit.gestorconsentimientos.service.services.kie.process.ConsentRequestProcessService;
import us.dit.gestorconsentimientos.service.services.mapper.MapToQuestionnaireResponse;
import us.dit.gestorconsentimientos.service.services.mapper.QuestionnaireResponseToViewForm;

/**
 * Controlador que va a atender las operaciones que van destinadas a los recursos que
 * pueden utilizar los facultativos.
 * 
 * @author Javier
 */
@Controller
@RequestMapping("/facultativo")
public class PractitionerController {

	private static final Logger logger = LogManager.getLogger();

    
    @Autowired
    private ConsentRequestProcessService consentRequestProcess;
    
    @Autowired
    private KieConsentService kieConsentService;
    
    private static FhirDAO fhirDAO = new FhirDAO();

    @Value("${fhirserver.location}")
	private String fhirServer;

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo".
     * Genera el contenido web pertinente al menú para los facultativo.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "practitioner-menu" plantilla thymeleaf
     */
    @GetMapping
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
     * <br><br>
     * Los parámetros que trae el WI de la tarea humana que se inicia son: 
     * + fhirServer
     * + requestQuestionnaireId
     * 
     * @param httpSession contiene los atributos de la sesión http
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "practitioner-request-questionnarie" plantilla thymeleaf
     */
    @GetMapping("/solicitar")
    //@ResponseBody
    public String getPractitionerRequestQuestionnarie(HttpSession httpSession, Model model) {
        
        logger.info("IN --- /facultativo/solicitar");

        Authentication auth = null;
        UserDetails userDetails = null;
        Map<String,Object> params = new HashMap<String,Object>();
        Long processInstanceId = null;
        Map<String,Object> vars = null;
        Long requestQuestionnaireId = null;
        FhirDTO requestQuestionnarie = null;

        // Obtención del usuario que opera sobre el recurso para toda la sesión
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        logger.info("+ practitioner: " + userDetails.getUsername());    

        // Instanciación del proceso de solicitud de consentimiento
        params.put("practitioner",userDetails.getUsername());
        processInstanceId = consentRequestProcess.createProcessInstance(params);
        logger.info("Vuelvo al controlador con processInstanceId: " + processInstanceId+" que meto en los datos de session");
        
        httpSession.setAttribute("processInstanceId", processInstanceId);
              
        // Obtención del cuestionario a mostrar e inicio de la tarea
        vars = consentRequestProcess.initRequestTask(processInstanceId,userDetails.getUsername());
        
        requestQuestionnaireId = (Long) vars.get("requestQuestionnaireId");
        logger.info("+ fhirServer: ", fhirServer);
        logger.info("+ requestQuestionnaireId: ", requestQuestionnaireId);
        httpSession.setAttribute("requestQuestionnaireId", requestQuestionnaireId);
        requestQuestionnarie = fhirDAO.get(fhirServer, "Questionnaire", requestQuestionnaireId);

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
     * <br><br>
     * Los parámetros de salida del WI de la tarea humana que completa son: 
     * + requestQuestionnaireResponseId
     * + patientList
     * 
     * @param httpSession contiene los atributos de la sesión http
     * @param request representa la petición HTTP recibida, y contiene la información de esta
     * @return 
     */    
    @PostMapping("/solicitud")
    public String postPractitionerRequest(HttpSession httpSession, HttpServletRequest request) {

        logger.info("IN --- POST /facultativo/solicitud");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
       
        Long processInstanceId = (Long) httpSession.getAttribute("processInstanceId");
        FhirDTO requestQuestionnarie = fhirDAO.get(
            fhirServer, "Questionnaire",(Long) httpSession.getAttribute("requestQuestionnaireId"));
        MapToQuestionnaireResponse mapToQuestionnaireResponseMapper = new MapToQuestionnaireResponse(
            requestQuestionnarie, 
            userDetails.getUsername(),
            "Practitioner",
            processInstanceId);

        List<String> patientList = null;
        FhirDTO questionnaireResponse = null;
        List <Long> requestQuestionnarieResponseIdList = new ArrayList<Long>();
        Map <String,Object> results = new HashMap<String,Object>();
        
        // Procesado de la respuesta al cuestionario que asiste en la creación de una solicitud de consentimiento
        System.out.println("LOG");
        patientList = Arrays.asList(request.getParameter("patients").split(","));
        System.out.println(patientList);
        
        for (String patient:patientList){
            request.setAttribute("patients", patient);
            System.out.println(request.getParameter("patients").toString());
            questionnaireResponse = mapToQuestionnaireResponseMapper.map(request.getParameterMap());
            questionnaireResponse.setServer(fhirServer);
            requestQuestionnarieResponseIdList.add(fhirDAO.save(questionnaireResponse));
        }

        // Finalizalización de la tarea humana que corresponde a contestar al cuestionario
        results.put("requestQuestionnaireResponseId",requestQuestionnarieResponseIdList.get(0));
        results.put("patientList",patientList);
        consentRequestProcess.completeRequestTask(processInstanceId, results, userDetails.getUsername());
        logger.info("+ requestQuestionnarieResponseId: " + requestQuestionnarieResponseIdList.get(0));
        logger.info("+ patientList: " + patientList);
        
        logger.info("OUT --- POST /facultativo/solicitud");
        
        return "redirect:/facultativo/solicitudes/";
    }
     
    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/solicitudes".
     * Genera contenido web que muestra una lista con las solicitudes pendientes de 
     * respuesta, emitidas por el facultativo.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "practitioner-request-list" plantilla thymeleaf
     */        
    @GetMapping("/solicitudes")
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
        requestConsentList = fhirDAO.searchConsentRequestByPerson(fhirServer,"Practitioner",userDetails.getUsername());
        //requestConsentList = kieConsentService.getRequestedConsentsByPractitioner(userDetails.getUsername());
        logger.info("Lista de solicitudes de consentimientos emitidas por el facultativo: ");
        for (RequestedConsent reviewedConsent: requestConsentList){
            logger.info(reviewedConsent.toString());    
        }

        model.addAttribute("requestConsentList", requestConsentList);
        
        logger.info("OUT --- /facultativo/solicitudes");
        return "practitioner-request-list";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/solicitudes/{id}".
     * Genera contenido web que muestra en detalle la solicitud de consentimiento solicitada.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param id identifica la instancia de proceso "ConsentReview" que tiene asociada la solicitud de consentimiento
     * @return "practitioner-request-individual" plantilla thymeleaf
     */
    @GetMapping("/solicitudes/{id}")
    @ResponseBody
    public String getPractitionerRequestById(@PathVariable Long id) {
        
        logger.info("IN --- /facultativo/solicitudes/"+Long.toString(id));
        
        RequestedConsent requestedConsent = null;
        Long questionnaireResponseId = null;
        FhirDTO questionnaireResponse = null;
        QuestionnaireResponseToViewForm questionnaireResponseToViewForm = new QuestionnaireResponseToViewForm();
        String result = null;

        // Obtención del ID del questionnaireResponse que es la respuesta al cuestionario con el que un facultativo ha creado una solicitud de consentimiento.
        requestedConsent = kieConsentService.getRequestedConsentByConsentReviewInstanceId(id);

        if (requestedConsent != null){
            questionnaireResponseId = requestedConsent.getRequestQuestionnaireResponseId();
            questionnaireResponse = fhirDAO.get(fhirServer, "QuestionnaireResponse", questionnaireResponseId);
        
            result = questionnaireResponseToViewForm.map(questionnaireResponse);
        }else{
            // TODO Poner plantilla de error cuando se implemente la plantilla para la respuesta correcta en lugar de utilizar el mapper
            result = "ERROR";
        }
        
        logger.info("OUT --- /facultativo/solicitudes/"+Long.toString(id));
        //TODO plantilla Thymeleaf que muestre una solicitud de consentimiento, a partir de un recurso FHIR de tipo QuestionnaireResponse
        // return "practitioner-request-individual";
        return result;
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/consentimientos".
     * Genera contenido web que muestra una lista con los consentimientos obtenidos por el 
     * facultativo (solicitudes de consentimiento revisadas por los pacientes).
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "practitioner-consent-list" plantilla thymeleaf
     */ 
    @GetMapping("/consentimientos")
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
        //consentList = kieConsentService.getConsentsByPractitioner(userDetails.getUsername());
        consentList = fhirDAO.searchConsentReviewByPerson(fhirServer,"Practitioner",userDetails.getUsername());
        logger.info("Lista de consentimientos obtenidos por el facultativo: ");
        for (ReviewedConsent reviewedConsent: consentList){
            logger.info(reviewedConsent.toString());
        }

        model.addAttribute("consentList", consentList);

        logger.info("OUT --- /facultativo/consentimientos");
        return "practitioner-consent-list";
    }

    /**
     * Controlador que gestiona las operaciones GET al recurso "/facultativo/consentimientos/{id}".
     * Genera contenido web que muestra en detalle un consentimiento otorgado por un 
     * paciente, que puede ser aceptados o rechazados.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @param id identifica la instancia de proceso "ConsentReview" que tiene asociada el consentimiento
     * @return "practitioner-consent-individual" plantilla thymeleaf
     */
    @GetMapping("/consentimientos/{id}")
    @ResponseBody
    public String getPractitionerConsentById(@PathVariable Long id) {

        logger.info("IN --- /facultativo/consentimientos/"+Long.toString(id));
        
        //TODO Hay que hacer el cambio en RequestedConsent (definición de proceso, y de la entidad del modelo de datos) y añadir el id del recurso Fhir Consent, y dejar de utilizar por tanto QuestionnaireResponse para representar Consent (aunque se siga manteniendo)

        ReviewedConsent reviewedConsent = null;
        Long questionnaireResponseId = null;
        FhirDTO questionnaireResponse = null;
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
        
        logger.info("OUT --- /facultativo/consentimientos/"+Long.toString(id));
        
        //TODO plantilla Thymeleaf que muestre un consentimiento, a partir de un recurso FHIR de tipo Consent
        //return "practitioner-consent-individual";

        return result;
    }
}
