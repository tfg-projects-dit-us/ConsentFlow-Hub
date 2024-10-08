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
package us.dit.gestorconsentimientos.service.services.mapper;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemAnswerOptionComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.UriType;
import org.springframework.stereotype.Service;

import us.dit.gestorconsentimientos.service.model.FhirDAO;
import us.dit.gestorconsentimientos.service.model.FhirDTO;


/**
 * Mapper que permite la generación del contenido HTML para mostrar las respuestas de un 
 * formulario a partir de un recurso Fhir de tipo QuestionnaireResponse.
 * 
 * @author Jose Antonio García Linares
 */
@Service
public class QuestionnaireResponseToViewForm{

	private static final Logger logger = LogManager.getLogger();

	private static FhirDAO fhirDAO = new FhirDAO();

	private String fhirServer;

	public String map(FhirDTO questionnaireResponseDTO) {
				
		logger.info("Mapeo de QuestionnaireResponse a HTML");		
		
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) questionnaireResponseDTO.getResource();
		
		fhirServer = questionnaireResponseDTO.getServer();

		String contentHtml = generateHtml(questionnaireResponse);
		
		return contentHtml;
	}

	private String generateHtml(QuestionnaireResponse questionnaireResponse) {
		String html = "<!DOCTYPE html>\r\n"
				+ "<html>\r\n";
		
		String head = generateHead();
		html = html + head;
		
		String body = generateBody(questionnaireResponse);
		html = html + body;
		
		html = html + "</html>";
		
		return html;
	}
	
	private String generateHead() {
		String head = null;
		String style = generateStyle();
		
		head = "<head>\r\n"
				+ "<meta charset=\"ISO-8859-1\">\r\n"
				+ "<title>Gestor Consentimientos</title>\r\n"
				+ style
				+ "</head>\r\n";
		
		return head;
	}
	
	private String generateBody(QuestionnaireResponse questionnaireResponse) {
		String body = null;
		
		body = "<body>\r\n"
				+ "<a class=\"home-link\" href=\"/menu/\">Back</a>\r\n"
				+"<div class=\"box\">\r\n"
				+ "<h2>Meta-Questionnaire</h2>\r\n"
				+ "<form>\r\n";
		
		Questionnaire metaQuestionnaire = (Questionnaire) fhirDAO.get(
			fhirServer,"Questionnaire",
			new IdType(new UriType(questionnaireResponse.getQuestionnaire())).getIdPartAsLong()).getResource();
		
		for (Questionnaire.QuestionnaireItemComponent item : metaQuestionnaire.getItem()) {
			QuestionnaireResponse.QuestionnaireResponseItemComponent it = null;
			
			for (QuestionnaireResponse.QuestionnaireResponseItemComponent itemResponse : questionnaireResponse.getItem()) {
				if (itemResponse.getLinkId().equals(item.getLinkId())) {
					it = itemResponse;
				}
			}
			
			String question = generateQuestion(item, it);
			body = body + question;
		}
		
		body = body
				+ "</form>\r\n"
				+ "</div>\r\n"
				+ "</body>\r\n";
		
		return body;
	}
	
	private String generateQuestion(Questionnaire.QuestionnaireItemComponent item, QuestionnaireResponse.QuestionnaireResponseItemComponent itemResponse) {
		String question = null;
		
		switch (item.getType()) {
		case STRING:
			question = createStringComponent(item, itemResponse);
			break;
		case DATE:
			question = createDateComponent(item, itemResponse);
			break;
		case CODING:
			question = createChoiceComponent(item, itemResponse);
			break;
		case GROUP:
			question = createGroupComponent(item, itemResponse);
			break;
		default:
			throw new UnsupportedOperationException("Tipo de componente no soportado: " + item.getType().getDisplay());
		}
		
		return question;
	}
	
	private String createGroupComponent(Questionnaire.QuestionnaireItemComponent item, QuestionnaireResponse.QuestionnaireResponseItemComponent itemResponse) {
		String nameGroup = item.getText() + ":";
		String component = "<fieldset>\r\n"
				+ "<legend>+" + nameGroup + "+</legend>\r\n";
		
		for (Questionnaire.QuestionnaireItemComponent item1 : item.getItem()) {
			QuestionnaireResponse.QuestionnaireResponseItemComponent it1 = null;
			
			for (QuestionnaireResponse.QuestionnaireResponseItemComponent itemResponse1 : itemResponse.getItem()) {
				if (itemResponse1.getLinkId().equals(item1.getLinkId())) {
					it1 = itemResponse1;
				}
			}
			
			switch (item1.getType()) {
			case STRING:
				component = component + createStringComponent(item1, it1);
				break;
			case DATE:
				component = component + createDateComponent(item1, it1);
				break;
			case CODING:
				component = component + createChoiceComponent(item1, it1);
				break;
			case GROUP:
				component = component + createGroupComponent(item1, it1);
				break;
			default:
				throw new UnsupportedOperationException("Tipo de componente no soportado: " + item1.getType().getDisplay());
			}
		}
		
		component = component + "</fieldset>\r\n";
		
		return component;
	}
	
	private String createStringComponent(Questionnaire.QuestionnaireItemComponent item, QuestionnaireResponse.QuestionnaireResponseItemComponent itemResponse) {
		String question = item.getText();
		String id = item.getLinkId();
		
		String component = null;
		
		if (itemResponse != null) {
			String value = itemResponse.getAnswer().get(0).getValue().toString();
		
			component = "<label for=\"" + id + "\">" + question + "</label>\r\n"
				+ "<input type=\"text\" id=\"" + id + "\" name=\"" + id + "\" value=\"" + value + "\" readonly>\r\n";
		} else {
			component = "<label for=\"" + id + "\">" + question + "</label>\r\n"
					+ "<input type=\"text\" id=\"" + id + "\" name=\"" + id + "\" readonly>\r\n";
		}
		
		return component;
	}
	
	private String createDateComponent(Questionnaire.QuestionnaireItemComponent item, QuestionnaireResponse.QuestionnaireResponseItemComponent itemResponse) {
		String question = item.getText();
		String id = item.getLinkId();
		
		String component = null;
		
		if (itemResponse != null) {
			String value = itemResponse.getAnswer().get(0).getValue().toString();
		
			component = "<label for=\"" + id + "\">" + question + "</label>\r\n"
					+ "<div>\r\n"
					+ "<span>End:</span>\r\n"
					+ "<input type=\"date\" id=\"" + id + "\" name=\"" + id + "\" value=\"" + value + "\" readonly>\r\n"
					+ "</div>\r\n";
		} else {
			component = "<label for=\"" + id + "\">" + question + "</label>\r\n"
					+ "<div>\r\n"
					+ "<span>Start:</span>\r\n"
					+ "<input type=\"date\" id=\"" + id + "\" name=\"" + id + "\" readonly>\r\n"
					+ "</div>\r\n"
					+ "<div>\r\n"
					+ "<span>End:</span>\r\n"
					+ "<input type=\"date\" id=\"" + id + "\" name=\"" + id + "\" readonly>\r\n"
					+ "</div>\r\n";
		}
		
		return component;
	}
	
	private String createChoiceComponent(Questionnaire.QuestionnaireItemComponent item, QuestionnaireResponse.QuestionnaireResponseItemComponent itemResponse) {
		String question = item.getText();
		String id = item.getLinkId();
		List<QuestionnaireItemAnswerOptionComponent> options = item.getAnswerOption();
		
		String component = null;
		
		if (itemResponse != null) {
			String values = itemResponse.getAnswer().get(itemResponse.getAnswer().size()-1).getValue().toString();
			component = "<label>" + question + "</label>\r\n"
					+ "      <div class=\"checkbox-group\">\r\n";
			
			for (QuestionnaireItemAnswerOptionComponent option : options) {
				String code = option.getValueCoding().getCode();
				String display = option.getValueCoding().getDisplay();
				if (checkMark(values, code)) {
					component = component + "<label><input type=\"checkbox\" name=\"" + id + "\" value=\"" + code + "\" checked disabled><span>" + display + "</span></label>\r\n";
				} else {
					component = component + "<label><input type=\"checkbox\" name=\"" + id + "\" value=\"" + code + "\" disabled><span>" + display + "</span></label>\r\n";
				}
			}
			
			component = component + "      </div>\r\n";
		} else {
			component = "<label>" + question + "</label>\r\n"
					+ "      <div class=\"checkbox-group\">\r\n";
			
			for (QuestionnaireItemAnswerOptionComponent option : options) {
				String code = option.getValueCoding().getCode();
				String display = option.getValueCoding().getDisplay();
				component = component + "<label><input type=\"checkbox\" name=\"" + id + "\" value=\"" + code + "\" disabled><span>" + display + "</span></label>\r\n";
			}
			
			component = component + "      </div>\r\n";
		}
		
		return component;
	}
	
	private boolean checkMark(String values, String code) {
		Boolean check = false;
		
		for (String value : values.split(";")) {
			if (value.equals(code)) {
				check = true;
			}
		}
		
		return check;
	}
	
	private String generateStyle() {
		String style = "<style>\r\n"
				+ "html {\r\n"
				+ "	height: 100%;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ "body {\r\n"
				+ "	margin: 0;\r\n"
				+ "	padding: 0;\r\n"
				+ "	font-family: sans-serif;\r\n"
				+ "	background: linear-gradient(#141e30, #243b55);\r\n"
				+ "	background-attachment: fixed;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ "h1 {\r\n"
				+ "	color: #333333;\r\n"
				+ "	text-align: center;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ ".box {\r\n"
				+ "	max-width: 500px;\r\n"
				+ "	margin: 0 auto;\r\n"
				+ "	background-color: #ffffff;\r\n"
				+ "	padding: 20px;\r\n"
				+ "	border-radius: 5px;\r\n"
				+ "	box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ "label {\r\n"
				+ "	display: block;\r\n"
				+ "	margin-bottom: 10px;\r\n"
				+ "	font-weight: bold;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ "input[type=\"text\"], input[type=\"date\"] {\r\n"
				+ "	width: 100%;\r\n"
				+ "	height: 20px;\r\n"
				+ "	padding: 10px;\r\n"
				+ "	border: 1px solid #cccccc;\r\n"
				+ "	border-radius: 4px;\r\n"
				+ "	box-sizing: border-box;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ ".checkbox-group {\r\n"
				+ "	display: grid;\r\n"
				+ "	grid-template-columns: repeat(3, 1fr);\r\n"
				+ "	grid-gap: 10px;\r\n"
				+ "	margin-bottom: 10px;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ ".checkbox-group label {\r\n"
				+ "	display: flex;\r\n"
				+ "	align-items: center;\r\n"
				+ "	font-size: 14px;\r\n"
				+ "	font-weight: normal;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ ".checkbox-group label input[type=\"checkbox\"] {\r\n"
				+ "	margin-right: 5px;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ ".box input[type=\"submit\"] {\r\n"
				+ "	display: block;\r\n"
				+ "	margin: 20px auto;\r\n"
				+ "	background-color: #4CAF50;\r\n"
				+ "	color: #ffffff;\r\n"
				+ "	padding: 10px 20px;\r\n"
				+ "	border: none;\r\n"
				+ "	border-radius: 4px;\r\n"
				+ "	cursor: pointer;\r\n"
				+ "}\r\n"
				+ "\r\n"
				+ ".box input[type=\"submit\"]:hover {\r\n"
				+ "	background-color: #45a049;\r\n"
				+ "}\r\n"
				+ ".home-link {\r\n"
				+ "    position: absolute;\r\n"
				+ "    top: 20px;\r\n"
				+ "    left: 20px;\r\n"
				+ "    font-size: 18px;\r\n"
				+ "    color: #fff;\r\n"
				+ "    text-decoration: none;\r\n"
				+ "    cursor: pointer;\r\n"
				+ "}\r\n"
				+ ".checkbox-group label input[type=\"checkbox\"]:checked + span {\r\n"
				+ "  font-weight: bold;\r\n"
				+ "}\r\n"
				+ "</style>\r\n";
		
		return style;
	}

}
