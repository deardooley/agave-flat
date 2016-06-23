<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Softwares
 * @ORM\Entity
 * @ORM\Table(name="softwares")
 */
class Softwares
{
  /**
  * @ORM\Id
  * @ORM\GeneratedValue
  * @ORM\Column(type="integer")
  */
  private $id;

    /**
     * @var boolean
     * @ORM\Column(type="boolean", name="available")
     */
    private $available;

    /**
     * @var boolean
     * @ORM\Column(type="boolean", name="checkpointable")
     */
    private $checkpointable;

    /**
     * @var string
     * @ORM\Column(type="string", name="checksum")
     */
    private $checksum;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="created")
     */
    private $created;

    /**
     * @var string
     * @ORM\Column(type="string", name="deployment_path")
     */
    private $deploymentPath;

    /**
     * @var string
     * @ORM\Column(type="string", name="executable_path")
     */
    private $executablePath;

    /**
     * @var string
     * @ORM\Column(type="string", name="execution_type")
     */
    private $executionType;

    /**
     * @var string
     * @ORM\Column(type="string", name="helpuri")
     */
    private $helpuri;

    /**
     * @var string
     * @ORM\Column(type="string", name="icon")
     */
    private $icon;

    /**
     * @var string
     * @ORM\Column(type="string", name="label")
     */
    private $label;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="last_updated")
     */
    private $lastUpdated;

    /**
     * @var string
     * @ORM\Column(type="string", name="long_description")
     */
    private $longDescription;

    /**
     * @var string
     * @ORM\Column(type="string", name="modules")
     */
    private $modules;

    /**
     * @var string
     * @ORM\Column(type="integer", name="Name")
     */
    private $name;

    /**
     * @var string
     * @ORM\Column(type="string", name="ontology")
     */
    private $ontology;

    /**
     * @var string
     * @ORM\Column(type="integer", name="Owner")
     */
    private $owner;

    /**
     * @var string
     * @ORM\Column(type="string", name="parallelism")
     */
    private $parallelism;

    /**
     * @var boolean
     * @ORM\Column(type="boolean", name="publicly_available")
     */
    private $publiclyAvailable;

    /**
     * @var integer
     * @ORM\Column(type="integer", name="revision_count")
     */
    private $revisionCount;

    /**
     * @var string
     * @ORM\Column(type="string", name="short_description")
     */
    private $shortDescription;

    /**
     * @var string
     * @ORM\Column(type="string", name="tags")
     */
    private $tags;

    /**
     * @var string
     * @ORM\Column(type="string", name="tenant_id")
     */
    private $tenantId;

    /**
     * @var string
     * @ORM\Column(type="string", name="test_path")
     */
    private $testPath;

    /**
     * @var string
     * @ORM\Column(type="string", name="uuid")
     */
    private $uuid;

    /**
     * @var string
     * @ORM\Column(type="string", name="version")
     */
    private $version;

    /**
     * @var float
     * @ORM\Column(type="float", name="default_memory")
     */
    private $defaultMemory;

    /**
     * @var integer
     * @ORM\Column(type="integer", name="default_processors")
     */
    private $defaultProcesors;

    /**
     * @var string
     * @ORM\Column(type="string", name="default_queue")
     */
    private $defaultQueue;

    /**
     * @var string
     * @ORM\Column(type="string", name="default_requested_time")
     */
    private $defaultRequestedTime;

    /**
     * @var integer
     * @ORM\Column(type="integer", name="default_nodes")
     */
    private $defaultNodes;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Executionsystems
     */
    private $system;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Storagesystems
     */
    private $storageSystem;

    /**
     * @var \Doctrine\Common\Collections\Collection
     */
    private $inputs;

    /**
     * @var \Doctrine\Common\Collections\Collection
     */
    private $outputs;

    /**
     * @var \Doctrine\Common\Collections\Collection
     */
    private $parameters;

    /**
     * Constructor
     */
    public function __construct()
    {
        $this->inputs = new \Doctrine\Common\Collections\ArrayCollection();
        $this->outputs = new \Doctrine\Common\Collections\ArrayCollection();
        $this->parameters = new \Doctrine\Common\Collections\ArrayCollection();
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
     * Set available
     *
     * @param boolean $available
     * @return Softwares
     */
    public function setAvailable($available)
    {
        $this->available = $available;

        return $this;
    }

    /**
     * Get available
     *
     * @return boolean
     */
    public function getAvailable()
    {
        return $this->available;
    }

    /**
     * Set checkpointable
     *
     * @param boolean $checkpointable
     * @return Softwares
     */
    public function setCheckpointable($checkpointable)
    {
        $this->checkpointable = $checkpointable;

        return $this;
    }

    /**
     * Get checkpointable
     *
     * @return boolean
     */
    public function getCheckpointable()
    {
        return $this->checkpointable;
    }

    /**
     * Set checksum
     *
     * @param string $checksum
     * @return Softwares
     */
    public function setChecksum($checksum)
    {
        $this->checksum = $checksum;

        return $this;
    }

    /**
     * Get checksum
     *
     * @return string
     */
    public function getChecksum()
    {
        return $this->checksum;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return Softwares
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
     * Set deploymentPath
     *
     * @param string $deploymentPath
     * @return Softwares
     */
    public function setDeploymentPath($deploymentPath)
    {
        $this->deploymentPath = $deploymentPath;

        return $this;
    }

    /**
     * Get deploymentPath
     *
     * @return string
     */
    public function getDeploymentPath()
    {
        return $this->deploymentPath;
    }

    /**
     * Set executablePath
     *
     * @param string $executablePath
     * @return Softwares
     */
    public function setExecutablePath($executablePath)
    {
        $this->executablePath = $executablePath;

        return $this;
    }

    /**
     * Get executablePath
     *
     * @return string
     */
    public function getExecutablePath()
    {
        return $this->executablePath;
    }

    /**
     * Set executionType
     *
     * @param string $executionType
     * @return Softwares
     */
    public function setExecutionType($executionType)
    {
        $this->executionType = $executionType;

        return $this;
    }

    /**
     * Get executionType
     *
     * @return string
     */
    public function getExecutionType()
    {
        return $this->executionType;
    }

    /**
     * Set helpuri
     *
     * @param string $helpuri
     * @return Softwares
     */
    public function setHelpuri($helpuri)
    {
        $this->helpuri = $helpuri;

        return $this;
    }

    /**
     * Get helpuri
     *
     * @return string
     */
    public function getHelpuri()
    {
        return $this->helpuri;
    }

    /**
     * Set icon
     *
     * @param string $icon
     * @return Softwares
     */
    public function setIcon($icon)
    {
        $this->icon = $icon;

        return $this;
    }

    /**
     * Get icon
     *
     * @return string
     */
    public function getIcon()
    {
        return $this->icon;
    }

    /**
     * Set label
     *
     * @param string $label
     * @return Softwares
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
     * @return Softwares
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
     * Set longDescription
     *
     * @param string $longDescription
     * @return Softwares
     */
    public function setLongDescription($longDescription)
    {
        $this->longDescription = $longDescription;

        return $this;
    }

    /**
     * Get longDescription
     *
     * @return string
     */
    public function getLongDescription()
    {
        return $this->longDescription;
    }

    /**
     * Set modules
     *
     * @param string $modules
     * @return Softwares
     */
    public function setModules($modules)
    {
        $this->modules = $modules;

        return $this;
    }

    /**
     * Get modules
     *
     * @return string
     */
    public function getModules()
    {
        return $this->modules;
    }

    /**
     * Set name
     *
     * @param string $name
     * @return Softwares
     */
    public function setName($name)
    {
        $this->name = $name;

        return $this;
    }

    /**
     * Get name
     *
     * @return string
     */
    public function getName()
    {
        return $this->name;
    }

    /**
     * Set ontology
     *
     * @param string $ontology
     * @return Softwares
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
     * Set owner
     *
     * @param string $owner
     * @return Softwares
     */
    public function setOwner($owner)
    {
        $this->owner = $owner;

        return $this;
    }

    /**
     * Get owner
     *
     * @return string
     */
    public function getOwner()
    {
        return $this->owner;
    }

    /**
     * Set parallelism
     *
     * @param string $parallelism
     * @return Softwares
     */
    public function setParallelism($parallelism)
    {
        $this->parallelism = $parallelism;

        return $this;
    }

    /**
     * Get parallelism
     *
     * @return string
     */
    public function getParallelism()
    {
        return $this->parallelism;
    }

    /**
     * Set publiclyAvailable
     *
     * @param boolean $publiclyAvailable
     * @return Softwares
     */
    public function setPubliclyAvailable($publiclyAvailable)
    {
        $this->publiclyAvailable = $publiclyAvailable;

        return $this;
    }

    /**
     * Get publiclyAvailable
     *
     * @return boolean
     */
    public function getPubliclyAvailable()
    {
        return $this->publiclyAvailable;
    }

    /**
     * Set revisionCount
     *
     * @param integer $revisionCount
     * @return Softwares
     */
    public function setRevisionCount($revisionCount)
    {
        $this->revisionCount = $revisionCount;

        return $this;
    }

    /**
     * Get revisionCount
     *
     * @return integer
     */
    public function getRevisionCount()
    {
        return $this->revisionCount;
    }

    /**
     * Set shortDescription
     *
     * @param string $shortDescription
     * @return Softwares
     */
    public function setShortDescription($shortDescription)
    {
        $this->shortDescription = $shortDescription;

        return $this;
    }

    /**
     * Get shortDescription
     *
     * @return string
     */
    public function getShortDescription()
    {
        return $this->shortDescription;
    }

    /**
     * Set tags
     *
     * @param string $tags
     * @return Softwares
     */
    public function setTags($tags)
    {
        $this->tags = $tags;

        return $this;
    }

    /**
     * Get tags
     *
     * @return string
     */
    public function getTags()
    {
        return $this->tags;
    }

    /**
     * Set tenantId
     *
     * @param string $tenantId
     * @return Softwares
     */
    public function setTenantId($tenantId)
    {
        $this->tenantId = $tenantId;

        return $this;
    }

    /**
     * Get tenantId
     *
     * @return string
     */
    public function getTenantId()
    {
        return $this->tenantId;
    }

    /**
     * Set testPath
     *
     * @param string $testPath
     * @return Softwares
     */
    public function setTestPath($testPath)
    {
        $this->testPath = $testPath;

        return $this;
    }

    /**
     * Get testPath
     *
     * @return string
     */
    public function getTestPath()
    {
        return $this->testPath;
    }

    /**
     * Set uuid
     *
     * @param string $uuid
     * @return Softwares
     */
    public function setUuid($uuid)
    {
        $this->uuid = $uuid;

        return $this;
    }

    /**
     * Get uuid
     *
     * @return string
     */
    public function getUuid()
    {
        return $this->uuid;
    }

    /**
     * Set version
     *
     * @param string $version
     * @return Softwares
     */
    public function setVersion($version)
    {
        $this->version = $version;

        return $this;
    }

    /**
     * Get version
     *
     * @return string
     */
    public function getVersion()
    {
        return $this->version;
    }

    /**
     * Set defaultMemory
     *
     * @param float $defaultMemory
     * @return Softwares
     */
    public function setDefaultMemory($defaultMemory)
    {
        $this->defaultMemory = $defaultMemory;

        return $this;
    }

    /**
     * Get defaultMemory
     *
     * @return float
     */
    public function getDefaultMemory()
    {
        return $this->defaultMemory;
    }

    /**
     * Set defaultProcesors
     *
     * @param integer $defaultProcesors
     * @return Softwares
     */
    public function setDefaultProcesors($defaultProcesors)
    {
        $this->defaultProcesors = $defaultProcesors;

        return $this;
    }

    /**
     * Get defaultProcesors
     *
     * @return integer
     */
    public function getDefaultProcesors()
    {
        return $this->defaultProcesors;
    }

    /**
     * Set defaultQueue
     *
     * @param string $defaultQueue
     * @return Softwares
     */
    public function setDefaultQueue($defaultQueue)
    {
        $this->defaultQueue = $defaultQueue;

        return $this;
    }

    /**
     * Get defaultQueue
     *
     * @return string
     */
    public function getDefaultQueue()
    {
        return $this->defaultQueue;
    }

    /**
     * Set defaultRequestedTime
     *
     * @param string $defaultRequestedTime
     * @return Softwares
     */
    public function setDefaultRequestedTime($defaultRequestedTime)
    {
        $this->defaultRequestedTime = $defaultRequestedTime;

        return $this;
    }

    /**
     * Get defaultRequestedTime
     *
     * @return string
     */
    public function getDefaultRequestedTime()
    {
        return $this->defaultRequestedTime;
    }

    /**
     * Set defaultNodes
     *
     * @param integer $defaultNodes
     * @return Softwares
     */
    public function setDefaultNodes($defaultNodes)
    {
        $this->defaultNodes = $defaultNodes;

        return $this;
    }

    /**
     * Get defaultNodes
     *
     * @return integer
     */
    public function getDefaultNodes()
    {
        return $this->defaultNodes;
    }

    /**
     * Set system
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Executionsystems $system
     * @return Softwares
     */
    public function setSystem(\Agave\Bundle\ApiBundle\Entity\Executionsystems $system = null)
    {
        $this->system = $system;

        return $this;
    }

    /**
     * Get system
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Executionsystems
     */
    public function getSystem()
    {
        return $this->system;
    }

    /**
     * Set storageSystem
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Storagesystems $storageSystem
     * @return Softwares
     */
    public function setStorageSystem(\Agave\Bundle\ApiBundle\Entity\Storagesystems $storageSystem = null)
    {
        $this->storageSystem = $storageSystem;

        return $this;
    }

    /**
     * Get storageSystem
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Storagesystems
     */
    public function getStorageSystem()
    {
        return $this->storageSystem;
    }

    /**
     * Add inputs
     *
     * @param \Agave\Bundle\ApiBundle\Entity\SoftwareInputs $inputs
     * @return Softwares
     */
    public function addInput(\Agave\Bundle\ApiBundle\Entity\SoftwareInputs $inputs)
    {
        $this->inputs[] = $inputs;

        return $this;
    }

    /**
     * Remove inputs
     *
     * @param \Agave\Bundle\ApiBundle\Entity\SoftwareInputs $inputs
     */
    public function removeInput(\Agave\Bundle\ApiBundle\Entity\SoftwareInputs $inputs)
    {
        $this->inputs->removeElement($inputs);
    }

    /**
     * Get inputs
     *
     * @return \Doctrine\Common\Collections\Collection
     */
    public function getInputs()
    {
        return $this->inputs;
    }

    /**
     * Add outputs
     *
     * @param \Agave\Bundle\ApiBundle\Entity\SoftwareOutputs $outputs
     * @return Softwares
     */
    public function addOutput(\Agave\Bundle\ApiBundle\Entity\SoftwareOutputs $outputs)
    {
        $this->outputs[] = $outputs;

        return $this;
    }

    /**
     * Remove outputs
     *
     * @param \Agave\Bundle\ApiBundle\Entity\SoftwareOutputs $outputs
     */
    public function removeOutput(\Agave\Bundle\ApiBundle\Entity\SoftwareOutputs $outputs)
    {
        $this->outputs->removeElement($outputs);
    }

    /**
     * Get outputs
     *
     * @return \Doctrine\Common\Collections\Collection
     */
    public function getOutputs()
    {
        return $this->outputs;
    }

    /**
     * Add parameters
     *
     * @param \Agave\Bundle\ApiBundle\Entity\SoftwareParameters $parameters
     * @return Softwares
     */
    public function addParameter(\Agave\Bundle\ApiBundle\Entity\SoftwareParameters $parameters)
    {
        $this->parameters[] = $parameters;

        return $this;
    }

    /**
     * Remove parameters
     *
     * @param \Agave\Bundle\ApiBundle\Entity\SoftwareParameters $parameters
     */
    public function removeParameter(\Agave\Bundle\ApiBundle\Entity\SoftwareParameters $parameters)
    {
        $this->parameters->removeElement($parameters);
    }

    /**
     * Get parameters
     *
     * @return \Doctrine\Common\Collections\Collection
     */
    public function getParameters()
    {
        return $this->parameters;
    }
}
