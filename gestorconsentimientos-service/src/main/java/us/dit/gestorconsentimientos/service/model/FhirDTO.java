package us.dit.gestorconsentimientos.service.model;

import org.hl7.fhir.instance.model.api.IBaseResource;

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
