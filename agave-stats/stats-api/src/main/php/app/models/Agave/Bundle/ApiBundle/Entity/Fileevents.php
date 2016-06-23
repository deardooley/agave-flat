<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Fileevents
 */
class Fileevents
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
     * @var \Agave\Bundle\ApiBundle\Entity\LogicalFiles
     */
    private $logicalfile;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Transfertasks
     */
    private $transfertask;


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
     * @return Fileevents
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
     * Set description
     *
     * @param string $description
     * @return Fileevents
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
     * @return Fileevents
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
     * @return Fileevents
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
     * Set logicalfile
     *
     * @param \Agave\Bundle\ApiBundle\Entity\LogicalFiles $logicalfile
     * @return Fileevents
     */
    public function setLogicalfile(\Agave\Bundle\ApiBundle\Entity\LogicalFiles $logicalfile = null)
    {
        $this->logicalfile = $logicalfile;

        return $this;
    }

    /**
     * Get logicalfile
     *
     * @return \Agave\Bundle\ApiBundle\Entity\LogicalFiles 
     */
    public function getLogicalfile()
    {
        return $this->logicalfile;
    }

    /**
     * Set transfertask
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Transfertasks $transfertask
     * @return Fileevents
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
}
