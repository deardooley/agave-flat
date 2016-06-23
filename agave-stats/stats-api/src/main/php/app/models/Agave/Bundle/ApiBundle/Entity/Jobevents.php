<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Jobevents
 */
class Jobevents
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
    private $createdBy;

    /**
     * @var string
     */
    private $description;

    /**
     * @var string
     */
    private $ipAddress;

    /**
     * @var string
     */
    private $status;

    /**
     * @var string
     */
    private $tenantId;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Transfertasks
     */
    private $transfertask;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Jobs
     */
    private $job;


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
     * @return Jobevents
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
     * Set createdBy
     *
     * @param string $createdBy
     * @return Jobevents
     */
    public function setCreatedBy($createdBy)
    {
        $this->createdBy = $createdBy;

        return $this;
    }

    /**
     * Get createdBy
     *
     * @return string 
     */
    public function getCreatedBy()
    {
        return $this->createdBy;
    }

    /**
     * Set description
     *
     * @param string $description
     * @return Jobevents
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
     * Set ipAddress
     *
     * @param string $ipAddress
     * @return Jobevents
     */
    public function setIpAddress($ipAddress)
    {
        $this->ipAddress = $ipAddress;

        return $this;
    }

    /**
     * Get ipAddress
     *
     * @return string 
     */
    public function getIpAddress()
    {
        return $this->ipAddress;
    }

    /**
     * Set status
     *
     * @param string $status
     * @return Jobevents
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
     * Set tenantId
     *
     * @param string $tenantId
     * @return Jobevents
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
     * Set transfertask
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Transfertasks $transfertask
     * @return Jobevents
     */
    public function setTransfertask(\Agave\Bundle\ApiBundle\Entity\Transfertasks $transfertask = null)
    {
        $this->transfertask = $transfertask;

        return $this;
    }

    /**
     * Get transfertask
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Transfertasks 
     */
    public function getTransfertask()
    {
        return $this->transfertask;
    }

    /**
     * Set job
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Jobs $job
     * @return Jobevents
     */
    public function setJob(\Agave\Bundle\ApiBundle\Entity\Jobs $job = null)
    {
        $this->job = $job;

        return $this;
    }

    /**
     * Get job
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Jobs 
     */
    public function getJob()
    {
        return $this->job;
    }
}
