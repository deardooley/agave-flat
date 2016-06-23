<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Jobs
 * @ORM\Entity
 * @ORM\Table(name="jobs")
 */
class Jobs
{
    /**
    * @ORM\Id
    * @ORM\GeneratedValue
    * @ORM\Column(type="integer")
    */
    private $id;

    /**
     * @var boolean
     * @ORM\Column(type="boolean", name="archive_output")
     */
    private $archiveOutput;

    /**
     * @var string
     * @ORM\Column(type="string", name="archive_path")
     */
    private $archivePath;

    /**
     * @var string
     */
    private $callbackUrl;

    /**
     * @var float
     */
    private $charge;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="created")
     */
    private $created;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="end_time")
     */
    private $endTime;

    /**
     * @var string
     * @ORM\Column(type="string", name="error_message")
     */
    private $errorMessage;

    /**
     * @var string
     * @ORM\Column(type="string", name="inputs")
     */
    private $inputs;

    /**
     * @var string
     */
    private $internalUsername;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="last_updated")
     */
    private $lastUpdated;

    /**
     * @var string
     * @ORM\Column(type="string")
     */
    private $localJobId;

    /**
     * @var integer
     * @ORM\Column(type="integer", name="memory_request")
     */
    private $memoryRequest;

    /**
     * @var string
     * @ORM\Column(type="string")
     */
    private $name;

    /**
     * @var string
     */
    private $outputPath;

    /**
     * @var string
     * @ORM\Column(type="string")
     */
    private $owner;

    /**
     * @var string
     * @ORM\Column(type="string", name="parameters")
     */
    private $parameters;

    /**
     * @var integer
     * @ORM\Column(type="integer", name="processor_count")
     */
    private $processorCount;

    /**
     * @var string
     * @ORM\Column(type="string", name="requested_time")
     */
    private $requestedTime;

    /**
     * @var integer
     * @ORM\Column(type="integer", name="retries")
     */
    private $retries;

    /**
     * @var string
     * @ORM\Column(type="string", name="scheduler_job_id")
     */
    private $schedulerJobId;

    /**
     * @var string
     * @ORM\Column(type="string", name="software_name")
     */
    private $softwareName;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="start_time")
     */
    private $startTime;

    /**
     * @var string
     * @ORM\Column(type="string", name="status")
     */
    private $status;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="submit_time")
     */
    private $submitTime;

    /**
     * @var string
     * @ORM\Column(type="string", name="execution_system")
     */
    private $executionSystem;

    /**
     * @var string
     * @ORM\Column(type="string", name="tenant_id")
     */
    private $tenantId;

    /**
     * @var string
     * @ORM\Column(type="string", name="update_token")
     */
    private $updateToken;

    /**
     * @var string
     * @ORM\Column(type="string", name="uuid")
     */
    private $uuid;

    /**
     * @var integer
     */
    private $optlock;

    /**
     * @var boolean
     * @ORM\Column(type="boolean", name="visible")
     */
    private $visible;

    /**
     * @var string
     * @ORM\Column(type="string", name="work_path")
     */
    private $workPath;

    /**
     * @var string
     */
    private $queueRequest;

    /**
     * @var integer
     * @ORM\Column(type="integer", name="node_count")
     */
    private $nodeCount;

    /**
     * @var integer
     * @ORM\Column(type="integer", name="status_checks")
     */
    private $statusChecks;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Systems
     */
    private $archiveSystem;


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
     * Set archiveOutput
     *
     * @param boolean $archiveOutput
     * @return Jobs
     */
    public function setArchiveOutput($archiveOutput)
    {
        $this->archiveOutput = $archiveOutput;

        return $this;
    }

    /**
     * Get archiveOutput
     *
     * @return boolean
     */
    public function getArchiveOutput()
    {
        return $this->archiveOutput;
    }

    /**
     * Set archivePath
     *
     * @param string $archivePath
     * @return Jobs
     */
    public function setArchivePath($archivePath)
    {
        $this->archivePath = $archivePath;

        return $this;
    }

    /**
     * Get archivePath
     *
     * @return string
     */
    public function getArchivePath()
    {
        return $this->archivePath;
    }

    /**
     * Set callbackUrl
     *
     * @param string $callbackUrl
     * @return Jobs
     */
    public function setCallbackUrl($callbackUrl)
    {
        $this->callbackUrl = $callbackUrl;

        return $this;
    }

    /**
     * Get callbackUrl
     *
     * @return string
     */
    public function getCallbackUrl()
    {
        return $this->callbackUrl;
    }

    /**
     * Set charge
     *
     * @param float $charge
     * @return Jobs
     */
    public function setCharge($charge)
    {
        $this->charge = $charge;

        return $this;
    }

    /**
     * Get charge
     *
     * @return float
     */
    public function getCharge()
    {
        return $this->charge;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return Jobs
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
     * Set endTime
     *
     * @param \DateTime $endTime
     * @return Jobs
     */
    public function setEndTime($endTime)
    {
        $this->endTime = $endTime;

        return $this;
    }

    /**
     * Get endTime
     *
     * @return \DateTime
     */
    public function getEndTime()
    {
        return $this->endTime;
    }

    /**
     * Set errorMessage
     *
     * @param string $errorMessage
     * @return Jobs
     */
    public function setErrorMessage($errorMessage)
    {
        $this->errorMessage = $errorMessage;

        return $this;
    }

    /**
     * Get errorMessage
     *
     * @return string
     */
    public function getErrorMessage()
    {
        return $this->errorMessage;
    }

    /**
     * Set inputs
     *
     * @param string $inputs
     * @return Jobs
     */
    public function setInputs($inputs)
    {
        $this->inputs = $inputs;

        return $this;
    }

    /**
     * Get inputs
     *
     * @return string
     */
    public function getInputs()
    {
        return $this->inputs;
    }

    /**
     * Set internalUsername
     *
     * @param string $internalUsername
     * @return Jobs
     */
    public function setInternalUsername($internalUsername)
    {
        $this->internalUsername = $internalUsername;

        return $this;
    }

    /**
     * Get internalUsername
     *
     * @return string
     */
    public function getInternalUsername()
    {
        return $this->internalUsername;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Jobs
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
     * Set localJobId
     *
     * @param string $localJobId
     * @return Jobs
     */
    public function setLocalJobId($localJobId)
    {
        $this->localJobId = $localJobId;

        return $this;
    }

    /**
     * Get localJobId
     *
     * @return string
     */
    public function getLocalJobId()
    {
        return $this->localJobId;
    }

    /**
     * Set memoryRequest
     *
     * @param integer $memoryRequest
     * @return Jobs
     */
    public function setMemoryRequest($memoryRequest)
    {
        $this->memoryRequest = $memoryRequest;

        return $this;
    }

    /**
     * Get memoryRequest
     *
     * @return integer
     */
    public function getMemoryRequest()
    {
        return $this->memoryRequest;
    }

    /**
     * Set name
     *
     * @param string $name
     * @return Jobs
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
     * Set outputPath
     *
     * @param string $outputPath
     * @return Jobs
     */
    public function setOutputPath($outputPath)
    {
        $this->outputPath = $outputPath;

        return $this;
    }

    /**
     * Get outputPath
     *
     * @return string
     */
    public function getOutputPath()
    {
        return $this->outputPath;
    }

    /**
     * Set owner
     *
     * @param string $owner
     * @return Jobs
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
     * Set parameters
     *
     * @param string $parameters
     * @return Jobs
     */
    public function setParameters($parameters)
    {
        $this->parameters = $parameters;

        return $this;
    }

    /**
     * Get parameters
     *
     * @return string
     */
    public function getParameters()
    {
        return $this->parameters;
    }

    /**
     * Set processorCount
     *
     * @param integer $processorCount
     * @return Jobs
     */
    public function setProcessorCount($processorCount)
    {
        $this->processorCount = $processorCount;

        return $this;
    }

    /**
     * Get processorCount
     *
     * @return integer
     */
    public function getProcessorCount()
    {
        return $this->processorCount;
    }

    /**
     * Set requestedTime
     *
     * @param string $requestedTime
     * @return Jobs
     */
    public function setRequestedTime($requestedTime)
    {
        $this->requestedTime = $requestedTime;

        return $this;
    }

    /**
     * Get requestedTime
     *
     * @return string
     */
    public function getRequestedTime()
    {
        return $this->requestedTime;
    }

    /**
     * Set retries
     *
     * @param integer $retries
     * @return Jobs
     */
    public function setRetries($retries)
    {
        $this->retries = $retries;

        return $this;
    }

    /**
     * Get retries
     *
     * @return integer
     */
    public function getRetries()
    {
        return $this->retries;
    }

    /**
     * Set schedulerJobId
     *
     * @param string $schedulerJobId
     * @return Jobs
     */
    public function setSchedulerJobId($schedulerJobId)
    {
        $this->schedulerJobId = $schedulerJobId;

        return $this;
    }

    /**
     * Get schedulerJobId
     *
     * @return string
     */
    public function getSchedulerJobId()
    {
        return $this->schedulerJobId;
    }

    /**
     * Set softwareName
     *
     * @param string $softwareName
     * @return Jobs
     */
    public function setSoftwareName($softwareName)
    {
        $this->softwareName = $softwareName;

        return $this;
    }

    /**
     * Get softwareName
     *
     * @return string
     */
    public function getSoftwareName()
    {
        return $this->softwareName;
    }

    /**
     * Set startTime
     *
     * @param \DateTime $startTime
     * @return Jobs
     */
    public function setStartTime($startTime)
    {
        $this->startTime = $startTime;

        return $this;
    }

    /**
     * Get startTime
     *
     * @return \DateTime
     */
    public function getStartTime()
    {
        return $this->startTime;
    }

    /**
     * Set status
     *
     * @param string $status
     * @return Jobs
     */
    public function setStatus($status)
    {
        $this->status = $status;

        return $this;
    }

    /**
     * Get status
     *
     * @return string
     */
    public function getStatus()
    {
        return $this->status;
    }

    /**
     * Set submitTime
     *
     * @param \DateTime $submitTime
     * @return Jobs
     */
    public function setSubmitTime($submitTime)
    {
        $this->submitTime = $submitTime;

        return $this;
    }

    /**
     * Get submitTime
     *
     * @return \DateTime
     */
    public function getSubmitTime()
    {
        return $this->submitTime;
    }

    /**
     * Set executionSystem
     *
     * @param string $executionSystem
     * @return Jobs
     */
    public function setExecutionSystem($executionSystem)
    {
        $this->executionSystem = $executionSystem;

        return $this;
    }

    /**
     * Get executionSystem
     *
     * @return string
     */
    public function getExecutionSystem()
    {
        return $this->executionSystem;
    }

    /**
     * Set tenantId
     *
     * @param string $tenantId
     * @return Jobs
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
     * Set updateToken
     *
     * @param string $updateToken
     * @return Jobs
     */
    public function setUpdateToken($updateToken)
    {
        $this->updateToken = $updateToken;

        return $this;
    }

    /**
     * Get updateToken
     *
     * @return string
     */
    public function getUpdateToken()
    {
        return $this->updateToken;
    }

    /**
     * Set uuid
     *
     * @param string $uuid
     * @return Jobs
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
     * Set optlock
     *
     * @param integer $optlock
     * @return Jobs
     */
    public function setOptlock($optlock)
    {
        $this->optlock = $optlock;

        return $this;
    }

    /**
     * Get optlock
     *
     * @return integer
     */
    public function getOptlock()
    {
        return $this->optlock;
    }

    /**
     * Set visible
     *
     * @param boolean $visible
     * @return Jobs
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
     * Set workPath
     *
     * @param string $workPath
     * @return Jobs
     */
    public function setWorkPath($workPath)
    {
        $this->workPath = $workPath;

        return $this;
    }

    /**
     * Get workPath
     *
     * @return string
     */
    public function getWorkPath()
    {
        return $this->workPath;
    }

    /**
     * Set queueRequest
     *
     * @param string $queueRequest
     * @return Jobs
     */
    public function setQueueRequest($queueRequest)
    {
        $this->queueRequest = $queueRequest;

        return $this;
    }

    /**
     * Get queueRequest
     *
     * @return string
     */
    public function getQueueRequest()
    {
        return $this->queueRequest;
    }

    /**
     * Set nodeCount
     *
     * @param integer $nodeCount
     * @return Jobs
     */
    public function setNodeCount($nodeCount)
    {
        $this->nodeCount = $nodeCount;

        return $this;
    }

    /**
     * Get nodeCount
     *
     * @return integer
     */
    public function getNodeCount()
    {
        return $this->nodeCount;
    }

    /**
     * Set statusChecks
     *
     * @param integer $statusChecks
     * @return Jobs
     */
    public function setStatusChecks($statusChecks)
    {
        $this->statusChecks = $statusChecks;

        return $this;
    }

    /**
     * Get statusChecks
     *
     * @return integer
     */
    public function getStatusChecks()
    {
        return $this->statusChecks;
    }

    /**
     * Set archiveSystem
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systems $archiveSystem
     * @return Jobs
     */
    public function setArchiveSystem(\Agave\Bundle\ApiBundle\Entity\Systems $archiveSystem = null)
    {
        $this->archiveSystem = $archiveSystem;

        return $this;
    }

    /**
     * Get archiveSystem
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Systems
     */
    public function getArchiveSystem()
    {
        return $this->archiveSystem;
    }
}
