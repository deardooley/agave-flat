<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Batchqueues
 */
class Batchqueues
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
    private $customDirectives;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var integer
     */
    private $maxJobs;

    /**
     * @var integer
     */
    private $maxMemory;

    /**
     * @var string
     */
    private $name;

    /**
     * @var boolean
     */
    private $systemDefault;

    /**
     * @var integer
     */
    private $maxNodes;

    /**
     * @var integer
     */
    private $maxProcesors;

    /**
     * @var string
     */
    private $maxRequestedTime;

    /**
     * @var integer
     */
    private $maxUserJobs;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Executionsystems
     */
    private $executionSystem;


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
     * @return Batchqueues
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
     * Set customDirectives
     *
     * @param string $customDirectives
     * @return Batchqueues
     */
    public function setCustomDirectives($customDirectives)
    {
        $this->customDirectives = $customDirectives;

        return $this;
    }

    /**
     * Get customDirectives
     *
     * @return string 
     */
    public function getCustomDirectives()
    {
        return $this->customDirectives;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Batchqueues
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
     * Set maxJobs
     *
     * @param integer $maxJobs
     * @return Batchqueues
     */
    public function setMaxJobs($maxJobs)
    {
        $this->maxJobs = $maxJobs;

        return $this;
    }

    /**
     * Get maxJobs
     *
     * @return integer 
     */
    public function getMaxJobs()
    {
        return $this->maxJobs;
    }

    /**
     * Set maxMemory
     *
     * @param integer $maxMemory
     * @return Batchqueues
     */
    public function setMaxMemory($maxMemory)
    {
        $this->maxMemory = $maxMemory;

        return $this;
    }

    /**
     * Get maxMemory
     *
     * @return integer 
     */
    public function getMaxMemory()
    {
        return $this->maxMemory;
    }

    /**
     * Set name
     *
     * @param string $name
     * @return Batchqueues
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
     * Set systemDefault
     *
     * @param boolean $systemDefault
     * @return Batchqueues
     */
    public function setSystemDefault($systemDefault)
    {
        $this->systemDefault = $systemDefault;

        return $this;
    }

    /**
     * Get systemDefault
     *
     * @return boolean 
     */
    public function getSystemDefault()
    {
        return $this->systemDefault;
    }

    /**
     * Set maxNodes
     *
     * @param integer $maxNodes
     * @return Batchqueues
     */
    public function setMaxNodes($maxNodes)
    {
        $this->maxNodes = $maxNodes;

        return $this;
    }

    /**
     * Get maxNodes
     *
     * @return integer 
     */
    public function getMaxNodes()
    {
        return $this->maxNodes;
    }

    /**
     * Set maxProcesors
     *
     * @param integer $maxProcesors
     * @return Batchqueues
     */
    public function setMaxProcesors($maxProcesors)
    {
        $this->maxProcesors = $maxProcesors;

        return $this;
    }

    /**
     * Get maxProcesors
     *
     * @return integer 
     */
    public function getMaxProcesors()
    {
        return $this->maxProcesors;
    }

    /**
     * Set maxRequestedTime
     *
     * @param string $maxRequestedTime
     * @return Batchqueues
     */
    public function setMaxRequestedTime($maxRequestedTime)
    {
        $this->maxRequestedTime = $maxRequestedTime;

        return $this;
    }

    /**
     * Get maxRequestedTime
     *
     * @return string 
     */
    public function getMaxRequestedTime()
    {
        return $this->maxRequestedTime;
    }

    /**
     * Set maxUserJobs
     *
     * @param integer $maxUserJobs
     * @return Batchqueues
     */
    public function setMaxUserJobs($maxUserJobs)
    {
        $this->maxUserJobs = $maxUserJobs;

        return $this;
    }

    /**
     * Get maxUserJobs
     *
     * @return integer 
     */
    public function getMaxUserJobs()
    {
        return $this->maxUserJobs;
    }

    /**
     * Set executionSystem
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Executionsystems $executionSystem
     * @return Batchqueues
     */
    public function setExecutionSystem(\Agave\Bundle\ApiBundle\Entity\Executionsystems $executionSystem = null)
    {
        $this->executionSystem = $executionSystem;

        return $this;
    }

    /**
     * Get executionSystem
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Executionsystems 
     */
    public function getExecutionSystem()
    {
        return $this->executionSystem;
    }
}
