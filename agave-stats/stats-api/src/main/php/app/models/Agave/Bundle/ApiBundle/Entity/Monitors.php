<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Monitors
 * @ORM\Entity
 * @ORM\Table(name="monitors")
 */
class Monitors
{
  /**
  * @ORM\Id
  * @ORM\GeneratedValue
  * @ORM\Column(type="integer")
  */
  private $id;

    /**
     * @var boolean
     */
    private $isActive;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var integer
     */
    private $frequency;

    /**
     * @var string
     */
    private $internalUsername;

    /**
     * @var \DateTime
     */
    private $lastSuccess;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var \DateTime
     */
    private $nextUpdateTime;

    /**
     * @var string
     */
    private $owner;

    /**
     * @var string
     */
    private $tenantId;

    /**
     * @var boolean
     */
    private $updateSystemStatus;

    /**
     * @var string
     */
    private $uuid;

    /**
     * @var \Agave\Bundle\ApiBundle\Entity\Systems
     */
    private $system;


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
     * Set isActive
     *
     * @param boolean $isActive
     * @return Monitors
     */
    public function setIsActive($isActive)
    {
        $this->isActive = $isActive;

        return $this;
    }

    /**
     * Get isActive
     *
     * @return boolean
     */
    public function getIsActive()
    {
        return $this->isActive;
    }

    /**
     * Set created
     *
     * @param \DateTime $created
     * @return Monitors
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
     * Set frequency
     *
     * @param integer $frequency
     * @return Monitors
     */
    public function setFrequency($frequency)
    {
        $this->frequency = $frequency;

        return $this;
    }

    /**
     * Get frequency
     *
     * @return integer
     */
    public function getFrequency()
    {
        return $this->frequency;
    }

    /**
     * Set internalUsername
     *
     * @param string $internalUsername
     * @return Monitors
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
     * Set lastSuccess
     *
     * @param \DateTime $lastSuccess
     * @return Monitors
     */
    public function setLastSuccess($lastSuccess)
    {
        $this->lastSuccess = $lastSuccess;

        return $this;
    }

    /**
     * Get lastSuccess
     *
     * @return \DateTime
     */
    public function getLastSuccess()
    {
        return $this->lastSuccess;
    }

    /**
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Monitors
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
     * Set nextUpdateTime
     *
     * @param \DateTime $nextUpdateTime
     * @return Monitors
     */
    public function setNextUpdateTime($nextUpdateTime)
    {
        $this->nextUpdateTime = $nextUpdateTime;

        return $this;
    }

    /**
     * Get nextUpdateTime
     *
     * @return \DateTime
     */
    public function getNextUpdateTime()
    {
        return $this->nextUpdateTime;
    }

    /**
     * Set owner
     *
     * @param string $owner
     * @return Monitors
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
     * Set tenantId
     *
     * @param string $tenantId
     * @return Monitors
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
     * Set updateSystemStatus
     *
     * @param boolean $updateSystemStatus
     * @return Monitors
     */
    public function setUpdateSystemStatus($updateSystemStatus)
    {
        $this->updateSystemStatus = $updateSystemStatus;

        return $this;
    }

    /**
     * Get updateSystemStatus
     *
     * @return boolean
     */
    public function getUpdateSystemStatus()
    {
        return $this->updateSystemStatus;
    }

    /**
     * Set uuid
     *
     * @param string $uuid
     * @return Monitors
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
     * Set system
     *
     * @param \Agave\Bundle\ApiBundle\Entity\Systems $system
     * @return Monitors
     */
    public function setSystem(\Agave\Bundle\ApiBundle\Entity\Systems $system = null)
    {
        $this->system = $system;

        return $this;
    }

    /**
     * Get system
     *
     * @return \Agave\Bundle\ApiBundle\Entity\Systems
     */
    public function getSystem()
    {
        return $this->system;
    }
}
