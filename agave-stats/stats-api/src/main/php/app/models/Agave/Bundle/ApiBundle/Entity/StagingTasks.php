<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * StagingTasks
 */
class StagingTasks
{
    /**
     * @var integer
     */
    private $id;

    /**
     * @var integer
     */
    private $bytesTransferred;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var integer
     */
    private $retryCount;

    /**
     * @var string
     */
    private $status;

    /**
     * @var integer
     */
    private $totalBytes;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\LogicalFiles
     */
    private $logicalFile;


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
     * Set bytesTransferred
     *
     * @param integer $bytesTransferred
     * @return StagingTasks
     */
    public function setBytesTransferred($bytesTransferred)
    {
        $this->bytesTransferred = $bytesTransferred;

        return $this;
    }

    /**
     * Get bytesTransferred
     *
     * @return integer 
     */
    public function getBytesTransferred()
    {
        return $this->bytesTransferred;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return StagingTasks
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
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return StagingTasks
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
     * Set retryCount
     *
     * @param integer $retryCount
     * @return StagingTasks
     */
    public function setRetryCount($retryCount)
    {
        $this->retryCount = $retryCount;

        return $this;
    }

    /**
     * Get retryCount
     *
     * @return integer 
     */
    public function getRetryCount()
    {
        return $this->retryCount;
    }

    /**
     * Set status
     *
     * @param string $status
     * @return StagingTasks
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
     * Set totalBytes
     *
     * @param integer $totalBytes
     * @return StagingTasks
     */
    public function setTotalBytes($totalBytes)
    {
        $this->totalBytes = $totalBytes;

        return $this;
    }

    /**
     * Get totalBytes
     *
     * @return integer 
     */
    public function getTotalBytes()
    {
        return $this->totalBytes;
    }

    /**
     * Set logicalFile
     *
     * @param \Agave\Bundle\ApiBundle\Entity\LogicalFiles $logicalFile
     * @return StagingTasks
     */
    public function setLogicalFile(\Agave\Bundle\ApiBundle\Entity\LogicalFiles $logicalFile = null)
    {
        $this->logicalFile = $logicalFile;

        return $this;
    }

    /**
     * Get logicalFile
     *
     * @return \Agave\Bundle\ApiBundle\Entity\LogicalFiles 
     */
    public function getLogicalFile()
    {
        return $this->logicalFile;
    }
}
