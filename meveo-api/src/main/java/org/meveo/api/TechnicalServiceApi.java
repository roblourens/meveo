/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.api;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.TechnicalServicesDto;
import org.meveo.api.dto.technicalservice.InputOutputDescription;
import org.meveo.api.dto.technicalservice.ProcessDescriptionsDto;
import org.meveo.api.dto.technicalservice.TechnicalServiceDto;
import org.meveo.api.dto.technicalservice.TechnicalServiceFilters;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.technicalservice.DescriptionApi;
import org.meveo.exceptions.EntityAlreadyExistsException;
import org.meveo.model.technicalservice.Description;
import org.meveo.model.technicalservice.TechnicalService;
import org.meveo.service.technicalservice.TechnicalServiceService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TechnicalService management api
 *
 * @author Clément Bareth
 */
public abstract class TechnicalServiceApi<T extends TechnicalService, D extends TechnicalServiceDto> extends BaseApi {

    @Inject
    private DescriptionApi descriptionApi;

    private TechnicalServiceService<T> persistenceService;

    @PostConstruct
    private void init(){
        persistenceService = technicalServiceService();
    }

    protected D toDto(T technicalService) {
        D dto = newDto();
        dto.setCode(technicalService.getCode());
        List<InputOutputDescription> descriptionDtos = descriptionApi.fromDescriptions(technicalService);
        dto.setDescriptions(descriptionDtos);
        dto.setName(technicalService.getName());
        dto.setVersion(technicalService.getFunctionVersion());
        return dto;
    }

    protected T fromDto(D postData) throws EntityDoesNotExistsException, BusinessException {
        final T technicalService = newInstance();
        technicalService.setCode(postData.getCode());
        List<Description> descriptions = descriptionApi.fromDescriptionsDto(technicalService, postData);
        technicalService.setDescriptions(descriptions);
        technicalService.setName(postData.getName());
        technicalService.setFunctionVersion(postData.getVersion());
        return technicalService;
    }


    /**
     * Service required for database operations
     *
     * @return A TechnicalServiceService implementation instance
     */
    protected abstract TechnicalServiceService<T> technicalServiceService();

    /**
     * Instance of the entity to manage
     *
     * @return A new instance of the entity to manage
     */
    protected abstract T newInstance();

    protected abstract D newDto();

    /**
     * Create a technical service based on data provided in the dto.
     * If the version is not specified, create a new version of the technical service if it already exists
     *
     * @param postData Data used to create the technical service
     * @throws BusinessException If technical service already exists for specified name and version
     * @throws EntityDoesNotExistsException If elements referenced in the dto does not exists
     */
    public void create(@Valid @NotNull D postData) throws BusinessException, EntityDoesNotExistsException {
        final T technicalService = fromDto(postData);
        if (postData.getVersion() == null) {
            int versionNumber = 1;
            final Optional<Integer> latestVersion = persistenceService.latestVersionNumber(postData.getName());
            if (latestVersion.isPresent()) {
                versionNumber = latestVersion.get() + 1;
            }
            technicalService.setFunctionVersion(versionNumber);
        }
        try{
            if(getTechnicalService(postData.getName(), postData.getVersion()) != null){
                throw new EntityAlreadyExistsException(TechnicalService.class, postData.getCode());
            }
        } catch (EntityDoesNotExistsException e){
            persistenceService.create(technicalService);
        }

    }

    /**
     * Update the technical service with the specified information
     *
     * @param postData New data of the technical service
     * @throws EntityDoesNotExistsException if the technical service to update does not exists
     * @throws BusinessException            if the technical service can't be updated
     */
    public void update(@Valid @NotNull D postData) throws EntityDoesNotExistsException, BusinessException {
        final T technicalService = getTechnicalService(postData.getName(), postData.getVersion());
        final T updatedService = updateService(technicalService, postData);
        persistenceService.update(updatedService);
    }

    protected T updateService(T service, D data) throws EntityDoesNotExistsException, BusinessException {
        service.setDescriptions(descriptionApi.fromDescriptionsDto(service, data));
        return service;
    }

    /**
     * Update the technical service description with the specified information
     *
     * @param name            Name of the service
     * @param version         Version of the service - If not provided, will take highest version
     * @param descriptionsDto Description of the service
     * @throws EntityDoesNotExistsException If service to update does not exists
     * @throws BusinessException If an error occurs
     */
    public void updateDescription(String name, Integer version, @Valid ProcessDescriptionsDto descriptionsDto) throws EntityDoesNotExistsException, BusinessException {
        final T technicalService = getTechnicalService(name, version);
        persistenceService.removeDescription(technicalService.getId());
        final List<Description> descriptions = descriptionApi.fromDescriptionsDto(technicalService, descriptionsDto);
        technicalService.setDescriptions(descriptions);
        persistenceService.update(technicalService);
    }

    /**
     * Rename the technical service
     *
     * @param oldName Current name of the service
     * @param newName New name of the service
     * @throws BusinessException If an error occurs
     */
    public void rename(String oldName, String newName) throws BusinessException {
        final List<T> technicalServices = persistenceService.findByName(oldName);
        for (T t : technicalServices) {
            t.setName(newName);
            t.setCode(newName + "." + t.getFunctionVersion());
            persistenceService.update(t);
        }
    }

    /**
     * Re-number a technical service's version
     *
     * @param name       Name of the service
     * @param oldVersion Old version number
     * @param newVersion New version number
     * @throws EntityDoesNotExistsException If the service does not exists
     * @throws BusinessException If an error occurs
     */
    public void renameVersion(String name, Integer oldVersion, Integer newVersion) throws EntityDoesNotExistsException, BusinessException {
        final T service = persistenceService.findByNameAndVersion(name, oldVersion)
                .orElseThrow(() -> new EntityDoesNotExistsException(name + "." + oldVersion));
        service.setFunctionVersion(newVersion);
        service.setCode(name + "." + service.getFunctionVersion());
        persistenceService.update(service);
    }

    /**
     * Retrieve the technical service with the specified name and version. If version is not
     * provided, retrieve the last version of the technical service.
     *
     * @param name    Name of the technical service to retrieve
     * @param version Version of the technical service to retrieve
     * @return The DTO object corresponding to the technical service retrieved
     * @throws EntityDoesNotExistsException if the technical service does not exists
     */
    public TechnicalServiceDto findByNameAndVersionOrLatest(String name, Integer version) throws EntityDoesNotExistsException {
        T technicalService = getTechnicalService(name, version);
        return toDto(technicalService);
    }

    /**
     * List all the technical services present in database
     *
     * @param filters Filter used restrict results
     * @return The list of all technical services DTOs object retrieved
     */
    public TechnicalServicesDto list(TechnicalServiceFilters filters) {
        List<T> list = persistenceService.list(filters);
        List<TechnicalServiceDto> customEntityInstanceDTOs = list.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new TechnicalServicesDto(customEntityInstanceDTOs);
    }

    public TechnicalServicesDto findByNewerThan(TechnicalServiceFilters filters, Date sinceDate) {
        List<T> list = persistenceService.findByNewerThan(filters, sinceDate);
        List<TechnicalServiceDto> customEntityInstanceDTOs = list.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new TechnicalServicesDto(customEntityInstanceDTOs);
    }
    /**
     * List all the technical services present in database
     *
     * @param filters Filter used restrict results
     * @return The number of technical service corresponding to the specified filters
     */
    public long count(TechnicalServiceFilters filters) {
        return persistenceService.count(filters);
    }

    /**
     * List of all the versions for a specified technical service name
     *
     * @param name Name of the technical service to retrieve versions
     * @return The different versions of the technical service
     * @throws EntityDoesNotExistsException If no versions of the technical service exists
     */
    public TechnicalServicesDto listByName(String name) throws EntityDoesNotExistsException {
        List<T> list = persistenceService.findByName(name);
        if (list.isEmpty()) {
            throw new EntityDoesNotExistsException("TechnicalService with name " + name + " does not exists");
        }
        List<TechnicalServiceDto> customEntityInstanceDTOs = list.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new TechnicalServicesDto(customEntityInstanceDTOs);
    }

    /**
     * Remove a technical service. If version is provided, only remove the specified version.
     *
     * @param name    Name of the technical service to remove
     * @param version Specific version of the technical service to remove - All versions will be
     *                removed if not provided.
     * @throws EntityDoesNotExistsException If no technical service corresponds to the provided name
     * @throws BusinessException            If the technical service can't be removed
     */
    public void remove(String name, Integer version) throws EntityDoesNotExistsException, BusinessException {
        if (version != null) {
            final T service = getTechnicalServiceByNameAndVersion(name, version);
            persistenceService.remove(service);
        } else {
            List<T> list = persistenceService.findByName(name);
            for (T technicalService : list) {
                persistenceService.remove(technicalService);
            }
        }
    }

    /**
     * Create or update a technical service based upon the provided data
     *
     * @param postData Data used to create or update the technical service
     * @throws BusinessException if the technical service can't be created or updated
     * @throws EntityDoesNotExistsException If an entity referenced in the dto does not exist
     */
    public void createOrUpdate(@Valid @NotNull D postData) throws BusinessException, EntityDoesNotExistsException {
        try {
            update(postData);
        } catch (EntityDoesNotExistsException e) {
            create(postData);
        }
    }

    /**
     * Check for the specified service existance
     *
     * @param name Name of the service
     * @param version version of the service - if not provided, will check if at least one service with that
     *                name exists
     * @return {@code true} if the service exists
     */
    public boolean exists(String name, Integer version) {
        if (name == null) {
            throw new IllegalArgumentException("Name must be provided");
        }
        if (version != null) {
            return persistenceService.findByNameAndVersion(name, version).isPresent();
        }
        return !persistenceService.findByName(name).isEmpty();
    }

    /**
     *
     * @return Names of every service present in the database
     */
    public List<String> names() {
        return persistenceService.names();
    }

    /**
     *
     * @param name Name of the service
     * @return Versions numbers for a service with the given name
     */
    public List<Integer> versions(String name) {
        return persistenceService.versions(name);
    }

    /**
     *
     * @param name Name of the service
     * @param version Version of the service - if not provided, will take last version
     * @return The description of the specified service
     * @throws EntityDoesNotExistsException If service does not exists
     */
    public List<InputOutputDescription> description(String name, Integer version) throws EntityDoesNotExistsException {
        // First, retrieve code of the technical service
        if (version == null) {
            // Use latest version if version is not provided
            version = persistenceService.latestVersionNumber(name)
                    .orElseThrow(() -> getEntityDoesNotExistsException(name));
        }
        final Integer finalVersion = version;
        List<Description> description = persistenceService.description(buildCode(name, finalVersion));
        return InputOutputDescription.fromDescriptions(description);
    }



    /**
     * @param name    Name of the service to find
     * @param version Version of the service to find - if not specified, will take the last version
     * @return The technical service corresponding to specified name and version
     * @throws EntityDoesNotExistsException If no match is found
     */
    private T getTechnicalService(String name, Integer version) throws EntityDoesNotExistsException {
        T technicalService;
        if (version != null) {
            technicalService = getTechnicalServiceByNameAndVersion(name, version);
        } else {
            technicalService = persistenceService.findLatestByName(name)
                    .orElseThrow(() -> getEntityDoesNotExistsException(name));
        }
        return technicalService;
    }

    private T getTechnicalServiceByNameAndVersion(String name, Integer version) throws EntityDoesNotExistsException {
        return persistenceService.findByNameAndVersion(name, version)
                .orElseThrow(() -> getEntityDoesNotExistsException(name, version));
    }

    private EntityDoesNotExistsException getEntityDoesNotExistsException(String name) {
        return new EntityDoesNotExistsException("TechnicalService with name " + name + " does not exists");
    }

    private EntityDoesNotExistsException getEntityDoesNotExistsException(String name, Integer version) {
        return new EntityDoesNotExistsException("TechnicalService with name " + name + " and version " + version + " does not exists");
    }

    private String buildCode(String name, Integer version) {
        return name + "." + version;
    }
}
