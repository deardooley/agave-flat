<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * SoftwareOutputs
 */
class SoftwareOutputs
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
    private $fileTypes;

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
     * @var integer
     */
    private $maxCardinality;

    /**
     * @var integer
     */
    private $minCardinality;

    /**
     * @var string
     */
    private $ontology;

    /**
     * @var string
     */
    private $pattern;

    /**
     * @var integer
     */
    private $displayOrder;

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
     * @return SoftwareOutputs
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
     * @return SoftwareOutputs
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
     * @return SoftwareOutputs
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
     * Set fileTypes
     *
     * @param string $fileTypes
     * @return SoftwareOutputs
     */
    public function setFileTypes($fileTypes)
    {
        $this->fileTypes = $fileTypes;

        return $this;
    }

    /**
     * Get fileTypes
     *
     * @return string 
     */
    public function getFileTypes()
    {
        return $this->fileTypes;
    }

    /**
     * Set outputKey
     *
     * @param string $outputKey
     * @return SoftwareOutputs
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
     * @return SoftwareOutputs
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
     * @return SoftwareOutputs
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
     * Set maxCardinality
     *
     * @param integer $maxCardinality
     * @return SoftwareOutputs
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
     * @return SoftwareOutputs
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
     * Set ontology
     *
     * @param string $ontology
     * @return SoftwareOutputs
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
     * Set pattern
     *
     * @param string $pattern
     * @return SoftwareOutputs
     */
    public function setPattern($pattern)
    {
        $this->pattern = $pattern;

        return $this;
    }

    /**
     * Get pattern
     *
     * @return string 
     */
    public function getPattern()
    {
        return $this->pattern;
    }

    /**
     * Set displayOrder
     *
     * @param integer $displayOrder
     * @return SoftwareOutputs
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
     * Set software
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Softwares $software
     * @return SoftwareOutputs
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
     * @return SoftwareOutputs
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
