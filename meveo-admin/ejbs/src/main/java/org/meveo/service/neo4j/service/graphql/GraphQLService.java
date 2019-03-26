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

package org.meveo.service.neo4j.service.graphql;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.model.customEntities.CustomRelationshipTemplate;
import org.meveo.model.customEntities.GraphQLQueryField;
import org.meveo.model.neo4j.GraphQLRequest;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.custom.CustomRelationshipTemplateService;
import org.meveo.service.neo4j.base.Neo4jDao;

@Stateless
public class GraphQLService {

    @Inject
    private Neo4jDao neo4jDao;

    @Inject
    private CustomEntityTemplateService customEntityTemplateService;

    @Inject
    private CustomRelationshipTemplateService customRelationshipTemplateService;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    public Map<String, Object> executeGraphQLRequest(GraphQLRequest graphQLRequest, String neo4jConfiguration) {

        return neo4jDao.executeGraphQLQuery(
                neo4jConfiguration,
                graphQLRequest.getQuery(),
                graphQLRequest.getVariables(),
                graphQLRequest.getOperationName()
        );
    }

    public Map<String, Object> executeGraphQLRequest(String query, String neo4jConfiguration) {
        return neo4jDao.executeGraphQLQuery(neo4jConfiguration, query, null, null);
    }

    public void updateIDL() {

        final Collection<GraphQLEntity> entities = getEntities();
        String idl = getIDL(entities);
        System.out.println(idl);

    }

    public String getIDL() {
        final Collection<GraphQLEntity> entities = getEntities();
        return getIDL(entities);
    }

    private String getIDL(Collection<GraphQLEntity> graphQLEntities) {
        StringBuilder idl = new StringBuilder();

        idl.append("scalar GraphQLLong\n");
        idl.append("scalar GraphQLBigDecimal\n\n");

        for (GraphQLEntity graphQLEntity : graphQLEntities) {
            idl.append("type ").append(graphQLEntity.getName()).append(" {\n");
            for (GraphQLField graphQLField : graphQLEntity.getGraphQLFields()) {
                idl.append("\t").append(graphQLField.getFieldName()).append(": ");

                if (graphQLField.isMultivialued()) {
                    idl.append("[");
                }
                idl.append(graphQLField.getFieldType());

                if (graphQLField.isMultivialued()) {
                    idl.append("]");
                }

                if (graphQLField.isRequired()) {
                    idl.append("!");
                }

                if (graphQLField.getQuery() != null) {
                    idl.append(" ").append(graphQLField.getQuery());
                }

                idl.append("\n");

            }

            idl.append("}\n\n");
        }

        return idl.toString();
    }

    private Collection<GraphQLEntity> getEntities() {

        Map<String, GraphQLEntity> graphQLEntities = new HashMap<>();


        // Entities
        final List<CustomEntityTemplate> ceTsWithSubTemplates = customEntityTemplateService.getCETsWithSubTemplates();
        final Map<String, CustomEntityTemplate> cetsByName = ceTsWithSubTemplates
                .stream()
                .collect(Collectors.toMap(CustomEntityTemplate::getCode, Function.identity()));

        for (CustomEntityTemplate cet : cetsByName.values()) {
            final Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(cet.getAppliesTo());
            GraphQLEntity graphQLEntity = new GraphQLEntity();
            graphQLEntity.setName(cet.getCode());

            List<GraphQLField> graphQLFields = getGraphQLFields(cfts);

            // Additional queries defined
            for (GraphQLQueryField graphqlQueryField : Optional.ofNullable(cet.getGraphqlQueryFields()).orElse(Collections.emptyList())) {
                GraphQLField graphQLField = new GraphQLField();
                graphQLField.setQuery(graphqlQueryField.getQuery());
                graphQLField.setFieldType(graphqlQueryField.getFieldType());
                graphQLField.setFieldName(graphqlQueryField.getFieldName());
                graphQLField.setMultivalued(graphqlQueryField.isMultivalued());
                graphQLFields.add(graphQLField);
            }

            // Primitive type
            if (cet.isPrimitiveEntity()) {
                final boolean valueExists = graphQLFields.stream().anyMatch(f -> f.getFieldName().equals("value"));
                if (!valueExists) {
                    GraphQLField value = new GraphQLField();
                    switch (cet.getPrimitiveType()) {
                        case STRING:
                            value.setFieldType("String");
                            break;
                        case LONG:
                        case DATE:
                            value.setFieldType("GraphQLLong");
                            break;
                        case DOUBLE:
                            value.setFieldType("GraphQLBigDecimal");
                            break;
                    }
                    value.setFieldName("value");
                    value.setMultivalued(false);
                    value.setRequired(true);
                    graphQLFields.add(value);
                }
            }

            graphQLEntity.setGraphQLFields(graphQLFields);
            graphQLEntities.put(graphQLEntity.getName(), graphQLEntity);
        }

        // Relationships
        final List<CustomRelationshipTemplate> customRelationshipTemplates = customRelationshipTemplateService.list();

        for (CustomRelationshipTemplate relationshipTemplate : customRelationshipTemplates) {

            // Create Graphql relationship type
            final Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(relationshipTemplate.getAppliesTo());
            GraphQLEntity graphQLEntity = new GraphQLEntity();
            String typeName = relationshipTemplate.getEndNode().getCode() + "Relation";
            graphQLEntity.setName(typeName);

            List<GraphQLField> graphQLFields = getGraphQLFields(cfts);

            GraphQLField to = new GraphQLField();
            to.setFieldName("to");
            to.setFieldType(relationshipTemplate.getEndNode().getCode());
            to.setQuery("@cypher(statement: \"MATCH ()-[this]->(to) RETURN to\")");
            graphQLFields.add(to);

            GraphQLField from = new GraphQLField();
            from.setFieldName("from");
            from.setFieldType(relationshipTemplate.getStartNode().getCode());
            from.setQuery("@cypher(statement: \"MATCH (from)-[this]->() RETURN from\")");
            graphQLFields.add(from);

            graphQLEntity.setGraphQLFields(graphQLFields);

            // Add fields to sources (and sub-sources)
            cetsByName.get(relationshipTemplate.getStartNode().getCode())
                    .descendance()
                    .stream()
                    .map(sourceCet -> graphQLEntities.get(sourceCet.getCode()))
                    .forEach(source -> {
                        // Source singular field
                        if (relationshipTemplate.getSourceNameSingular() != null) {
                            GraphQLField sourceNameSingular = new GraphQLField();
                            sourceNameSingular.setFieldName(relationshipTemplate.getSourceNameSingular());
                            sourceNameSingular.setMultivalued(false);
                            sourceNameSingular.setFieldType(relationshipTemplate.getEndNode().getCode());
                            sourceNameSingular.setQuery("@relation(name: " + relationshipTemplate.getName() + ", direction: OUT)");
                            source.getGraphQLFields().add(sourceNameSingular);
                        }

                        // Source plural field
                        if (relationshipTemplate.getSourceNamePlural() != null) {
                            GraphQLField sourceNamePlural = new GraphQLField();
                            sourceNamePlural.setFieldName(relationshipTemplate.getSourceNamePlural());
                            sourceNamePlural.setMultivalued(true);
                            sourceNamePlural.setFieldType(relationshipTemplate.getEndNode().getCode());
                            sourceNamePlural.setQuery("@relation(name: " + relationshipTemplate.getName() + ", direction: OUT)");
                            source.getGraphQLFields().add(sourceNamePlural);
                        }

                        // Relationships field
                        if (relationshipTemplate.getRelationshipsFieldSource() != null) {
                            GraphQLField outgoingRelationship = new GraphQLField();
                            outgoingRelationship.setFieldName(relationshipTemplate.getRelationshipsFieldSource());
                            outgoingRelationship.setMultivalued(true);
                            outgoingRelationship.setFieldType(typeName);

                            final String query = String.format(
                                    "@cypher(statement: \"MATCH (this)-[rel:%s]->(n:%s) RETURN rel\")",
                                    relationshipTemplate.getName(),
                                    relationshipTemplate.getEndNode().getCode()
                            );

                            outgoingRelationship.setQuery(query);

                            source.getGraphQLFields().add(outgoingRelationship);
                        }
                    });

            // Add fields to target (and sub-targets)
            cetsByName.get(relationshipTemplate.getEndNode().getCode())
                    .descendance()
                    .stream()
                    .map(targetCet -> graphQLEntities.get(targetCet.getCode()))
                    .forEach(target -> {
                        // Target singular field
                        if (relationshipTemplate.getTargetNameSingular() != null) {
                            GraphQLField targetNameSingular = new GraphQLField();
                            targetNameSingular.setFieldName(relationshipTemplate.getTargetNameSingular());
                            targetNameSingular.setMultivalued(false);
                            targetNameSingular.setFieldType(relationshipTemplate.getStartNode().getCode());
                            targetNameSingular.setQuery("@relation(name: " + relationshipTemplate.getName() + ", direction: IN)");
                            target.getGraphQLFields().add(targetNameSingular);
                        }

                        // Target plural field
                        if (relationshipTemplate.getTargetNamePlural() != null) {
                            GraphQLField targetNamePlural = new GraphQLField();
                            targetNamePlural.setFieldName(relationshipTemplate.getTargetNamePlural());
                            targetNamePlural.setMultivalued(true);
                            targetNamePlural.setFieldType(relationshipTemplate.getStartNode().getCode());
                            targetNamePlural.setQuery("@relation(name: " + relationshipTemplate.getName() + ", direction: IN)");
                            target.getGraphQLFields().add(targetNamePlural);
                        }

                        // Relationships field
                        if (relationshipTemplate.getRelationshipsFieldTarget() != null) {
                            GraphQLField relationship = new GraphQLField();
                            relationship.setFieldName(relationshipTemplate.getRelationshipsFieldTarget());
                            relationship.setMultivalued(true);
                            relationship.setFieldType(typeName);

                            final String query = String.format(
                                    "@cypher(statement: \"MATCH (n:%s)-[rel:%s]->(this) RETURN rel\")",
                                    relationshipTemplate.getEndNode().getCode(),
                                    relationshipTemplate.getName()
                            );

                            relationship.setQuery(query);

                            target.getGraphQLFields().add(relationship);
                        }
                    });

            graphQLEntities.put(graphQLEntity.getName(), graphQLEntity);

        }

        return graphQLEntities.values();
    }

    private List<GraphQLField> getGraphQLFields(Map<String, CustomFieldTemplate> cfts) {
        List<GraphQLField> graphQLFields = new ArrayList<>();
        for (CustomFieldTemplate customFieldTemplate : cfts.values()) {

            // Skip the field if it is an entity reference
            if (customFieldTemplate.getFieldType() == CustomFieldTypeEnum.ENTITY) {
                continue;
            }

            GraphQLField graphQLField = new GraphQLField();
            graphQLField.setFieldName(customFieldTemplate.getCode());

            if (customFieldTemplate.isIdentifier()) {
                graphQLField.setRequired(true);
                graphQLField.setFieldType("ID");
                graphQLField.setMultivalued(false);
            } else {
                graphQLField.setMultivalued(customFieldTemplate.getStorageType() == CustomFieldStorageTypeEnum.LIST);
                graphQLField.setRequired(customFieldTemplate.isValueRequired());

                switch (customFieldTemplate.getFieldType()) {
                    case STRING:
                    case TEXT_AREA:
                        graphQLField.setFieldType("String");
                        break;
                    case LONG:
                    case DATE:
                        graphQLField.setFieldType("GraphQLLong");
                        break;
                    case DOUBLE:
                        graphQLField.setFieldType("GraphQLBigDecimal");
                        break;
                    case BOOLEAN:
                        graphQLField.setFieldType("Boolean");
                        break;
                }
            }

            graphQLFields.add(graphQLField);
        }
        return graphQLFields;
    }

}