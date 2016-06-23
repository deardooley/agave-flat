<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Logicalfilenotifications
 */
class Logicalfilenotifications
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
     * @var \DateTime
     */
    private $lastSent;

    /**
     * @var string
     */
    private $status;

    /**
     * @var boolean
     */
    private $stillPending;

    /**
     * @var string
     */
    private $callback;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\LogicalFiles
     */
    private $logicalfile;


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
     * @return Logicalfilenotifications
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
     * Set lastSent
     *
     * @param \DateTime $lastSent
     * @return Logicalfilenotifications
     */
    public function setLastSent($lastSent)
    {
        $this->lastSent = $lastSent;

        return $this;
    }

    /**
     * Get lastSent
     *
     * @return \DateTime 
     */
    public function getLastSent()
    {
        return $this->lastSent;
    }

    /**
     * Set status
     *
     * @param string $status
     * @return Logicalfilenotifications
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
     * Set stillPending
     *
     * @param boolean $stillPending
     * @return Logicalfilenotifications
     */
    public function setStillPending($stillPending)
    {
        $this->stillPending = $stillPending;

        return $this;
    }

    /**
     * Get stillPending
     *
     * @return boolean 
     */
    public function getStillPending()
    {
        return $this->stillPending;
    }

    /**
     * Set callback
     *
     * @param string $callback
     * @return Logicalfilenotifications
     */
    public function setCallback($callback)
    {
        $this->callback = $callback;

        return $this;
    }

    /**
     * Get callback
     *
     * @return string 
     */
    public function getCallback()
    {
        return $this->callback;
    }

    /**
     * Set logicalfile
     *
     * @param \Agave\Bundle\ApiBundle\Entity\LogicalFiles $logicalfile
     * @return Logicalfilenotifications
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
}
