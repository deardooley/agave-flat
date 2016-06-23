<?php

namespace Agave\Bundle\ApiBundle\Entity;

use Doctrine\ORM\Mapping as ORM;

/**
 * Notifications
 * @ORM\Entity
 * @ORM\Table(name="notifications")
 */
class Notifications
{
  /**
  * @ORM\Id
  * @ORM\GeneratedValue
  * @ORM\Column(type="integer")
  */
  private $id;

    /**
     * @var string
     */
    private $associatedUuid;

    /**
     * @var integer
     */
    private $attempts;

    /**
     * @var string
     */
    private $callbackUrl;

    /**
     * @var \DateTime
     */
    private $created;

    /**
     * @var \DateTime
     */
    private $lastSent;

    /**
     * @var \DateTime
     */
    private $lastUpdated;

    /**
     * @var string
     */
    private $notificationEvent;

    /**
     * @var string
     */
    private $owner;

    /**
     * @var boolean
     */
    private $isPersistent;

    /**
     * @var integer
     */
    private $responseCode;

    /**
     * @var boolean
     */
    private $isSuccess;

    /**
     * @var string
     */
    private $tenantId;

    /**
     * @var string
     */
    private $uuid;

    /**
     * @var boolean
     */
    private $isTerminated;


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
     * Set associatedUuid
     *
     * @param string $associatedUuid
     * @return Notifications
     */
    public function setAssociatedUuid($associatedUuid)
    {
        $this->associatedUuid = $associatedUuid;

        return $this;
    }

    /**
     * Get associatedUuid
     *
     * @return string
     */
    public function getAssociatedUuid()
    {
        return $this->associatedUuid;
    }

    /**
     * Set attempts
     *
     * @param integer $attempts
     * @return Notifications
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
     * Set callbackUrl
     *
     * @param string $callbackUrl
     * @return Notifications
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
     * Set created
     *
     * @param \DateTime $created
     * @return Notifications
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
     * @return Notifications
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
     * Set lastUpdated
     *
     * @param \DateTime $lastUpdated
     * @return Notifications
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
     * Set notificationEvent
     *
     * @param string $notificationEvent
     * @return Notifications
     */
    public function setNotificationEvent($notificationEvent)
    {
        $this->notificationEvent = $notificationEvent;

        return $this;
    }

    /**
     * Get notificationEvent
     *
     * @return string
     */
    public function getNotificationEvent()
    {
        return $this->notificationEvent;
    }

    /**
     * Set owner
     *
     * @param string $owner
     * @return Notifications
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
     * Set isPersistent
     *
     * @param boolean $isPersistent
     * @return Notifications
     */
    public function setIsPersistent($isPersistent)
    {
        $this->isPersistent = $isPersistent;

        return $this;
    }

    /**
     * Get isPersistent
     *
     * @return boolean
     */
    public function getIsPersistent()
    {
        return $this->isPersistent;
    }

    /**
     * Set responseCode
     *
     * @param integer $responseCode
     * @return Notifications
     */
    public function setResponseCode($responseCode)
    {
        $this->responseCode = $responseCode;

        return $this;
    }

    /**
     * Get responseCode
     *
     * @return integer
     */
    public function getResponseCode()
    {
        return $this->responseCode;
    }

    /**
     * Set isSuccess
     *
     * @param boolean $isSuccess
     * @return Notifications
     */
    public function setIsSuccess($isSuccess)
    {
        $this->isSuccess = $isSuccess;

        return $this;
    }

    /**
     * Get isSuccess
     *
     * @return boolean
     */
    public function getIsSuccess()
    {
        return $this->isSuccess;
    }

    /**
     * Set tenantId
     *
     * @param string $tenantId
     * @return Notifications
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
     * Set uuid
     *
     * @param string $uuid
     * @return Notifications
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
     * Set isTerminated
     *
     * @param boolean $isTerminated
     * @return Notifications
     */
    public function setIsTerminated($isTerminated)
    {
        $this->isTerminated = $isTerminated;

        return $this;
    }

    /**
     * Get isTerminated
     *
     * @return boolean
     */
    public function getIsTerminated()
    {
        return $this->isTerminated;
    }
}
