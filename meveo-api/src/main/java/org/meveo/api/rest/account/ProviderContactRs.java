/**
 * @author Tyshan Shi(tyshan@manaty.net)
 * @since Jun 3, 2016 3:51:34 AM 
 */
package org.meveo.api.rest.account;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.ProviderContactDto;
import org.meveo.api.dto.response.account.ProviderContactResponseDto;
import org.meveo.api.dto.response.account.ProviderContactsResponseDto;
import org.meveo.api.rest.IBaseRs;

/**
 * @author Tyshan　Shi(tyshan@manaty.net)
 * @since Jun 3, 2016 3:51:34 AM
 *
 */

@Path("/account/providerContact")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Api("ProviderContact")
public interface ProviderContactRs extends IBaseRs {

	/**
	 * Create a provider contact
	 * @param providerContactDto The provider contact's data
	 * @return Request processing status
	 */
    @POST
    @Path("/")
    @ApiOperation(value="Create provider contact information")
    ActionStatus create(@ApiParam("Provider contact information") ProviderContactDto providerContactDto);

    /**
     * Update an existing provider contact
     * 
     * @param providerContactDto The provider contact's data
     * @return Request processing status
     */
    @PUT
    @Path("/")
    @ApiOperation(value="Update provider contact information")
    ActionStatus update(@ApiParam("Provider contact information") ProviderContactDto providerContactDto);

    /**
     * Search for a provider contact with a given code 
     * @param providerContactCode The provider contact's code
     * @return A provider contact
     */
    @GET
    @Path("/")
    @ApiOperation(value="Find provider contact information")
    ProviderContactResponseDto find(@QueryParam("providerContactCode") @ApiParam("The provider contact's code") String providerContactCode);

    /**
     * Remove an existing provider contact with a given code 
     * 
     * @param providerContactCode The provider contact's code
     * @return Request processing status
     */
    @DELETE
    @Path("/{code}")
    @ApiOperation(value="Remove provider contact information")
    ActionStatus remove(@PathParam("providerContactCode") @ApiParam("The provider contact's code") String providerContactCode);

    /**
     * List of provider contacts
     *
     * @return A list of provider contacts
     */
    @GET
    @Path("/list")
    ProviderContactsResponseDto list();
    
    /**
     * Create new or update an existing provider contact
     * 
     * @param providerContactDto The provider contact's data
     * @return Request processing status
     */
    @POST
    @Path("/createOrUpdate")
    ActionStatus createOrUpdate(@ApiParam("Provider contact information") ProviderContactDto providerContactDto);
}

