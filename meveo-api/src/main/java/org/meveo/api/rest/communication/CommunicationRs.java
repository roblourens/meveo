package org.meveo.api.rest.communication;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.communication.CommunicationRequestDto;
import org.meveo.api.rest.IBaseRs;

/**
 * @author phung
 *
 */
@Path("Communication")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Api("Communication")
public interface CommunicationRs extends IBaseRs {

    /**
     * Receives inbound communication from external source given the rest url above. MEVEO handles it by throwing an inbount communication event with the communicationRequestDto.
     * 
     * @param communicationRequestDto communication request
     * @return Request processing status
     */
    @POST
    @Path("/inbound")
    @ApiOperation(value ="Inbound communication")
    ActionStatus inboundCommunication(@ApiParam("Communication request information") CommunicationRequestDto communicationRequestDto);

}
