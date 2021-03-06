/*
 * (C) Copyright 2018-2020 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
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
package org.meveo.service.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.infinispan.Cache;
import org.meveo.admin.exception.BusinessException;
import org.meveo.cache.CustomFieldsCacheContainerProvider;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.customEntities.CustomRelationshipTemplate;
import org.meveo.model.persistence.DBStorageType;
import org.meveo.model.persistence.sql.SQLStorageConfiguration;
import org.meveo.service.admin.impl.PermissionService;
import org.meveo.service.base.BusinessService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.storage.RepositoryService;
import org.meveo.util.EntityCustomizationUtils;

/**
 * Class used for persisting CustomRelationshipTemplate entities
 * @author Clément Bareth
 * @author Edward P. Legaspi | czetsuya@gmail.com
 * @version 6.12
 */
@Stateless
public class CustomRelationshipTemplateService extends BusinessService<CustomRelationshipTemplate> {

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;
    
    @Inject
    private CustomTableCreatorService customTableCreatorService;

    @Inject
    private PermissionService permissionService;

    @Inject
    private CustomFieldsCacheContainerProvider customFieldsCache;

    @Resource(lookup = "java:jboss/infinispan/cache/meveo/unique-crt")
    private Cache<String, Boolean> uniqueRelations;
    
    @Inject
    private RepositoryService repositoryService;

    private ParamBean paramBean = ParamBean.getInstance();
    
    @Override
    public void create(CustomRelationshipTemplate crt) throws BusinessException {
        if (!EntityCustomizationUtils.validateOntologyCode(crt.getCode())) {
            throw new IllegalArgumentException("The code of ontology elements must not contain numbers");
        }
        
        if(crt.getStartNode() == null) {
        	throw new IllegalArgumentException("Can't create relation " + crt.getCode() + ": start node can't be null");
        }
        
        if(crt.getEndNode() == null) {
        	throw new IllegalArgumentException("Can't create relation " + crt.getCode() + ": end node can't be null");
        }
        
        super.create(crt);
        
        try {
            permissionService.createIfAbsent("modify", crt.getPermissionResourceName(), paramBean.getProperty("role.modifyAllCE", "ModifyAllCE"));
            permissionService.createIfAbsent("read", crt.getPermissionResourceName(), paramBean.getProperty("role.readAllCE", "ReadAllCE"));
            if(crt.getAvailableStorages().contains(DBStorageType.SQL)) {
            	customTableCreatorService.createCrtTable(crt);
            }
            customFieldsCache.addUpdateCustomRelationshipTemplate(crt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CustomRelationshipTemplate update(CustomRelationshipTemplate crt) throws BusinessException {
        if (!EntityCustomizationUtils.validateOntologyCode(crt.getCode())) {
            throw new IllegalArgumentException("The code of ontology elements must not contain numbers");
        }
        CustomRelationshipTemplate cetUpdated = super.update(crt);
        
        permissionService.createIfAbsent("modify", crt.getPermissionResourceName(), paramBean.getProperty("role.modifyAllCE", "ModifyAllCE"));
        permissionService.createIfAbsent("read", crt.getPermissionResourceName(), paramBean.getProperty("role.readAllCE", "ReadAllCE"));
        
        // SQL Storage logic
        if(crt.getAvailableStorages().contains(DBStorageType.SQL)) {
        	boolean created = customTableCreatorService.createCrtTable(crt);
        	// Create the custom fields for the table if the table has been created
        	if(created) {
        		for(CustomFieldTemplate cft : customFieldTemplateService.findByAppliesTo(crt.getAppliesTo()).values()) {
    				customTableCreatorService.addField(SQLStorageConfiguration.getDbTablename(crt), cft);
        		}
        	}
        }else {
            // Remove table if storage previously contained SQL
            if(customFieldsCache.getCustomRelationshipTemplate(crt.getCode()).getAvailableStorages().contains(DBStorageType.SQL)) {
                customTableCreatorService.removeTable(null, SQLStorageConfiguration.getDbTablename(crt));
            }
        }

        customFieldsCache.addUpdateCustomRelationshipTemplate(crt);

        return cetUpdated;
    }

    /**
	 * Synchronize storages.
	 *
	 * @param crt the crt
	 * @throws BusinessException if we can't remove a storage
	 */
    public void synchronizeStorages(CustomRelationshipTemplate crt) throws BusinessException {
    	// Synchronize custom fields storages with CRT available storages
    	for (CustomFieldTemplate cft : customFieldTemplateService.findByAppliesToNoCache(crt.getAppliesTo()).values()) {
    		if(cft.getStoragesNullSafe() == null){
    			cft.setStorages(new ArrayList<>());
    		}

    		for (DBStorageType storage : new ArrayList<>(cft.getStoragesNullSafe())) {
    			if (!crt.getAvailableStorages().contains(storage)) {
    				log.info("Remove storage '{}' from CFT '{}' of CRT '{}'", storage, cft.getCode(), crt.getCode());
    				cft.getStoragesNullSafe().remove(storage);
    				customFieldTemplateService.update(cft);
    			}
    		}
    	}
    }


    @Override
    public void remove(CustomRelationshipTemplate crt) throws BusinessException {
        Map<String, CustomFieldTemplate> fields = customFieldTemplateService.findByAppliesTo(crt.getAppliesTo());

        for (CustomFieldTemplate cft : fields.values()) {
            customFieldTemplateService.remove(cft.getId());
        }

        if(crt.getAvailableStorages().contains(DBStorageType.SQL)) {
            customTableCreatorService.removeTable(repositoryService.findDefaultRepository().getCode(), SQLStorageConfiguration.getDbTablename(crt));
        }

        customFieldsCache.removeCustomRelationshipTemplate(crt);

        super.remove(crt);
    }

    /**
     * Whether the relation is unique
     *
     * @param code Code of the relationship template
     * @return {@code true} if the relationship is unique
     */
    public boolean isUnique(String code){
        return uniqueRelations.computeIfAbsent(code, key -> {
            try {
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
                CriteriaQuery<Boolean> query = cb.createQuery(Boolean.class);
                Root<CustomRelationshipTemplate> root = query.from(getEntityClass());
                query.select(root.get("unique"));
                query.where(cb.equal(root.get("code"), key));
                query.distinct(true);
                return getEntityManager().createQuery(query).getSingleResult();
            } catch (NoResultException e) {
                return false;
            }
        });
    }

    /**
     * Get a list of custom entity templates for cache
     * 
     * @return A list of custom entity templates
     */
    public List<CustomRelationshipTemplate> getCRTForCache() {
        return getEntityManager().createNamedQuery("CustomRelationshipTemplate.getCRTForCache", CustomRelationshipTemplate.class).getResultList();
    }

    /**
     * Find entity by code
     *
     * @param code Code to match
     */
    @Override
    public CustomRelationshipTemplate findByCode(String code){
        return super.findByCode(code);
    }

    /**
	 * Find {@link CustomRelationshipTemplate} by start code, end code and name.
	 *
	 * @param startCode the start code
	 * @param endCode   the end code
	 * @param name      the name
	 * @return the query results
	 */
    public List<CustomRelationshipTemplate> findByStartEndAndName(String startCode, String endCode, String name){
        return getEntityManager().createNamedQuery("CustomRelationshipTemplate.findByStartEndAndName", CustomRelationshipTemplate.class)
                .setParameter("startCode", startCode)
                .setParameter("endCode", endCode)
                .setParameter("name", name)
                .getResultList();

    }

    /**
     * Find all relationships related to a given custom entity template
     * 
     * @param cet the custom entity template
     * @param name the name of the relationship
     * @return all relationships related to the entity template
     */
    @SuppressWarnings("unchecked")
    public List<String> findByCetAndName(CustomEntityTemplate cet, String name) {
        String query = "WITH RECURSIVE ancestors AS (\n" +
                "   SELECT id, code, super_template_id FROM cust_cet\n" +
                "   WHERE id = :cetId\n" +
                "   UNION\n" +
                "      SELECT cet.id, cet.code, cet.super_template_id FROM cust_cet cet\n" +
                "      INNER JOIN ancestors s ON s.super_template_id = cet.id\n" +
                ") \n" +
                "\n" +
                "SELECT crt.code FROM cust_crt crt\n" +
                "WHERE crt.name = :crtName\n" +
                "AND EXISTS(SELECT 1 FROM ancestors a WHERE crt.start_node_id = a.id OR crt.end_node_id = a.id)";

        List<Tuple> tuples = getEntityManager().createNativeQuery(query, Tuple.class)
                .setParameter("cetId", cet.getId())
                .setParameter("crtName", name)
                .getResultList();

        return tuples.stream().map(t -> t.get("code", String.class))
                .collect(Collectors.toList());

    }
    
	/**
	 * Find all relationships with the given name that links source and target
	 * 
	 * @param source Code of the source template
	 * @param target Code of the target template
	 * @param name   Name of the relationships
	 * @return the matching relationships
	 */
	public List<CustomRelationshipTemplate> findByNameAndSourceOrTarget(String source, String target, String name) {
		return getEntityManager().createQuery("FROM CustomRelationshipTemplate crt "
				+ "WHERE crt.name = :name "
				+ "AND ("
				+ "    (crt.startNode.code = :source AND crt.endNode.code = :target)"
				+ "    OR (crt.startNode.code = :target AND crt.endNode.code = :source)"
				+ ")", 
				CustomRelationshipTemplate.class)
				.setParameter("name", name)
				.setParameter("target", target)
				.setParameter("source", source)
				.getResultList();
	}
	
	/**
	 * Find all relationships that links source and target
	 * 
	 * @param source Code of the source template
	 * @param target Code of the target template
	 * @return the matching relationships
	 */
	public List<CustomRelationshipTemplate> findBySourceOrTarget(String source, String target) {
		return getEntityManager().createQuery("FROM CustomRelationshipTemplate crt "
				+ "WHERE crt.startNode.code = :source AND crt.endNode.code = :target "
				+ "OR (crt.startNode.code = :target AND crt.endNode.code = :source)", 
				CustomRelationshipTemplate.class)
				.setParameter("target", target)
				.setParameter("source", source)
				.getResultList();
	}
	

}
