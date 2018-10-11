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
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.EntityDescriptionDto;
import org.meveo.api.dto.TechnicalServicesDto;
import org.meveo.api.dto.technicalservice.*;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.customEntities.CustomRelationshipTemplate;
import org.meveo.model.technicalservice.*;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.custom.CustomRelationshipTemplateService;
import org.meveo.service.technicalservice.TechnicalServiceService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TechnicalService management api
 *
 * @author Clément Bareth
 */
@Stateless
public abstract class TechnicalServiceApi<T extends TechnicalService, S extends TechnicalServiceService<T>>
        extends BaseApi {

    @Inject
    private CustomEntityTemplateService customEntityTemplateService;

    @Inject
    private CustomRelationshipTemplateService customRelationshipTemplateService;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    private TechnicalServiceDto toDto(TechnicalService technicalService) {
        final TechnicalServiceDto dto = new TechnicalServiceDto();
        dto.setCode(technicalService.getCode());
        List<InputOutputDescriptionDto> descriptionDtos = toDescriptionsDto(technicalService);
        dto.setDescriptions(descriptionDtos);
        dto.setName(technicalService.getName());
        dto.setVersion(technicalService.getVersion());
        dto.setScript(technicalService.getScript());
        dto.setServiceType(technicalService.getServiceType());
        return dto;
    }

    private List<InputOutputDescriptionDto> toDescriptionsDto(TechnicalService technicalService) {
        return technicalService.getDescriptions()
                .stream()
                .map(this::fromDescription)
                .collect(Collectors.toList());
    }

    private InputOutputDescriptionDto fromDescription(Description desc) {
        InputOutputDescriptionDto descriptionDto = new EntityDescriptionDto();
        descriptionDto.setName(desc.getName());
        descriptionDto.setType(desc.getTypeName());
        descriptionDto.setInput(desc.isInput());
        descriptionDto.setOutput(desc.isOutput());
        if (desc instanceof RelationDescription) {
            descriptionDto = new RelationDescriptionDto();
            ((RelationDescriptionDto) descriptionDto).setSource(((RelationDescription) desc).getSource());
            ((RelationDescriptionDto) descriptionDto).setTarget(((RelationDescription) desc).getTarget());
        }
        final List<InputPropertyDto> inputProperties = new ArrayList<>();
        final List<OutputPropertyDto> outputProperties = new ArrayList<>();
        for (InputProperty p : desc.getInputProperties()) {
            InputPropertyDto inputPropertyDto = new InputPropertyDto();
            String property = p.getProperty().getCode();
            inputPropertyDto.setProperty(property);
            inputPropertyDto.setComparator(p.getComparator());
            inputPropertyDto.setComparisonValue(p.getComparisonValue());
            inputPropertyDto.setDefaultValue(p.getDefaultValue());
            inputPropertyDto.setRequired(p.isRequired());
            inputProperties.add(inputPropertyDto);
        }
        for (OutputProperty p : desc.getOutputProperties()) {
            OutputPropertyDto outputPropertyDto = new OutputPropertyDto();
            String property = p.getProperty().getCode();
            outputPropertyDto.setProperty(property);
            outputPropertyDto.setTrustness(p.getTrustness());
            outputProperties.add(outputPropertyDto);
        }
        return descriptionDto;
    }

    private Description toDescription(InputOutputDescriptionDto dto) throws EntityDoesNotExistsException {
        Description description;
        String code;
        if (dto instanceof EntityDescriptionDto) {
            description = new EntityDescription();
            ((EntityDescription) description).setName(dto.getName());
            final CustomEntityTemplate customEntityTemplate = customEntityTemplateService.findByCode(dto.getType());
            if (customEntityTemplate == null) {
                throw new EntityDoesNotExistsException(CustomEntityTemplate.class, dto.getType());
            }
            ((EntityDescription) description).setType(customEntityTemplate);
            code = customEntityTemplate.getCode();
        } else {
            description = new RelationDescription();
            ((RelationDescription) description).setSource(((RelationDescriptionDto) dto).getSource());
            ((RelationDescription) description).setTarget(((RelationDescriptionDto) dto).getTarget());
            final CustomRelationshipTemplate customRelationshipTemplate = customRelationshipTemplateService.findByCode(dto.getType());
            if (customRelationshipTemplate == null) {
                throw new EntityDoesNotExistsException(CustomRelationshipTemplate.class, dto.getType());
            }
            ((RelationDescription) description).setType(customRelationshipTemplate);
            code = customRelationshipTemplate.getCode();
        }
        Map<String, CustomFieldTemplate> customFields = customFieldTemplateService.findByAppliesTo(code);
        description.setInput(dto.isInput());
        description.setOutput(dto.isOutput());
        final List<InputProperty> inputProperties = new ArrayList<>();
        final List<OutputProperty> outputProperties = new ArrayList<>();
        for (InputPropertyDto p : dto.getInputProperties()) {
            InputProperty inputProperty = new InputProperty();
            CustomFieldTemplate property = customFields.get(p.getProperty());
            if (property == null) {
                throw new EntityDoesNotExistsException(CustomRelationshipTemplate.class, p.getProperty());
            }
            inputProperty.setProperty(property);
            inputProperty.setComparator(p.getComparator());
            inputProperty.setComparisonValue(p.getComparisonValue());
            inputProperty.setDefaultValue(p.getDefaultValue());
            inputProperty.setRequired(p.isRequired());
            inputProperties.add(inputProperty);
        }
        for (OutputPropertyDto p : dto.getOutputProperties()) {
            OutputProperty outputProperty = new OutputProperty();
            CustomFieldTemplate property = customFields.get(p.getProperty());
            if (property == null) {
                throw new EntityDoesNotExistsException(CustomRelationshipTemplate.class, p.getProperty());
            }
            outputProperty.setProperty(property);
            outputProperty.setTrustness(p.getTrustness());
            outputProperties.add(outputProperty);
        }
        return description;
    }

    private T fromDto(TechnicalServiceDto postData) throws EntityDoesNotExistsException {
        final T technicalService = newInstance();
        technicalService.setCode(postData.getCode());
        List<Description> descriptions = fromDescriptionsDto(postData);
        technicalService.setDescriptions(descriptions);
        technicalService.setName(postData.getName());
        technicalService.setVersion(postData.getVersion());
        technicalService.setScript(postData.getScript());
        technicalService.setServiceType(postData.getServiceType());
        return technicalService;
    }

    private List<Description> fromDescriptionsDto(TechnicalServiceDto postData) throws EntityDoesNotExistsException {
        List<Description> descriptions = new ArrayList<>();
        for (InputOutputDescriptionDto descDto : postData.getDescriptions()) {
            descriptions.add(toDescription(descDto));
        }
        return descriptions;
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

    /**
     * Create a technical service based on data provided in the dto.
     * If the version is not specified, create a new version of the technical service if it already exists
     *
     * @param postData Data used to create the technical service
     * @throws BusinessException            If technical service already exists for specified name and version
     * @throws EntityDoesNotExistsException
     */
    public void create(@Valid @NotNull TechnicalServiceDto postData) throws BusinessException, EntityDoesNotExistsException {
        final T technicalService = fromDto(postData);
        if (postData.getVersion() == null) {
            int versionNumber = 1;
            final Optional<Integer> latestVersion = technicalServiceService().latestVersionNumber(postData.getName());
            if (latestVersion.isPresent()) {
                versionNumber = latestVersion.get() + 1;
            }
            technicalService.setVersion(versionNumber);
        }
        technicalServiceService().create(technicalService);
    }

    /**
     * Update the technical service with the specified information
     *
     * @param postData New data of the technical service
     * @throws EntityDoesNotExistsException if the technical service to update does not exists
     * @throws BusinessException            if the technical service can't be updated
     */
    public void update(@Valid @NotNull TechnicalServiceDto postData) throws EntityDoesNotExistsException, BusinessException {
        T technicalService;
        technicalService = getTechninicalService(postData.getName(), postData.getVersion());
        technicalService.setDescriptions(fromDescriptionsDto(postData));
        technicalService.setScript(postData.getScript());
        technicalServiceService().update(technicalService);
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
        TechnicalService technicalService = getTechninicalService(name, version);
        return toDto(technicalService);
    }

    /**
     * List all the technical services present in database
     *
     * @param name Name filter - Search for technical service that has a similar name
     * @return The list of all technical services DTOs object retrieved
     */
    public TechnicalServicesDto list(String name) {
        Map<String, Object> filter = new HashMap<>();
        if (name != null) {
            filter.put("wildcardOr " + name, null);
        }
        PaginationConfiguration config = new PaginationConfiguration(filter);
        List<T> list = technicalServiceService().list(config);
        List<TechnicalServiceDto> customEntityInstanceDTOs = list.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new TechnicalServicesDto(customEntityInstanceDTOs);
    }

    /**
     * List of all the versions for a specified technical service name
     *
     * @param name Name of the technical service to retrieve versions
     * @return The different versions of the technical service
     * @throws EntityDoesNotExistsException If no versions of the technical service exists
     */
    public TechnicalServicesDto listByName(String name) throws EntityDoesNotExistsException {
        List<T> list = technicalServiceService().findByName(name);
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
            technicalServiceService().remove(service);
        } else {
            List<T> list = technicalServiceService().findByName(name);
            for (T technicalService : list) {
                technicalServiceService().remove(technicalService);
            }
        }
    }

    /**
     * Create or update a technical service based upon the provided data
     *
     * @param postData Data used to create or update the technical service
     * @throws BusinessException            if the technical service can't be created or updated
     * @throws EntityDoesNotExistsException
     */
    public void createOrUpdate(@Valid @NotNull TechnicalServiceDto postData) throws BusinessException, EntityDoesNotExistsException {
        try {
            update(postData);
        } catch (EntityDoesNotExistsException e) {
            create(postData);
        }
    }

    private T getTechninicalService(String name, Integer version) throws EntityDoesNotExistsException {
        T technicalService;
        if (version != null) {
            technicalService = getTechnicalServiceByNameAndVersion(name, version);
        } else {
            technicalService = technicalServiceService().findLatestByName(name)
                    .orElseThrow(() -> new EntityDoesNotExistsException("TechnicalService with name " + name + " does not exists"));
        }
        return technicalService;
    }

    private T getTechnicalServiceByNameAndVersion(String name, Integer version) throws EntityDoesNotExistsException {
        return technicalServiceService().findByNameAndVersion(name, version)
                .orElseThrow(() -> new EntityDoesNotExistsException("TechnicalService with name " + name + " and version " + version + " does not exists"));
    }
}