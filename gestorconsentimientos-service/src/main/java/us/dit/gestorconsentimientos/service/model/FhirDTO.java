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
package us.dit.gestorconsentimientos.service.model;

import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Clase DTO para los recursos FHIR.
 * 
 * @author Javier
 */
public class FhirDTO {
    
    private IBaseResource resource;

    private String server;

    public FhirDTO(IBaseResource resource){
        this.resource = resource;
    }

    public FhirDTO(String server, IBaseResource resource){
        this.resource = resource;
        this.server = server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public IBaseResource getResource(){
        return resource;
    }

    public String getServer() {
        return server;
    }
    
}
