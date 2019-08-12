/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. This program is
 * not suitable for any direct or indirect application in MILITARY industry See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.meveo.api.rest.technicalservice.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Class representing the response of the call of an endpoint.
 * It allows to override the default comportment of the endpoint servlet.
 * 
 * @author clement.bareth
 *
 */
/**
 * @author clement.bareth
 *
 */
public class EndpointResponse {

	/**
	 * Servlet response
	 */
    private HttpServletResponse httpServletResponse;
    
    /**
     * Content to write on the servlet response
     */
    private byte[] output;
    
    /**
     * If error, content to write on the servlet response
     */
    private String errorMessage;
    
    /**
     * MIME type of the response
     */
    private String contentType;
    
    /**
     * Status of the response
     */
    private Integer status;

    public EndpointResponse(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }
    
    public void setHeader(String headerName, String value) {
    	httpServletResponse.setHeader(headerName, value);
    }
    
    public void setOutput(byte[] output) throws IOException {
    	this.output = output;
    }
    
    public void setError(String errorMessage) {
    	this.errorMessage = errorMessage;
    }

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public byte[] getOutput() {
		return output;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
    
}