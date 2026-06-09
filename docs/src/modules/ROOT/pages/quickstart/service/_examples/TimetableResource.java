package org.acme.schooltimetabling.rest;

import jakarta.ws.rs.Path;
import ai.timefold.sdk.rest.api.ModelRest;

@Path("/v1/timetables")
public interface TimetableResource extends ModelRest {
}
