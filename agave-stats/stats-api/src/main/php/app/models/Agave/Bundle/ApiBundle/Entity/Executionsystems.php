<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Executionsystems
 */
class Executionsystems
{
    /**
     * @var string
     */
    private $environment;

    /**
     * @var string
     */
    private $executionType;

    /**
     * @var integer
     */
    private $maxSystemJobs;

    /**
     * @var integer
     */
    private $maxSystemJobsPerUser;

    /**
     * @var string
     */
    private $schedulerType;

    /**
     * @var string
     */
    private $scratchDir;

    /**
     * @var string
     */
    private $startupScript;

    /**
     * @var string
     */
    private $type;

    /**
     * @var string
     */
    private $workDir;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Systems
     */
    private $id;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Loginconfigs
     */
    private $loginConfig;


    /**
     * Set environment
     *
     * @param string $environment
     * @return Executionsystems
     */
    public function setEnvironment($environment)
    {
        $this->environment = $environment;

        return $this;
    }

    /**
     * Get environment
     *
     * @return string 
     */
    public function getEnvironment()
    {
        return $this->environment;
    }

    /**
     * Set executionType
     *
     * @param string $executionType
     * @return Executionsystems
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
     * Set maxSystemJobs
     *
     * @param integer $maxSystemJobs
     * @return Executionsystems
     */
    public function setMaxSystemJobs($maxSystemJobs)
    {
        $this->maxSystemJobs = $maxSystemJobs;

        return $this;
    }

    /**
     * Get maxSystemJobs
     *
     * @return integer 
     */
    public function getMaxSystemJobs()
    {
        return $this->maxSystemJobs;
    }

    /**
     * Set maxSystemJobsPerUser
     *
     * @param integer $maxSystemJobsPerUser
     * @return Executionsystems
     */
    public function setMaxSystemJobsPerUser($maxSystemJobsPerUser)
    {
        $this->maxSystemJobsPerUser = $maxSystemJobsPerUser;

        return $this;
    }

    /**
     * Get maxSystemJobsPerUser
     *
     * @return integer 
     */
    public function getMaxSystemJobsPerUser()
    {
        return $this->maxSystemJobsPerUser;
    }

    /**
     * Set schedulerType
     *
     * @param string $schedulerType
     * @return Executionsystems
     */
    public function setSchedulerType($schedulerType)
    {
        $this->schedulerType = $schedulerType;

        return $this;
    }

    /**
     * Get schedulerType
     *
     * @return string 
     */
    public function getSchedulerType()
    {
        return $this->schedulerType;
    }

    /**
     * Set scratchDir
     *
     * @param string $scratchDir
     * @return Executionsystems
     */
    public function setScratchDir($scratchDir)
    {
        $this->scratchDir = $scratchDir;

        return $this;
    }

    /**
     * Get scratchDir
     *
     * @return string 
     */
    public function getScratchDir()
    {
        return $this->scratchDir;
    }

    /**
     * Set startupScript
     *
     * @param string $startupScript
     * @return Executionsystems
     */
    public function setStartupScript($startupScript)
    {
        $this->startupScript = $startupScript;

        return $this;
    }

    /**
     * Get startupScript
     *
     * @return string 
     */
    public function getStartupScript()
    {
        return $this->startupScript;
    }

    /**
     * Set type
     *
     * @param string $type
     * @return Executionsystems
     */
    public function setType($type)
    {
        $this->type = $type;

        return $this;
    }

    /**
     * Get type
     *
     * @return string 
     */
    public function getType()
    {
        return $this->type;
    }

    /**
     * Set workDir
     *
     * @param string $workDir
     * @return Executionsystems
     */
    public function setWorkDir($workDir)
    {
        $this->workDir = $workDir;

        return $this;
    }

    /**
     * Get workDir
     *
     * @return string 
     */
    public function getWorkDir()
    {
        return $this->workDir;
    }

    /**
     * Set id
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systems $id
     * @return Executionsystems
     */
    public function setId(\Agave\Bundle\ApiBundle\Entity\Systems $id = null)
    {
        $this->id = $id;

        return $this;
    }

    /**
     * Get id
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Systems 
     */
    public function getId()
    {
        return $this->id;
    }

    /**
     * Set loginConfig
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Loginconfigs $loginConfig
     * @return Executionsystems
     */
    public function setLoginConfig(\Agave\Bundle\ApiBundle\Entity\Loginconfigs $loginConfig = null)
    {
        $this->loginConfig = $loginConfig;

        return $this;
    }

    /**
     * Get loginConfig
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Loginconfigs 
     */
    public function getLoginConfig()
    {
        return $this->loginConfig;
    }
}
