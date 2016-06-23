<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Transfertasks
 * @ORM\Entity
 * @ORM\Table(name="transfertasks")
 */
class Transfertasks
{
  /**
  * @ORM\Id
  * @ORM\GeneratedValue
  * @ORM\Column(type="integer")
  */
  private $id;

    /**
     * @var integer
     * @ORM\Column(type="datetime", name="attempts")
     */
    private $attempts;

    /**
     * @var int
     * @ORM\Column(type="integer", name="bytes_transferred")
     */
    private $bytesTransferred;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="created")
     */
    private $created;

    /**
     * @var string
     * @ORM\Column(type="datetime", name="dest")
     */
    private $dest;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="end_time")
     */
    private $endTime;

    /**
     * @var string
     * @ORM\Column(type="string", name="event_id")
     */
    private $eventId;

    /**
     * @var \DateTime
     * @ORM\Column(type="datetime", name="last_updated")
     */
    private $lastUpdated;

    /**
     * @var string
     * @ORM\Column(type="string", name="owner")
     */
    private $owner;

    /**
     * @var string
     */
    private $source;

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
     * @var string
     * @ORM\Column(type="string", name="tenant_id")
     */
    private $tenantId;

    /**
     * @var float
     * @ORM\Column(type="float", name="total_size")
     */
    private $totalSize;

    /**
     * @var float
     * @ORM\Column(type="float", name="transfer_rate")
     */
    private $transferRate;

    /**
     * @var string
     * @ORM\Column(type="string", name="uuid")
     */
    private $uuid;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Transfertasks
     */
    private $parentTask;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Transfertasks
     */
    private $rootTask;


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
     * Set attempts
     *
     * @param integer $attempts
     * @return Transfertasks
     */
    public function setAttempts($attempts)
    {
        $this->attempts = $attempts;

        return $this;
    }

    /**
     * Get attempts
     *
     * @return integer
     */
    public function getAttempts()
    {
        return $this->attempts;
    }

    /**
     * Set bytesTransferred
     *
     * @param float $bytesTransferred
     * @return Transfertasks
     */
    public function setBytesTransferred($bytesTransferred)
    {
        $this->bytesTransferred = $bytesTransferred;

        return $this;
    }

    /**
     * Get bytesTransferred
     *
     * @return float
     */
    public function getBytesTransferred()
    {
        return $this->bytesTransferred;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return Transfertasks
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
     * Set dest
     *
     * @param string $dest
     * @return Transfertasks
     */
    public function setDest($dest)
    {
        $this->dest = $dest;

        return $this;
    }

    /**
     * Get dest
     *
     * @return string
     */
    public function getDest()
    {
        return $this->dest;
    }

    /**
     * Set endTime
     *
     * @param \DateTime $endTime
     * @return Transfertasks
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
     * Set eventId
     *
     * @param string $eventId
     * @return Transfertasks
     */
    public function setEventId($eventId)
    {
        $this->eventId = $eventId;

        return $this;
    }

    /**
     * Get eventId
     *
     * @return string
     */
    public function getEventId()
    {
        return $this->eventId;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Transfertasks
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
     * Set owner
     *
     * @param string $owner
     * @return Transfertasks
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
     * Set source
     *
     * @param string $source
     * @return Transfertasks
     */
    public function setSource($source)
    {
        $this->source = $source;

        return $this;
    }

    /**
     * Get source
     *
     * @return string
     */
    public function getSource()
    {
        return $this->source;
    }

    /**
     * Set startTime
     *
     * @param \DateTime $startTime
     * @return Transfertasks
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
     * @return Transfertasks
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
     * @return Transfertasks
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
     * Set totalSize
     *
     * @param float $totalSize
     * @return Transfertasks
     */
    public function setTotalSize($totalSize)
    {
        $this->totalSize = $totalSize;

        return $this;
    }

    /**
     * Get totalSize
     *
     * @return float
     */
    public function getTotalSize()
    {
        return $this->totalSize;
    }

    /**
     * Set transferRate
     *
     * @param float $transferRate
     * @return Transfertasks
     */
    public function setTransferRate($transferRate)
    {
        $this->transferRate = $transferRate;

        return $this;
    }

    /**
     * Get transferRate
     *
     * @return float
     */
    public function getTransferRate()
    {
        return $this->transferRate;
    }

    /**
     * Set uuid
     *
     * @param string $uuid
     * @return Transfertasks
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
     * Set parentTask
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Transfertasks $parentTask
     * @return Transfertasks
     */
    public function setParentTask(\Agave\Bundle\ApiBundle\Entity\Transfertasks $parentTask = null)
    {
        $this->parentTask = $parentTask;

        return $this;
    }

    /**
     * Get parentTask
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Transfertasks
     */
    public function getParentTask()
    {
        return $this->parentTask;
    }

    /**
     * Set rootTask
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Transfertasks $rootTask
     * @return Transfertasks
     */
    public function setRootTask(\Agave\Bundle\ApiBundle\Entity\Transfertasks $rootTask = null)
    {
        $this->rootTask = $rootTask;

        return $this;
    }

    /**
     * Get rootTask
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Transfertasks
     */
    public function getRootTask()
    {
        return $this->rootTask;
    }
}
