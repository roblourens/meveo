package org.meveo.model.customEntities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.BusinessEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.ModuleItem;
import org.meveo.model.billing.RelationshipDirectionEnum;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@ModuleItem
@ExportIdentifier({ "code"})
@Table(name = "CUST_CRT", uniqueConstraints = @UniqueConstraint(columnNames = {"code"}))
@GenericGenerator(name = "ID_GENERATOR", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {@org.hibernate.annotations.Parameter(name = "sequence_name", value = "CUST_CRT_SEQ")}
)
@NamedQueries({
        @NamedQuery(name = "CustomRelationshipTemplate.getCRTForCache", query = "SELECT crt from CustomRelationshipTemplate crt where crt.disabled=false  "),
        @NamedQuery(name = "CustomRelationshipTemplate.findByStartEndAndName",
        query = "SELECT crt from CustomRelationshipTemplate crt " +
                "WHERE crt.startNode.code = :startCode " +
                "AND crt.endNode.code = :endCode " +
                "AND crt.name = :name")
})
public class CustomRelationshipTemplate extends BusinessEntity implements Comparable<CustomRelationshipTemplate> {

    private static final long serialVersionUID = 8281478284763353310L;

    public static String CRT_PREFIX = "CRT";

    @Column(name = "name", length = 100, nullable = false)
    @Size(max = 100)
    @NotNull
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "START_NODE_ID")
    private CustomEntityTemplate startNode;
    
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "END_NODE_ID")
    private CustomEntityTemplate  endNode;
    
    @Enumerated(EnumType.STRING)
	@Column(name = "DIRECTION", length = 100)
    private RelationshipDirectionEnum direction = RelationshipDirectionEnum.OUTGOING;

    /**
     * Json list type. ex : ["firstName","lastName","birthDate"]
     */
    @Column(name = "START_NODE_KEYS", length = 100)
    private String startNodeKeys; 

    /**
     * Json list type. ex : ["firstName","lastName","birthDate"]
     */
    @Column(name = "END_NODE_KEYS", length = 100)
    private String endNodeKeys;

    /**
     * Whether the relation is used for unicity of nodes
     */
    @Column(name = "is_unique")
    @Type(type="numeric_boolean")
    @ColumnDefault("0")
    private boolean unique;

    /**
     * Name of the field that will be added to the source entity to refer the most recent target entity
     */
    @Column(name = "source_name_singular")
    private String sourceNameSingular;

    /**
     * Name of the field that will be added to the source entity to refer every target entities
     */
    @Column(name = "source_name_plural")
    private String sourceNamePlural;

    /**
     * Name of the field that will be added to the source entity and that refer to the outgoing relationships of this type
     */
    public String getOutgoingRelationshipsField() {
        return sourceNameSingular + "Relationships";
    }

    /**
     * Name of the graphql type corresponding to the relationship
     */
    public String getGraphQlTypeName(){
        return Character.toUpperCase(sourceNameSingular.charAt(0)) + sourceNameSingular.substring(1) + "Relationship";
    }

    public String getSourceNameSingular() {
        return sourceNameSingular;
    }

    public void setSourceNameSingular(String sourceNameSingular) {
        this.sourceNameSingular = sourceNameSingular;
    }

    public String getSourceNamePlural() {
        return sourceNamePlural;
    }

    public void setSourceNamePlural(String sourceNamePlural) {
        this.sourceNamePlural = sourceNamePlural;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CustomEntityTemplate getStartNode() {
		return startNode;
	}

	public void setStartNode(CustomEntityTemplate startNode) {
		this.startNode = startNode;
	}

	public CustomEntityTemplate getEndNode() {
		return endNode;
	}

	public void setEndNode(CustomEntityTemplate endNode) {
		this.endNode = endNode;
	}

	public void setEndEntity(CustomEntityTemplate endNode) {
        this.endNode = endNode;
    }

    public void setStartEntity(CustomEntityTemplate startEntity){
        this.startNode = startEntity;
    }

	public RelationshipDirectionEnum getDirection() {
		return direction;
	}

	public void setDirection(RelationshipDirectionEnum direction) {
		if(direction!=null){
			this.direction = direction;
		}
		
	}

	public String getStartNodeKeys() {
		return startNodeKeys;
	}

	public void setStartNodeKey(String startNodeKeys) {
		this.startNodeKeys = startNodeKeys;
	}

	public String getEndNodeKeys() {
		return endNodeKeys;
	}

	public void setEndNodeKey(String endNodeKeys) {
		this.endNodeKeys = endNodeKeys;
	}

	public String getAppliesTo() {
        return CRT_PREFIX + "_" + getCode();
    }
    
    public static String getAppliesTo(String code) {
        return CRT_PREFIX + "_" + code;
    }

    public String getPermissionResourceName() {
        return CustomRelationshipTemplate.getPermissionResourceName(code);
    }

    @Override
    public int compareTo(CustomRelationshipTemplate cet1) {
        return StringUtils.compare(name, cet1.getName());
    }

    public static String getPermissionResourceName(String code) {
        return "CRT_" + code;
    }

}