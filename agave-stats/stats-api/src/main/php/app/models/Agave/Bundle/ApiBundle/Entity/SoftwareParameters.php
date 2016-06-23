<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * SoftwareParameters
 */
class SoftwareParameters
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var string
     */
    private $defaultValue;

    /**
     * @var string
     */
    private $description;

    /**
     * @var string
     */
    private $outputKey;

    /**
     * @var string
     */
    private $label;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var string
     */
    private $ontology;

    /**
     * @var integer
     */
    private $displayOrder;

    /**
     * @var boolean
     */
    private $required;

    /**
     * @var string
     */
    private $valueType;

    /**
     * @var string
     */
    private $validator;

    /**
     * @var boolean
     */
    private $visible;

    /**
     * @var string
     */
    private $cliArgument;

    /**
     * @var boolean
     */
    private $showCliArgument;

    /**
     * @var boolean
     */
    private $enquoted;

    /**
     * @var integer
     */
    private $maxCardinality;

    /**
     * @var integer
     */
    private $minCardinality;

    /**
     * @var boolean
     */
    private $repeatCliArgument;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Softwares
     */
    private $software;

    /**
     * @var \Doctrine\Common\Collections\Collection
     */
    private $softwares;

    /**
     * Constructor
     */
    public function __construct()
    {
        $this->softwares = new \Doctrine\Common\Collections\ArrayCollection();
    }

    /**
     * Get id
     *
     * @return integer 
     */
    public function getId()
    {
        return $this->id;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return SoftwareParameters
     */
    public function setCreated($created)
    {
        $this->created = $created;

        return $this;
    }

    /**
     * Get created
     *
     * @return \DateTime 
     */
    public function getCreated()
    {
        return $this->created;
    }

    /**
     * Set defaultValue
     *
     * @param string $defaultValue
     * @return SoftwareParameters
     */
    public function setDefaultValue($defaultValue)
    {
        $this->defaultValue = $defaultValue;

        return $this;
    }

    /**
     * Get defaultValue
     *
     * @return string 
     */
    public function getDefaultValue()
    {
        return $this->defaultValue;
    }

    /**
     * Set description
     *
     * @param string $description
     * @return SoftwareParameters
     */
    public function setDescription($description)
    {
        $this->description = $description;

        return $this;
    }

    /**
     * Get description
     *
     * @return string 
     */
    public function getDescription()
    {
        return $this->description;
    }

    /**
     * Set outputKey
     *
     * @param string $outputKey
     * @return SoftwareParameters
     */
    public function setOutputKey($outputKey)
    {
        $this->outputKey = $outputKey;

        return $this;
    }

    /**
     * Get outputKey
     *
     * @return string 
     */
    public function getOutputKey()
    {
        return $this->outputKey;
    }

    /**
     * Set label
     *
     * @param string $label
     * @return SoftwareParameters
     */
    public function setLabel($label)
    {
        $this->label = $label;

        return $this;
    }

    /**
     * Get label
     *
     * @return string 
     */
    public function getLabel()
    {
        return $this->label;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return SoftwareParameters
     */
    public function setLastUpdated($lastUpdated)
    {
        $this->lastUpdated = $lastUpdated;

        return $this;
    }

    /**
     * Get lastUpdated
     *
     * @return \DateTime 
     */
    public function getLastUpdated()
    {
        return $this->lastUpdated;
    }

    /**
     * Set ontology
     *
     * @param string $ontology
     * @return SoftwareParameters
     */
    public function setOntology($ontology)
    {
        $this->ontology = $ontology;

        return $this;
    }

    /**
     * Get ontology
     *
     * @return string 
     */
    public function getOntology()
    {
        return $this->ontology;
    }

    /**
     * Set displayOrder
     *
     * @param integer $displayOrder
     * @return SoftwareParameters
     */
    public function setDisplayOrder($displayOrder)
    {
        $this->displayOrder = $displayOrder;

        return $this;
    }

    /**
     * Get displayOrder
     *
     * @return integer 
     */
    public function getDisplayOrder()
    {
        return $this->displayOrder;
    }

    /**
     * Set required
     *
     * @param boolean $required
     * @return SoftwareParameters
     */
    public function setRequired($required)
    {
        $this->required = $required;

        return $this;
    }

    /**
     * Get required
     *
     * @return boolean 
     */
    public function getRequired()
    {
        return $this->required;
    }

    /**
     * Set valueType
     *
     * @param string $valueType
     * @return SoftwareParameters
     */
    public function setValueType($valueType)
    {
        $this->valueType = $valueType;

        return $this;
    }

    /**
     * Get valueType
     *
     * @return string 
     */
    public function getValueType()
    {
        return $this->valueType;
    }

    /**
     * Set validator
     *
     * @param string $validator
     * @return SoftwareParameters
     */
    public function setValidator($validator)
    {
        $this->validator = $validator;

        return $this;
    }

    /**
     * Get validator
     *
     * @return string 
     */
    public function getValidator()
    {
        return $this->validator;
    }

    /**
     * Set visible
     *
     * @param boolean $visible
     * @return SoftwareParameters
     */
    public function setVisible($visible)
    {
        $this->visible = $visible;

        return $this;
    }

    /**
     * Get visible
     *
     * @return boolean 
     */
    public function getVisible()
    {
        return $this->visible;
    }

    /**
     * Set cliArgument
     *
     * @param string $cliArgument
     * @return SoftwareParameters
     */
    public function setCliArgument($cliArgument)
    {
        $this->cliArgument = $cliArgument;

        return $this;
    }

    /**
     * Get cliArgument
     *
     * @return string 
     */
    public function getCliArgument()
    {
        return $this->cliArgument;
    }

    /**
     * Set showCliArgument
     *
     * @param boolean $showCliArgument
     * @return SoftwareParameters
     */
    public function setShowCliArgument($showCliArgument)
    {
        $this->showCliArgument = $showCliArgument;

        return $this;
    }

    /**
     * Get showCliArgument
     *
     * @return boolean 
     */
    public function getShowCliArgument()
    {
        return $this->showCliArgument;
    }

    /**
     * Set enquoted
     *
     * @param boolean $enquoted
     * @return SoftwareParameters
     */
    public function setEnquoted($enquoted)
    {
        $this->enquoted = $enquoted;

        return $this;
    }

    /**
     * Get enquoted
     *
     * @return boolean 
     */
    public function getEnquoted()
    {
        return $this->enquoted;
    }

    /**
     * Set maxCardinality
     *
     * @param integer $maxCardinality
     * @return SoftwareParameters
     */
    public function setMaxCardinality($maxCardinality)
    {
        $this->maxCardinality = $maxCardinality;

        return $this;
    }

    /**
     * Get maxCardinality
     *
     * @return integer 
     */
    public function getMaxCardinality()
    {
        return $this->maxCardinality;
    }

    /**
     * Set minCardinality
     *
     * @param integer $minCardinality
     * @return SoftwareParameters
     */
    public function setMinCardinality($minCardinality)
    {
        $this->minCardinality = $minCardinality;

        return $this;
    }

    /**
     * Get minCardinality
     *
     * @return integer 
     */
    public function getMinCardinality()
    {
        return $this->minCardinality;
    }

    /**
     * Set repeatCliArgument
     *
     * @param boolean $repeatCliArgument
     * @return SoftwareParameters
     */
    public function setRepeatCliArgument($repeatCliArgument)
    {
        $this->repeatCliArgument = $repeatCliArgument;

        return $this;
    }

    /**
     * Get repeatCliArgument
     *
     * @return boolean 
     */
    public function getRepeatCliArgument()
    {
        return $this->repeatCliArgument;
    }

    /**
     * Set software
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Softwares $software
     * @return SoftwareParameters
     */
    public function setSoftware(\Agave\Bundle\ApiBundle\Entity\Softwares $software = null)
    {
        $this->software = $software;

        return $this;
    }

    /**
     * Get software
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Softwares 
     */
    public function getSoftware()
    {
        return $this->software;
    }

    /**
     * Add softwares
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Softwares $softwares
     * @return SoftwareParameters
     */
    public function addSoftware(\Agave\Bundle\ApiBundle\Entity\Softwares $softwares)
    {
        $this->softwares[] = $softwares;

        return $this;
    }

    /**
     * Remove softwares
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Softwares $softwares
     */
    public function removeSoftware(\Agave\Bundle\ApiBundle\Entity\Softwares $softwares)
    {
        $this->softwares->removeElement($softwares);
    }

    /**
     * Get softwares
     *
     * @return \Doctrine\Common\Collections\Collection 
     */
    public function getSoftwares()
    {
        return $this->softwares;
    }
}
